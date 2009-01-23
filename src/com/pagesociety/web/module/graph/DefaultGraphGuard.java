package com.pagesociety.web.module.graph;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.PermissionsModule;


public class DefaultGraphGuard extends PermissionsModule implements IGraphGuard
{

	public boolean canCreateGraph(Entity user) throws PersistenceException
	{
		return false;
	}
	public boolean canUpdateGraph(Entity user, Entity graph, String name) throws PersistenceException
	{
		return false;
	}
	public boolean canDeleteGraph(Entity user, Entity graph)throws PersistenceException
	{
		return false;
	}
	public boolean canGetGraph(Entity user, Entity graph) throws PersistenceException
	{
		return false;
	}
	public boolean canCreateVertex(Entity user,Entity graph,String vertice_class,String vertice_id, Entity data) throws PersistenceException
	{
		return false;
	}
	public boolean canUpdateVertex(Entity user,Entity vertex,String vertice_class,String vertice_id,Entity data)throws PersistenceException
	{
		return false;
	}
	public boolean canDeleteVertex(Entity user, Entity vertex) throws PersistenceException
	{
		return false;
	}
	public boolean canGetGraphVertices(Entity user, Entity graph) throws PersistenceException
	{
		return false;
	}
	public boolean canGetGraphVerticesByClass(Entity user, Entity graph,String vertice_classname) throws PersistenceException
	{
		return false;
	}
	public boolean canGetGraphVertexById(Entity user, String vertice_id)throws PersistenceException
	{
		return false;
	}
	public boolean canFillVerticeData(Entity user, Entity vertex,int data_ref_fill_depth) throws PersistenceException
	{
		return false;
	}
	
	public boolean canCreateEdge(Entity user, Entity graph) throws PersistenceException
	{
		return false;
	}
	public boolean canUpdateEdge(Entity user, Entity graph,Entity edge) throws PersistenceException
	{
		return false;
	}
	public boolean canDeleteEdge(Entity user, Entity graph,Entity edge) throws PersistenceException
	{
		return false;
	}
	
	public boolean canCloneGraph(Entity user, Entity tree)throws PersistenceException
	{
		return false;
	}
	public boolean canGetGraphsForUser(Entity user, Entity user2)throws PersistenceException
	{
		return false;
	}

	
}
