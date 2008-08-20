package com.pagesociety.transcode;

import java.io.File;
import java.util.ArrayList;

public abstract class TranscodeWorkImpl implements TranscodeWork
{
	File input;
	File output;
	private ArrayList<TranscodeWorkListener> listeners;

	public TranscodeWorkImpl(File input, File output)
	{
		this.input = input;
		this.output = output;
		this.listeners = new ArrayList<TranscodeWorkListener>();
	}

	public void addTranscodeWorkListener(TranscodeWorkListener listener)
	{
		listeners.add(listener);
	}

	public void fireWorkStart()
	{
		for (TranscodeWorkListener listener : listeners)
		{
			listener.onFileWorkStart();
		}
	}

	public void fireWorkProgress(Object progress)
	{
		for (TranscodeWorkListener listener : listeners)
		{
			listener.onFileWorkProgress(progress);
		}
	}

	public void fireWorkComplete(File output)
	{
		for (TranscodeWorkListener listener : listeners)
		{
			listener.onFileWorkComplete(output);
		}
	}

	public void fireWorkError(String err_msg)
	{
		for (TranscodeWorkListener listener : listeners)
		{
			listener.onFileWorkError(err_msg);
		}
	}
}
