package com.pagesociety.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessReader extends Thread
{
	public static final int OUT = 0;
	public static final int ERR = 1;
	//
	private InputStream is;
	private CmdWorkListener observer;
	private long id;
	private int type;

	public ProcessReader(InputStream is, CmdWorkListener o, long id, int type)
	{
		this.is = is;
		this.observer = o;
		this.id = id;
		this.type = type;
	}

	public void run()
	{
		try
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = br.readLine()) != null)
			{
				switch (type)
				{
					case OUT:
						observer.stdout(id, line);
						break;
					case ERR:
						observer.stderr(id, line);
						break;
					default:
						break;
				}
			}
			is.close();
			is = null;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			observer.stderr(id, ioe.getMessage());
		}
	}

	public boolean isReading()
	{
		return is != null;
	}
}
