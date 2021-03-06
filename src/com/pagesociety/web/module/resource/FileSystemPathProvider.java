package com.pagesociety.web.module.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	private static final String PARAM_DIRECTORY_DEPTH			  = "path-provider-directory-depth";

	protected String base_dir;
	protected String base_url;
	protected String image_magick_path;
	protected String image_magick_convert_cmd;
	protected int    depth = 8;//myst be <=32


	protected static final String SLASH = "/";
	protected static final char	C_SLASH = '/';


	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		base_dir = GET_REQUIRED_CONFIG_PARAM(PARAM_RESOURCE_BASE_DIR, config);
		base_url = GET_REQUIRED_CONFIG_PARAM(PARAM_RESOURCE_BASE_URL, config);
		image_magick_path 		  = GET_REQUIRED_CONFIG_PARAM(PARAM_IMAGE_MAGICK_PATH, config);
		depth 		  = GET_OPTIONAL_INT_CONFIG_PARAM(PARAM_DIRECTORY_DEPTH, 8, config);

		if(!new File(image_magick_path).exists())
			throw new InitializationException("CANT FIND IMAGE MAGICK INSTALL AT "+image_magick_path);
		image_magick_convert_cmd = image_magick_path+File.separator+"convert";
		if(System.getProperty("os.name").startsWith("Windows"))
		{
			image_magick_convert_cmd = image_magick_convert_cmd+".exe";
		}
		/*
		System.out.println("FileSystemPathProvider base resource dir is "+base_dir);
		System.out.println("FileSystemPathProvider base url is "+base_url);
		System.out.println("FileSystemPathProvider image magick path is "+image_magick_path);
		System.out.println("FileSystemPathProvider image magick convert path is "+image_magick_convert_path);
		*/
		ImageMagick.setRuntimeExecPath(image_magick_convert_cmd);
		if(!base_dir.endsWith(SLASH) &&
		   !base_dir.endsWith(File.separator))
			base_dir = base_dir+SLASH;

	}

	public String getPathToken(Entity user,String filename) throws WebApplicationException
	{
		String relative_path = get_save_directory(user,filename);
		File dest_dir = new File(base_dir,relative_path);
		dest_dir.mkdirs();

		String save_filename = get_save_filename(user,dest_dir,filename);
		return relative_path+save_filename;
	}

	/* returns relative/path/to/dir/ */
	protected String get_save_directory(Entity user,String filename)
	{
		String guid = RandomGUID.getGUID();
		StringBuilder path = new StringBuilder();
		if(user == null)
			path.append(0);
		else
			path.append(user.getId());
		path.append(C_SLASH);
		byte[] b = guid.getBytes();
		for(int i = 0;i < depth;i++)
		{
			path.append((char)b[i]);
			path.append(C_SLASH);
		}
		return path.toString();
	}


	protected String get_save_filename(Entity user,File dest_dir,String filename)
	{
		StringBuilder buf = new StringBuilder();
		buf.append(RandomGUID.getGUID().substring(16));
		buf.append('.');
		String ext = FileInfo.getExtension(filename);
		if(ext != null)
			buf.append(ext);
		return buf.toString();
	}



	/* deletes file pointed to by this token as well as all previews */
	public void delete(String path_token) throws WebApplicationException
	{

		File f = new File(base_dir,path_token);

		if(!f.exists())
		{
			WARNING("ATTEMPTING TO DELETE FILE WHICH DOES NOT EXIST:\n"+f.getAbsolutePath());
			return;
		}
		if(f.isDirectory())
		{
			DELETE_DIR(f);
			return;
		}
		/* delete file and all generated previews */
		String filename = f.getName();
		int dot_idx = filename.lastIndexOf('.');
		if(dot_idx != -1)
			filename = filename.substring(0,dot_idx);

		File parent_dir = f.getParentFile();
		File[] ff = parent_dir.listFiles();
		for(int i =0;i < ff.length;i++)
		{
			String fname = ff[i].getName();
			if(fname.equals(f.getName()) || fname.startsWith(filename) &&
			   PreviewUtil.isLikelyPreview(filename))
			{
				ff[i].delete();
				if(ff.length == 1)
					parent_dir.delete();
				continue;
			}
			// FIX ME ///
			//	IF IT IS THE FILENAME ITSELF OR IF IT IS A PREVIEW  //
			//	DO THE DELETE										//
			//	OTHERWISE LEAVE IT ALONE							//
		}

	}


	public void deletePreviews(String path_token) throws WebApplicationException
	{
		File f = new File(base_dir,path_token);
		if(!f.exists())
			throw new WebApplicationException("ATTEMPTING TO DELETE FILE WHICH DOES NOT EXIST:\n"+f.getAbsolutePath());

		/* delete file and all generated previews */
		String filename = f.getName();
		int dot_idx = filename.lastIndexOf('.');
		String trimmed_filename=filename;
		if(dot_idx != -1)
			trimmed_filename = filename.substring(0,dot_idx);

		File parent_dir = f.getParentFile();
		File[] ff = parent_dir.listFiles();
		for(int i =0;i < ff.length;i++)
		{
			if(ff[i].equals(f))
				continue;
			//SAME HERE LOOK AT THIS//
			if(ff[i].getName().startsWith(trimmed_filename))
				ff[i].delete();
		}
	}
	/*in the case of a directory this will just list the files under the directory in a relative fashion */
	public List<String> listPreviews(String path_token) throws WebApplicationException
	{
		List<String> s = new ArrayList<String>();
		File f = new File(base_dir,path_token);
		if(!f.exists())
			throw new WebApplicationException("ATTEMPTING TO LIST DIR WHICH DOES NOT EXIST:\n"+f.getAbsolutePath());

		/* delete file and all generated previews */
		String filename = f.getName();
		int dot_idx = filename.lastIndexOf('.');
		String trimmed_filename=filename;
		if(dot_idx != -1)
			trimmed_filename = filename.substring(0,dot_idx);

		if(f.isDirectory())
		{
			List<String> flattened_dir = new ArrayList<String>();
			flatten_dir_listing(f,null , flattened_dir);
			return flattened_dir;
		}
		else
		{
			File parent_dir = f.getParentFile();
			File[] ff = parent_dir.listFiles();
			for(int i =0;i < ff.length;i++)
			{
				if(ff[i].equals(f))
					continue;

				if(ff[i].getName().startsWith(trimmed_filename))
					s.add(ff[i].getName());
			}
		}
		//SAME HERE//
		return s;
	}

	private List<String> flatten_dir_listing(File dir,String name,List<String> ret)
	{
		File[] ff = dir.listFiles();
		for(int i =0;i < ff.length;i++)
		{
			if(ff[i].isDirectory())
			{
				if(name == null)
					flatten_dir_listing(ff[i], ff[i].getName(), ret);
				else
					flatten_dir_listing(ff[i], name+"/"+ff[i].getName(), ret);
			}
			else
			{
				if(name == null)
					ret.add(ff[i].getName());
				else
					ret.add(name+"/"+ff[i].getName());
			}
		}
		return ret;
	}

	public String getUrl(String path_token)	throws WebApplicationException
	{
		StringBuilder buf = new StringBuilder();
		buf.append(base_url);
		buf.append(C_SLASH);
		buf.append(path_token);
		return buf.toString();
	}

	public String getBaseUrl()	throws WebApplicationException
	{
		return base_url;
	}

	public String getPreviewUrl(String path_token,Map<String,String> options)	throws WebApplicationException
	{
		String preview_relative_path = get_preview_file_relative_path(path_token, options);
		StringBuilder buf = new StringBuilder();
		buf.append(base_dir);
		buf.append(C_SLASH);
		buf.append(preview_relative_path);
		File preview = new File(buf.toString());

		//TODO: right here we could queue this stuff up and return a //
		//preview not ready. we would look in the queue and if it wasnt in//
		//there we would create a new queue item and put it on.//

		if(!preview.exists())
			PreviewUtil.createPreview(getFile(path_token),preview,options);

		buf.setLength(0);
		buf.append(base_url);
		buf.append(C_SLASH);
		buf.append(preview_relative_path);
		return buf.toString();
	}

	public OutputStream[] getOutputStreams(String path_token,String content_type,long content_length) throws WebApplicationException
	{
		File f = new File(base_dir,path_token);
		f.getParentFile().mkdirs();
		try{
			return new OutputStream[]{new FileOutputStream(f)};
		}catch(FileNotFoundException fnf)
		{
			//should never get here//
			fnf.printStackTrace();
			return null;
		}
	}

	public InputStream getInputStream(String path_token) throws WebApplicationException
	{
		File f = getFile(path_token);
		if(f != null)
		{
			try{
				return new FileInputStream(f);
			}catch(FileNotFoundException fne)
			{
				fne.printStackTrace();
				throw new WebApplicationException("SHOULDNT BE HERE.SEE LOGS");
			}
		}
		return null;
	}

	public File getFile(String path_token) throws WebApplicationException
	{
		File f = new File(base_dir,path_token);
		if(f.exists())
			return f;
		return null;
	}

	private String get_preview_file_relative_path(String path_token,Map<String,String> options) throws WebApplicationException
	{
		File original_file 				 = getFile(path_token);
		if(original_file == null)
			throw new WebApplicationException("NO FILE EXISTS FOR PATH TOKEN "+path_token+" CANT GENERATE PREVIEW.");

		int last_slash_index 		= path_token.lastIndexOf(C_SLASH);
		String dir 				  	= path_token.substring(0,last_slash_index); //original_file_relative_path.getParent();
		String original_file_name 	= path_token.substring(last_slash_index+1);
		int dot_idx 			  	= original_file_name.lastIndexOf('.');
		String ext 					= FileInfo.EXTENSIONS[FileInfo.JPG][0];

		if(dot_idx != -1)
		{
			ext = FileInfo.getExtension(original_file_name);
			original_file_name = original_file_name.substring(0,dot_idx);
		}

		StringBuilder preview_name = new StringBuilder();
		preview_name.append(dir);
		preview_name.append(C_SLASH);
		preview_name.append(original_file_name);
		preview_name.append(PreviewUtil.getOptionsSuffix(options, ext));
		return preview_name.toString();
	}

	@Override
	public void beginParse(String path_token) throws WebApplicationException {
		// TODO Auto-generated method stub

	}

	@Override
	public void endParse(String path_token) throws WebApplicationException {
		// TODO Auto-generated method stub

	}


}
