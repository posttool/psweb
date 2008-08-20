package com.pagesociety.web.upload;

import java.io.File;

public interface MultipartFormListener
{
	public void onFileComplete(MultipartForm upload, File f);

	public void onUploadComplete(MultipartForm upload);

	public void onUploadException(MultipartForm upload, Exception e);
}
