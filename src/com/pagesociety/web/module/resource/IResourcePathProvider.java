package com.pagesociety.web.module.resource;

import java.io.File;
import com.pagesociety.web.WebApplicationException;

public interface IResourcePathProvider 
{
	public String/*path token*/ save(File f) throws WebApplicationException;
	public void delete(String path_token) throws WebApplicationException;
	public String getUrl(String path_token) throws WebApplicationException;
	public String getPreviewUrl(String path_token,int width,int height) throws WebApplicationException;
	public File getFile(String path_token) throws WebApplicationException;
	
}