package com.pagesociety.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionNameSpaceManager
{
	private ConcurrentMap<String, SessionManager> _name_spaced_session_maps;
	private SessionManager _next_session_manager;

	public SessionNameSpaceManager()
	{
		_name_spaced_session_maps = new ConcurrentHashMap<String, SessionManager>();
		_next_session_manager = new SessionManager();
	}
	
	public void destroy()
	{
		for (SessionManager s : _name_spaced_session_maps.values())
			s.destroy();
		_next_session_manager.destroy();
	}

	public SessionManager get(String name_space)
	{
		return get_name_spaced_session_map(name_space);
	}

	private SessionManager get_name_spaced_session_map(String name_space)
	{
		if (_name_spaced_session_maps.putIfAbsent(name_space, _next_session_manager) == null)
		{
			_next_session_manager = new SessionManager();
		}
		return _name_spaced_session_maps.get(name_space);
	}

	public SessionManager remove(String name_space)
	{
		return _name_spaced_session_maps.remove(name_space);
	}
}
