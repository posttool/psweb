package com.pagesociety.web.module.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
			return "--- MD5 ALGORITHM AVAILABLE";
		}
	}
}
