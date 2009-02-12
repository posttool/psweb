package com.pagesociety.web.module.tree;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;

public interface ITreeGuard 
{

	public boolean canCreateTree(Entity user) throws PersistenceException;
	public boolean canUpdateTree(Entity user, Entity tree, String name,Entity root_node) throws PersistenceException;
	public boolean canCreateTreeNode(Entity user,Entity parent_node,String node_class,String node_id, Entity data) throws PersistenceException;
	public boolean canUpdateTreeNode(Entity user,Entity entity_node,String node_class,String node_id,Entity data)throws PersistenceException;
	public boolean canReparentTreeNode(Entity user,Entity entity_node,Entity new_parent_node)throws PersistenceException;
	public boolean canDeleteTreeNode(Entity user, Entity tree_node)throws PersistenceException;
	public boolean canDeleteTree(Entity user, Entity tree)throws PersistenceException;
	public boolean canCloneTree(Entity user, Entity tree)throws PersistenceException;
	public boolean canGetTreesForUser(Entity user, Entity user2)throws PersistenceException;//user 1 wants to get user 2's trees
	public boolean canGetTree(Entity user, Entity tree)throws PersistenceException;
	public boolean canFillNode(Entity user, Entity node,int subtree_fill_depth,int data_fill_depth)throws PersistenceException;
	public boolean canGetTreeNodesByClass(Entity user, String node_classname)throws PersistenceException;
	public boolean canGetTreeNodeById(Entity user, String node_id)throws PersistenceException;
	public boolean canGetAncestors(Entity user, Entity tree_node) throws PersistenceException;
	
	
}
