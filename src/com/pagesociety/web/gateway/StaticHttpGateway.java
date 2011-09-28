package com.pagesociety.web.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StaticHttpGateway
{
	public StaticHttpGateway()
	{
	}

	private static final int SERVE_BLOCK_SIZE = 1024;
	private static final String REQUEST_DATE_HEADER_IF_MOD_SINCE = "If-Modified-Since";
	private static final String RESPONSE_DATE_HEADER_LAST_MOD = "Last-Modified";

	
	//
	//TODO add headers
	// response.setHeader("Expires", "0");
	// response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
	// response.setHeader("Pragma", "public");
	// see http://www.mnot.net/cache_docs/
	//
	// add this to make it download
	// response.setHeader("Content-disposition", "attachment; filename=" + f.getName());
	// 

	public static void serveFile(File file, String mimeType, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException
	{
		if (!file.exists())
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}	
		if (file.isDirectory())
		{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
			
		long lastMod   = file.lastModified() / 1000 * 1000;
		long ifModSinc = request.getDateHeader(REQUEST_DATE_HEADER_IF_MOD_SINCE);
		//System.out.println("IF MOD SINCE "+new Date(ifModSinc));
		//System.out.println(file.getAbsolutePath()+" LAST MOD "+new Date(lastMod));
		if (lastMod <= ifModSinc)
		{
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		
		response.setContentType(mimeType);
		response.setContentLength((int) file.length());
		response.setDateHeader(RESPONSE_DATE_HEADER_LAST_MOD, lastMod);
		FileInputStream in = new FileInputStream(file);
		OutputStream out = response.getOutputStream();
		byte[] buf = new byte[SERVE_BLOCK_SIZE];
		int count = 0;
		while ((count = in.read(buf)) >= 0)
		{
			out.write(buf, 0, count);
		}
		in.close();
		out.close();
	}
}
