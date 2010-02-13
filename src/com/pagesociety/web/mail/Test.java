package com.pagesociety.web.mail;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.Store;

public class Test
{
	static String host = "mail.tabithasoren.com";
	static String username = "postmaster";
	static String password = "58a8419f";

	public static void main(String[] args)
	{
		try
		{
			// Create empty properties
			Properties props = new Properties();
			// Get session
			Session session = Session.getDefaultInstance(props, null);
			session.setDebug(true);
			Provider[] ps = session.getProviders();
			for (Provider p : ps)
			{
				System.out.println(">" + p.getProtocol() + " " + p.getClassName());
			}
			String protocol = "imaps";
			System.out.println("CONNECTING WITH " + protocol);
			// Get the store
			Store store = session.getStore(protocol);
			store.connect(host, 143, username, password);
			// Get folder
			Folder folder = store.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			// Get directory
			Message message[] = folder.getMessages();
			for (int i = 0, n = message.length; i < n; i++)
			{
				System.out.println(i + ": " + message[i].getFrom()[0] + "\t" + message[i].getSubject());
			}
			// Close connection
			folder.close(false);
			store.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
