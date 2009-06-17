package com.pagesociety.web.module.dump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionsModule;
import com.pagesociety.web.module.user.UserModule;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.MultipartFormException;

public class ExcelDumpModule extends WebStoreModule
{
	private static final String NULL_VALUE = "NULL";

	private int _page_size = 100;
	
	private SimpleDateFormat spreadsheet_date_formatter;

	private static final char LIST_SEP_CHAR = (char)','; //GROUP SEPERATOR CHAR//
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		verifyLibraryDependencies();
		spreadsheet_date_formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss Z");

	}
	
	protected void verifyLibraryDependencies() throws InitializationException
	{
		try{
			Class.forName("org.apache.poi.hssf.usermodel.HSSFWorkbook");
		}catch(Exception e)
		{
			throw new InitializationException(getName()+" SEEMS TO BE MISSING THE apache.poi LIBRARY. IS IT PART OF YOUR PROJECT?");
		}		
	}
	
	public void dumpDbToExcelFormat(OutputStream out) throws IOException,
			PersistenceException
	{
		store.checkpoint();
		
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet;
		HSSFRow row;
		HSSFCell cell;
		QueryResult results = null;
		Entity entity = null;
		int offset;
		List<EntityDefinition> defs;
		try
		{
			defs = store.getEntityDefinitions();
		}
		catch (PersistenceException e)
		{
			e.printStackTrace();
			throw e;
		}
		for (int i = 0; i < defs.size(); i++)
		{
			EntityDefinition def = defs.get(i);
			List<String> field_names = def.getFieldNames();
			field_names.remove(Entity.ID_ATTRIBUTE);
			// create the worksheet for each def
			sheet = wb.createSheet(def.getName());
			// use row 0 to add the def name and the number of fields
			row = sheet.createRow(0);
			row.createCell(0).setCellValue(new HSSFRichTextString(def.getName()));
			row.createCell(1).setCellValue(field_names.size() + 1);
			// add the field names to row 1
			row = sheet.createRow(1);
			row.createCell(0).setCellValue(new HSSFRichTextString(Entity.ID_ATTRIBUTE));
			for (int j = 0; j < field_names.size(); j++)
			{
				cell = row.createCell(j + 1);
				cell.setCellValue(new HSSFRichTextString(field_names.get(j)));
			}
			// add the results
			offset = 0;
			while (results == null || results.size() == _page_size)
			{
				Query q = get_query(def.getName(), offset, _page_size);
				try
				{
					results = store.executeQuery(q);
				}
				catch (PersistenceException e)
				{
					e.printStackTrace();
					throw e;
				}
				for (int r = 0; r < results.size(); r++)
				{
					entity = results.getEntities().get(r);
					row = sheet.createRow(offset + r + 2);
					row.createCell(0).setCellValue(new HSSFRichTextString(Long.toString(entity.getId())));
					for (int j = 0; j < field_names.size(); j++)
					{
						String fname = field_names.get(j);
						Object o = entity.getAttribute(fname);
						FieldDefinition f = def.getField(fname);
						cell = row.createCell(j + 1);
						cell.setCellValue(new HSSFRichTextString(get_value(f, o)));
					}
				}
				offset += _page_size;
			}
			results = null;
		}
		W w = new W(out);
		wb.write(w);
		//System.out.println("!!! WROTE "+w.getSize()+" BYTES");
		out.close();
	}

	class W extends OutputStream
	{
		private OutputStream o;
		int c = 0;
		public W(OutputStream s)
		{
			o = s;
		}

		public void write(int b) throws IOException 
		{
			o.write(b);
			c++;
		}
		
		public int getSize()
		{
			return c;
		}
	}

	
	public void restoreDbFromExcelFile(File excel_file) throws IOException,
			PersistenceException
	{
		InputStream myxls = new FileInputStream(excel_file);
		HSSFWorkbook wb = new HSSFWorkbook(myxls);
		HSSFSheet sheet;
		HSSFRow row;
		HSSFCell cell;
		Entity entity;
		EntityDefinition def;
		String[] field_names;
		String entity_type;
		List<Entity> entities;
		int s = wb.getNumberOfSheets();
		for (int i = 0; i < s; i++)
		{
			// collect rows here
			entities = new ArrayList<Entity>();
			// get the worksheet
			sheet = (wb.getSheetAt(i));
			row = sheet.getRow(0);
			cell = row.getCell(0);
			// get the entity type & number of fields from the first row
			entity_type = cell.getRichStringCellValue().getString();
			
			int col_width = (int) (row.getCell(1).getNumericCellValue());
			try
			{
				def = store.getEntityDefinition(entity_type);
			}
			catch (PersistenceException e)
			{
				e.printStackTrace();
				throw e;
			}
			//
			field_names = new String[col_width];
			row = sheet.getRow(1);
			for (int j = 0; j < col_width; j++)
			{
				cell = row.getCell(j);
				HSSFRichTextString sr = cell.getRichStringCellValue();
				field_names[j] = sr.getString();
			}
			int row_height = sheet.getLastRowNum();
			for (int r = 0; r < row_height - 1; r++)
			{
				row = sheet.getRow(r + 2);
				entity = new Entity();
				entity.setType(entity_type);
				for (int j = 0; j < col_width; j++)
				{
					cell = row.getCell(j);
					if (j == 0)
						entity.setId(parse_id(cell));
					else
					{
						FieldDefinition f = def.getField(field_names[j]);
						if(cell == null)
						{
							//this is a work around for open office
							//encoding cells with the empty string in them
							//as null cells!
							if(f.getBaseType() == Types.TYPE_STRING || f.getBaseType() == Types.TYPE_TEXT)
							{
								entity.setAttribute(field_names[j],"");
								continue;
							}
							INFO("BULK EXCEL IMPORTING ENTITY TYPE COL ERROR "+entity_type);
							INFO("J IS "+j+" AND COL WIDTH IS "+col_width);
							INFO(" THIS SHOULD NOT BE HAPPENING");
							continue;
						}
						
						entity.setAttribute(field_names[j], parse_field_value(f, cell));
					}
				}
				entities.add(entity);
			}
			try
			{
				store.truncate(entity_type, false);
				store.insertEntities(entities);
			}
			catch (PersistenceException e)
			{
				e.printStackTrace();
				throw e;
			}
		}
	}

	private Query get_query(String type, int offset, int page_size)
	{
		Query q = new Query(type);
		q.idx(Query.PRIMARY_IDX);
		q.gt(0);
		q.offset(offset);
		q.pageSize(page_size);
		return q;
	}

	// utils for building
	private String get_value(FieldDefinition f, Object o)
	{
		if (o == null)
			return NULL_VALUE;
		if (f.isArray())
		{
			StringBuilder b = new StringBuilder();
			List<?> os = (List<?>) o;
			b.append("[");
			if(f.getBaseType() == Types.TYPE_STRING || f.getBaseType() == Types.TYPE_TEXT)
				b.append(encode_string_list_members((List<String>)o));
			else
			{
				for (int i = 0; i < os.size(); i++)
				{
					b.append(get_simple_value(f, os.get(i)));
					if(i != os.size()-1)
						b.append((char)LIST_SEP_CHAR);
				}
			}

			b.append("]");
			return b.toString();
		}
		else
		{
			return get_simple_value(f, o);
		}
	}

	private String get_simple_value(FieldDefinition f, Object o)
	{
		if (o == null)
			return NULL_VALUE;
		switch (f.getBaseType())
		{
		case Types.TYPE_BOOLEAN:
			return Boolean.toString((Boolean) o);
		case Types.TYPE_LONG:
		case Types.TYPE_INT:
		case Types.TYPE_DOUBLE:
		case Types.TYPE_FLOAT:
			return o.toString();
		case Types.TYPE_STRING:
		case Types.TYPE_TEXT:
			String s =  o.toString();
			//if(s.equals(""))//this is a workaround. open office makes cells with just the empty string 'null' and it throws us into a funk.
			//	s = NULL_VALUE;
			return s;
		case Types.TYPE_DATE:
			return (spreadsheet_date_formatter.format((Date) o));
		case Types.TYPE_BLOB:
			return o.toString();
		case Types.TYPE_REFERENCE:
			Entity e = (Entity) o;
			return e.getType() + ":" + e.getId();
		default:
			return o.toString();
		}
	}

	
	// utils for parsing
	private Object parse_field_value(FieldDefinition f, HSSFCell cell)
	{

		String s = cell.getRichStringCellValue().getString();
		if (s.equals(NULL_VALUE))
			return null;
		if (f.isArray())
		{
			/* strip braces */
			char[] cc = s.toCharArray();
			char[] ccc = new char[cc.length-2];
			System.arraycopy(cc, 1, ccc, 0, cc.length-2);
			s = new String(ccc);

			
			if(f.getBaseType() == Types.TYPE_STRING || f.getBaseType() == Types.TYPE_TEXT)
			{
				return decode_string_list_members(s);
			}
			else
			{
				List<Object> os = new ArrayList<Object>();
				StringTokenizer st = new StringTokenizer(s,String.valueOf(LIST_SEP_CHAR));
				while(st.hasMoreTokens())
					os.add(parse_simple_value(f,st.nextToken()));
				return os;
			}
		}
		else
		{
			return parse_simple_value(f, s);
		}
	}

	private Object parse_simple_value(FieldDefinition f, String s)
	{
		if (s.equalsIgnoreCase(NULL_VALUE))
			return null;
		switch (f.getBaseType())
		{
		case Types.TYPE_BOOLEAN:
			return Boolean.parseBoolean(s);
		case Types.TYPE_LONG:
			return Long.parseLong(s);
		case Types.TYPE_INT:
			return Integer.parseInt(s);
		case Types.TYPE_DOUBLE:
			return Double.parseDouble(s);
		case Types.TYPE_FLOAT:
			return Float.parseFloat(s);
		case Types.TYPE_STRING:
		case Types.TYPE_TEXT:
			return s;
		case Types.TYPE_DATE:
			try
			{
				return spreadsheet_date_formatter.parse(s);
			}
			catch (ParseException e1)
			{
				e1.printStackTrace();
				return null;
			}
		case Types.TYPE_BLOB:
			return Byte.parseByte(s);
		case Types.TYPE_REFERENCE:
			Entity e = new Entity();
			String[] type_id = s.split(":");
			e.setType(type_id[0]);
			e.setId(Long.parseLong(type_id[1]));
			return e;
		default:
			return s;
		}
	}

	private String encode_string_list_members(List<String> ss)
	{
		StringBuilder buf = new StringBuilder();
		for(int i = 0;i < ss.size();i++)
		{
			if(ss.get(i) == null)
				buf.append(NULL_VALUE);
			else
				buf.append(encode_string_escape_list_delimiter(ss.get(i)));
			
			if(i!=ss.size()-1)
				buf.append(LIST_SEP_CHAR);
		}
		return buf.toString();
	}
	
	private List<String> decode_string_list_members(String ss)
	{
		List<String> list = new ArrayList<String>();
		char[] cc = ss.toCharArray();
		StringBuilder buf = new StringBuilder();
		for(int i = 0;i < cc.length;i++)
		{
			char c = cc[i];
			if(i == cc.length-1)
			{
				buf.append(c);
				list.add(buf.toString().trim());
				return list;
			}
			if(c == '\\')
			{

				if(cc[i+1] == LIST_SEP_CHAR)
				{
					buf.append(LIST_SEP_CHAR);
					i++;
					continue;
				}

			}
			else if(c == LIST_SEP_CHAR)
			{
				list.add(buf.toString().trim());
				buf.setLength(0);
				continue;
			}				
			
			buf.append(c);
						
		}
		return list;	
	}
	
	private String encode_string_escape_list_delimiter(String s)
	{
		char[] cc = s.toCharArray();
		StringBuilder buf = new StringBuilder(cc.length+3);
		for(int i = 0;i < cc.length;i++)
		{
			char c = cc[i];
			if(c == LIST_SEP_CHAR)
			{
				buf.append('\\');
				buf.append(LIST_SEP_CHAR);
			}
			else
				buf.append(c);
		}
		return buf.toString();
	}


	private long parse_id(HSSFCell cell)
	{
		return Long.parseLong(cell.getRichStringCellValue().getString());
	}



	public static void main(String[] a) throws IOException, PersistenceException
	{
		String test_path = "C:/Users/david/Desktop/workbook.xls";
		ExcelDumpModule m = new ExcelDumpModule();
		m.dumpDbToExcelFormat(new FileOutputStream( test_path));
		m.restoreDbFromExcelFile(new File(test_path));
	}
}
