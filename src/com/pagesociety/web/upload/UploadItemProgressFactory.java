package com.pagesociety.web.upload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

public class UploadItemProgressFactory extends DiskFileItemFactory
{
	private long contentLength;
	private MultipartForm multipart;

	public UploadItemProgressFactory(MultipartForm mpi, long contentLen)
	{
		super();
		this.multipart = mpi;
		this.contentLength = contentLen;
	}

	public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName)
	{
		UploadProgressInfo observer = new UploadProgressInfo();
		observer.fieldName = fieldName;
		observer.fileName = fileName;
		observer.fileSize = contentLength;
		FileItemMonitor item = new FileItemMonitor(fieldName, contentType, isFormField, fileName, multipart.upload_directory, observer);
		if (!isFormField)
			multipart.addUploadProgress(observer);
		return item;
	}
}
