package com.pagesociety.web.bean;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * The bean factory maintains a list of beans that can be retreived by a name
 * (alias) key or a Class key. Beans are registered as a java class and
 * optionally a list of omitted properties. Currently, the alias is restricted
 * to the canonical name of the class.
 * 
 * @see Bean
 * 
 */
public class BeanRegistry
{
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(BeanRegistry.class);
	private static Map<String, Bean> BEANS = new HashMap<String, Bean>();
	private static Map<Class<?>, Bean> BEANS_BY_CLASS = new HashMap<Class<?>, Bean>();

	public static void register(Class<?> javaClass)
	{
		register(javaClass, null);
	}

	public static void register(Class<?> javaClass, String[] omittedProperties)
	{
		Bean bcm = new Bean(javaClass, omittedProperties);
		BEANS.put(javaClass.getCanonicalName(), bcm);
		BEANS_BY_CLASS.put(javaClass, bcm);
		logger.info("BeanRegistry " + javaClass.getCanonicalName());
		StringBuffer b = new StringBuffer();
		b.append("/ getters / ");
		for (String reader : bcm.getReadablePropertyNames())
			b.append(reader + " ");
		b.append(" / setters / ");
		for (String writer : bcm.getWritablePropertyNames())
			b.append(writer + " ");
		logger.info(b);
	}

	public static Bean getBeanByName(String alias)
	{
		return BEANS.get(alias);
	}

	public static Bean getBeanByClass(Class<?> c)
	{
		return BEANS_BY_CLASS.get(c);
	}
}
