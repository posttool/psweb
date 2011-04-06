package com.pagesociety.web.module.resource;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.exception.WebApplicationException;

public interface IResourcePathProvider 
{

	public String getPathToken(Entity user,String filename) throws WebApplicationException;
	public void delete(String path_token) throws WebApplicationException;
	public void deletePreviews(String path_token) throws WebApplicationException;
	public List<String> listPreviews(String path_token) throws WebApplicationException;
	public String getUrl(String path_token) throws WebApplicationException;
	public String getBaseUrl() throws WebApplicationException;
	public String getPreviewUrl(String path_token,int width,int height) throws WebApplicationException;
	public String getPreviewUrl(String path_token,Map<String,String> options) throws WebApplicationException;
	public OutputStream[] getOutputStreams(String path_token,String content_type,long content_length) throws WebApplicationException;
	public InputStream getInputStream(String path_token) throws WebApplicationException;
	public File getFile(String path_token) throws WebApplicationException;
	public void beginParse(String path_token) throws WebApplicationException;
	public void endParse(String path_token) throws WebApplicationException;
	
	
}
