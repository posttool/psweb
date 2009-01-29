package com.pagesociety.web.module.resource;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.pagesociety.persistence.Entity;
import com.pagesociety.transcode.ImageMagick;
import com.pagesociety.util.FileInfo;
import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;

public class FileSystemPathProvider extends WebModule implements IResourcePathProvider
{
	private static final String PARAM_RESOURCE_BASE_DIR   		  = "path-provider-base-dir";
	private static final String PARAM_RESOURCE_BASE_URL   		  = "path-provider-base-url";
	private static final String PARAM_IMAGE_MAGICK_PATH   		  = "path-provider-image-magick-path";
	
	protected String base_dir;
	protected String base_url;
	protected String image_magick_path;
	protected String image_magick_convert_path;
	protected int    depth = 8;//myst be <=32

	
	protected static final String SLASH = "/";
	protected static final char	C_SLASH = '/';
	
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		base_dir = GET_REQUIRED_CONFIG_PARAM(PARAM_RESOURCE_BASE_DIR, config);
		base_url = GET_REQUIRED_CONFIG_PARAM(PARAM_RESOURCE_BASE_URL, config);
		image_magick_path 		  = GET_REQUIRED_CONFIG_PARAM(PARAM_IMAGE_MAGICK_PATH, config);
		
		if(!new File(image_magick_path).exists())
			throw new InitializationException("CANT FIND IMAGE MAGICK INSTALL AT "+image_magick_path);
		image_magick_convert_path = image_magick_path+File.separator+"convert";
		if(System.getProperty("os.name").startsWith("Windows"))
		{
			image_magick_convert_path = image_magick_convert_path+".exe";
		}
		/*
		System.out.println("FileSystemPathProvider base resource dir is "+base_dir);
		System.out.println("FileSystemPathProvider base url is "+base_url);
		System.out.println("FileSystemPathProvider image magick path is "+image_magick_path);
		System.out.println("FileSystemPathProvider image magick convert path is "+image_magick_convert_path);
		*/
		ImageMagick.setRuntimeExecPath(image_magick_convert_path);
		if(!base_dir.endsWith(SLASH) && 
		   !base_dir.endsWith(File.separator))
			base_dir = base_dir+SLASH;

	}
	
	public String/*path token*/ save(Entity user,File f) throws WebApplicationException
	{
		
		String relative_path = get_save_directory(user,f);
		
		File dest_dir = new File(base_dir,relative_path);
		dest_dir.mkdirs();

		String save_filename = get_save_filename(user,f);
		File dest_file = new File(dest_dir,save_filename);
		
		f.renameTo(dest_file);
		System.out.println("PathProvider SAVING TO "+dest_file+" RP:"+relative_path+" FN:"+save_filename);
		return relative_path+save_filename;
	}
	
	/* returns relative/path/to/dir/ */
	protected String get_save_directory(Entity user,File f) 
	{
		String guid = RandomGUID.getGUID();
		StringBuilder path = new StringBuilder();
		byte[] b = guid.getBytes();
		for(int i = 0;i < depth;i++)
		{
			path.append((char)b[i]);
			path.append(C_SLASH);
		}
		return path.toString();
	}
	
	
	protected String get_save_filename(Entity user,File f) 
	{
		StringBuilder buf = new StringBuilder();
		buf.append(RandomGUID.getGUID().substring(16));
		buf.append('.');
		String ext = FileInfo.getExtension(f.getName()); 
		if(ext != null)
			buf.append(ext);
		return buf.toString();
	}
	
	/* deletes file pointed to by this token as well as all previews */
	public void delete(String path_token) throws WebApplicationException
	{
		File f = new File(base_dir,path_token);
		if(!f.exists())
			throw new WebApplicationException("ATTEMPTING TO DELETE FILE WHICH DOES NOT EXIST:\n"+f.getAbsolutePath());
		
		/* delete file and all generated previews */
		String filename = f.getName();
		int dot_idx = filename.lastIndexOf('.');
		if(dot_idx != -1)
			filename = filename.substring(0,dot_idx);

		File parent_dir = f.getParentFile();
		File[] ff = parent_dir.listFiles();
		for(int i =0;i < ff.length;i++)
		{
			if(ff[i].getName().startsWith(filename))
				ff[i].delete();
		}
	}
	
	public String getUrl(String path_token)	throws WebApplicationException
	{
		StringBuilder buf = new StringBuilder();
		buf.append(base_url);
		buf.append(C_SLASH);
		buf.append(path_token);
		return buf.toString();
	}
	
	/* this should take preview params */
	public String getPreviewUrl(String path_token,int width,int height)	throws WebApplicationException
	{
		String preview_relative_path = get_preview_file_relative_path(path_token, width, height);
		StringBuilder buf = new StringBuilder();
		buf.append(base_dir);
		buf.append(C_SLASH);
		buf.append(preview_relative_path);
		File preview = new File(buf.toString());
		if(!preview.exists())
		{
			try {
				create_preview(getFile(path_token),preview,width,height);
			}catch(Exception e)
			{
				//FIXME handle this better!
				preview_relative_path = "no_preview_available.jpg"; 
			}
		}

		buf.setLength(0);
		buf.append(base_url);
		buf.append(C_SLASH);
		buf.append(preview_relative_path);
		return buf.toString();			

	}
		
	public File getFile(String path_token) throws WebApplicationException
	{
		File f = new File(base_dir,path_token);
		if(f.exists())
			return f;
		return null;
	}

	private static void create_preview(File original,File dest,int w, int h) throws WebApplicationException
	{
		// TODO imagemagick can create a preview for a tiff and other non-web formats,
		// but we need to return it as a jpg, for flash/html compatibility
		ImageMagick i = new ImageMagick(original,dest);
		i.setSize(w, h);
		
		try{
			i.exec();
		}catch(Exception e)
		{
			throw new WebApplicationException("PROBLEM CREATING PREVIEW "+dest.getAbsolutePath(),e);
		}
	}
	
	private String get_preview_file_relative_path(String path_token,int width,int height) throws WebApplicationException
	{
		File original_file 				 = getFile(path_token);
		if(original_file == null)
			throw new WebApplicationException("NO FILE EXISTS FOR PATH TOKEN "+path_token+" CANT GENERATE PREVIEW.");
		
		int last_slash_index = path_token.lastIndexOf(C_SLASH);
		String dir 				  	= path_token.substring(0,last_slash_index); //original_file_relative_path.getParent();
		String original_file_name 	= path_token.substring(last_slash_index+1);
		int dot_idx 			  	= original_file_name.lastIndexOf('.');
		String ext 					= null;
		
		if(dot_idx != -1)
		{
			ext = FileInfo.getExtension(original_file_name);
			original_file_name = original_file_name.substring(0,dot_idx);
		}
		
		StringBuilder preview_name = new StringBuilder();
		preview_name.append(dir);
		preview_name.append(C_SLASH);
		preview_name.append(original_file_name);
		preview_name.append('_');
		preview_name.append(String.valueOf(width));
		preview_name.append('x');
		preview_name.append(String.valueOf(height));
		if(ext != null)
		{
			preview_name.append('.');
			preview_name.append(ext);
		}
		return preview_name.toString();
	}
}
