package com.pagesociety.web.module.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.EntityRelationshipDefinition;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.resource.DefaultResourceGuard;
import com.pagesociety.web.module.resource.IResourceGuard;
import com.pagesociety.web.module.user.UserModule;


public class TreeModule extends WebStoreModule 
{
	private static final String SLOT_GUARD = "tree-guard";
	private ITreeGuard guard;

	private static final String ROOT_NODE_CLASS 	 = "root_node_class";
	private static final String ROOT_NODE_ID		 = "root_node_id";
	
	private static final String NODE_UNDEFINED_ID    = "undefined";
	private static final String NODE_UNDEFINED_CLASS = "undefined";

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		guard = (ITreeGuard)getSlot(SLOT_GUARD);
	}
	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_GUARD,IResourceGuard.class,false,DefaultResourceGuard.class);
	}	
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////

	@Export
	public Entity CreateTree(UserApplicationContext uctx,String name) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canCreateTree(user));
		return createTree(user,name);
	}
	
	public Entity createTree(Entity user,String tree_name) throws PersistenceException
	{
		Entity root_node = createTreeNode(user, null,ROOT_NODE_ID,ROOT_NODE_CLASS,null);
		Entity tree =  NEW(TREE_ENTITY,
						user,
						TREE_FIELD_NAME,tree_name,
						TREE_FIELD_ROOT_NODE,root_node);
		UPDATE(root_node,
				TREE_NODE_FIELD_TREE,tree);
		
		return tree;
	}
	
	@Export 
	public Entity UpdateTree(UserApplicationContext uctx,long tree_id,String name,Entity root_node) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree = GET(TREE_ENTITY,tree_id);
		GUARD(guard.canUpdateTree(user,tree,name,root_node));
		return updateTree(tree,name,root_node);
	}
	
	Entity updateTree(Entity tree,String name,Entity root_node) throws WebApplicationException,PersistenceException
	{
		return UPDATE(tree, 
						TREE_FIELD_NAME,name,
						TREE_FIELD_ROOT_NODE,root_node);
	}
	
	@Export
	public Entity CreateTreeNode(UserApplicationContext uctx,long tree_id,long parent_node_id,Entity data) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity parent_node = GET(TREE_NODE_ENTITY,parent_node_id);
		GUARD(guard.canCreateTreeNode(user,parent_node,data));

		String node_id 	  = NODE_UNDEFINED_ID;
		String node_class = NODE_UNDEFINED_CLASS;
		if(data != null)
		{
			node_id    = data.getType()+":"+data.getId();
			node_class = data.getType();
		}
		return createTreeNode(user,parent_node,node_id,node_class,data);
	}
	
	public Entity createTreeNode(Entity creator, Entity parent_node,String node_id,String node_class,Entity data) throws PersistenceException 
	{
		Entity new_node = NEW(TREE_NODE_ENTITY,
								creator,
								TREE_NODE_FIELD_TREE,(parent_node == null)?null:parent_node.getAttribute(TREE_NODE_FIELD_TREE),
								TREE_NODE_FIELD_ID,node_id,
								TREE_NODE_FIELD_CLASS,node_class,
								TREE_NODE_FIELD_PARENT_NODE,parent_node,
								TREE_NODE_FIELD_CHILDREN,new ArrayList<Entity>(),
								TREE_NODE_FIELD_DATA,data);
	
		return new_node;
	}

	@Export
	public Entity UpdateTreeNode(UserApplicationContext uctx,long tree_node_id,Entity data) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree_node = GET(TREE_NODE_ENTITY,tree_node_id);
	
		GUARD(guard.canUpdateTreeNodeData(user,tree_node,data));
		String node_id 	  = NODE_UNDEFINED_ID;
		String node_class = NODE_UNDEFINED_CLASS;
		if(data != null)
		{
			node_id    = data.getType()+":"+data.getId();
			node_class = data.getType();
		}
		return updateTreeNode(tree_node,node_id,node_class,data);
	}

	public Entity updateTreeNode(Entity node,String node_id,String node_class,Entity data) throws PersistenceException
	{
		return UPDATE(node,
					  TREE_NODE_FIELD_ID,node_id,
					  TREE_NODE_FIELD_CLASS,node_class,
					  TREE_NODE_FIELD_DATA, data);
	}
	
	@Export
	public Entity ReparentTreeNode(UserApplicationContext uctx,long tree_id,long entity_node_id,long old_parent_id,long new_parent_id,int new_parent_child_index) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree_node = GET(TREE_NODE_ENTITY,entity_node_id);
		Entity parent_node = GET(TREE_NODE_ENTITY,new_parent_id);
		//!test indexout of bounds here
		GUARD(guard.canReparentTreeNode(user,tree_node,parent_node));
		return reparentTreeNode(tree_node,parent_node,new_parent_child_index);
	}
	
	public Entity reparentTreeNode(Entity tree_node,Entity new_parent,int new_parent_idx) throws PersistenceException
	{
		if(new_parent == null)
			throw new PersistenceException("NEW PARENT CANNOT BE NULL WHEN YOU ARE REPARENTING TREE NODE");
		
		List<Entity> children = (List<Entity>)new_parent.getAttribute(TREE_NODE_FIELD_CHILDREN);
		
		if(new_parent_idx < 0)
			new_parent_idx = 0;
		if(new_parent_idx > children.size() - 1)
			new_parent_idx = children.size() - 1;
		
		children.add(new_parent_idx, tree_node);
		//TODO:even though the reference is changed here we still need to set the attribute so that
		// it is dirty. could ovveride list at some point and have it automagically mark it
		//dirty but not now fer sure !
		new_parent.setAttribute(TREE_NODE_FIELD_CHILDREN, children);
		SAVE_ENTITY(new_parent);
		
		tree_node.setAttribute(TREE_NODE_FIELD_TREE,new_parent.getAttribute(TREE_NODE_FIELD_TREE));
		return SAVE_ENTITY(tree_node);
	}


	@Export
	public Entity DeleteTreeNode(UserApplicationContext uctx,long taxonomy_id,long entity_node_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	 = (Entity)uctx.getUser();
		Entity tree_node = GET(TREE_NODE_ENTITY,entity_node_id);
		GUARD(guard.canDeleteTreeNode(user,tree_node));
		return deleteTreeNode(tree_node);
	}

	//deletes children nodes as well//
	public Entity deleteTreeNode(Entity tree_node) throws PersistenceException
	{
		return deleteTree(tree_node);
	}
	
	@Export
	public Entity DeleteTree(UserApplicationContext uctx,long tree_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree = GET(TREE_ENTITY,tree_id);
		GUARD(guard.canDeleteTree(user,tree));
		return deleteTree((Entity)tree.getAttribute(TREE_FIELD_ROOT_NODE));
	}
	
	public Entity deleteTree(Entity node) throws PersistenceException
	{
		TreeFunctor f = new TreeFunctor()
		{
			public void apply(Entity entity_node) throws Exception
			{
				DELETE(entity_node);
			}


			public int getIterationStyle() {
				return ITERATE_STYLE_PREORDER;
			}

			@Override
			public Object getReturnObject() {
				return null;
			}
		};
		
		try{
			applyTreeFunctor(node, f);
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new PersistenceException("PROBLEM DELETEING ENTITY "+e.getMessage(),e);
		}
		return node;
	}
	
	@Export
	public List<Entity> GetTrees(UserApplicationContext uctx) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canGetTreesForUser(user,user));
		return getTrees(user);
	}
	
	@Export
	public List<Entity> GetTreesForUser(UserApplicationContext uctx,long user_id) throws WebApplicationException,PersistenceException
	{
		Entity user   = (Entity)uctx.getUser();
		Entity target = GET(UserModule.USER_ENTITY,user_id);//is this bad??..should user module be a slot?
		GUARD(guard.canGetTreesForUser(user,target));
		return getTrees(user);
	}
	
	public List<Entity> getTrees(Entity user) throws WebApplicationException,PersistenceException
	{
		Query q = new Query(TREE_ENTITY);
		q.idx(IDX_BY_USER_BY_TREE_NAME);
		q.eq(q.list(user,Query.VAL_GLOB));
		return QUERY(q).getEntities();
	}
	
	@Export
	public Entity GetTreeForUserByName(UserApplicationContext uctx,long user_id,String name) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity target = GET(UserModule.USER_ENTITY,user_id);//is this bad??..should user module be a slot?
		Entity tree = getTreeForUserByName(target,name);
		if(tree == null)
			return null;
		GUARD(guard.canGetTree(user,tree));
		return tree;
	}
	
	@Export
	public Entity GetTreeByName(UserApplicationContext uctx,String name) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree = getTreeForUserByName(user,name);
		if(tree == null)
			return null;
		GUARD(guard.canGetTree(user,tree));
		return tree;
	}
	
	public Entity getTreeForUserByName(Entity user,String name) throws PersistenceException
	{
		Query q = new Query(TREE_ENTITY);
		q.idx(IDX_BY_USER_BY_TREE_NAME);
		q.eq(q.list(user,name));
		QueryResult result = QUERY(q);			
		
		if(result.size() == 0)
			return null;
		else
			return result.getEntities().get(0);
	}
	
	
	@Export
	public Entity FillNode(UserApplicationContext uctx,long node_id,int subtree_fill_depth,int data_ref_fill_depth) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity node = GET(TREE_NODE_ENTITY,node_id);
		GUARD(guard.canFillNode(user,node,subtree_fill_depth,data_ref_fill_depth));
		fillNode(node, subtree_fill_depth, data_ref_fill_depth);
		return node;
	}
	
	public void fillNode(Entity node,int subtree_fill_depth,int data_ref_fill_depth) throws WebApplicationException,PersistenceException
	{
		do_fill_node(node,0,subtree_fill_depth,data_ref_fill_depth);
	}
	
	public void do_fill_node(Entity node,int cc,int subtree_fill_depth,int data_fill_depth) throws PersistenceException,WebApplicationException
	{
		if(cc == subtree_fill_depth)
			return;
		
		FILL_REFS(node);
		List<Entity> child_nodes = (List<Entity>)node.getAttribute(TREE_NODE_FIELD_CHILDREN);
		int s = child_nodes.size();
		cc++;
		for(int i = 0; i < s;i++)////expand the child node//
			do_fill_node(child_nodes.get(i),cc,subtree_fill_depth,data_fill_depth);
				
		do_fill_data((Entity)node.getAttribute(TREE_NODE_FIELD_DATA),0,data_fill_depth);

	}
	
	
	public void do_fill_data(Entity data,int c,int d) throws PersistenceException
	{
		if(data == null || c == d)
			return;
		
		FILL_REFS(data);
		EntityDefinition def = store.getEntityDefinition(data.getType());
		List<FieldDefinition> r_fields = def.getReferenceFields();
		int s = r_fields.size();
		for(int i = 0;i < s;i++)
		{
			FieldDefinition fd = r_fields.get(i);
			String ref_field_name = fd.getName();
			if(fd.isArray())
			{
				c++;
				List<Entity> l = (List<Entity>)data.getAttribute(ref_field_name);
				if(l == null)
					return;
				for(int ii = 0;ii < l.size();ii++)
					do_fill_data(l.get(ii),c,d);

			}
			else
			{
				Entity val = (Entity)data.getAttribute(ref_field_name);
				do_fill_data(val, c++, d);
			}
		}
	}
		
	///////////////////////////////////
	//http://en.wikipedia.org/wiki/Tree_traversal
	////////////////
	public static final int ITERATE_STYLE_PREORDER 	= 0x01;//DEPTH FIRST 
	public static final int ITERATE_STYLE_POSTORDER = 0x02;
	//public static final int ITERATE_STYLE_LEVELORDER = 0x03;//unimplemented BREADTH FIRST
	public void applyTreeFunctor(Entity entity_node,TreeFunctor f) throws Exception
	{
		applyTreeFunctor(entity_node, f, ITERATE_STYLE_POSTORDER);
	}
	
	//make this non recursive! //
	public void applyTreeFunctor(Entity entity_node,TreeFunctor f,int iterate_style) throws Exception
	{
		List<Entity> child_nodes = (List<Entity>)entity_node.getAttribute(TREE_NODE_FIELD_CHILDREN);
		
		if(iterate_style == ITERATE_STYLE_PREORDER)
			f.apply(entity_node);
		
		int s = child_nodes.size();
		for(int i = 0; i < s;i++)
			applyTreeFunctor(child_nodes.get(i),f,iterate_style);		

		if(iterate_style == ITERATE_STYLE_POSTORDER)
			f.apply(entity_node);
	}

	
	public interface TreeFunctor
	{
		public int getIterationStyle();
		public void apply(Entity entity_node) throws Exception;
		public Object getReturnObject();
	}
	
	public class clone_functor implements TreeFunctor
	{
		Entity last_parent_node;
		Entity cloned_tree;
		ArrayDeque<Entity> parent_stack;
		Map<Long,Entity> parent_map;
		
		public clone_functor(Entity tree) throws PersistenceException
		{
			cloned_tree = CLONE_SHALLOW(tree);
			parent_map = new HashMap<Long, Entity>();
		}
		
		public void apply(Entity entity_node) throws Exception
		{
			Entity parent_node = (Entity)entity_node.getAttribute(TREE_NODE_FIELD_PARENT_NODE);
			Entity cloned_node   = entity_node.cloneShallow();
			if(parent_node == null)//root node//
			{
				cloned_node.setAttribute(TREE_NODE_FIELD_CHILDREN, new ArrayList<Entity>());
				cloned_node.setAttribute(TREE_NODE_FIELD_TREE, cloned_tree);
				cloned_node = CREATE_ENTITY((Entity)cloned_node.getAttribute(FIELD_CREATOR),cloned_node);				
				parent_map.put(entity_node.getId(), cloned_node);
				
				updateTree(cloned_tree, 
						  (String)cloned_tree.getAttribute(TREE_FIELD_NAME),
						   cloned_node);
			}
			else
			{
				cloned_node.setAttribute(TREE_NODE_FIELD_TREE, cloned_tree);
				cloned_node.setAttribute(TREE_NODE_FIELD_CHILDREN, new ArrayList<Entity>());
				cloned_node.setAttribute(TREE_NODE_FIELD_PARENT_NODE, parent_map.get(parent_node.getId()));				
				cloned_node = CREATE_ENTITY((Entity)cloned_node.getAttribute(FIELD_CREATOR),cloned_node);
				
				if(((List<Entity>)entity_node.getAttribute(TREE_NODE_FIELD_CHILDREN)).size()!=0)
					parent_map.put(entity_node.getId(),cloned_node);
				
			}
			
		}
		
		public int getIterationStyle() 
		{
			return ITERATE_STYLE_PREORDER;
		}

		public Object getReturnObject() 
		{
			return cloned_tree;
		}
	}
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String TREE_ENTITY 				= "Tree";
	public static String TREE_FIELD_NAME 			= "name";
	public static String TREE_FIELD_ROOT_NODE 		= "root";

	public static String TREE_NODE_ENTITY				= "TreeNode";
	public static String TREE_NODE_FIELD_TREE		 	= "tree";
	public static String TREE_NODE_FIELD_ID				= "node_id";
	public static String TREE_NODE_FIELD_CLASS			= "node_class";
	public static String TREE_NODE_FIELD_PARENT_NODE 	= "parent_node";
	public static String TREE_NODE_FIELD_CHILDREN 		= "children";
	public static String TREE_NODE_FIELD_DATA 			= "data";
	public static String TREE_NODE_FIELD_METADATA 		= "metadata";
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY(TREE_ENTITY,
			TREE_FIELD_NAME,Types.TYPE_STRING,null,
			TREE_FIELD_ROOT_NODE,Types.TYPE_REFERENCE, TREE_NODE_ENTITY,null);
		
		DEFINE_ENTITY(TREE_NODE_ENTITY,
				TREE_NODE_FIELD_TREE,Types.TYPE_REFERENCE, TREE_ENTITY,null,
				TREE_NODE_FIELD_ID,Types.TYPE_STRING,"",
				TREE_NODE_FIELD_CLASS,Types.TYPE_STRING,"",
				TREE_NODE_FIELD_PARENT_NODE,Types.TYPE_REFERENCE, TREE_NODE_ENTITY,null,
				TREE_NODE_FIELD_CHILDREN,Types.TYPE_ARRAY|Types.TYPE_REFERENCE, TREE_NODE_ENTITY,null,
				TREE_NODE_FIELD_DATA,Types.TYPE_REFERENCE, FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
				TREE_NODE_FIELD_METADATA,Types.TYPE_STRING,"");
	}

	public static final String IDX_BY_USER_BY_TREE_NAME = "byUserbyTreeName";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_INDEX(TREE_ENTITY,IDX_BY_USER_BY_TREE_NAME , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,TREE_FIELD_NAME);
	}
	
	protected void defineRelationships(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_RELATIONSHIP(TREE_NODE_ENTITY, TREE_NODE_FIELD_PARENT_NODE, EntityRelationshipDefinition.TYPE_ONE_TO_MANY, TREE_NODE_ENTITY, TREE_NODE_FIELD_CHILDREN);
	}

}
