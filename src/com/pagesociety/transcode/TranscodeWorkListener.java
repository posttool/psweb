package com.pagesociety.transcode;

import java.io.File;

/**
 * 
 * 
 * @author David
 * 
 */
public interface TranscodeWorkListener
{
	public void onFileWorkStart();

	public void onFileWorkProgress(Object progress);

	public void onFileWorkComplete(File output);

	public void onFileWorkError(String err_msg);
}
