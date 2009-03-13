package com.pagesociety.web.upload;

public class UploadProgressInfo
{
	protected String content_type;
	protected String file_name;
	protected double progress 	= 0;
	protected long 	 file_size  = 0;
	protected long 	 bytes_read = 0;
	private Object completionObject;

	
	public String getFileName()
	{
		return file_name;
	}

	public void setFileName(String file_name)
	{
		this.file_name = file_name;
	}
	
	public double getProgress()
	{
		return progress;
	}

	public void setProgress(double progress)
	{
		this.progress = progress;
	}
	
	public String getContentType()
	{
		return content_type;
	}
	
	public void setContentType(String content_type)
	{
		this.content_type = content_type;
	}
	
	public long getFileSize()
	{
		return file_size;
	}

	public void setFileSize(long file_size)
	{
		this.file_size = file_size;
	}
	
	public long getBytesRead()
	{
		return bytes_read;
	}
	
	public void setBytesRead(long bytes_read)
	{
		this.bytes_read = bytes_read;
	}
	

	public synchronized void setCompletionObject(Object o)
	{
		completionObject = o;
	}
	
	public synchronized Object getCompletionObject()
	{
		return completionObject;
	}
	
}
