package com.pagesociety.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class Files
{
	public static boolean deleteDir(File dir)
	{
		if (dir.isDirectory())
		{
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++)
			{
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success)
				{
					return false;
				}
			}
		}
		return dir.delete();
	}

	public static void copy(File src, File dst)
	{
		try
		{
			copy(new FileInputStream(src), dst);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public static void copy(InputStream in, File dst)
	{
		try
		{
			OutputStream out = new FileOutputStream(dst);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

//    public static boolean cleanDir(String path, Logger logger)
//    {
//        boolean succ = true; 
//        
//        File dest = new File(path);
//        if (!dest.exists())
//        {
//            succ = dest.mkdirs();
//
//            if (!succ)
//                logger.error("Failed to create directory at " + path); 
//        }
//        else
//        {
//            succ = deleteDir(dest);
//            
//            if (!succ)
//                logger.error("Failed to delete " + path);
//        }
//        
//        return succ; 
//    }
//    
//    public static boolean deleteDir(File dest)
//    {
//        boolean ret = true;
//        
//        File[] files = dest.listFiles();
//        for (File file: files)
//        {
//            if (file.isFile())
//                ret &= file.delete();
//            else
//                ret &= deleteDir(file);
//        }
//        
//        return ret;
//    }
//    
//	public static final String CLASSPATH_FILE = "env.properties"; 
//	private static java.util.Properties  properties = null; 
//	
//	public static String getProperty(String key, Logger logger)
//	{
//	    InputStream inStream = null; 
//	    String      ret      = null; 
//
//	    try
//	    {
//	        if (properties == null)
//	        {
//	            properties = new java.util.Properties();
//	            ClassLoader cl = Files.class.getClassLoader();
//	            java.net.URL url = cl.getResource(CLASSPATH_FILE);
//	            inStream = url.openStream();	            
//	            properties.load(inStream);
//	        }
//	        
//            ret = properties.getProperty(key);
//	    }
//	    catch (Exception e)
//	    {
//	        logger.error("Failed to read classpath resource " + CLASSPATH_FILE, e);
//	    }
//	    finally
//	    {
//	        close(inStream, logger);
//	    }
//	    
//	    return ret;
//	}
//	
//	
//	public static  void close(Closeable target, Logger logger)
//	{
//	    try
//        {
//            if (target != null)
//                target.close();
//        }
//        catch (Exception e)
//        {
//            logger.error("Failed to close resource " + target, e);
//        }
//	}
}
