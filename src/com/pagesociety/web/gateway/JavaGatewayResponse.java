package com.pagesociety.web.gateway;

import java.io.Serializable;


	public class JavaGatewayResponse implements Serializable
	{
		public long 	request_time;
		public long 	response_time;
		public String 	routing_id;
		public Object 	value;
	}