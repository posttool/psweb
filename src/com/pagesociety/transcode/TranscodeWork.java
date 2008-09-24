package com.pagesociety.transcode;

public interface TranscodeWork
{
	public void exec() throws Exception;

	public void addTranscodeWorkListener(TranscodeWorkListener listener);
}
