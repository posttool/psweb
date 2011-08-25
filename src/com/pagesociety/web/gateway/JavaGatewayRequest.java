package com.pagesociety.web.gateway;

import java.io.Serializable;

	public class JavaGatewayRequest implements Serializable
	{
		public long 	request_time;
		public String 	routing_id;
		public Object[] arguments;
	}
