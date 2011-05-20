package com.pagesociety.web.module.email;

import java.util.Map;

import com.pagesociety.web.exception.WebApplicationException;


public interface IEmailModule
{
	public void sendEmail(String from,String[] to, String subject,String template_name,Map<String,Object>template_data) throws WebApplicationException;
	public void sendAdminNotification(String[] to, String subject,String message) throws WebApplicationException;
}
