package com.pagesociety.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
				int total = 0;
				int c;
				while ((c = origin.read(data, 0, BUFFER)) != -1)
				{
					out.write(data, 0, c);
					total += c;
				}
				origin.close();
			}
			out.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void zipDir(File dir, File zip) throws Exception
	{
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
		System.out.println("Creating : " + zip);
		addDir(dir, dir, out);
		out.close();
	}

	public static void addDir(File root, File dirObj, ZipOutputStream out) throws IOException
	{
		File[] files = dirObj.listFiles();
		byte[] tmpBuf = new byte[1024];
		int split_idx = root.getAbsolutePath().length() + 1;

		for (int i = 0; i < files.length; i++)
		{
			String path = files[i].getAbsolutePath().substring(split_idx) ;
			if (files[i].isDirectory())
			{
				System.out.println(" Adding directory: " + path);
				out.putNextEntry(new ZipEntry(path+ "/"));
				addDir(root, files[i], out);
				continue;
			}
			System.out.println(" Adding file: " + path);
			FileInputStream in = new FileInputStream(files[i]);
			out.putNextEntry(new ZipEntry(path));
			int len;
			while ((len = in.read(tmpBuf)) > 0)
			{
				out.write(tmpBuf, 0, len);
			}
			out.closeEntry();
			in.close();
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
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
