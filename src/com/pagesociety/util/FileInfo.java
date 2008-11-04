package com.pagesociety.util;

import java.io.File;

public class FileInfo
{
	public static final int JPG = 0;
	public static final int GIF = 1;
	public static final int PNG = 2;
	public static final int MP3 = 3;
	public static final int WAV = 4;
	public static final int AIF = 5;
	public static final int VOG = 6;
	public static final int AVI = 7;
	public static final int MOV = 8;
	public static final int DIVX = 9;
	public static final int MPG = 10;
	public static final int PDF = 11;
	public static final int DOC = 12;
	public static final int SWF = 13;
	public static final int EXTENSIONS_LENGTH = 14;
	public static final String[][] EXTENSIONS = new String[EXTENSIONS_LENGTH][];
	static
	{
		EXTENSIONS[JPG]  = new String[] { "jpg", "jpeg" };
		EXTENSIONS[GIF]  = new String[] { "gif" };
		EXTENSIONS[PNG]  = new String[] { "png" };
		EXTENSIONS[MP3]  = new String[] { "mp3" };
		EXTENSIONS[WAV]  = new String[] { "wav" };
		EXTENSIONS[AIF]  = new String[] { "aif", "aiff" };
		EXTENSIONS[VOG]  = new String[] { "ogm", "ogg", "vog" };
		EXTENSIONS[AVI]  = new String[] { "avi" };
		EXTENSIONS[MOV]  = new String[] { "mov" };
		EXTENSIONS[DIVX] = new String[] { "divx", "xvid" };
		EXTENSIONS[MPG]  = new String[] { "mpg", "mpeg", "mp4" };
		EXTENSIONS[PDF]  = new String[] { "pdf" };
		EXTENSIONS[DOC]  = new String[] { "doc" };
		EXTENSIONS[SWF]  = new String[] { "swf" };
	}
	// "simple" types
	public static final int SIMPLE_TYPE_IMAGE    = 100;
	public static final int SIMPLE_TYPE_AUDIO    = 101;
	public static final int SIMPLE_TYPE_VIDEO    = 102;
	public static final int SIMPLE_TYPE_DOCUMENT = 103;
	public static final int SIMPLE_TYPE_SWF 	 = 104;

	
	public static int getType(File file)
	{
		if (file == null)
			return -1;
		String file_extention = getExtension(file.getName());
		for (int i = 0; i < EXTENSIONS_LENGTH; i++)
		{
			String[] ext = EXTENSIONS[i];
			for (int j = 0; j < ext.length; j++)
			{
				if (ext[j].equals(file_extention))
				{
					return i;
				}
			}
		}
		return -1;
	}

	
	public static int getSimpleType(File file)
	{
		int type = getType(file);
		switch (type)
		{
		case JPG:
		case GIF:
		case PNG:
			return SIMPLE_TYPE_IMAGE;
		case AIF:
		case MP3:
		case WAV:
		case VOG:
			return SIMPLE_TYPE_AUDIO;
		case AVI:
		case MOV:
		case DIVX:
		case MPG:
			return SIMPLE_TYPE_VIDEO;
		case PDF:
		case DOC:
			return SIMPLE_TYPE_DOCUMENT;	
		case SWF:
			return SIMPLE_TYPE_SWF;	
		}
		return -1;
	}
	
	public static String getSimpleTypeAsString(File file)
	{
		int type = getType(file);
		switch (type)
		{
			case JPG:
			case GIF:
			case PNG:
				return "IMAGE";
			case AIF:
			case MP3:
			case WAV:
			case VOG:
				return "AUDIO";
			case AVI:
			case MOV:
			case DIVX:
			case MPG:
				return "VIDEO";
			case PDF:
			case DOC:
				return "DOCUMENT";	
			case SWF:
				return "SWF";		
		}
		return "????";
	}

	public static boolean isMovie(File file)
	{
		int t = getType(file);
		switch (t)
		{
		case AVI:
		case MOV:
		case DIVX:
		case MPG:
			return true;
		default:
			return false;
		}
	}

	public static boolean isImage(File file)
	{
		int t = getType(file);
		switch (t)
		{
		case JPG:
		case GIF:
		case PNG:
			return true;
		default:
			return false;
		}
	}

	public static boolean isAudio(File file)
	{
		int t = getType(file);
		switch (t)
		{
		case AIF:
		case MP3:
		case WAV:
		case VOG:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isDocument(File file)
	{
		int t = getType(file);
		switch (t)
		{
		case PDF:
		case DOC:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isSWF(File file)
	{
		int t = getType(file);
		switch (t)
		{
		case SWF:
			return true;
		default:
			return false;
		}
	}

	public static String getExtension(String name)
	{
		int n = name.lastIndexOf('.');
		if (n == -1)
		{
			return null;
		}
		return name.substring(n + 1).toLowerCase();
	}
}
