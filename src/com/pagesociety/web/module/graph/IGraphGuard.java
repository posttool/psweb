package com.pagesociety.web.module.graph;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface IGraphGuard 
{
	public boolean canCreateGraph(Entity user) throws PersistenceException;
	public boolean canUpdateGraph(Entity user, Entity graph, String name) throws PersistenceException;
	public boolean canDeleteGraph(Entity user, Entity graph)throws PersistenceException;
	public boolean canGetGraph(Entity user, Entity graph) throws PersistenceException;
	
	public boolean canCreateVertex(Entity user,Entity graph,String vertice_class,String vertice_id, Entity data) throws PersistenceException;
	public boolean canUpdateVertex(Entity user,Entity vertex,String vertice_class,String vertice_id,Entity data)throws PersistenceException;
	public boolean canDeleteVertex(Entity user, Entity vertex) throws PersistenceException;
	public boolean canGetGraphVertices(Entity user, Entity graph) throws PersistenceException;	
	public boolean canGetGraphVerticesByClass(Entity user, Entity graph,String vertice_classname) throws PersistenceException;	
	public boolean canGetGraphVertexById(Entity user, String vertice_id)throws PersistenceException;
	public boolean canFillVerticeData(Entity user, Entity vertex,int data_ref_fill_depth) throws PersistenceException;
	
	public boolean canCreateEdge(Entity user, Entity graph) throws PersistenceException;
	public boolean canUpdateEdge(Entity user, Entity graph,Entity edge) throws PersistenceException;
	public boolean canDeleteEdge(Entity user, Entity graph,Entity edge) throws PersistenceException;
	
	public boolean canCloneGraph(Entity user, Entity tree)throws PersistenceException;
	public boolean canGetGraphsForUser(Entity user, Entity user2)throws PersistenceException;//user 1 wants to get user 2's trees

	

	
}
