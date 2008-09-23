package com.pagesociety.web.upload;

public class UploadProgressInfo
{
	protected String contentType;
	protected String fieldName;
	protected String fileName;
	protected double progress = 0;
	protected long fileSize = -1;
	protected long bytesRead = -1;
	/*this thing is complete when its resource object is set */
	private boolean complete = false;
	private Object completionObject;

	public String getFieldName()
	{
		return fieldName;
	}

	public String getFileName()
	{
		return fileName;
	}

	public double getProgress()
	{
		return progress;
	}

	public String getContentType()
	{
		return contentType;
	}
	
	public long getFileSize()
	{
		return fileSize;
	}

	public long getBytesRead()
	{
		return bytesRead;
	}
	
	public synchronized boolean isComplete()
	{
		return complete;		
	}
	

	public synchronized void setCompletionObject(Object o)
	{
		completionObject = o;
		complete 		 = true;
	}
	
	public synchronized Object getCompletionObject()
	{
		return completionObject;
	}
	
}
