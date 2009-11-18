package com.pagesociety.web.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;


import com.pagesociety.util.Text;

public class MultipartForm
{
	private String name;
	private Map<String, List<String>> form_parameters;
	private long max_upload_item_size;
	private UploadProgressInfo upload_progress_info;
	private Object STATE_LOCK = new Object();
	private ServletFileUpload s_upload;
	//
	private WeakReference<HttpServletRequest> request;
	//
	public static final int INIT 		= 0;
	public static final int PARSING 	= 1;
	public static final int COMPLETE 	= 2;
	public static final int CANCELLED 	= 3;
	public static final int ERROR 		= 4;
	private int state;


	String upload_content_type;
	long   upload_file_size;
	String upload_file_name;
	InputStream upload_input_stream;
	
	private static String upload_tmp_dir;
	
	public MultipartForm(HttpServletRequest request) throws MultipartFormException
	{
		this.request 			= new WeakReference<HttpServletRequest>(request);
		this.name 	 			= request.getRequestURI();
		max_upload_item_size 	= 0L;
		form_parameters 		= new LinkedHashMap<String, List<String>>();
		upload_progress_info 	= new UploadProgressInfo();
		s_upload				= new ServletFileUpload();
		parse_query_string();
		setup_upload();
		setState(INIT);
		
	}

	public String getName()
	{
		return name;
	}

	public void parse(OutputStream... os) throws MultipartFormException
	{
		if (this.request == null)
			throw new MultipartFormException("UPLOAD " + name + " WAS NOT CONSTRUCTED WITH A REQUEST");
		parse_multipart_form(os);
		this.request = null;
	}


	private void parse_query_string()
	{
		HttpServletRequest l_request = request.get();
		if (l_request == null)
			return;
		Enumeration<String> e = l_request.getParameterNames();
		while (e.hasMoreElements())
		{
			String k = e.nextElement();
			String[] v = l_request.getParameterValues(k);
			form_parameters.put(k, Arrays.asList(v));
		}
		//System.out.println("FORM PARAMETERS: "+form_parameters);
	}
	
	//NOTE: WHEN YOU ARE SUBMITTING A MULTIPART FORM ALWAYS PUT YOUR FILE FIELD LAST IN THE FORM DATA//
	//flash does it like this....
	//http://livedocs.adobe.com/flash/9.0/ActionScriptLangRefV3/flash/net/FileReference.html
	//we ignore the Submit form parameter
	private void setup_upload() throws MultipartFormException
	{

		try{
			HttpServletRequest l_request = request.get();
			if(form_parameters.get("size") != null)
			{
				//System.out.println("SIZE PARAMTER IS "+form_parameters.get("size"));
				upload_file_size = Long.parseLong(form_parameters.get("size").get(0));
			
			}else
			{
				System.out.println("SIZE PARAMTER IS NOT SET");
				upload_file_size   = Long.parseLong(l_request.getHeader(MultipartFormConstants.CONTENT_LENGTH));
			}
			upload_progress_info.setFileSize(upload_file_size);
			upload_progress_info.setBytesRead(0);
			if(max_upload_item_size != 0)
				s_upload.setFileSizeMax(max_upload_item_size);
			
			FileItemIterator iter = s_upload.getItemIterator(l_request);

			while (iter.hasNext())
			{
				FileItemStream item   = iter.next();
			    String 		   name	  = item.getFieldName();
			    InputStream    stream = item.openStream();
				if(item.isFormField())
				{
					
					List<String> list = form_parameters.get(name);
					if (list == null)
					{
						list = new ArrayList<String>();
						form_parameters.put(name, list);
					}
					list.add(Streams.asString(stream));
					stream.close();
				}
				else
				{
					FileItemHeaders headers = item.getHeaders();
					upload_file_name = item.getName();					
					upload_input_stream = stream;
					upload_content_type = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(upload_file_name);
					break;
				}
			}
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new MultipartFormException("PROBLEM SETTING UPLOAD FILENAME "+e.getMessage());
		}
	}
	
	private void parse_multipart_form(OutputStream... os) throws MultipartFormException
	{
		HttpServletRequest l_request = request.get();
		if (l_request == null)
			throw new MultipartFormException("UPLOAD " + name + " WAS NOT CONSTRUCTED WITH A REQUEST OR REQUEST HAS BEEN GARBAGE COLLECTED");
		if(!ServletFileUpload.isMultipartContent(l_request))
			throw new MultipartFormException("YOU ARE TRYING TO POST NON MULTIPART DATA");
		if(upload_input_stream == null)
			throw new MultipartFormException("NO UPLOAD FILE WAS FOUND IN THIS FORM");
		try
		{
			setState(PARSING);
			byte[] buffer = new byte[16384];
			int l;
			int tot = 0;
			try{
		        while ((l = upload_input_stream.read(buffer)) != -1)
		        {
		        	for(int i = 0;i < os.length;i++)
		        	{
		        		os[i].write(buffer, 0, l);
		        		os[i].flush();
		        	}
		        	tot += l;
		          
		            double progress = (tot/(double)upload_file_size)*100.0;
		            //System.out.println("PROGRESS "+progress+" WROTE "+tot+" BYTES");
		            upload_progress_info.setBytesRead(tot);
		            upload_progress_info.setProgress(progress);

		            if(MultipartFormConstants.TESTING)
		            {
		            	try{
		            		Thread.sleep(20);
		            	}catch(InterruptedException ie){}
		            }
		        }
			} finally 
			{
				upload_input_stream.close();
				for(int i = 0;i < os.length;i++)
					os[i].close();
			}
			upload_progress_info.setProgress(100);
			setState(COMPLETE);
		}
		catch (Exception ne)
		{

			/* when we cancel it is going */
			/* to cause an exception      */
			if(!isCancelled())
			{
				setState(ERROR);
				throw new MultipartFormException("UPLOAD ERROR " + ne.getMessage(), ne);
			}
		}
	}
	


	public UploadProgressInfo getUploadProgressInfo()
	{
		return upload_progress_info;
	}
	
	public String getFileName()
	{
		return upload_file_name;
	}
	
	public long getFileSize()
	{
		return upload_file_size;
	}
	
	public String getContentType()
	{
		return upload_content_type;
	}
	

	
	//0 means unlimited upload size
	public void setMaxUploadItemSize(long size)
	{
		this.max_upload_item_size = size;
	}


	public boolean isComplete()
	{
		synchronized (STATE_LOCK) 
		{
			return state == COMPLETE;
		}
	}
	
	public void setState(int state)
	{
		synchronized (STATE_LOCK)
		{
			this.state = state;
		}
	}

	public boolean isError()
	{
		synchronized (STATE_LOCK) 
		{
			return state == ERROR;			
		}
	}

	public boolean isInProgress()
	{
		synchronized (STATE_LOCK) 
		{
			return state == PARSING || state == INIT;
		}
	}

	public boolean isCancelled()
	{
		synchronized (STATE_LOCK) 
		{
			return state == CANCELLED;
		}
	}
	
	public void cancel() throws IOException
	{
		if (this.request==null)
			return;
		
		HttpServletRequest request = this.request.get();
		if(request == null)
			return;

		setState(CANCELLED);
		request.getInputStream().close();
		request = null;	
	}

//////////////////STUFF TO ACCESS FORM PARAMETERS/////////////////////////
	public String getParameter(String name)
	{
		return form_parameters.get(name).get(0);
	}

	public String getStringParameter(String name)
	{
		if (form_parameters.get(name) == null || form_parameters.get(name).isEmpty())
			return null;
		return form_parameters.get(name).get(0);
	}

	public List<String> getStringArrayParameter(String name)
	{
		return form_parameters.get(name);
	}

	public int getIntParameter(String name)
	{
		return Text.toInt(form_parameters.get(name).get(0));
	}

	public List<Integer> getIntArrayParameter(String name)
	{
		if (form_parameters.get(name) == null)
			return null;
		List<Integer> ints = new ArrayList<Integer>();
		int s = form_parameters.get(name).size();
		for (int i = 0; i < s; i++)
		{
			ints.add(Text.toInt(form_parameters.get(name).get(i)));
		}
		return ints;
	}
	
	public long getLongParameter(String name)
	{
		return Text.toLong(form_parameters.get(name).get(0));
	}

	public List<Long> getLongArrayParameter(String name)
	{
		if (form_parameters.get(name) == null)
			return null;
		List<Long> longs = new ArrayList<Long>();
		int s = form_parameters.get(name).size();
		for (int i = 0; i < s; i++)
		{
			longs.add(Text.toLong(form_parameters.get(name).get(i)));
		}
		return longs;
	}

	public float getFloatParameter(String name)
	{
		return Text.toFloat(form_parameters.get(name).get(0));
	}
	
	public List<Float> getFloatArrayParameter(String name)
	{
		if (form_parameters.get(name) == null)
			return null;
		List<Float> floats = new ArrayList<Float>();
		int s = form_parameters.get(name).size();
		for (int i = 0; i < s; i++)
		{
			floats.add(Text.toFloat(form_parameters.get(name).get(i)));
		}
		return floats;
	}

	public float getDateParameter(String name)
	{
		throw new RuntimeException("UNIMPLEMENTED MultipartForm.getDate");
	}

	public Map<String, List<String>> getParameterMap()
	{
		return form_parameters;
	}

	public List<String> getParameterNames()
	{
		return new ArrayList<String>(form_parameters.keySet());
	}
	
	

}