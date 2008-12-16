package com.pagesociety.web.module.resource;

import java.io.File;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.util.FileInfo;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.resource.FileSystemPathProvider;

public class SimpleResourcePathProvider extends FileSystemPathProvider
{
	private static final String PARAM_DIRECTORY_NAME = "path-provider-directory-name";
	private String directory_name;
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		directory_name = GET_REQUIRED_CONFIG_PARAM(PARAM_DIRECTORY_NAME, config);
		
	}
	/* returns relative/path/to/dir/ */
	protected String get_save_directory(Entity user,File f) 
	{
		return directory_name;
	}
	
	
	protected String get_save_filename(Entity user,File f) 
	{
		String name = f.getName().substring(0,f.getName().lastIndexOf('.'));
		String ext = FileInfo.getExtension(f.getName()); 
		String dir = get_save_directory(user, f);		
		return get_next_unique_filename(dir, name, ext, 0); 
	}

	private String get_next_unique_filename(String dir,String filename,String ext,int no)
	{
		String t_filename;
		String num_ext	= null;
		if(no != 0)
			num_ext = String.valueOf(no);
		
		if(num_ext == null)
			t_filename = filename+"."+ext;
		else
			t_filename = filename+num_ext+"."+ext;
		
		File f = new File(dir,filename);
		if(f.exists())
			return get_next_unique_filename(dir,filename, ext, ++no);
		else
			return t_filename;
	}
}
