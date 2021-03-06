package com.pagesociety.web.module;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.util.ARRAY;
import com.pagesociety.util.Base64;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.json.JsonDecoder;
import com.pagesociety.web.module.permissions.PermissionsModule;




public abstract class WebModule extends Module
{
	public static final String SLOT_PERMISSIONS_MODULE  = "permissions-module";
	protected PermissionsModule permissions;

	public List<?> EMPTY_LIST = new ArrayList<Object>();

	public void system_init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.system_init(app, config);
		permissions = (PermissionsModule)getSlot(SLOT_PERMISSIONS_MODULE);
		exportPermissions();
	}

	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
	}


	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PERMISSIONS_MODULE, PermissionsModule.class, false,getApplication().getDefaultPermissionsModule());
	}

	protected void exportPermissions()
	{
		//do nothing by default//
	}

	protected void EXPORT_PERMISSION(String permission_id)
	{
		permissions.definePermission(getName(), permission_id);
	}

	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required)
	{
		super.defineSlot(slot_name, slot_type, required);
	}

	protected void DEFINE_SLOT(String slot_name,Class<?> slot_type,boolean required,Object default_val)
	{
		super.defineSlot(slot_name, slot_type, required,default_val);
	}

	public String GET_REQUIRED_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			throw new InitializationException("MISSING REQUIRED CONFIG PARAM..."+name);
		return (String)val;
	}
	public Object GET_REQUIRED_CONFIG_PARAM_OBJ(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			throw new InitializationException("MISSING REQUIRED CONFIG PARAM..."+name);
		return val;
	}

	public String GET_OPTIONAL_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		return (String)val;
	}

	public String GET_OPTIONAL_CONFIG_PARAM(String name,String default_val,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			return default_val;
		return (String)val;
	}

	public int GET_REQUIRED_INT_CONFIG_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			if(val == null)
				throw new InitializationException("MISSING REQUIRED CONFIG PARAM..."+name);
		try
		{
			if(val.getClass() == Integer.class || val.getClass() == int.class)
				return (Integer)val;
			else
				return Integer.parseInt((String)val);
		}catch(NumberFormatException nfe)
		{
			throw new InitializationException("REQUIRED CONFIG PARAM "+name+" SHOULD BE OF TYPE INT.");
		}

	}

	public int GET_OPTIONAL_INT_CONFIG_PARAM(String name,int default_val,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			return default_val;
		return Integer.parseInt((String)val);
	}

	public boolean GET_OPTIONAL_BOOLEAN_CONFIG_PARAM(String name,boolean default_val,Map<String,Object> config) throws InitializationException
	{
		Object val = config.get(name);
		if(val == null)
			return default_val;
		return Boolean.parseBoolean((String)val);
	}


	public void INFO(String message)
	{
		_application.INFO(getName()+": "+message);
	}

	public void WARNING(String message)
	{
		_application.WARNING(getName()+": "+message);
	}

	public void ERROR(String message)
	{
		_application.ERROR(getName()+": "+message);
	}

	public void ERROR(Exception e)
	{
		_application.ERROR(e);
	}

	public void ERROR(String message,Exception e)
	{
		_application.ERROR(getName()+": "+message,e);
	}

	protected void WAE(Exception e) throws WebApplicationException
	{
		throw new WebApplicationException(e.getMessage(),e);
	}

	protected void WAE(String prefix,Exception e) throws WebApplicationException
	{
		throw new WebApplicationException(prefix+" "+e.getMessage(),e);
	}

	protected String GUARD_INSTANCE 		= "instance";
	protected String GUARD_TYPE				= "entity_type";
	protected String GUARD_USER				= "user";
	protected String GUARD_BROWSE_INDEX		= "browse_index";
	protected String GUARD_BROWSE_OP		= "browse_op";
	protected String GUARD_BROWSE_VALUE		= "browse_value";
	public void GUARD(Entity user,String permission_id,Object... flattened_context) throws PermissionsException,PersistenceException
	{
		Map<String,Object> context = new HashMap<String, Object>();
		for(int i = 0;i < flattened_context.length;i+=2)
			context.put((String)flattened_context[i], flattened_context[i+1]);

		boolean b = permissions.checkPermission(user, getName(), permission_id, context);
		if(!b)
			throw new PermissionsException("NO PERMISSION");
	}

	protected static void GUARD(boolean b) throws PermissionsException
	{
		try{
			if(b)
				return;
			else
				throw new PermissionsException("INADEQUATE PERMISSIONS");
		}catch(PermissionsException pe)
		{/* if permissions exception happens in guard just forward it */
			throw pe;
		}

	}

	protected  File GET_MODULE_DATA_DIRECTORY(WebApplication app)
	{
		File f =  new File(app.getConfig().getModuleDataDirectory(),getName()+"Data");
		if(!f.exists())
		{
			INFO("CREATING DATA DIRECTORY FOR MODULE: "+getName()+"\n\t"+f.getAbsolutePath());
			f.mkdirs();
		}
		return f;
	}

	protected  File GET_MODULE_DATA_FILE(WebApplication app,String filename,boolean create) throws WebApplicationException
	{
		File data_dir  =  GET_MODULE_DATA_DIRECTORY(app);
		File data_file = new File(data_dir,filename);
		if(!data_file.exists() && create)
		{
			System.out.println("CREATING DATA FILE FOR MODULE: "+getName()+"\n\t"+data_file.getAbsolutePath());
			CREATE_MODULE_DATA_FILE(app,filename);
		}
		else if(!data_file.exists() && ! create)
		{
			return null;
		}
		return data_file;
	}

	protected  File GET_MODULE_DATA_DIR(WebApplication app,String dirname,boolean create) throws WebApplicationException
	{
		File data_dir  =  GET_MODULE_DATA_DIRECTORY(app);
		File data_file = new File(data_dir,dirname);
		if(!data_file.exists() && create)
		{
			System.out.println("CREATING DATA DIRECTORY FOR MODULE: "+getName()+"\n\t"+data_file.getAbsolutePath());
			boolean success = data_file.mkdirs();
			if(!success)
				throw new WebApplicationException("UNABLE TO CREATE "+data_file);
		}
		else if(!data_file.exists() && ! create)
		{
			return null;
		}
		return data_file;
	}


	protected  FileReader GET_MODULE_DATA_FILE_READER(WebApplication app,String filename,boolean create) throws WebApplicationException
	{
		File data_dir  =  GET_MODULE_DATA_DIRECTORY(app);
		File data_file = new File(data_dir,filename);
		if(!data_file.exists() && create)
		{
			System.out.println("CREATING DATA FILE FOR MODULE: "+getName()+"\n\t"+data_file.getAbsolutePath());
			CREATE_MODULE_DATA_FILE(app,filename);
		}
		else if(!data_file.exists() && ! create)
		{
			return null;
		}
		FileReader ret;
		try{
			ret =  new FileReader(data_file);
		}catch(FileNotFoundException fnfe)
		{
			throw new WebApplicationException("COULD NOT OPEN READER FOR MODULE DATA FILE "+data_file.getAbsolutePath());
		}
		return ret;
	}

	protected File CREATE_MODULE_DATA_FILE(WebApplication app,String filename) throws WebApplicationException
	{
		File data_dir  =  GET_MODULE_DATA_DIRECTORY(app);
		File data_file = new File(data_dir,filename);
		try{
			data_file.createNewFile();
		}catch(IOException ioe)
		{
			throw new WebApplicationException("FAILED CREATING MODULE FILE "+data_file.getAbsolutePath());
		}
		return data_file;
	}


	protected static String GET_CONSOLE_INPUT(String prompt)throws WebApplicationException
	{
		return GET_CONSOLE_INPUT(5, prompt);
	}

	protected static String GET_CONSOLE_INPUT(int num_times,String prompt)throws WebApplicationException
	{
		while(num_times > 0)
		{
			if(prompt != null)
			{
				System.out.print(prompt);
				System.out.flush();
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String input = "";

			try{
				input = in.readLine();
			}catch(IOException ioe)
	        {
	    	   ioe.printStackTrace();
	    	   throw new WebApplicationException("ERROR READING CONSOLE INPUT "+ioe.getMessage());
	        }
			//if(input == null || input.equals(""))
			//	num_times--;
			//else
				return input;
		}
		throw new WebApplicationException("THE APP REQUIRES CONSOLE INPUT TO STARTUP.");
	}

	//string functions//
	protected String REMOVE_WHITE_SPACE(String s)
	{
		StringBuilder buf = new StringBuilder();
		byte[] bb = s.getBytes();
		for(int i=0;i < bb.length;i++)
		{
			char c = (char)bb[i];
			if(Character.isWhitespace(c))
				continue;
			else
				buf.append(c);
		}
		return buf.toString();
	}

	public String STRIP_TO_ALPHA_NUMERIC(String s)
	{
		if(s == null)
			return null;
		StringBuilder buf = new StringBuilder();
		char[] cc = new char[s.length()];
		s.getChars(0, s.length(), cc, 0);
		for(int i = 0;i < cc.length;i++)
		{
			char c = cc[i];
			if(Character.isLetterOrDigit(c))
				buf.append(c);
		}
		return buf.toString();
	}

	protected String[] GET_REQUIRED_LIST_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		String p = GET_REQUIRED_CONFIG_PARAM(name, config);
		p = REMOVE_WHITE_SPACE(p);
		return p.split(",");
	}

	protected String[] GET_OPTIONAL_LIST_PARAM(String name,Map<String,Object> config) throws InitializationException
	{
		String p = GET_OPTIONAL_CONFIG_PARAM(name, config);
		if(p==null)
			return null;
		p = REMOVE_WHITE_SPACE(p);
		return p.split(",");
	}

	protected String[] GET_OPTIONAL_LIST_PARAM(String name,Map<String,Object> config,String... default_value) throws InitializationException
	{
		String p = GET_OPTIONAL_CONFIG_PARAM(name, config);
		if(p==null)
			return default_value;
		p = REMOVE_WHITE_SPACE(p);
		return p.split(",");
	}


	protected void DISPATCH_EVENT(int event_type,Object... event_context) throws WebApplicationException
	{
		dispatchEvent(new ModuleEvent(event_type,event_context));
	}

	protected void DISPATCH_EVENT(int event_type,Map<String,Object> event_context) throws WebApplicationException
	{
		dispatchEvent(new ModuleEvent(event_type,event_context));
	}

	//EXPERIMENTAL currently used by recurring order module//

	File   current_log_file;
	Writer current_log_writer;
	private static final String LOG_EXTENSION = "log";
	private Writer get_current_log_file_writer()
	{
		Calendar now = Calendar.getInstance();

		int n_month = now.get(Calendar.MONTH)+1;
		int n_day   = now.get(Calendar.DATE);
		String year  = String.valueOf(now.get(Calendar.YEAR));
		String month = String.valueOf(n_month);
		String day   = String.valueOf(n_day);

		StringBuilder buf = new StringBuilder();
		buf.append(year);
		if(n_month < 10)
			buf.append('0');
		buf.append(month);
		if(n_day < 10)
			buf.append('0');
		buf.append(day);
		buf.append('.');
		buf.append(getName());
		buf.append('.');
		buf.append(LOG_EXTENSION);
		String current_log_filename = buf.toString();

		try{
			current_log_file = GET_MODULE_DATA_FILE(getApplication(), current_log_filename, false);
			if(current_log_file == null || current_log_writer == null)
			{
				current_log_file = GET_MODULE_DATA_FILE(getApplication(), current_log_filename, true);
				try{
					if(current_log_writer != null)
						current_log_writer.close();
					current_log_writer = new BufferedWriter(new FileWriter(current_log_file,true));
				}catch(IOException ioe)
				{
					ERROR("BIG TIME BARF ON LOG FILE SWITCHING.",ioe);
				}
			}
		}catch(WebApplicationException wae)
		{
			ERROR("PROBLEM GETTING CURRENT LOG FILE "+current_log_filename);
		}
		return current_log_writer;
	}

	protected void MODULE_LOG(String message)
	{
		MODULE_LOG(0, message);
    }

	protected void MODULE_LOG(int indent,String message)
	{
		Writer output = get_current_log_file_writer();
		Date now = new Date();

		try {
			if(message.startsWith("\n"))
			{
				output.write('\n');
				message = message.substring(1);
			}
			output.write(now+": ");
			for(int i = 0;i < indent;i++)
				output.write('\t');
			output.write(message+"\n");
			output.flush();
		}catch(IOException ioe)
		{
			ERROR("BARF ON MODULE_LOG() FUNCTION.MESSAGE WAS "+message,ioe);
		}
    }


	public static OBJECT OBJECT(Object... args)
	{
		return new OBJECT(args);
	}

	public static ARRAY ARRAY(Object... args)
	{
		return new ARRAY(args);
	}

	public static String ENCODE(Serializable o) throws WebApplicationException
	{
		return OBJECT.encode(o);
	}

	public static OBJECT DECODE_OBJECT(String s) throws WebApplicationException
	{
		return OBJECT.decode(s);
	}

	public static ARRAY DECODE_ARRAY(String s) throws WebApplicationException
	{
		if (s==null)
			return null;
        try
		{
			return (ARRAY) Base64.decodeToObject(s);
		} catch (Exception e)
		{
			throw new WebApplicationException("Can't decode",e);
		}
	}




	protected void COPY(File file, File destination_directory,String filename) throws PersistenceException
	{
		FileChannel ic = null;
		FileChannel oc =  null;
		try
		{
			ic = new FileInputStream(file).getChannel();
			oc = new FileOutputStream(new File(destination_directory, (filename!=null)?filename:file.getName())).getChannel();
			ic.transferTo(0, ic.size(), oc);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new PersistenceException("Cant make archive copy!",e);
		}
		finally
		{
			try
			{
				if (ic!=null)
					ic.close();
				if (oc!=null)
					oc.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new PersistenceException("Cant make archive copy!",e);
			}
		}

	}

    protected void COPY_DIR(File sourceLocation , File targetLocation)throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                COPY_DIR(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public static String READ_FILE_AS_STRING(String filename) throws WebApplicationException
    {
    	File f = new File(filename);
    	return READ_FILE_AS_STRING(f);
    }

    public static String READ_FILE_AS_STRING(File f) throws WebApplicationException
    {
        byte[] buffer = new byte[(int)f.length()];
        BufferedInputStream ff;
		try {
			ff = new BufferedInputStream(new FileInputStream(f));
			ff.read(buffer);
		} catch (Exception e) {
			throw new WebApplicationException("PROBLEM READING FILE "+f.getAbsolutePath()+" :"+e.getMessage());
		}

        return new String(buffer);
    }


    ///UTIL STUFF//

    public static String PREPARE_REQUIRED_USER_INPUT(String fieldname ,String s ) throws WebApplicationException
    {
    	if(s == null )
    		throw new WebApplicationException(fieldname+" is required.");
    	s = s.trim();
    	if("".equals(s))
    		throw new WebApplicationException(fieldname+" is required.");
    	return s;
    }


    public static Map<String,Object> KEY_VALUE_PAIRS_TO_MAP(Object... kvp)
    {


    	Map<String,Object> map = new HashMap<String,Object>();
    	kvp_helper(map,kvp);


    	return map;
    }

    private static void kvp_helper(Map<String,Object> map,Object[] kvp)
    {
    	for(int i = 0;i < kvp.length;)
    	{
    		if(kvp[i].getClass() == Object[].class)
			{
				kvp_helper(map,(Object[]) kvp[i]);
				i++;
			}
    		else
    		{
    			map.put((String)kvp[i],kvp[i+1]);
    			i+=2;
    		}
    	}
    }

    public static Object[] MAP_TO_KEY_VALUE_PAIRS(Map<String,Object> map)
    {
    	int size = 	map.entrySet().size();
    	Object[] ret = new Object[size*2];
    	Iterator<String> it = map.keySet().iterator();
    	int i = 0;
    	while(it.hasNext())
    	{
    		String key = it.next();
    		Object val = map.get(key);
    		ret[i++] = key;
    		ret[i++] = val;
    	}
    	return ret;
    }


    public static Object[] JOIN_KVP(Object[] kvp,Object... kvp2)
    {
    	Object[] ret = new Object[kvp.length + kvp2.length];
    	System.arraycopy(kvp, 0, ret, 0, kvp.length);
    	System.arraycopy(kvp2, 0, ret, kvp.length, kvp2.length);
    	return ret;
    }


    public static <T> T[] CONCAT(T[] first, T[]... rest) {
    	  int totalLength = first.length;
    	  for (T[] array : rest) {
    	    totalLength += array.length;
    	  }
    	  T[] result = Arrays.copyOf(first, totalLength);
    	  int offset = first.length;
    	  for (T[] array : rest) {
    	    System.arraycopy(array, 0, result, offset, array.length);
    	    offset += array.length;
    	  }
    	  return result;
    	}


    public Map<String,Object> JSON_CONFIG_TO_MAP(File f) throws InitializationException
	{

		Map<String,Object> map = null;
		try{
			String contents = READ_FILE_AS_STRING(f.getAbsolutePath());
			return JsonDecoder.decodeAsMap(contents);
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("PROBLEM READING CONFIG FILE: "+f.getAbsolutePath());
		}
	}




	 public static String GET_STACK_TRACE(Throwable aThrowable) {
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    aThrowable.printStackTrace(printWriter);
		    return result.toString();
		  }


	 ///MATCHER STUFF, regexp to Object//
		@SuppressWarnings("serial")
		protected class PS_MATCHER_LIST extends ArrayList<PSMatcher>
		{
			public PS_MATCHER_LIST()
			{
				super();
			}

			public PS_MATCHER_LIST(Map<String,Object> match_map,boolean case_insenstive)
			{
				super();
				for(String key: match_map.keySet())
				{
					add(new PSMatcher(key, match_map.get(key), case_insenstive));
				}
			}

			public PSMatcher add(String regexp,Object ret,boolean case_insensitive)
			{
				PSMatcher m = new PSMatcher(regexp, ret, case_insensitive);
				add(m);
				return m;
			}

			public Object getFirstMatch(String in)
			{
				int s= this.size();
				for(int i = 0;i < s;i++)
				{
					PSMatcher m = get(i);
					Object o = null;
					if((o = m.getMatch(in)) != null)
						return o;
				}
				return null;
			}

			public List<Object> getAllMatches(String in)
			{
				List<Object> ret = new ArrayList<Object>();
				int s= this.size();
				for(int i = 0;i < s;i++)
				{
					PSMatcher m = get(i);
					Object o = null;
					if((o = m.matches(in)) != null)
						ret.add(o);
				}
				return ret;
			}
		}


		class PSMatcher
		{
			private Object ret;
			private Pattern p;
			public PSMatcher(String regexp,Object ret,boolean case_insensitive)
			{
				if(case_insensitive)
					this.p = Pattern.compile(regexp,Pattern.CASE_INSENSITIVE);
				else
					this.p = Pattern.compile(regexp);
				this.ret = ret;
			}

			public boolean matches(String in)
			{
				return p.matcher(in).matches();
			}

			public Object getMatch(String in)
			{
				if(matches(in))
					return ret;
				return null;
			}

			public String toString()
			{
				return p.toString()+" -> "+ret.toString();
			}
		}

	 ////END MATCHER STUFF//

		protected Method LOOKUP_METHOD(Object o,String methodname,Class... params)
		{
				try {
			            //
			            // We can also get method by their name and parameter types, here we
			            // are tryinh to get the add(int, int) method.
			            //
						Class clazz = o.getClass();
			            Method method = clazz.getDeclaredMethod(methodname, params);
			            method.setAccessible(true);
			           // System.out.println("Method name: " + method.getName());
			            return method;
					} catch (NoSuchMethodException e) {
			            //e.printStackTrace();
			            return null;
			        }
		}


		protected Process EXEC(String cmd,String... args) throws WebApplicationException
		{

			Runtime rt = Runtime.getRuntime();
			String[] cmd_array = new String[1+args.length];
			cmd_array[0] = cmd;
			for(int i = 0;i < args.length;i++)
			{
				cmd_array[i+1] = args[i];
			}
			try{
				Process pr = rt.exec(cmd_array);
				return pr;
			}catch(IOException e)
			{
				StringBuilder buf = new StringBuilder();
				for(int i = 0;i < cmd_array.length;i++)
				{
					buf.append(cmd_array[i]+" ");
				}
				throw new WebApplicationException("FAILED EXECUTING CMD "+buf.toString());
			}

		}

		public static boolean DELETE_DIR(File dir)
		{
			if (dir.isDirectory())
			{
				String[] children = dir.list();
					for (int i=0; i<children.length; i++)
					{
						boolean success = DELETE_DIR(new File(dir, children[i]));
						if (!success)
						{
							return false;
						}
					}
			}
			// The directory is now empty so delete it
			return dir.delete();
		}


		public static final void ZIP_DIRECTORY( File directory, File zip ) throws IOException {
		    ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zip ) );
		    zip( directory, directory, zos );
		      zos.close();
		  }

		  private static final void zip(File directory, File base,
		      ZipOutputStream zos) throws IOException {
		    File[] files = directory.listFiles();
		    byte[] buffer = new byte[8192];
		    int read = 0;
		    for (int i = 0, n = files.length; i < n; i++) {
		      if (files[i].isDirectory()) {
		    	  zip(files[i], base, zos);
		      } else {
		        FileInputStream in = new FileInputStream(files[i]);
		        ZipEntry entry = new ZipEntry(files[i].getPath().substring(
		            base.getPath().length() + 1));
		        zos.putNextEntry(entry);
		        while (-1 != (read = in.read(buffer))) {
		          zos.write(buffer, 0, read);
		        }
		        in.close();
		      }
		    }
		  }

		public static final void UNZIP(File zip, File extractTo) throws IOException {
		    ZipFile archive = new ZipFile(zip);
		    Enumeration e = archive.entries();
		    while (e.hasMoreElements()) {
		      ZipEntry entry = (ZipEntry) e.nextElement();
		      File file = new File(extractTo, entry.getName());
		      if (entry.isDirectory() && !file.exists()) {
		        file.mkdirs();
		      } else {
		        if (!file.getParentFile().exists()) {
		          file.getParentFile().mkdirs();
		        }

		        InputStream in = archive.getInputStream(entry);
		        BufferedOutputStream out = new BufferedOutputStream(
		            new FileOutputStream(file));

		        byte[] buffer = new byte[8192];
		        int read;

		        while (-1 != (read = in.read(buffer))) {
		          out.write(buffer, 0, read);
		        }

		        in.close();
		        out.close();
		      }
		    }
		  }


		public static final int RANDOM(int min,int max)
		{
			return min + (int)(Math.random() * ((max - min) + 1));
		}

}
