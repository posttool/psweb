package com.pagesociety.web.module.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
	        cis.close();
	    } catch (IOException e) {
	    }
	    return checksum;
	}
	
	public static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";
	 
	 public static String encodeURIComponent(String input) {
	  if(input==null)
	   return input;

	   
	  int l = input.length();
	  StringBuilder o = new StringBuilder(l * 3);
	  try {
	   for (int i = 0; i < l; i++) {
	    String e = input.substring(i, i + 1);
	    if (ALLOWED_CHARS.indexOf(e) == -1) {
	     byte[] b = e.getBytes("utf-8");
	     o.append(getHex(b));
	     continue;
	    }
	    o.append(e);
	   }
	   return o.toString();
	  } catch(UnsupportedEncodingException e) {
	   e.printStackTrace();
	  }
	  return input;
	 }
	  
	 private static String getHex(byte buf[]) {
	  StringBuilder o = new StringBuilder(buf.length * 3);
	  for (int i = 0; i < buf.length; i++) {
	   int n = (int) buf[i] & 0xff;
	   o.append("%");
	   if (n < 0x10) {
	    o.append("0");
	   }
	   o.append(Long.toString(n, 16).toUpperCase());
	  }
	  return o.toString();
	 }
	 
	 public static String decodeURIComponent(String encodedURI) {
	  char actualChar;
	 
	  StringBuffer buffer = new StringBuffer();
	 
	  int bytePattern, sumb = 0;
	 
	  for (int i = 0, more = -1; i < encodedURI.length(); i++) {
	   actualChar = encodedURI.charAt(i);
	 
	   switch (actualChar) {
	    case '%': {
	     actualChar = encodedURI.charAt(++i);
	     int hb = (Character.isDigit(actualChar) ? actualChar - '0'
	       : 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
	     actualChar = encodedURI.charAt(++i);
	     int lb = (Character.isDigit(actualChar) ? actualChar - '0'
	       : 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
	     bytePattern = (hb << 4) | lb;
	     break;
	    }
	    case '+': {
	     bytePattern = ' ';
	     break;
	    }
	    default: {
	     bytePattern = actualChar;
	    }
	   }
	 
	   if ((bytePattern & 0xc0) == 0x80) { // 10xxxxxx
	    sumb = (sumb << 6) | (bytePattern & 0x3f);
	    if (--more == 0)
	     buffer.append((char) sumb);
	   } else if ((bytePattern & 0x80) == 0x00) { // 0xxxxxxx
	    buffer.append((char) bytePattern);
	   } else if ((bytePattern & 0xe0) == 0xc0) { // 110xxxxx
	    sumb = bytePattern & 0x1f;
	    more = 1;
	   } else if ((bytePattern & 0xf0) == 0xe0) { // 1110xxxx
	    sumb = bytePattern & 0x0f;
	    more = 2;
	   } else if ((bytePattern & 0xf8) == 0xf0) { // 11110xxx
	    sumb = bytePattern & 0x07;
	    more = 3;
	   } else if ((bytePattern & 0xfc) == 0xf8) { // 111110xx
	    sumb = bytePattern & 0x03;
	    more = 4;
	   } else { // 1111110x
	    sumb = bytePattern & 0x01;
	    more = 5;
	   }
	  }
	  return buffer.toString();
	 }
}
