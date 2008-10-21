package com.pagesociety.web.module.tree;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.PermissionsModule;


public class DefaultTreeGuard extends PermissionsModule implements ITreeGuard
{

	public boolean canCreateTree(Entity user) {return false;}
	public boolean canUpdateTree(Entity user, Entity tree, String name,Entity root_node) {return false;}
	public boolean canCreateTreeNode(Entity user,Entity parent_node, Entity data) {return false;}
	public boolean canUpdateTreeNodeData(Entity user,Entity entity_node,Entity data) {return false;}
	public boolean canReparentTreeNode(Entity user,Entity entity_node,Entity new_parent_node) {return false;}
	public boolean canDeleteTreeNode(Entity user, Entity tree_node) {return false;}
	public boolean canDeleteTree(Entity user, Entity tree) {return false;}
	public boolean canGetTreesForUser(Entity user, Entity user2){return false;}//user 1 wants to get user 2's trees
	public boolean canGetTree(Entity user, Entity tree) {return false;}
	public boolean canFillNode(Entity user, Entity node,int subtree_fill_depth,int data_fill_depth){ return false;}
	

}
