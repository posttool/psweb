package com.pagesociety.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zip
{
	final static int BUFFER = 2048;

	public static void zip(File baseDirectory, File destinationFile)
	{
		try
		{
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(destinationFile);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			// out.setMethod(ZipOutputStream.DEFLATED);
			byte data[] = new byte[BUFFER];
			// get a list of files from current directory
			File f = baseDirectory;
			String files[] = f.list();
			for (int i = 0; i < files.length; i++)
			{
				System.out.println("Adding: " + files[i]);
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER);
				ZipEntry entry = new ZipEntry(files[i]);
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, BUFFER)) != -1)
				{
					out.write(data, 0, count);
				}
				origin.close();
			}
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static final void unzip(InputStream zip, String dir)
	{
		try
		{
			BufferedOutputStream dest = null;
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zip));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null)
			{
				File extractedFile = new File(dir, entry.getName());
				if (entry.isDirectory())
				{
					extractedFile.mkdir();
					continue;
				}
				int count;
				byte data[] = new byte[BUFFER];
				FileOutputStream fos = new FileOutputStream(extractedFile);
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1)
				{
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
