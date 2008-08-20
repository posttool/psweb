package com.pagesociety.util;

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class SimpleTemplate {
	private static final Logger logger = Logger.getLogger(SimpleTemplate.class);
	/**
	 * the basic pattern to match is like this: ${name}
	 */
	static Pattern elPattern = Pattern.compile("\\$\\{.*?\\}");

	/**
	 * takes an input stream & hashmap, and substitutes matched patterns with
	 * values from the hashmap
	 *
	 * @param is
	 * @param values
	 * @return
	 */
	public static String templatize(InputStream is, Map<String, String> values) {
		String text = Text.read(is);
		if (text == null) {
			logger.warn("No input stream to process");
			return "";
		}
		return templatize(text, values);
	}

	/**
	 * takes a string and hashmap, and substitutes matched patterns with values
	 * from the hashmap
	 *
	 * @param template
	 * @param values
	 * @return
	 */
	public static String templatize(String template, Map<String, String> values) {
		if (template == null || values == null) {
			logger.warn("Null values for templatize");
			return "";
		}
		StringBuffer sb = new StringBuffer();
		Matcher m = elPattern.matcher(template);
		while (m.find()) {
			String n = m.group();
			String v = values.get(n.substring(2, n.length() - 1));
			if (v == null)
				v = "null";
			m.appendReplacement(sb, v);
		}
		m.appendTail(sb);
		return sb.toString();
	}

//	public static String templatize(String template, PageContext pageContext) {
//		if (template == null || pageContext == null) {
//			logger.warn("Null values for templatize");
//			return "";
//		}
//		StringBuffer sb = new StringBuffer();
//		Matcher em = elPattern.matcher(template);
//		while (em.find()) {
//			String n = em.group();
//			em.appendReplacement(sb, evaluateString(pageContext, n));
//		}
//		em.appendTail(sb);
//		//
//		return sb.toString();
//	}

//	private static String evaluateString(PageContext pageContext, String el) {
//		return (String) evaluate(pageContext, el, String.class);
//	}
//
//	private static Object evaluate(PageContext pageContext, String el, Class<?> clazz) {
//		ExpressionEvaluator ee = pageContext.getExpressionEvaluator();
//		try {
//			return ee.evaluate(el, clazz, pageContext.getVariableResolver(), null);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}
}
