package com.pagesociety.web.test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.gateway.GatewayConstants;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.UploadProgressInfo;

public class TestModule extends Module
{
	private static boolean _test_1_complete;
	public static final String UPLOAD_FOR_USER_KEY = "upload";

	public TestModule()
	{
	}

	@Export
	public static String test1(UserApplicationContext user_context)
	{
		_test_1_complete = true;
		return "Test 1 Complete! ";
	}

	@Export
	public boolean isTest1Complete(UserApplicationContext user_context)
	{
		return _test_1_complete;
	}

	@Export
	public String undoTest1(UserApplicationContext user_context)
	{
		_test_1_complete = false;
		return "OK";
	}

	@Export
	public String test2(UserApplicationContext user_context, int id)
	{
		return "YOU PASSED THIS ID: " + id;
	}

	@Export
	public String test3(UserApplicationContext user_context, String a, String b, Date c,
			int d)
	{
		return "YOU PASSED THESE: " + a + " " + b + " " + c + " " + d;
	}

	@Export
	public String[] test4(UserApplicationContext user_context, int a, Date d,
			List<Float> f)
	{
		return new String[] { "Test 4 a=" + a, "Date=" + d, "Floats=" + f };
	}

	@Export
	public String formTest1(UserApplicationContext user_context, String first_name,
			String last_name, String about_me, String thingy)
	{
		return "FORMTEST1 OK " + first_name + " " + last_name + " " + thingy;
	}

	@Export
	public String getSessionId(UserApplicationContext user_context)
	{
		return GatewayConstants.SESSION_ID_KEY + "=" + user_context.getId();
	}

	@Export
	public Map<String, Object> formTest2(UserApplicationContext user_context,
			MultipartForm upload)
	{
		MultipartForm current_upload = (MultipartForm) user_context.getProperty(UPLOAD_FOR_USER_KEY);
		if (current_upload != null && !current_upload.isComplete())
			throw new RuntimeException("UPLOAD IS ALREADY IN PROGRESS");
		//
		user_context.setProperty(UPLOAD_FOR_USER_KEY, upload);
		Map<String, Object> result = new HashMap<String, Object>();
		try
		{
			//upload.parse();
		}
		catch (Exception e)
		{
			user_context.removeProperty(UPLOAD_FOR_USER_KEY);
			result.put("error", e.getMessage());
			return result;
		}
		//upload.getUploadProgress().get(0).setCompletionObject(result);
		//result.put("picture_path", upload.getFile("picture").getAbsolutePath());
		result.put("first_name", upload.getStringArrayParameter("first_name"));
		result.put("ints", upload.getIntArrayParameter("ints"));
		result.put("last_name", upload.getStringParameter("last_name"));
		result.put("thingy", upload.getStringParameter("thingy"));
		result.put("cb", upload.getStringArrayParameter("checkbox-test"));
		result.put("rb", upload.getStringParameter("radio-test"));
		return result;
	}

	@Export
	public UploadProgressInfo getUploadInfo(UserApplicationContext user_context)
	{
		MultipartForm current_upload = (MultipartForm) user_context.getProperty(UPLOAD_FOR_USER_KEY);
	//	if (current_upload == null || current_upload.getUploadProgress().isEmpty())
	//		return null;
	//	return current_upload.getUploadProgress().get(0);
		return null;
	}

	@Export
	public String formTest3(UserApplicationContext user_context, String first_name,
			String last_name, int age, List<String> thingy)
	{
		String s = "FORMTEST1 OK " + first_name + " " + last_name + " " + age + " " + thingy;
		System.out.println("TestModule/formTest3" + s);
		return s;
	}

	// amf tests
	@Export
	public Object processHash(UserApplicationContext user_context, Map<String, Object> o)
	{
		System.out.println(o);
		return o;
	}

	@Export
	@SuppressWarnings("unchecked")
	public Object processArray(UserApplicationContext user_context, List a)
	{
		System.out.println(a);
		return a;
	}

	@Export
	public Entity getEntity(UserApplicationContext user_context)
	{
		Entity e = Entity.createInstance();
		e.setAttribute("X", "yyy");
		e.setAttribute("A", 123);
		return e;
	}
}
