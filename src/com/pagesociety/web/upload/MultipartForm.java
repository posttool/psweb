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
	// a listener object that could get called back (optional)
	private MultipartFormListener listener;
	// the upload observers, by file name
	private Map<String, UploadProgressInfo> progress;
	private List<UploadProgressInfo> progress_list;
	//
	private WeakReference<HttpServletRequest> request;
	//
	public static final int INIT 		= 0;
	public static final int PARSING 	= 1;
	public static final int COMPLETE 	= 2;
	public static final int CANCELLED 	= 3;
	public static final int ERROR 		= 4;
	private int state;

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

	public MultipartForm(String channelName)
	{
		this();
		name = channelName;
	}

	public MultipartForm(HttpServletRequest request)
	{
		this();
		this.request = new WeakReference<HttpServletRequest>(request);

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
		parseRequest(request.get());
		this.request = null;
	}

	@SuppressWarnings("unchecked")
	public void parseRequest(HttpServletRequest request) throws MultipartFormException
	{
		state = PARSING;
		long contentLength = Long.parseLong(request.getHeader(MultipartFormConstants.CONTENT_LENTH));
		try
		{
			ServletFileUpload upload = new ServletFileUpload();
			upload.setFileItemFactory(new UploadItemProgressFactory(this, contentLength));
			Enumeration<String> e = request.getParameterNames();
			while (e.hasMoreElements())
			{
				String k = e.nextElement();
				String[] v = request.getParameterValues(k);
				form_parameters.put(k, Arrays.asList(v));
			}
			List<?> fileItems = upload.parseRequest(request);
			for (int i = 0; i < fileItems.size(); i++)
			{
				FileItem item = (FileItem) fileItems.get(i);
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
			state = COMPLETE;
			for (int i=0; i < this.progress_list.size(); i++)
			{
				this.progress_list.get(i).progress = 100;
				this.progress_list.get(i).complete = true;
			}
			this.request = null;
			if (listener != null)
				listener.onUploadComplete(this);
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

	public String getString(String name)
	{
		if (form_parameters.get(name) == null || form_parameters.get(name).isEmpty())
			return null;
		return form_parameters.get(name).get(0);
	}

	public List<String> getStringArray(String name)
	{
		return form_parameters.get(name);
	}

	public int getInt(String name)
	{
		return Text.toInt(form_parameters.get(name).get(0));
	}

	public List<Integer> getIntArray(String name)
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
	
	public long getLong(String name)
	{
		return Text.toLong(form_parameters.get(name).get(0));
	}

	public List<Long> getLongArray(String name)
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

	public float getFloat(String name)
	{
		return Text.toFloat(form_parameters.get(name).get(0));
	}

	public float getDate(String name)
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
	{
		return state == COMPLETE;
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