package com.pagesociety.web.module.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator 
{
	private static Pattern email_pattern = Pattern.compile("([0-9a-zA-Z]+[-._+&])*[0-9a-zA-Z]+@([-0-9a-zA-Z]+[.])+[a-zA-Z]{2,6}");
	public static boolean isValidEmail(String email)
	{
		Matcher m =  email_pattern.matcher(email);
		return m.matches();
	}
	
	public static boolean isEmptyOrNull(String s) 
	{
		if(s== null)
			return true;
		if(s.equals(""))
			return true;
		return false;
	}
}
