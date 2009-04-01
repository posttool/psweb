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
import com.pagesociety.web.module.user.UserModule;


public class TreeModule extends WebStoreModule 
{
	private static final String SLOT_GUARD = "tree-guard";
	private ITreeGuard guard;

	private static final String ROOT_NODE_CLASS 	 = "root_node_class";
	private static final String ROOT_NODE_ID		 = "root_node_id";
	

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		guard = (ITreeGuard)getSlot(SLOT_GUARD);
	}
	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_GUARD,ITreeGuard.class,false,DefaultTreeGuard.class);
	}	
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////


	@Export(ParameterNames={"name","root_class","root_id","root_data"})
	public Entity CreateTree(UserApplicationContext uctx,String name,String root_class,String root_id,Entity root_data) throws WebApplicationException,PersistenceException
	{
		
		Entity user = (Entity)uctx.getUser();
		Entity t = getTreeForUserByName(user,name);
		if(t != null)
			throw new WebApplicationException("TREE NAMED "+name+" ALREADY EXISTS FOR USER "+user.getId());
		GUARD(guard.canCreateTree(user));
		return createTree(user,name,root_class,root_id,root_data);
	}
	
	public Entity createTree(Entity user,String tree_name,String root_class,String root_id,Entity root_data) throws PersistenceException
	{
		Entity root_node = createTreeNode(user, null,root_class,root_id,root_data);
		Entity tree =  NEW(TREE_ENTITY,
						user,
						TREE_FIELD_NAME,tree_name,
						TREE_FIELD_ROOT_NODE,root_node);
	
		UPDATE(root_node,
				TREE_NODE_FIELD_TREE,tree);
		
		System.out.println("CREATED TREE "+tree);
		return tree;
	}
	
	@Export(ParameterNames={"tree_id","root_node"})
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
	
	@Export(ParameterNames={"parent_node_id","node_class","node_id","data"})
	public Entity CreateTreeNode(UserApplicationContext uctx,long parent_node_id,String node_class,String node_id,Entity data) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity parent_node = GET(TREE_NODE_ENTITY,parent_node_id);
		GUARD(guard.canCreateTreeNode(user,parent_node,node_class,node_id,data));
		return createTreeNode(user,parent_node,node_class,node_id,data);
	}
	
	@Export(ParameterNames={"parent_node_id","parent_child_index","node_class","node_id","data"})
	public Entity CreateTreeNode(UserApplicationContext uctx,long parent_node_id,int parent_child_index, String node_class,String node_id,Entity data) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity parent_node = GET(TREE_NODE_ENTITY,parent_node_id);
		GUARD(guard.canCreateTreeNode(user,parent_node,node_class,node_id,data));
		return createTreeNode(user,parent_node,parent_child_index,node_class,node_id,data);
	}
	
	public Entity createTreeNode(Entity creator, Entity parent_node,String node_class,String node_id,Entity data) throws PersistenceException 
	{
		return createTreeNode(creator, parent_node, Integer.MAX_VALUE, node_class, node_id, data);
	}
	
	public Entity createTreeNode(Entity creator, Entity parent_node,int parent_child_index,String node_class,String node_id,Entity data) throws PersistenceException 
	{
		Entity new_node = NEW(TREE_NODE_ENTITY,
								creator,
								TREE_NODE_FIELD_TREE,(parent_node == null)?null:parent_node.getAttribute(TREE_NODE_FIELD_TREE),
								TREE_NODE_FIELD_ID,node_id,
								TREE_NODE_FIELD_CLASS,node_class,
								TREE_NODE_FIELD_PARENT_NODE,parent_node,
								TREE_NODE_FIELD_CHILDREN,new ArrayList<Entity>(),
								TREE_NODE_FIELD_DATA,data);
		//by default tree nodes are appended to the parents list of children. 
		//we use Integer.MAX_VALUE to mean create and place last.
		if(parent_child_index != Integer.MAX_VALUE)
		{
			try{
				reparentTreeNode(new_node, parent_node, parent_child_index);
			}catch(WebApplicationException e)
			{
				//this will never happen..ha ha famous last words//
				ERROR(e);
			}
		}
		return new_node;
	}

	@Export(ParameterNames={"tree_node_id","node_class","node_id","data"})
	public Entity UpdateTreeNode(UserApplicationContext uctx,long tree_node_id,String node_class,String node_id,Entity data) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree_node = GET(TREE_NODE_ENTITY,tree_node_id);
	
		GUARD(guard.canUpdateTreeNode(user,tree_node,node_class,node_id,data));
		return updateTreeNode(tree_node,node_class,node_id,data);
	}

	public Entity updateTreeNode(Entity node,String node_class,String node_id,Entity data) throws PersistenceException
	{
		return UPDATE(node,
					  TREE_NODE_FIELD_ID,node_id,
					  TREE_NODE_FIELD_CLASS,node_class,
					  TREE_NODE_FIELD_DATA, data);
	}
	
	@Export(ParameterNames={"entity_node_id","new_parent_id","new_parent_child_index"})
	public Entity ReparentTreeNode(UserApplicationContext uctx,long entity_node_id,long new_parent_id,int new_parent_child_index) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree_node = GET(TREE_NODE_ENTITY,entity_node_id);
		Entity parent_node = GET(TREE_NODE_ENTITY,new_parent_id);

		if(tree_node.getAttribute(TREE_NODE_FIELD_PARENT_NODE) == null)
			throw new WebApplicationException("CANT REPARENT A ROOT TREE NODE");
		GUARD(guard.canReparentTreeNode(user,tree_node,parent_node));
		return reparentTreeNode(tree_node,parent_node,new_parent_child_index);
	}
	
	public Entity reparentTreeNode(Entity tree_node,Entity new_parent,int new_parent_idx) throws PersistenceException,WebApplicationException
	{
		if(new_parent == null)
			throw new PersistenceException("NEW PARENT CANNOT BE NULL WHEN YOU ARE REPARENTING TREE NODE");
		if(new_parent.equals(tree_node))
			throw new PersistenceException("CANNOT MAKE A NODE A PARENT OF ITSELF.");

		List<Entity> ancestors = getAncestors(new_parent, new ArrayList());
		if(ancestors.contains(tree_node))
			throw new WebApplicationException("CANNOT REPARENT "+tree_node.getId()+" TO ONE OF ITS CHILDREN!");
	
		List<Entity> children = (List<Entity>)new_parent.getAttribute(TREE_NODE_FIELD_CHILDREN);
		
		if(new_parent_idx < 0)
			new_parent_idx = 0;
		if(new_parent_idx > children.size())
			new_parent_idx = children.size();
		
		if(tree_node.getAttribute(TREE_NODE_FIELD_PARENT_NODE).equals(new_parent))
		{
			int original_idx = children.indexOf(tree_node);

			
			children.add(new_parent_idx,tree_node);
			if(new_parent_idx < original_idx)
				children.remove(original_idx+1);
			else
				children.remove(original_idx);
		}
		else
			children.add(new_parent_idx, tree_node);
		//TODO:even though the reference is changed here we still need to set the attribute so that
		// it is dirty. could ovveride list at some point and have it automagically mark it
		//dirty but not now fer sure !
		new_parent.setAttribute(TREE_NODE_FIELD_CHILDREN, children);
		SAVE_ENTITY(new_parent);
		
		tree_node.setAttribute(TREE_NODE_FIELD_TREE,new_parent.getAttribute(TREE_NODE_FIELD_TREE));
		return SAVE_ENTITY(tree_node);
	}

	@Export (ParameterNames={"entity_node_id"})
	public List<Entity> getAncestors(UserApplicationContext uctx,long entity_node_id) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree_node = GET(TREE_NODE_ENTITY,entity_node_id);
		GUARD(guard.canGetAncestors(user,tree_node));
		return getAncestors(tree_node,new ArrayList<Entity>() );
	}

	public List<Entity> getAncestors(Entity node,List<Entity> return_list) throws PersistenceException
	{
		Entity parent = (Entity)node.getAttribute(TREE_NODE_FIELD_PARENT_NODE);
		if(parent != null)
		{
			return_list.add(0,parent);
			getAncestors(EXPAND(parent), return_list);
		}
		return return_list;
	}
	
	@Export(ParameterNames={"entity_node_id"})
	public List<Entity> DeleteTreeNode(UserApplicationContext uctx,long entity_node_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	 = (Entity)uctx.getUser();
		Entity tree_node = GET(TREE_NODE_ENTITY,entity_node_id);
		GUARD(guard.canDeleteTreeNode(user,tree_node));
		return deleteSubTree(tree_node);
	}
	
	@Export(ParameterNames={"tree_id"})
	public List<Entity> DeleteTree(UserApplicationContext uctx,long tree_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree = GET(TREE_ENTITY,tree_id);
		GUARD(guard.canDeleteTree(user,tree));
		return deleteTree(tree);
	}
	
	public List<Entity> deleteTree(Entity tree) throws PersistenceException
	{
		return deleteSubTree((Entity)tree.getAttribute(TREE_FIELD_ROOT_NODE),false,false,null);	
	}
	
	public List<Entity> deleteTree(Entity tree,boolean delete_data,boolean delete_data_deep,delete_policy data_delete_deep_policy) throws PersistenceException
	{
		return deleteSubTree((Entity)tree.getAttribute(TREE_FIELD_ROOT_NODE),delete_data,delete_data_deep,data_delete_deep_policy);	
	}
	
	public List<Entity> deleteSubTree(Entity node) throws PersistenceException
	{
		return deleteSubTree(node,false,false,null);		
	}
	
	public List<Entity> deleteSubTree(Entity node,boolean delete_data,boolean delete_data_deep,delete_policy data_delete_deep_policy) throws PersistenceException
	{
		node = EXPAND(node);
		delete_functor df = new delete_functor(delete_data,delete_data_deep,data_delete_deep_policy);
		Entity tree = null;
		if(node.getAttribute(TREE_NODE_FIELD_PARENT_NODE) == null)
		{
			tree = (Entity)node.getAttribute(TREE_NODE_FIELD_TREE);
		}
		
		try{
			applyTreeFunctor(node, df);
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new PersistenceException("PROBLEM DELETEING ENTITY "+e.getMessage()+" NODE WAS "+node,e);
		}
		List<Entity> deletees = (List<Entity>)df.getReturnObject();
		if(tree != null)
			DELETE(tree);
		
		return deletees;
	}
	
	@Export(ParameterNames={"tree_id","new_tree_name"})
	public Entity CloneTree(UserApplicationContext uctx,long tree_id,String new_tree_name) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree = GET(TREE_ENTITY,tree_id);
		GUARD(guard.canCloneTree(user,tree));
		return cloneTree(tree,new_tree_name);
	}
	
	public Entity cloneTree(Entity tree,String new_tree_name) throws PersistenceException
	{
		return cloneSubTree((Entity)tree.getAttribute(TREE_FIELD_ROOT_NODE),new_tree_name,false,false,null);	
	}
	
	public Entity cloneTree(Entity tree,String new_tree_name,boolean clone_data,boolean clone_data_deep,clone_policy data_deep_clone_policy) throws PersistenceException
	{
		return cloneSubTree((Entity)tree.getAttribute(TREE_FIELD_ROOT_NODE),new_tree_name,clone_data,clone_data_deep,data_deep_clone_policy);	
	}
	
	public Entity cloneSubTree(Entity node,String new_tree_name) throws PersistenceException
	{
		return cloneSubTree(node, new_tree_name, false, false, null);
	}
	
	public Entity cloneSubTree(Entity node,String new_tree_name,boolean clone_data,boolean clone_data_deep,clone_policy data_deep_clone_policy) throws PersistenceException
	{
		clone_functor cf = new clone_functor(node,new_tree_name,clone_data,clone_data_deep,data_deep_clone_policy);
		try{
			applyTreeFunctor(node, cf);
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new PersistenceException("PROBLEM CLONING TREE "+e.getMessage(),e);
		}
		return (Entity)cf.getReturnObject();
	}
	
	@Export
	public List<Entity> GetTrees(UserApplicationContext uctx) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canGetTreesForUser(user,user));
		return getTrees(user);
	}
	
	@Export(ParameterNames={"user_id"})
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
	
	@Export(ParameterNames={"user_id","name"})
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
	
	@Export(ParameterNames={"name"})
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
		QueryResult result = QUERY_FILL(q);			
		
		if(result.size() == 0)
			return null;
		else
			return result.getEntities().get(0);
	}
	
	public Entity getTreeById(long tree_id) throws PersistenceException
	{
		return GET(TREE_ENTITY,tree_id);
	}
	
	@Export(ParameterNames={"tree_id","node_classname"})
	public List<Entity> GetTreeNodesByClass(UserApplicationContext uctx, long tree_id,String node_classname) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree = GET(TREE_ENTITY,tree_id);
		GUARD(guard.canGetTreeNodesByClass(user,node_classname));
		return getTreeNodesByClass(tree,node_classname);
		
	}
	
	public List<Entity> getTreeNodesByClass(Entity tree,String node_classname) throws PersistenceException
	{
		Query q = new Query(TREE_NODE_ENTITY);
		q.idx(IDX_BY_TREE_BY_NODE_CLASS);
		q.eq(q.list(tree,node_classname));
		QueryResult result = QUERY(q);			
		return result.getEntities();	
	}
	
	@Export(ParameterNames={"tree_id","node_id"})
	public Entity GetTreeNodeById(UserApplicationContext uctx, long tree_id,String node_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity tree = GET(TREE_ENTITY,tree_id);
		GUARD(guard.canGetTreeNodeById(user,node_id));
		return getTreeNodeById(tree,node_id);
		
	}
	
	public Entity getTreeNodeById(Entity tree,String node_id) throws PersistenceException
	{
		Query q = new Query(TREE_NODE_ENTITY);
		q.idx(IDX_BY_TREE_BY_NODE_ID);
		q.eq(q.list(tree,node_id));
		QueryResult result = QUERY(q);			
		if(result.size() == 0)
			return null;
		return result.getEntities().get(0);
	}
	
	
	@Export(ParameterNames={"node_id","subtree_fill_depth","data_ref_fill_depth"})
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
				do_fill_data(val, ++c, d);
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
		applyTreeFunctor(entity_node, f, f.getIterationStyle());
	}
	
	//make this non recursive! //
	public void applyTreeFunctor(Entity entity_node,TreeFunctor f,int iterate_style) throws Exception
	{
		entity_node = EXPAND(entity_node);
		List<Entity> child_nodes = (List<Entity>)entity_node.getAttribute(TREE_NODE_FIELD_CHILDREN);
		
		if(iterate_style == ITERATE_STYLE_PREORDER)
			f.apply(entity_node);
		
		int s = (child_nodes==null) ? 0 : child_nodes.size();
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
		Entity original_root_node;
		ArrayDeque<Entity> parent_stack;
		Map<Long,Entity> parent_map;
		boolean clone_data;
		boolean clone_data_deep;
		clone_policy data_clone_deep_policy;
		
		public clone_functor(Entity root_node,String new_tree_name,boolean clone_data,boolean clone_data_deep,clone_policy clone_data_deep_policy) throws PersistenceException
		{
			this.clone_data 			= clone_data;
			this.clone_data_deep		= clone_data_deep;
			this.data_clone_deep_policy = clone_data_deep_policy;
			
			parent_map = new HashMap<Long, Entity>();
			Entity creator = (Entity)root_node.getAttribute(FIELD_CREATOR);
			cloned_tree = createTree(creator, new_tree_name, (String)root_node.getAttribute(TREE_NODE_FIELD_CLASS), (String)root_node.getAttribute(TREE_NODE_FIELD_ID), (Entity)root_node.getAttribute(TREE_NODE_FIELD_DATA));
			Entity cloned_root_node = (Entity)cloned_tree.getAttribute(TREE_FIELD_ROOT_NODE);
			if(clone_data)
			{
				Entity data = (Entity)cloned_root_node.getAttribute(TREE_NODE_FIELD_DATA);
				System.out.println("CLONING DATA "+data);
				Entity cloned_data = null;
				if(clone_data_deep)
				{
					if(data_clone_deep_policy != null)
						cloned_data = CLONE_DEEP(data, data_clone_deep_policy); 
					else
						cloned_data = CLONE_DEEP(data); 
				
				}
				else
				{
					if(data != null)
						cloned_data = data.cloneShallow();
					cloned_data = CREATE_ENTITY((Entity)cloned_root_node.getAttribute(FIELD_CREATOR), cloned_data);
				}
				System.out.println("CLONED DATA IS "+cloned_data);
				cloned_root_node.setAttribute(TREE_NODE_FIELD_DATA,cloned_data);
				SAVE_ENTITY(cloned_root_node);
			}			
			original_root_node = root_node;
			parent_map.put(root_node.getId(),(Entity) cloned_root_node);
			System.out.println("CLONING TREE FROM NODE "+root_node);
			System.out.println("CLONED TREE IS "+cloned_tree);
			System.out.println("CLONED ROOT NODE IS "+cloned_root_node);
		}
		
		public void apply(Entity entity_node) throws Exception
		{
			
			if(entity_node.equals(original_root_node))//root node..we already accounted for this above//
			{
				System.out.println("CLONING ENTITY NODE  "+entity_node);
				
			}
			else
			{
				System.out.println("CLONING ENTITY NODE "+entity_node);
				
				Entity cloned_node   = entity_node.cloneShallow();	
				if(clone_data)
				{
					Entity data = (Entity)entity_node.getAttribute(TREE_NODE_FIELD_DATA);
					System.out.println("CLONING DATA "+data);
					Entity cloned_data = null;
					if(clone_data_deep)
					{
						if(data_clone_deep_policy != null)
							cloned_data = CLONE_DEEP(data, data_clone_deep_policy); 
						else
							cloned_data = CLONE_DEEP(data); 
					
					}
					else
					{
						if(data != null)
							cloned_data = data.cloneShallow();
						cloned_data = CREATE_ENTITY((Entity)cloned_node.getAttribute(FIELD_CREATOR), cloned_data);
					}
					System.out.println("CLONED DATA IS "+cloned_data);
					cloned_node.setAttribute(TREE_NODE_FIELD_DATA,cloned_data);
				}
				
				Entity parent_node 	 = (Entity)entity_node.getAttribute(TREE_NODE_FIELD_PARENT_NODE);
				Entity cloned_parent_node = parent_map.get(parent_node.getId()) ;
				
				cloned_node.setAttribute(TREE_NODE_FIELD_TREE, cloned_tree);
				cloned_node.setAttribute(TREE_NODE_FIELD_PARENT_NODE, cloned_parent_node);				
				cloned_node.setAttribute(TREE_NODE_FIELD_CHILDREN, new ArrayList());				
				cloned_node = CREATE_ENTITY((Entity)cloned_node.getAttribute(FIELD_CREATOR),cloned_node);
				
				List<Entity> original_children =  (List<Entity>)entity_node.getAttribute(TREE_NODE_FIELD_CHILDREN);
				if(original_children != null && original_children.size()!=0)
				{
					parent_map.put(entity_node.getId(),cloned_node);
				}
				
				System.out.println("CLONED NODE IS "+cloned_node);

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

	

	public class delete_functor implements TreeFunctor
	{
		private List<Entity> deleted_nodes;
		private boolean delete_data;
		private boolean delete_data_deep;
		private delete_policy data_delete_deep_policy;
		public delete_functor(boolean delete_data,boolean delete_data_deep,delete_policy data_delete_deep_policy)
		{
			this.delete_data 			 = delete_data;
			this.delete_data_deep 		 = delete_data_deep;
			this.data_delete_deep_policy = data_delete_deep_policy;
			deleted_nodes = new ArrayList<Entity>();
		}
		
		public void apply(Entity entity_node) throws Exception
		{
			System.out.println("DELETEING NODE: "+entity_node);
			if(delete_data)
			{
				Entity data = (Entity)entity_node.getAttribute(TREE_NODE_FIELD_DATA);
				if(delete_data_deep)
				{
					if(data_delete_deep_policy != null)
						DELETE_DEEP(data,data_delete_deep_policy);
					else
						DELETE_DEEP(data);
				}
				else
				{
					if(data != null)
						DELETE(data);
				}
			}
			deleted_nodes.add(DELETE(entity_node));
		}

		public int getIterationStyle() {
			return ITERATE_STYLE_PREORDER;
		}

		public Object getReturnObject() {
			return deleted_nodes;
		}
	};
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static final String TREE_ENTITY 				= "Tree";
	public static final String TREE_FIELD_NAME 			= "name";
	public static final String TREE_FIELD_ROOT_NODE 		= "root";

	public static final String TREE_NODE_ENTITY					= "TreeNode";
	public static final String TREE_NODE_FIELD_TREE		 		= "tree";
	public static final String TREE_NODE_FIELD_ID				= "node_id";
	public static final String TREE_NODE_FIELD_CLASS			= "node_class";
	public static final String TREE_NODE_FIELD_PARENT_NODE 		= "parent_node";
	public static final String TREE_NODE_FIELD_CHILDREN 		= "children";
	public static final String TREE_NODE_FIELD_DATA 			= "data";
	public static final String TREE_NODE_FIELD_METADATA 		= "metadata";
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
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

	public static final String IDX_BY_USER_BY_TREE_NAME 					= "byUserbyTreeName";
	public static final String IDX_BY_TREE_BY_NODE_CLASS		    		= "byTreeByNodeClass";
	public static final String IDX_BY_TREE_BY_NODE_ID		    			= "byTreeByNodeId";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_INDICES
		(
				TREE_ENTITY,
				ENTITY_INDEX(IDX_BY_USER_BY_TREE_NAME , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,TREE_FIELD_NAME)
		);
		DEFINE_ENTITY_INDICES
		(
				TREE_NODE_ENTITY,
				ENTITY_INDEX(IDX_BY_TREE_BY_NODE_CLASS , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX,TREE_NODE_FIELD_TREE,TREE_NODE_FIELD_CLASS),
				ENTITY_INDEX(IDX_BY_TREE_BY_NODE_ID , EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, TREE_NODE_FIELD_TREE,TREE_NODE_FIELD_ID)
		);
	}
	
	protected void defineRelationships(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY_RELATIONSHIP(TREE_NODE_ENTITY, TREE_NODE_FIELD_PARENT_NODE, EntityRelationshipDefinition.TYPE_ONE_TO_MANY, TREE_NODE_ENTITY, TREE_NODE_FIELD_CHILDREN);
	}

}
