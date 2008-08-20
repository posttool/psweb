package com.pagesociety.web.bean;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The bean reflects the public getters and setters of a class. It can refer to
 * these methods by their 'bean property name'. The bean property name is
 * determined by the method name without 'get' or 'set'. The first letter of the
 * bean property name is always forced to lower case. So if the bean has a
 * method getFirstName, its bean property name is firstName.
 * 
 * The bean can be used to get and set properties of an instance of the matching
 * class name. Ex:
 * 
 * <code>
 * Bean bean = new Bean(Author.class);
 * bean.setProperty(author, "firstName", "Daya");
 * String firstName = (String) bean.getProperty(author, "firstName");
 * </code>
 * 
 * @see com.pagesociety.web.bean.BeanRegistry
 */
public class Bean
{
	private final Class<?> _java_class;
	private final Map<String, Method> readableProperties = new HashMap<String, Method>();
	private final Map<String, Method> writableProperties = new HashMap<String, Method>();
	private final String[] omittedProperties;

	public Bean(Class<?> javaClass)
	{
		this(javaClass, null);
	}

	public Bean(Class<?> javaClass, String[] omittedProperties)
	{
		_java_class = javaClass;
		this.omittedProperties = omittedProperties == null ? new String[0] : omittedProperties;
		init();
	}

	String[] getOmittedProperties()
	{
		return omittedProperties;
	}

	private void init()
	{
		Method[] methods = _java_class.getDeclaredMethods();
		for (Method m : methods)
		{
			if (m.getName().length() < 4)
				continue;
			String propertyName = getPropNameFromEr(m.getName());
			if ((m.getName().startsWith("get") || m.getName().startsWith("is")) && !isOmitted(propertyName) && m.getParameterTypes().length == 0)
			{
				readableProperties.put(propertyName, m);
			}
			if (m.getName().startsWith("set") && !isOmitted(propertyName) && m.getParameterTypes().length == 1)
			{
				writableProperties.put(propertyName, m);
			}
		}
	}

	private boolean isOmitted(String propertyName)
	{
		for (String name : omittedProperties)
			if (propertyName.equals(name))
				return true;
		return false;
	}

	private String getPropNameFromEr(String name)
	{
		int s;
		if (name.startsWith("is"))
			s = 0;
		else
			s = 3;
		return name.substring(s, s + 1).toLowerCase() + name.substring(s + 1);
	}

	public Object newJavaInstance()
	{
		try
		{
			return _java_class.newInstance();
		}
		catch (Exception e)
		{
			String msg = "Bean ERROR - NO DEFAULT CONSTRUCTOR FOR "+_java_class;
			System.err.println(msg);
			throw new RuntimeException(msg);
		}
	}

	public Object getProperty(Object obj, String beanName)
	{
		Method property = readableProperties.get(beanName);
		if (property == null)
		{
			String message = "No getter for property " + beanName + " in instance " + obj;
			throw new RuntimeException(message);
		}
		try
		{
			return property.invoke(obj);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void setProperty(Object obj, String beanName, Object value)
	{
		Method property = writableProperties.get(beanName);
		if (property == null)
		{
			String message = "No setter for property " + beanName + " in instance " + obj;
			System.err.println(message);
			return;
		}
		try
		{
			property.invoke(obj, value);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public String[] getWritablePropertyNames()
	{
		Set<String> prop_names = writableProperties.keySet();
		return prop_names.toArray(new String[prop_names.size()]);
	}

	public Class<?> getJavaClass()
	{
		return _java_class;
	}

	public String[] getReadablePropertyNames()
	{
		Set<String> prop_names = readableProperties.keySet();
		return prop_names.toArray(new String[prop_names.size()]);
	}
}
