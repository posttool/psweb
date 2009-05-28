package com.pagesociety.web;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionManager
{
	private ConcurrentMap<String, ExpiringObject> _session_map;
	private long sessionTimeoutPeriod = 1000 * 60 * 30;

	public SessionManager()
	{
		_session_map = new ConcurrentHashMap<String, ExpiringObject>();
		start_reaper();
	}
	//5 minutes
	private static long REAP_INTERVAL = 1000 * 60 * 5;

	private void start_reaper()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				while (true)
				{
					long now = System.currentTimeMillis();
					Iterator<String> iter = _session_map.keySet().iterator();
					while (iter.hasNext())
					{
						ExpiringObject eo = _session_map.get(iter.next());
						if(eo.time + sessionTimeoutPeriod < now)
						{
							System.out.println("SESSION REAPER IS REMOVING "+eo);
							iter.remove();
						}
					}
					try
					{
						Thread.sleep(REAP_INTERVAL);
					}
					catch (InterruptedException ie)
					{
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	private class ExpiringObject
	{
		public Object object;
		long time;

		public ExpiringObject(Object o, long t)
		{
			object = o;
			time = t;
		}
	}

	public Object get(String sess_id)
	{
		ExpiringObject eo = _session_map.get(sess_id);
		if (eo == null)
			return null;
		eo.time = System.currentTimeMillis();
		return eo.object;
	}

	public void put(String sess_id, Object sess_obj)
	{
		ExpiringObject eo = new ExpiringObject(sess_obj, System.currentTimeMillis());
		_session_map.put(sess_id, eo);
	}

	public Object remove(String sess_id)
	{
		ExpiringObject eo = _session_map.remove(sess_id);
		if (eo == null)
			return null;
		return eo.object;
	}

	public void setTimeout(long timeout)
	{
		sessionTimeoutPeriod = timeout;
	}
}
