package com.pagesociety.web.module.S3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.transcode.ImageMagick;
import com.pagesociety.util.FileInfo;
import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.S3.amazon.AWSAuthConnection;
import com.pagesociety.web.module.S3.amazon.GetResponse;
import com.pagesociety.web.module.S3.amazon.ListAllMyBucketsResponse;
import com.pagesociety.web.module.S3.amazon.ListBucketResponse;
import com.pagesociety.web.module.S3.amazon.ListEntry;
import com.pagesociety.web.module.S3.amazon.Response;
import com.pagesociety.web.module.S3.amazon.S3Object;
import com.pagesociety.web.module.S3.amazon.Utils;
import com.pagesociety.web.module.resource.IResourcePathProvider;

public class PSS3PathProvider extends WebStoreModule implements IResourcePathProvider
{
	public static final String PARAM_S3_BUCKET = "s3-bucket";
	public static final String PARAM_S3_API_KEY = "s3-api-key";
	public static final String PARAM_S3_SECRET_KEY = "s3-secret-key";
	public static final String PARAM_SCRATCH_DIRECTORY = "scratch-directory";
	public static final String PARAM_IMAGE_MAGICK_PATH = "path-provider-image-magick-path";
	public static final String S3_DELETE_QUEUE_NAME = "s3-pp-delete-queue";
	protected String s3_bucket;
	protected String s3_api_key;
	protected String s3_secret_key;
	protected String base_s3_url;
	protected String scratch_directory;
	protected String image_magick_path;
	protected String image_magick_convert_cmd;
	protected Object DELETE_QUEUE_LOCK;;

	public void init(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		super.init(app, config);
		DELETE_QUEUE_LOCK = new Object();
		s3_bucket = GET_REQUIRED_CONFIG_PARAM(PARAM_S3_BUCKET, config);
		s3_api_key = GET_REQUIRED_CONFIG_PARAM(PARAM_S3_API_KEY, config);
		s3_secret_key = GET_REQUIRED_CONFIG_PARAM(PARAM_S3_SECRET_KEY, config);
		image_magick_path = GET_REQUIRED_CONFIG_PARAM(PARAM_IMAGE_MAGICK_PATH, config);
		base_s3_url = "http://" + s3_bucket + ".s3.amazonaws.com/";
		if (!new File(image_magick_path).exists())
			throw new InitializationException("CANT FIND IMAGE MAGICK INSTALL AT " + image_magick_path);
		image_magick_convert_cmd = image_magick_path + File.separator + "convert";
		if (System.getProperty("os.name").startsWith("Windows"))
		{
			image_magick_convert_cmd = image_magick_convert_cmd + ".exe";
		}
		ImageMagick.setRuntimeExecPath(image_magick_convert_cmd);
	}

	public void loadbang(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		setup_delete_queue();
		init_bucket();
		init_scratch_directory(app, config);
		setup_crossdomain_file(app, config);
		start_s3_delete_consumer();
		start_scratch_cleaner();
	}

	public void init_scratch_directory(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		scratch_directory = GET_OPTIONAL_CONFIG_PARAM(PARAM_SCRATCH_DIRECTORY, config);
		if (scratch_directory == null)
			scratch_directory = new File(GET_MODULE_DATA_DIRECTORY(app), "scratch-dir").getAbsolutePath();
		try
		{
			File f = new File(scratch_directory);
			if (!f.exists())
			{
				INFO("CREATING SCRATCH DIRECTORY " + f.getAbsolutePath());
				f.mkdirs();
			}
		}
		catch (Exception e)
		{
			throw new InitializationException("PROBLEM CREATING SCRATCH DIRECTORY " + scratch_directory);
		}
	}

	private void setup_crossdomain_file(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		try
		{
			if (!fileExists("crossdomain.xml"))
			{
				String crossdomain_file = "<cross-domain-policy>\n" + "\t<allow-access-from domain=\"*\" />\n" + "\t<site-control permitted-cross-domain-policies=\"all\"/>\n" + "</cross-domain-policy>\n";
				File cross_domain_temp_file = new File(scratch_directory + File.separator + "crossdomain.xml");
				FileOutputStream fos = new FileOutputStream(cross_domain_temp_file);
				fos.write(crossdomain_file.getBytes());
				fos.close();
				putFile(cross_domain_temp_file, "text/xml");
				INFO("PUT " + cross_domain_temp_file.getName() + " ON S3");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new InitializationException("FAILED SETTING UP CROSSDOMAIN FILE ", e);
		}
	}

	private void setup_delete_queue() throws InitializationException
	{
		try
		{
			CREATE_QUEUE(S3_DELETE_QUEUE_NAME, 128, 64);
		}
		catch (PersistenceException e)
		{
			throw new InitializationException("FAILED SETTING UP DELETE QUEUE ", e);
		}
	}

	private void init_bucket() throws InitializationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
		try
		{
			boolean bucket_exists = conn.checkBucketExists(s3_bucket);
			if (!bucket_exists)
			{
				INFO("...creating S3 bucket " + s3_bucket);
				Response r = conn.createBucket(s3_bucket, AWSAuthConnection.LOCATION_DEFAULT, null);
				if (r.connection.getResponseCode() != r.connection.HTTP_OK)
					throw new InitializationException("COULDNT CREATE S3 BUCKET " + s3_bucket + " HTTP RESPONSE CODE WAS " + r.connection.getResponseMessage());
			}
			ListAllMyBucketsResponse my_buckets = conn.listAllMyBuckets(null);
			INFO("FOLLOWING S3 BUCKETS ARE ASSOCIATED WITH YOUR AWS ACCOUNT:");
			for (int i = 0; i < my_buckets.entries.size(); i++)
				INFO("\t" + my_buckets.entries.get(i));
		}
		catch (Exception e)
		{
			ERROR(e);
			throw new InitializationException("FAILED DOING S3 BUCKET INITIALIZATION");
		}
	}

	public String getPathToken(Entity user, String filename)
			throws WebApplicationException
	{
		StringBuilder path_token = new StringBuilder();
		String rid = RandomGUID.getGUID().substring(24);
		path_token.append(user.getId());
		path_token.append('/');
		path_token.append(rid);
		path_token.append('_');
		path_token.append(filename);
		return path_token.toString();
	}

	/* deletes file pointed to by this token as well as all previews */
	public void delete(String path_token) throws WebApplicationException
	{
		String deletee_prefix = get_preview_file_prefix(path_token);
		List<ListEntry> delete_keys = list(deletee_prefix);
		for (int i = 0; i < delete_keys.size(); i++)
		{
			String delete_key = delete_keys.get(i).key;
			try
			{
				store.enqueue(S3_DELETE_QUEUE_NAME, delete_key.getBytes(), true);
				synchronized (DELETE_QUEUE_LOCK)
				{
					DELETE_QUEUE_LOCK.notifyAll();
				}
			}
			catch (PersistenceException pe)
			{
				// TODO: could sleep and retry here//
				ERROR("FAILED ADDING " + delete_key + "TO S3 DELETE QUEUE.", pe);
			}
			INFO("ADDED " + delete_key + "TO S3 DELETE QUEUE.");
		}
	}

	/* deletes file pointed to by this token as well as all previews */
	public void deletePreviews(String path_token) throws WebApplicationException
	{
		String deletee_prefix = get_preview_file_prefix(path_token);
		List<ListEntry> delete_keys = list(deletee_prefix);
		for (int i = 0; i < delete_keys.size(); i++)
		{
			String delete_key = delete_keys.get(i).key;
			if (delete_key.equals(path_token))
				continue;
			try
			{
				store.enqueue(S3_DELETE_QUEUE_NAME, delete_key.getBytes(), true);
				synchronized (DELETE_QUEUE_LOCK)
				{
					DELETE_QUEUE_LOCK.notifyAll();
				}
			}
			catch (PersistenceException pe)
			{
				// TODO: could sleep and retry here//
				ERROR("FAILED ADDING " + delete_key + "TO S3 DELETE QUEUE.", pe);
			}
			INFO("ADDED " + delete_key + "TO S3 DELETE QUEUE.");
		}
	}

	public List<String> listPreviews(String path_token) throws WebApplicationException
	{
		List<String> s = new ArrayList<String>();
		String delete_prefix = get_preview_file_prefix(path_token);
		List<ListEntry> delete_keys = list(delete_prefix);
		for (int i = 0; i < delete_keys.size(); i++)
		{
			String delete_key = delete_keys.get(i).key;
			if (delete_key.equals(path_token))
				continue;
			s.add(delete_key);
		}
		return s;
	}

	public boolean fileExists(String path_token) throws WebApplicationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
		try
		{
			return conn.checkKeyExists(s3_bucket, path_token);
		}
		catch (Exception e)
		{
			throw new WebApplicationException("FAILED CHECKING IF " + path_token + " EXISTS.", e);
		}
	}

	public void putFile(File f, String content_type) throws WebApplicationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
		String filename = f.getName();
		if (content_type == null)
			content_type = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(f);
		try
		{
			PSS3Object pobj = new PSS3Object(new FileInputStream(f), f.length(), content_type, PSAWSAuthConnection.PERMISSIONS_PUBLIC_READ);
			Response pr = conn.streamingPut(s3_bucket, f.getName(), pobj);
			if (pr.connection.getResponseCode() != pr.connection.HTTP_OK)
				throw new WebApplicationException("PROBLEM WRITNG PREVIEW TO S3 " + s3_bucket + " " + filename + " HTTP RESPONSE WAS " + pr.connection.getResponseCode());
		}
		catch (Exception e)
		{
			throw new WebApplicationException("FAILED PUTTING " + filename + " TO S3");
		}
	}

	public void deleteFromS3(PSAWSAuthConnection conn, String path_token)
			throws MalformedURLException, IOException, WebApplicationException
	{
		Response r = conn.delete(s3_bucket, path_token, null);
		if (r.connection.getResponseCode() != r.connection.HTTP_NO_CONTENT)
			throw new WebApplicationException("Failed S3 delete of " + s3_bucket + " " + path_token + " HTTP response code was " + r.connection.getResponseMessage());
		INFO("DELETED " + path_token + " FROM S3.");
	}

	public List<ListEntry> list(String prefix) throws WebApplicationException
	{
		return list(prefix, null, null);
	}

	public List<ListEntry> list(String prefix, String marker, Integer max_keys)
			throws WebApplicationException
	{
		if (prefix.equals("*"))
			prefix = null;
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
		try
		{
			ListBucketResponse r = conn.listBucket(s3_bucket, prefix, marker, max_keys, null);
			if (r.connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new WebApplicationException("Failed S3 LIST of " + s3_bucket + " HTTP response code was " + r.connection.getResponseMessage());
			return r.entries;
		}
		catch (MalformedURLException e)
		{
			ERROR(e);
			throw new WebApplicationException("Malformed URL for S3 LIST: " + s3_bucket);
		}
		catch (IOException e)
		{
			ERROR(e);
			throw new WebApplicationException("IOError for S3 LIST: " + s3_bucket);
		}
	}

	public String getUrl(String path_token) throws WebApplicationException
	{
		return base_s3_url + path_token;
	}

	/* this should take preview params */
	private ConcurrentHashMap<String, Object> current_thumbnail_generators = new ConcurrentHashMap<String, Object>();

	public String getPreviewUrl(String path_token, int width, int height)
			throws WebApplicationException
	{
		INFO("GETTING PREVIEW URL FOR " + path_token + " AT " + width + " " + height);
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
		String preview_key = get_preview_file_relative_path(path_token, width, height);
		INFO("PREVIEW KEY IS " + preview_key);
		Object lock = null;
		try
		{
			boolean resized_exists = conn.checkKeyExists(s3_bucket, preview_key);
			if (resized_exists)
			{
				System.out.println("FOUND " + preview_key + " PREVIEW AT " + width + " " + height);
				return base_s3_url + preview_key;
			}
			lock = current_thumbnail_generators.putIfAbsent(preview_key, new Object());
			if (lock != null)
			{
				try
				{
					synchronized (lock)
					{
						INFO("WAITING IN PREVIEW");
						lock.wait();
					}
					INFO("NOT WAITING ANYMORE");
					boolean resized_now_exists = conn.checkKeyExists(s3_bucket, preview_key);
					if (resized_now_exists)
						return base_s3_url + preview_key;
					else
						throw new Exception("WAITING FOR RESOURCE TO BE RESIZED " + s3_bucket + " " + path_token + " BUT IT STILL DOESN'T EXIST");
				}
				catch (Exception e)
				{
					WAE(e);
				}
			}
			else
			{
				lock = current_thumbnail_generators.get(preview_key);
			}
			System.out.println("DIDNT FIND " + preview_key + " PREVIEW AT " + width + " " + height);
			// look for it in the scratch directory//
			File original_file = new File(scratch_directory, path_token);
			if (!original_file.exists())
			{
				System.out.println("DIDNT FIND " + path_token + " IN SCRATCH DIRECTORY. GOING TO AMAZON FOR ORIGINAL.");
				GetResponse r = conn.get(s3_bucket, path_token, null);
				if (r.object == null)
				{					
					current_thumbnail_generators.remove(preview_key);
					synchronized (lock)
					{
						lock.notifyAll();
					}
					throw new WebApplicationException("TRYING TO RESIZE S3 RESOURCE " + s3_bucket + " " + path_token + " BUT IT DOESNT SEEM TO EXIST.");
				}
				original_file.getParentFile().mkdirs();
				FileOutputStream fos = new FileOutputStream(original_file);
				fos.write(r.object.data);
				fos.flush();
				fos.close();
			}
			File preview = new File(scratch_directory, preview_key);
			preview.getParentFile().mkdirs();
			try
			{
				create_preview(original_file, preview, width, height);
			}
			catch (Exception e)
			{
				current_thumbnail_generators.remove(preview_key);
				synchronized (lock)
				{
					lock.notifyAll();
				}
				WAE(e);
			}
			String content_type = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(preview_key);
			PSS3Object pobj = new PSS3Object(new FileInputStream(preview), preview.length(), content_type, PSAWSAuthConnection.PERMISSIONS_PUBLIC_READ);
			Response pr = conn.streamingPut(s3_bucket, preview_key, pobj);
			if (pr.connection.getResponseCode() != pr.connection.HTTP_OK)
			{
				current_thumbnail_generators.remove(preview_key);
				synchronized (lock)
				{
					lock.notifyAll();
				}
				throw new WebApplicationException("PROBLEM WRITING PREVIEW TO S3 " + s3_bucket + " " + preview_key + " HTTP RESPONSE WAS " + pr.connection.getResponseCode());
			}
			current_thumbnail_generators.remove(preview_key);
			synchronized (lock)
			{
				lock.notifyAll();
			}
			INFO("EXITING GET PREVIEW URL " + base_s3_url + preview_key);
			return base_s3_url + preview_key;
		}
		catch (IOException ioe)
		{
			if (lock != null)
			{
				if (preview_key != null)
					current_thumbnail_generators.remove(preview_key);
				synchronized (lock)
				{
					lock.notifyAll();
				}
			}
			ERROR(ioe);
			throw new WebApplicationException("IO ERROR IN S3 GEN PREVIEW. SEE LOGS. " + ioe.getMessage());
		}
	}

	// dont forget to close output stream if you use this method
	public OutputStream[] getOutputStreams(String path_token, String content_type,
			long content_size) throws WebApplicationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
		Map headers = new LinkedHashMap();
		headers.put("x-amz-acl", Arrays.asList(new String[] { PSAWSAuthConnection.PERMISSIONS_PUBLIC_READ }));
		headers.put("Content-Type", Arrays.asList(new String[] { content_type }));
		try
		{
			HttpURLConnection request = conn.makeRequest("PUT", s3_bucket, Utils.urlencode(path_token), null, headers, null);
			// cant use this because flash reports wrong content size//
			request.setFixedLengthStreamingMode((int) content_size);
			// request.setChunkedStreamingMode(16384);//StreamingMode((int)content_size);
			request.setDoOutput(true);
			current_transfer_map.put(path_token, request);
			// we also provide an output stream to a local file so we
			// dont always have to go to amazon when resizing and download
			// the original
			File local_copy = new File(scratch_directory, path_token);
			local_copy.getParentFile().mkdirs();
			FileOutputStream local_out_stream = new FileOutputStream(local_copy);
			return new OutputStream[] { request.getOutputStream(), local_out_stream };
		}
		catch (Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("PROBLEM GETTING OUTPUT STREAM FOR S3 UPLOAD " + s3_bucket + " " + path_token);
		}
	}

	// dont forget to close input stream if you use this method
	public InputStream getInputStream(String path_token) throws WebApplicationException
	{
		PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
		try
		{
			HttpURLConnection request = conn.makeRequest("GET", s3_bucket, Utils.urlencode(path_token), null, null, null);
			request.setDoInput(true);
			return request.getInputStream();
		}
		catch (Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("PROBLEM GETTING INPUT STREAM FOR S3 UPLOAD " + s3_bucket + " " + path_token);
		}
	}

	public File getFile(String path_token) throws WebApplicationException
	{
		File expanded_file = new File(scratch_directory, path_token);
		expanded_file.getParentFile().mkdirs();
		InputStream is = getInputStream(path_token);
		try
		{
			FileOutputStream fs = new FileOutputStream(expanded_file);
			byte[] buf = new byte[16484];
			int l = 0;
			int total_bytes = 0;
			while ((l = is.read(buf)) != -1)
			{
				fs.write(buf, 0, l);
				total_bytes += l;
			}
			is.close();
			fs.close();
			return expanded_file;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Error: either file exists but is a directory rather than a " +
					"regular file, does not exist but cannot be created, or cannot be opened for any other reason ",e);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new WebApplicationException("IOError",e);
		}
	}

	// public File getFile(String path_token) throws WebApplicationException
	// {
	// try{
	// PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key,
	// s3_secret_key);
	// GetResponse r;
	// try {
	// r = conn.get(s3_bucket,path_token,null);
	// } catch (OutOfMemoryError e)
	// {
	// e.printStackTrace();
	// return null;
	// }
	// if(r.object == null)
	// throw new
	// WebApplicationException("TRYING TO GET FILE "+s3_bucket+" "+path_token+" BUT IT DOESNT SEEM TO EXIST.");
	//			
	// File expanded_file = new File(scratch_directory,path_token);
	// expanded_file.getParentFile().mkdirs();
	// FileOutputStream fos = new FileOutputStream(expanded_file);
	// fos.write(r.object.data);
	// fos.flush();
	// fos.close();
	// return expanded_file;
	// }catch(Exception e)
	// {
	// ERROR(e);
	// throw new
	// WebApplicationException("PROBLEM GETTING FILE FOR S3 UPLOAD "+s3_bucket+" "+path_token);
	// }
	// }
	public boolean scratchFileExists(String path_token)
	{
		File f = new File(scratch_directory, path_token);
		return f.exists();
	}

	private static void create_preview(File original, File dest, int w, int h)
			throws WebApplicationException
	{
		int type = FileInfo.getSimpleType(original);
		switch (type)
		{
		case FileInfo.SIMPLE_TYPE_IMAGE:
			create_image_preview(original, dest, w, h);
			break;
		case FileInfo.SIMPLE_TYPE_DOCUMENT:
			throw new WebApplicationException("DOCUMENT PREVIEW NOT SUPPORTED YET");
		case FileInfo.SIMPLE_TYPE_SWF:
			throw new WebApplicationException("SWF PREVIEW NOT SUPPORTED YET");
		case FileInfo.SIMPLE_TYPE_VIDEO:
			throw new WebApplicationException("VIDEO PREVIEW NOT SUPPORTED YET");
		case FileInfo.SIMPLE_TYPE_AUDIO:
			throw new WebApplicationException("AUDIO PREVIEW NOT SUPPORTED YET");
		default:
			throw new WebApplicationException("UNKNOW SIMPLE TYPE " + type);
		}
	}

	private static void create_image_preview(File original, File dest, int w, int h)
			throws WebApplicationException
	{
		ImageMagick i = new ImageMagick(original, dest);
		i.setSize(w, h);
		try
		{
			i.exec();
		}
		catch (Exception e)
		{
			throw new WebApplicationException("PROBLEM CREATING PREVIEW " + dest.getAbsolutePath(), e);
		}
	}

	private String get_preview_file_relative_path(String path_token, int width, int height)
			throws WebApplicationException
	{
		int dot_idx = path_token.lastIndexOf('.');
		if (dot_idx != -1)
			path_token = path_token.substring(0, dot_idx);
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

	private String get_preview_file_prefix(String path_token)
			throws WebApplicationException
	{
		int dot_idx = path_token.lastIndexOf('.');
		if (dot_idx != -1)
			path_token = path_token.substring(0, dot_idx);
		return path_token;
	}

	@Override
	public void beginParse(String path_token) throws WebApplicationException
	{
		// TODO Auto-generated method stub
	}

	private Map<String, HttpURLConnection> current_transfer_map = new HashMap<String, HttpURLConnection>();

	@Override
	public void endParse(String path_token) throws WebApplicationException
	{
		HttpURLConnection c = current_transfer_map.get(path_token);
		try
		{
			Response r = new Response(c);
			int code = r.connection.getResponseCode();
			// System.out.println("RESPONSE STATUS "+path_token+" IS "+code+" "+r.connection.getResponseMessage());
		}
		catch (IOException ioe)
		{
			ERROR(ioe);
		}
		current_transfer_map.remove(path_token);
	}

	private static final int CLEANER_INTERVAL = (60 * 1000) * 15;// 15
																	// minutes...run
																	// every 15
																	// minutes
	private static final int SCRATCH_DIRECTORY_THRESHHOLD = (60 * 1000) * 60;// 1
																				// hour...delete
																				// ones
																				// older
																				// than
																				// this

	private void start_scratch_cleaner()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				while (true)
				{
					try
					{
						clean_scratch_directory();
						Thread.sleep(CLEANER_INTERVAL);
					}
					catch (Exception e)
					{
						ERROR(e);
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	private Thread delete_consumer_thread;

	private void start_s3_delete_consumer()
	{
		delete_consumer_thread = new Thread()
		{
			public void run()
			{
				PSAWSAuthConnection conn = new PSAWSAuthConnection(s3_api_key, s3_secret_key);
				while (true)
				{
					try
					{
						byte[] delete_key_b = store.dequeue(S3_DELETE_QUEUE_NAME, true, false);
						if (delete_key_b == null)
						{
							synchronized (DELETE_QUEUE_LOCK)
							{
								DELETE_QUEUE_LOCK.wait();
							}
						}
						else
						{
							String delete_key = new String(delete_key_b);
							deleteFromS3(conn, delete_key);
						}
					}
					catch (Exception e)
					{
						ERROR(e);
					}
				}
			}
		};
		delete_consumer_thread.setDaemon(true);
		delete_consumer_thread.start();
	}

	private void clean_scratch_directory()
	{
		do_clean_directory(new File(scratch_directory));
	}

	private void do_clean_directory(File dir)
	{
		long now = new Date().getTime();
		File[] files = dir.listFiles();
		sortFilesAsc(files);
		for (int i = 0; i < files.length; i++)
		{
			File f = files[i];
			if (f.isDirectory())
			{
				do_clean_directory(f);
				if (f.listFiles().length == 0)
					f.delete();
			}
			else
			{
				if (f.lastModified() + SCRATCH_DIRECTORY_THRESHHOLD < now)
					f.delete();
				else
					// files are sorted with oldest first so we can break here//
					break;
			}
		}
	}

	public static void sortFilesAsc(File[] files)
	{
		Arrays.sort(files, new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				File f1 = (File) o1;
				File f2 = (File) o2;
				if (f1.lastModified() > f2.lastModified())
				{
					return +1;
				}
				else if (((File) o1).lastModified() < ((File) o2).lastModified())
				{
					return -1;
				}
				else
				{
					return 0;
				}
			}
		});
	}
}
