package com.pagesociety.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class HttpDownload
{
	public static void download(String address, String localFileName)
	{
		File f = new File(localFileName);
		if (!f.getParentFile().exists() || !f.getParentFile().isDirectory())
			throw new RuntimeException("HttpDownload can't write a file to " + f.getParentFile());
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;
		try
		{
			URL url = new URL(address);
			out = new BufferedOutputStream(new FileOutputStream(f));
			conn = url.openConnection();
			in = conn.getInputStream();
			byte[] buffer = new byte[1024];
			int numRead;
			long numWritten = 0;
			while ((numRead = in.read(buffer)) != -1)
			{
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}
			System.out.println(localFileName + "\t" + numWritten);
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
		finally
		{
			try
			{
				if (in != null)
				{
					in.close();
				}
				if (out != null)
				{
					out.close();
				}
			}
			catch (IOException ioe)
			{
			}
		}
	}
}
