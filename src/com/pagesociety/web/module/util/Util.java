package com.pagesociety.web.module.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import com.pagesociety.util.RandomGUID;

public class Util 
{
	public static String stringToHexEncodedMD5(String s)
	{
		byte[] defaultBytes = s.getBytes();
		try
		{
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte messageDigest[] = algorithm.digest();
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
			{
				String hs = Integer.toHexString(0xFF & messageDigest[i]);
				if (hs.length() == 1)
					hs = "0" + hs;
				hexString.append(hs);
			}
			return hexString.toString();
		}
		catch (NoSuchAlgorithmException nsae)
		{
			nsae.printStackTrace();
			return "--- NO MD5 ALGORITHM AVAILABLE";
		}
	}
	
	public static String getGUID()
	{
		return RandomGUID.getGUID();
	}
	
	
	public static long getFileChecksum(File f)
	{
		long checksum = -1;
		if (f==null)
				return checksum;
	    try {
	        // Compute Adler-32 checksum
	        CheckedInputStream cis = new CheckedInputStream(
	            new FileInputStream(f), new Adler32());
	        byte[] tempBuf = new byte[128];
	        while (cis.read(tempBuf) >= 0) {
	        }
	        checksum = cis.getChecksum().getValue();
	    } catch (IOException e) {
	    }
	    return checksum;
	}
}
