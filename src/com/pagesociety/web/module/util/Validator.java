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
	
	private static Pattern alpha_pattern = Pattern.compile("[a-zA-Z]+");
	public static boolean isAlpha(String s)
	{
		Matcher m =  alpha_pattern.matcher(s);
		return m.matches();
	}
	
	private static Pattern alphanumeric_pattern = Pattern.compile("[0-9a-zA-Z]+");
	public static boolean isAlphaNumeric(String s)
	{
		Matcher m =  alphanumeric_pattern.matcher(s);
		return m.matches();
	}
	
	private static Pattern numeric_pattern = Pattern.compile("[0-9]+");
	public static boolean isNumeric(String s)
	{
		Matcher m =  numeric_pattern.matcher(s);
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
