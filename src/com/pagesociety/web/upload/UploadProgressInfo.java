package com.pagesociety.web.upload;

public class UploadProgressInfo
{
	public static UploadProgressInfo UPLOAD_PROGRESS_INIT = new UploadProgressInfo();
	//
	protected String fieldName;
	protected String fileName;
	protected double progress = 0;
	protected long fileSize = -1;
	protected long bytesRead = -1;
	protected boolean complete = false;
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

	public long getFileSize()
	{
		return fileSize;
	}

	public long getBytesRead()
	{
		return bytesRead;
	}
	
	public boolean isComplete()
	{
		return complete;
	}
	
	public void setCompletionObject(Object o)
	{
		complete = true;
		completionObject = o;
	}
	
	public Object getCompletionObject()
	{
		return completionObject;
	}
}
