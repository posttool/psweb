package com.pagesociety.web.module.S3;
import java.io.InputStream;
import java.util.Map;

import com.pagesociety.web.module.S3.amazon.S3Object;


public class PSS3Object extends S3Object
{

	private InputStream is = null;
	private long size	   = 0;
	private String content_type;
	private String permissions;
	public PSS3Object(InputStream in,long size,String content_type,String permissions)
	{
		super(null,null);
		this.is = in;
		this.size = size;
		this.content_type = content_type;
		this.permissions = permissions;
	}
	
	public InputStream getInputStream()
	{
		return is;
	}
	
	public long getSize()
	{
		return size;
	}
	
	public String getContentType()
	{
		return content_type;
	}
	
	public String getPermissions()
	{
		return permissions;
	}
}
