package com.pagesociety.web.test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;

import com.pagesociety.web.bean.Beans;
import com.pagesociety.web.gateway.JavaAMFClient;

public class JavaAMFClientTest
{
	public static void main(String[] args) throws HttpException, IOException
	{
		Beans.initDefault();
		JavaAMFClient jac = new JavaAMFClient("http://localhost:8081/PSWeb/amf_gateway");
		//
		Object response = jac.execute("TestModule/test1");
		System.out.println("test1: " + response);
		//
		response = jac.execute("TestModule/test2", 33);
		System.out.println("test2: " + response);
		//
		response = jac.execute("TestModule/test3", "s1", "s2", new Date(), 99);
		System.out.println("test3: " + response);
		//
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("xxx", 3232);
		m.put("YYY", new Date());
		m.put("float", 32.3f);
		m.put("string[]", new String[] { "a", "b", "c" });
		Map<String, Object> m2 = new HashMap<String, Object>();
		m2.put("d", 1);
		m2.put("dd", 2);
		m2.put("m", m);
		// m.put("map", m2);
		response = jac.execute("TestModule/processHash", m);
		System.out.println("hashit: " + response);
		//
		response = jac.execute("TestModule/getEntity");
		System.out.println("entity: " + response);
	}
}
