package com.pagesociety.web.module.tree;

import com.pagesociety.persistence.Entity;

public interface ITreeGuard 
{

	public boolean canCreateTree(Entity user);
	public boolean canUpdateTree(Entity user, Entity tree, String name,Entity root_node);
	public boolean canCreateTreeNode(Entity user,Entity parent_node, Entity data);
	public boolean canUpdateTreeNodeData(Entity user,Entity entity_node,Entity data);
	public boolean canReparentTreeNode(Entity user,Entity entity_node,Entity new_parent_node);
	public boolean canDeleteTreeNode(Entity user, Entity tree_node);
	public boolean canDeleteTree(Entity user, Entity tree);
	public boolean canGetTreesForUser(Entity user, Entity user2);//user 1 wants to get user 2's trees
	public boolean canGetTree(Entity user, Entity tree);
	public boolean canFillNode(Entity user, Entity node,int subtree_fill_depth,int data_fill_depth);
	
}
