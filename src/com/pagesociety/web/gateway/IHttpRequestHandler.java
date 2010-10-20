package com.pagesociety.web.gateway;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.WebApplicationException;

public interface IHttpRequestHandler 
{
	public boolean handleRequest(UserApplicationContext user_context,HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException, WebApplicationException;
}
