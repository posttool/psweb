package com.pagesociety.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XML
{
	public static final Document newDocument()
	{
		Document document = null;
		try
		{
			// create base document using javax.xml.parser
			DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
			dFactory.setNamespaceAware(true);
			DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
			document = dBuilder.newDocument();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return document;
	}

	public static final void print(Document d)
	{
		print(d, new PrintWriter(System.out), true);
	}

	public static final void print(Document d, Writer out)
	{
		print(d, out, false);
	}

	public static final void print(Document d, Writer out, boolean indent)
	{
		try
		{
			String indentYN = indent ? "yes" : "no";
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty("method", "xml");
			transformer.setOutputProperty("indent", indentYN);
			DOMSource source = new DOMSource(d);
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
			out.flush();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static final void print(Element e)
	{
		try
		{
			System.out.println("---------------------------------------------------------");
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			DOMSource source = new DOMSource(e);
			StreamResult result = new StreamResult(System.out);
			transformer.transform(source, result);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static final Document read(File file) throws ParserConfigurationException,
			SAXException, IOException
	{
		//
		Document document = null;
		// Instantiate a DocumentBuilderFactory.
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		dFactory.setNamespaceAware(true);
		dFactory.setValidating(false);
		// file input
		DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
		try
		{
			document = dBuilder.parse(file);
		}
		catch (java.net.UnknownHostException u)
		{
			System.out.println("Document.read ERROR CANT VALIDATE " + file + " " + u.getMessage());
		}
		// new InputSource(new StringReader("<XML/>"))
		return document;
	}

	public static final Document read(InputStream is)
			throws ParserConfigurationException, SAXException, IOException
	{
		//
		Document document = null;
		// Instantiate a DocumentBuilderFactory.
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		dFactory.setNamespaceAware(true);
		// dFactory.setValidating(false);
		// file input
		DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
		try
		{
			document = dBuilder.parse(is);
		}
		catch (java.net.UnknownHostException uhe)
		{
			// uhe.printStackTrace();
			System.out.println("Document.read Unknown host " + uhe.getMessage() + " " + is);
		}
		// new InputSource(new StringReader("<XML/>"))
		return document;
	}

	public static final Document read(String path) throws Exception
	{
		//
		File file = new File(path);
		return read(file);
	}

	public static final Document parse(String s) throws Exception
	{
		//
		Document document = null;
		// Instantiate a DocumentBuilderFactory.
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		dFactory.setNamespaceAware(true);
		// dFactory.setValidating(false);
		// file input
		DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
		document = dBuilder.parse(new InputSource(new StringReader(s)));
		//
		return document;
	}
}