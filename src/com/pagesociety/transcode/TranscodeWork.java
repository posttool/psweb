package com.pagesociety.transcode;

public interface TranscodeWork
{
	public void exec();

	public void addTranscodeWorkListener(TranscodeWorkListener listener);
}
