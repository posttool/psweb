package com.pagesociety.web.upload;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.pagesociety.util.Text;

public class MultipartForm
{
	private String name;
	// uses duplicate key names to indicate an array
	// TODO ... inconsistent w/ json & GET way of doing keys, which are order dependent,
	// not a hash... @see ServletGateway
	private Map<String, List<String>> form_parameters;
	// no arrays of files per namespace... name the form input items uniquely
	private Map<String, File> file_parameters;
	// this will get them all, but not by name
	private ArrayList<File> files;
	// defaults to java.io.tmpdir
	protected File upload_directory;
	//max size of any one file in the upload
	private long max_upload_item_size;
	// a listener object that could get called back (optional)
	private MultipartFormListener listener;
	// the upload observers, by file name
	private Map<String, UploadProgressInfo> progress;
	private List<UploadProgressInfo> 		progress_list;
	//
	private WeakReference<HttpServletRequest> request;
	//
	public static final int INIT 		= 0;
	public static final int PARSING 	= 1;
	public static final int COMPLETE 	= 2;
	public static final int CANCELLED 	= 3;
	public static final int ERROR 		= 4;
	private int state;


	public MultipartForm(HttpServletRequest request)
	{
		this();
		this.request = new WeakReference<HttpServletRequest>(request);
		parseQueryString();
	}

	
	private MultipartForm()
	{
		state = INIT;
		progress 			= new HashMap<String, UploadProgressInfo>();
		progress_list 		= new ArrayList<UploadProgressInfo>();
		upload_directory 	= new File(System.getProperty("java.io.tmpdir"));
		form_parameters 	= new HashMap<String, List<String>>();
		file_parameters 	= new HashMap<String, File>();
		files 				= new ArrayList<File>();
	}


	public String getName()
	{
		return name;
	}

	public void parse() throws MultipartFormException
	{
		if (this.request == null)
			throw new MultipartFormException("UPLOAD " + name + " WAS NOT CONSTRUCTED WITH A REQUEST");
		//
		parseQueryString();
		parseMultipartForm();
		this.request = null;
	}

	public void parseQueryString() 
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
	}
	
	@SuppressWarnings("unchecked")
	private void parseMultipartForm() throws MultipartFormException
	{
		HttpServletRequest l_request = request.get();
		if (l_request == null)
			throw new MultipartFormException("UPLOAD " + name + " WAS NOT CONSTRUCTED WITH A REQUEST OR REQUEST HAS BEEN GARBAGE COLLECTED");
		long contentLength = Long.parseLong(l_request.getHeader(MultipartFormConstants.CONTENT_LENTH));
		try
		{
			ServletFileUpload upload = new ServletFileUpload();
			upload.setFileItemFactory(new UploadItemProgressFactory(this, contentLength));
			if(max_upload_item_size != 0)
				upload.setFileSizeMax(max_upload_item_size);
			
			state = PARSING; // TODO this state change does not ensure the existence of ProgressInfo objects
			List<?> fileItems = upload.parseRequest(l_request);
			for (int i = 0; i < fileItems.size(); i++)
			{
				FileItem item = (FileItem) fileItems.get(i);
				String nn = item.getFieldName();
				if (item.isFormField())
				{
					String n = item.getFieldName();
					List<String> list = form_parameters.get(n);
					if (list == null)
					{
						list = new ArrayList<String>();
						form_parameters.put(n, list);
					}
					list.add(item.getString());
				}
				else
				{
					File f = new File(upload_directory, item.getName());
					item.write(f);
					file_parameters.put(item.getFieldName(), f);
					files.add(f);
					if (listener != null)
						listener.onFileComplete(this, f);
				}
			}

			for (int i=0; i < this.progress_list.size(); i++)
				this.progress_list.get(i).progress = 100;
			
			this.request = null;
			if (listener != null)
				listener.onUploadComplete(this);
			state = COMPLETE;
		}
		catch (Exception ne)
		{
			/* when we cancel it is going */
			/* to cause an exception      */
			if(state != CANCELLED)
				state = ERROR;
			
			this.request = null;
			if (listener != null)
				listener.onUploadException(this, ne);
			throw new MultipartFormException("UPLOAD ERROR " + ne.getMessage(), ne);
		}
	}

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

	public List<String> getFileNames()
	{
		return new ArrayList<String>(file_parameters.keySet());
	}

	public File getFile(String name)
	{
		return file_parameters.get(name);
	}

	public List<File> getFiles()
	{
		return files;
	}

	public File getFile(int i)
	{
		return files.get(i);
	}

	public Map<String, List<String>> getParameterMap()
	{
		return form_parameters;
	}

	public List<String> getParameterNames()
	{
		return new ArrayList<String>(form_parameters.keySet());
	}

	public void setUploadDirectory(File uploadDir)
	{
		this.upload_directory = uploadDir;
	}
	
	//0 means unlimited upload size
	public void setMaxUploadItemSize(long size)
	{
		this.max_upload_item_size = size;
	}

	public void setListener(MultipartFormListener listener)
	{
		this.listener = listener;
	}

	public void addUploadProgress(UploadProgressInfo observer)
	{
		this.progress.put(observer.fileName, observer);
		this.progress_list.add(observer);
	}

	public Map<String, UploadProgressInfo> getUploadProgressMap()
	{
		return progress;
	}

	public List<UploadProgressInfo> getUploadProgress()
	{
		return progress_list;
	}

	public boolean isComplete()
	{/* this upload is complete when all of the files have been parsed */
//	THIS IS UGLY
// TODO help!
		boolean pis_are_complete = true;
		int s = progress_list.size();
		for(int i = 0;i <s;i++)
		{
			UploadProgressInfo p = progress_list.get(i);
			if(!p.isComplete())
			{
				pis_are_complete = false;
				break;
			}
		}
		return state == COMPLETE && pis_are_complete;
	}

	public boolean isError()
	{
		return state == ERROR;
	}

	public boolean isInProgress()
	{
		return state == PARSING;
	}

	public boolean isCancelled()
	{
		return state == CANCELLED;
	}
	
	public void cancel() throws IOException
	{
		HttpServletRequest request = this.request.get();
		if(request == null)
			return;

		request.getInputStream().close();
		state = CANCELLED;
		request = null;	
	}
}