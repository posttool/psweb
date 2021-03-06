package com.pagesociety.web;

import java.util.Iterator;
import java.util.Set;
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
	private boolean reaping;
	private Thread session_reaper_thread;

	private void start_reaper()
	{
		session_reaper_thread = new Thread("SessionManager")
		{
			public void run()
			{
				reaping = true;
				while (reaping)
				{
					long now = System.currentTimeMillis();
					Iterator<String> iter = _session_map.keySet().iterator();
					while (iter.hasNext())
					{
						ExpiringObject eo = _session_map.get(iter.next());
						if(eo.time + sessionTimeoutPeriod < now)
						{
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
		session_reaper_thread.setDaemon(true);
		session_reaper_thread.start();
	}

	public void destroy()
	{
		reaping = false;
		session_reaper_thread.interrupt();
	}

	public class ExpiringObject
	{
		public Object object;
		public long time;

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
	/*look at session object without setting its time */
	public ExpiringObject inspect(String sess_id)
	{
		ExpiringObject eo = _session_map.get(sess_id);
		if (eo == null)
			return null;
		return eo;
	}

	public Set<String> keySet()
	{
		return _session_map.keySet();
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
