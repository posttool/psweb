package com.pagesociety.web.module.S3;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import com.pagesociety.persistence.Entity;
import com.pagesociety.transcode.ImageMagick;
import com.pagesociety.util.FileInfo;
import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.S3.amazon.AWSAuthConnection;
import com.pagesociety.web.module.S3.amazon.GetResponse;
import com.pagesociety.web.module.S3.amazon.ListAllMyBucketsResponse;
import com.pagesociety.web.module.S3.amazon.Response;
import com.pagesociety.web.module.S3.amazon.S3Object;
import com.pagesociety.web.module.S3.amazon.Utils;
import com.pagesociety.web.module.resource.IResourcePathProvider;
import com.sun.corba.se.pept.transport.Connection;

public class PSS3PathProvider extends WebModule implements IResourcePathProvider
{
	private static final String PARAM_S3_BUCKET			  		  = "s3-bucket";
	private static final String PARAM_S3_API_KEY  				  = "s3-api-key";
	private static final String PARAM_S3_SECRET_KEY  			  = "s3-secret-key";
	private static final String PARAM_IMAGE_MAGICK_PATH   		  = "path-provider-image-magick-path";
	
	protected String s3_bucket;
	protected String s3_api_key;
	protected String s3_secret_key;
	protected String base_s3_url;
	protected String resize_tmp_directory = System.getProperty("java.io.tmpdir");
	
	protected String image_magick_path;
	protected String image_magick_convert_cmd;
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		s3_bucket	  = GET_REQUIRED_CONFIG_PARAM(PARAM_S3_BUCKET, config);
		s3_api_key    = GET_REQUIRED_CONFIG_PARAM(PARAM_S3_API_KEY, config);
		s3_secret_key = GET_REQUIRED_CONFIG_PARAM(PARAM_S3_SECRET_KEY, config);
		
		init_bucket();
		
		image_magick_path = GET_REQUIRED_CONFIG_PARAM(PARAM_IMAGE_MAGICK_PATH, config);
		base_s3_url   	  = "http://"+s3_bucket+".s3.amazonaws.com/";
		
		if(!new File(image_magick_path).exists())
			throw new InitializationException("CANT FIND IMAGE MAGICK INSTALL AT "+image_magick_path);
		image_magick_convert_cmd = image_magick_path+File.separator+"convert";
		if(System.getProperty("os.name").startsWith("Windows"))
		{
			image_magick_convert_cmd = image_magick_convert_cmd+".exe";
		}
		ImageMagick.setRuntimeExecPath(image_magick_convert_cmd);
	}
	
	private void init_bucket() throws InitializationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key); 
		try{
		boolean bucket_exists = conn.checkBucketExists(s3_bucket);
		if(!bucket_exists)
		{
			INFO("...creating S3 bucket "+s3_bucket);
			Response r = conn.createBucket(s3_bucket,AWSAuthConnection.LOCATION_DEFAULT, null);
			if(r.connection.getResponseCode() != r.connection.HTTP_OK)
				throw new InitializationException("COULDNT CREATE S3 BUCKET "+s3_bucket+" HTTP RESPONSE CODE WAS "+r.connection.getResponseMessage());
		}

		ListAllMyBucketsResponse my_buckets = conn.listAllMyBuckets(null);
		INFO("FOLLOWING S3 BUCKETS ARE ASSOCIATED WITH YOUR AWS ACCOUNT:\n");
		for(int i = 0;i < my_buckets.entries.size();i++)
			INFO("\t"+my_buckets.entries.get(i)+"\n");
		}catch(Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED DOING S3 BUCKET INITIALIZATION");
		}
	}
	
	public String getPathToken(Entity user,String filename) throws WebApplicationException
	{
		StringBuilder path_token = new StringBuilder();
		String rid		   = RandomGUID.getGUID().substring(24);
		path_token.append(user.getId());
		path_token.append('_');
		path_token.append(rid);
		path_token.append('_');
		path_token.append(filename);
		return path_token.toString();
	}
	
	/* deletes file pointed to by this token as well as all previews */
	public void delete(String path_token) throws WebApplicationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key); 
	
		try {
			Response r =  conn.delete(s3_bucket, path_token, null);
			if(r.connection.getResponseCode() != r.connection.HTTP_OK)
				throw new WebApplicationException("Failed S3 delete of "+s3_bucket+" "+path_token+" HTTP response code was "+r.connection.getResponseMessage());

		} catch (MalformedURLException e) {
			ERROR(e);
			throw new WebApplicationException("Malformed URL for S3 Delete: "+s3_bucket+" "+path_token);
		} catch (IOException e) {
			ERROR(e);
			throw new WebApplicationException("IOError for S3 Delete: "+s3_bucket+" "+path_token);	
		}

	}
	
	public String getUrl(String path_token)	throws WebApplicationException
	{
		return base_s3_url+path_token;
	}
	
	/* this should take preview params */
	private ConcurrentHashMap<String,Object> current_thumbnail_generators = new ConcurrentHashMap<String, Object>();
	public String getPreviewUrl(String path_token,int width,int height)	throws WebApplicationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key); 
		String preview_key 			  =  get_preview_file_relative_path(path_token, width, height);
		//TODO lock on preview_key ..... need to do this.
		//
		//
		//
		try{
			boolean resized_exists 		  = conn.checkKeyExists(s3_bucket, preview_key);
			if(resized_exists)
			{
				System.out.println("FOUND "+preview_key+" PREVIEW AT "+width+" "+height);
				return base_s3_url+preview_key;
			}
			

			Object lock = current_thumbnail_generators.putIfAbsent(preview_key, new Object());


			if(lock != null)
			{
				try{
					synchronized (lock)
					{
						lock.wait();	
					}
					
					boolean resized_now_exists 		  = conn.checkKeyExists(s3_bucket, preview_key);
					if(resized_now_exists)
						return base_s3_url+preview_key;
					throw new WebApplicationException("WAITING FOR RESOURCE TO BE RESIZED "+s3_bucket+" "+path_token+" BUT IT STILL DOESN'T EXIST");
				}catch (InterruptedException e) {}
			
			}
			else
			{
				lock = current_thumbnail_generators.get(preview_key);
			}	

			System.out.println("DIDNT FIND "+preview_key+" PREVIEW AT "+width+" "+height);
			GetResponse r = conn.get(s3_bucket,path_token,null);
			if(r.object == null)
			{
				throw new WebApplicationException("TRYING TO RESIZE S3 RESOURCE "+s3_bucket+" "+path_token+" BUT IT DOESNT SEEM TO EXIST.");
			}
		
			File expanded_file = new File(resize_tmp_directory,path_token);
			FileOutputStream fos = new FileOutputStream(expanded_file);
			fos.write(r.object.data);
			fos.flush();
			fos.close();
		
		
			File preview = new File(resize_tmp_directory,preview_key);
			create_preview(expanded_file,preview,width,height);
			String content_type = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(preview_key);
			PSS3Object pobj = new PSS3Object(new FileInputStream(preview),preview.length(),content_type,PSAWSAuthConnection.PERMISSIONS_PUBLIC_READ);
			Response pr = conn.streamingPut(s3_bucket, preview_key, pobj);
			if(pr.connection.getResponseCode() != pr.connection.HTTP_OK)
				throw new WebApplicationException("PROBLEM WRITNG PREVIEW TO S3 "+s3_bucket+" "+preview_key+" HTTP RESPONSE WAS "+r.connection.getResponseCode());
			synchronized (lock) 
			{			
				lock.notifyAll();
			}
			current_thumbnail_generators.remove(preview_key);
			return base_s3_url+preview_key;
		}catch(IOException ioe)
		{
			ERROR(ioe);
			throw new WebApplicationException("IO ERROR IN S3 GEN PREVIEW. SEE LOGS. "+ioe.getMessage());
		}

	}

	//dont forget to close output stream if you use this method
	public OutputStream getOutputStream(String path_token,String content_type,long content_size) throws WebApplicationException
	{
		System.out.println("SETTING FIXED CONTENT SIZE TO "+content_size);
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key); 
		Map headers = new LinkedHashMap();
        headers.put("x-amz-acl", Arrays.asList(new String[] { PSAWSAuthConnection.PERMISSIONS_PUBLIC_READ}));
        headers.put("Content-Type", Arrays.asList(new String[] { content_type}));
		
        try{
			HttpURLConnection request =
				conn.makeRequest("PUT", s3_bucket, Utils.urlencode(path_token), null, headers, null);
			//cant use this because flash reports wrong content size//
			request.setFixedLengthStreamingMode((int)content_size);
			//request.setChunkedStreamingMode(16384);//StreamingMode((int)content_size);
			request.setDoOutput(true);
			current_transfer_map.put(path_token, request);
			System.out.println("REQUEST IS "+request);
			return request.getOutputStream();
		}catch(Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("PROBLEM GETTING OUTPUT STREAM FOR S3 UPLOAD "+s3_bucket+" "+path_token);
		}
	}

	//dont forget to close input stream if you use this method
	public InputStream getInputStream(String path_token) throws WebApplicationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key); 
		try{		
			HttpURLConnection request =
				conn.makeRequest("GET", s3_bucket, Utils.urlencode(path_token), null, null,null);
			request.setDoInput(true);
			return request.getInputStream();
		}catch(Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("PROBLEM GETTING INPUT STREAM FOR S3 UPLOAD "+s3_bucket+" "+path_token);
		}
	}

	public File getFile(String path_token) throws WebApplicationException
	{
		try{
			PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key); 
			GetResponse r = conn.get(s3_bucket,path_token,null);
			if(r.object == null)
				throw new WebApplicationException("TRYING TO RESIZE S3 RESOURCE "+s3_bucket+" "+path_token+" BUT IT DOESNT SEEM TO EXIST.");
			
			File expanded_file = new File(resize_tmp_directory,path_token);
			FileOutputStream fos = new FileOutputStream(expanded_file);
			fos.write(r.object.data);
			fos.flush();
			fos.close();
			return expanded_file;
		}catch(Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("PROBLEM GETTING FILE FOR S3 UPLOAD "+s3_bucket+" "+path_token);
		}
	}

	

	private static void create_preview(File original,File dest,int w, int h) throws WebApplicationException
	{
		int type = FileInfo.getSimpleType(original);
		switch(type)
		{
			case FileInfo.SIMPLE_TYPE_IMAGE:
				create_image_preview(original, dest, w, h);
				break;
			case FileInfo.SIMPLE_TYPE_DOCUMENT:
				throw new WebApplicationException("DOCUMENT PREVIEW NOT SUPPORTED YET");
			case FileInfo.SIMPLE_TYPE_SWF:
				throw new WebApplicationException("SWF PREVIEW NOT SUPPORTED YET");
			case FileInfo.SIMPLE_TYPE_VIDEO:
				throw new WebApplicationException("DOCUMENT PREVIEW NOT SUPPORTED YET");
			case FileInfo.SIMPLE_TYPE_AUDIO:
				throw new WebApplicationException("AUDIO PREVIEW NOT SUPPORTED YET");
			default:
				throw new WebApplicationException("UNKNOW SIMPLE TYPE "+type);
		}
	}
	
	
	private static void create_image_preview(File original,File dest,int w, int h) throws WebApplicationException
	{
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
		int dot_idx 			  	= path_token.lastIndexOf('.');
		if(dot_idx != -1)
			path_token = path_token.substring(0,dot_idx);
		
		StringBuilder preview_name = new StringBuilder();
		preview_name.append(path_token);
		preview_name.append('_');
		preview_name.append(String.valueOf(width));
		preview_name.append('x');
		preview_name.append(String.valueOf(height));
		preview_name.append('.');
		preview_name.append(FileInfo.EXTENSIONS[FileInfo.JPG][0]);
		return preview_name.toString();
	}

	@Override
	public void beginParse(String path_token) throws WebApplicationException {
		// TODO Auto-generated method stub
		
	}


	private Map <String,HttpURLConnection> current_transfer_map = new HashMap<String, HttpURLConnection>();
	@Override
	public void endParse(String path_token) throws WebApplicationException {
		// TODO Auto-generated method stub
		HttpURLConnection c = current_transfer_map.get(path_token);
		try{
			Response r = new Response(c);
			int code = r.connection.getResponseCode();
			System.out.println("RESPONSE STATUS "+path_token+" IS "+code+" "+r.connection.getResponseMessage());
		}catch(IOException ioe)
		{
			ERROR(ioe);
		}
		current_transfer_map.remove(path_token);
	}
}
