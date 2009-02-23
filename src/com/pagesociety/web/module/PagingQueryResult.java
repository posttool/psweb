package com.pagesociety.web.module;

import java.util.List;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.QueryResult;

public class PagingQueryResult 
{
	private List<Entity> _entities;
	private int 		 _size;
	private int 		 _offset;
	private int 		 _total_count;
	private int 		 _page_size;
	private double		 _execution_time;
	private double		 _rps;

	public PagingQueryResult(QueryResult result, int total_count, int offset, int page_size)
	{
		this(result.getEntities(),total_count,offset,page_size);
	}
	
	public PagingQueryResult(List<Entity> entities, int total_count, int offset, int page_size)
	{
		_entities 		= entities;
		_size 		 	= entities.size();
		_total_count 	= total_count;
		_offset 		= offset;
		_page_size 		= page_size;
	}
	
	public List<Entity> getEntities()
	{
		return _entities;
	}
	
	public int size()
	{
		return _size;
	}
	
	public int getOffset()
	{
		return _offset;
	}
	
	public int getTotalCount()
	{
		return _total_count;
	}
	
	public int getPageSize()
	{
		return _page_size;
	}

	public void setExecutionTime(double t)
	{
		_execution_time = t;
	}
	
	public double getExecutionTime()
	{
		return _execution_time;
	}
	
	public void setResultsPerSecond(double t)
	{
		_execution_time = t;
	}
	
	public double getResultsPerSdecond()
	{
		return _execution_time;
	}
}
