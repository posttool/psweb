package com.pagesociety.web.module.site;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import com.google.api.translate.Language;
//import com.google.api.translate.Translate;
import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.Query.QueryNode;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.util.Text;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.IHttpRequestHandler;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.IEventListener;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleEvent;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.cms.CmsModule;
import com.pagesociety.web.module.resource.ResourceModule;
import com.pagesociety.web.module.tree.TreeModule;
import com.pagesociety.web.module.user.UserModule;
import com.pagesociety.web.module.util.Util;
import com.pagesociety.web.template.FreemarkerRenderer;






public class SiteManagerModule extends TreeModule implements IHttpRequestHandler,IEventListener
{


	public static final String SLOT_CMS_MODULE	  		= "cms-module";
	public static final String SLOT_RESOURCE_MODULE	  	= "resource-module";

	public static final String PARAM_ADDITIONAL_LANGUAGES        = "additional-languages";
	public static final String PARAM_SEARCH_RANK_THREAD_INTERVAL_IN_HOURS = "search-rank-thread-interval";

	protected Entity 			site_tree;
	protected Entity 			admin_user;
	protected CmsModule 		cms_module;
	protected ResourceModule 	resource_module;


	protected FreemarkerRenderer fm_renderer;
	protected String[] additional_languages;
	private int search_rank_thread_interval_in_hours;
	private static final int DEFAULT_SEARCH_RANK_THREAD_INTERVAL_IN_HOURS = 12;
	private static final int DONT_RUNT_SEARCH_RANK_THREAD_INTERVAL 		  = 0;

	public String[] getAdditionalLanguages()
	{
		return additional_languages;
	}

	public Map<String,String[]> getMultilingualEntityFieldMap()
	{
		return multilingual_entity_field_map;
	}

	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		additional_languages = GET_OPTIONAL_LIST_PARAM(PARAM_ADDITIONAL_LANGUAGES, config,new String[]{});
		search_rank_thread_interval_in_hours = GET_OPTIONAL_INT_CONFIG_PARAM(PARAM_SEARCH_RANK_THREAD_INTERVAL_IN_HOURS, DEFAULT_SEARCH_RANK_THREAD_INTERVAL_IN_HOURS, config);
		fm_renderer 	= new FreemarkerRenderer(getApplication().getConfig().getWebRootDir()+"/templates");
		resource_module = (ResourceModule)getSlot(SLOT_RESOURCE_MODULE);
		cms_module 		= (CmsModule)getSlot(SLOT_CMS_MODULE);
		cms_module.addEventListener(this);
		super.init(app,config);

	}

	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		try{

			admin_user = GET(UserModule.USER_ENTITY,1);
			set_edit_tree();
			do_define_site();
			definePageInputTypes();
			definePagePreparers();
			set_published_site_root();
			if(search_rank_thread_interval_in_hours != DONT_RUNT_SEARCH_RANK_THREAD_INTERVAL)
				start_search_rank_thread();
			store.checkpoint();

			load_locale_data();



		}catch(Exception e)
		{
			e.printStackTrace();
			throw new InitializationException("FAILED SETTING UP "+getApplication().getConfig().getName());
		}

	}

	protected void defineSlots()
	{
		super.defineSlots();
		defineSlot(SLOT_RESOURCE_MODULE, ResourceModule.class, true);
		defineSlot(SLOT_CMS_MODULE, CmsModule.class, true);
	}


	public static final String CAN_MANAGE_PAGE_TYPES		 = "CAN_MANAGE_PAGE_TYPES";
	public static final String CAN_MANAGE_SITE_TREE		 	 = "CAN_MANAGE_SITE_TREE";
	public static final String CAN_MANAGE_TRANSLATIONS		 = "CAN_MANAGE_TRANSLATIONS";


	public void exportPermissions()
	{
		super.exportPermissions();
		EXPORT_PERMISSION(CAN_MANAGE_PAGE_TYPES);
		EXPORT_PERMISSION(CAN_MANAGE_SITE_TREE);
		EXPORT_PERMISSION(CAN_MANAGE_TRANSLATIONS);
	}


	//TODO: break this into two methods//
	public static final String KEY_SITEMANAGER_METAINFO_ENTITY_DEFINITIONS 	 			= "entity_definitions";
	public static final String KEY_SITEMANAGER_METAINFO_ENTITY_INDICES		 			= "entity_indices";
	public static final String KEY_SITEMANAGER_METAINFO_ADDITIONAL_LANGUAGES 			= "additional_languages";
	public static final String KEY_SITEMANAGER_METAINFO_MULTILINGUAL_ENTITIES 			= "multilingual_entities";
	public static final String KEY_SITEMANAGER_METAINFO_MULTILINGUAL_ENTITY_FIELD_MAP 	= "multilingual_entity_field_map";
	public static final String KEY_SITEMANAGER_METAINFO_PUBLISHABLE_ENTITIES			= "publishable_entities";
	public static final String KEY_SITEMANAGER_METAINFO_PAGE_TYPE_ENTITIES				= "page_type_entities";

	@Export
	public Map<String,Object> GetSiteManagerMetaInfo(UserApplicationContext uctx) throws WebApplicationException,PersistenceException
	{
		GUARD((Entity)uctx.getUser(), CAN_MANAGE_SITE_TREE);

		Map<String,Object> ret = new HashMap<String,Object>();
		List<EntityDefinition> entity_definitions = store.getEntityDefinitions();
		ret.put(KEY_SITEMANAGER_METAINFO_ENTITY_DEFINITIONS, store.getEntityDefinitions());
		Map<String,List<EntityIndex>> entity_indices = new HashMap<String,List<EntityIndex>>();
		for(int i = 0;i < entity_definitions.size();i++)
		{
			EntityDefinition d = entity_definitions.get(i);
			List<EntityIndex> idxs = store.getEntityIndices(d.getName());
			entity_indices.put(d.getName(),idxs);
		}

		ret.put(KEY_SITEMANAGER_METAINFO_ENTITY_INDICES, entity_indices);
		ret.put(KEY_SITEMANAGER_METAINFO_ADDITIONAL_LANGUAGES, additional_languages);
		List<String> multilingual_entities = new ArrayList<String>(multilingual_entity_field_map.keySet());
		Collections.sort(multilingual_entities);
		ret.put(KEY_SITEMANAGER_METAINFO_MULTILINGUAL_ENTITIES, multilingual_entities);
		ret.put(KEY_SITEMANAGER_METAINFO_MULTILINGUAL_ENTITY_FIELD_MAP, multilingual_entity_field_map);
		List<String> publishable_entities = new ArrayList<String>(publishable_entity_field_map.keySet());
		publishable_entities.addAll(managable_entities);
		Collections.sort(publishable_entities);
		ret.put(KEY_SITEMANAGER_METAINFO_PUBLISHABLE_ENTITIES, publishable_entities);
		List<String> page_type_entities = new ArrayList<String>();

		for(int i = 0;i < page_type_entity_list.size();i++)
			page_type_entities.add(page_type_entity_list.get(i).getName());
		Collections.sort(page_type_entities);
		ret.put(KEY_SITEMANAGER_METAINFO_PAGE_TYPE_ENTITIES,page_type_entities);

		return ret;
	}


	@Export(ParameterNames={"parent_node_id","unsaved_page_type"})
	@TransactionProtect
	public Entity CreatePageInstance(UserApplicationContext uctx,long parent_tree_node_id,String unsaved_page_type) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user,CAN_MANAGE_SITE_TREE);
		Entity parent_tree_node = GET(TREE_NODE_ENTITY,parent_tree_node_id);

		return createPageInstance(user,parent_tree_node, unsaved_page_type);
	}

	public Entity createPageInstance(Entity creator,Entity parent_tree_node,String unsaved_page_type)  throws WebApplicationException,PersistenceException
	{
		if(!isPageTypeEntity(unsaved_page_type))
			throw new WebApplicationException("CANT CREATE PAGE INSTANCE WHEN ENTITY IS NOT AN INSTANCE OF A PAGE TYPE ENTITY: "+unsaved_page_type);
	//	boolean in_menu   = (Boolean)unsaved_page_type_entity.getAttribute(PAGE_TYPE_FIELD_IN_NAV);
	//	String menu_name  = (String)unsaved_page_type_entity.getAttribute(PAGE_TYPE_FIELD_MENU_NAME);
	//	if(in_menu && menu_name == null)
	//		throw new WebApplicationException("MENU NAME IS REQUIRED FOR THE PAGE IF "+PAGE_TYPE_FIELD_IN_NAV+" IS SET TO TRUE");

	//	String node_id = null;
	//	if(in_menu)
	//	{
			//make sure the id is unique before we save anything//
			//this is in case this method is called internally without//
			//the transaction protect. we dont want to have lingering references//
	//		Entity temp_tn = new Entity();
	//		temp_tn.setType(TREE_NODE_ENTITY);
	//		temp_tn.setAttribute(TREE_NODE_FIELD_PARENT_NODE, parent_tree_node);
	//		temp_tn.setAttribute(TREE_NODE_FIELD_DATA, unsaved_page_type_entity);
	//		node_id 	  = get_node_id(temp_tn);
	//		Entity exisiting_node = getTreeNodeById(edit_tree, node_id);
	//		if(exisiting_node != null)
	//			throw new WebApplicationException("NODE ID "+node_id+" ALREADY EXISTS.");
	//	}

		Entity data = NEW(unsaved_page_type,creator);
		Entity tn 	= createTreeNode(creator, parent_tree_node, data.getType(),null, data);
		update_site_trees();
		return tn;

	}


	@Export(ParameterNames={"page_instance_tree_node_id"})
	@TransactionProtect
	public  Entity DeletePageInstance(UserApplicationContext uctx,long page_instance_tree_node_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_MANAGE_SITE_TREE);
		Entity tn = GET(TREE_NODE_ENTITY,page_instance_tree_node_id);
		return deletePageInstance(tn);
	}

	public Entity deletePageInstance(Entity tn) throws PersistenceException
	{
		deleteSubTree(tn, true, false, null);
		update_site_trees();
		return tn;
	}


	@Export(ParameterNames={})
	@TransactionProtect
	public Entity ReparentPageInstance(UserApplicationContext uctx,long tree_node_id,long new_parent_id,int new_parent_idx) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_MANAGE_SITE_TREE);
		Entity tn = GET(TREE_NODE_ENTITY,tree_node_id);
		Entity np = GET(TREE_NODE_ENTITY,new_parent_id);
		return reparentPageInstance(tn, np, new_parent_idx);
	}

	public Entity reparentPageInstance(Entity tree_node,Entity new_parent_node,int new_parent_idx) throws PersistenceException,WebApplicationException
	{
		Entity data =  EXPAND((Entity)tree_node.getAttribute(TREE_NODE_FIELD_DATA));
		if(data.getAttribute(PAGE_TYPE_FIELD_MENU_NAME) != null && !tree_node.getAttribute(TREE_NODE_FIELD_PARENT_NODE).equals(new_parent_node))
		{
			Entity temp_tn = new Entity();
			temp_tn.setType(TREE_NODE_ENTITY);
			temp_tn.setAttribute(TREE_NODE_FIELD_PARENT_NODE, new_parent_node);
			temp_tn.setAttribute(TREE_NODE_FIELD_DATA, data);
			String node_id 	  = get_node_id(temp_tn);
			Entity exisiting_node = getTreeNodeById(site_tree, node_id);

			if(exisiting_node != null)
				throw new WebApplicationException("CANT REPARENT: NODE ID "+node_id+" ALREADY EXISTS.");


		}
		tree_node = reparentTreeNode(tree_node, new_parent_node, new_parent_idx);

		tree_node =  UPDATE(tree_node,
					  TREE_NODE_FIELD_ID,get_node_id(tree_node));

		update_site_trees();
		return tree_node;
	}


	public Entity getTreeNodeByData(Entity tree,Entity data) throws PersistenceException
	{

		Query q = new Query(TREE_NODE_ENTITY);
		q.idx(IDX_TREE_NODE_BY_TREE_BY_DATA);
		q.eq(q.list(tree,data));
		QueryResult qr = QUERY(q);
		if(qr.getEntities().size() == 0)
			return null;
		else
		{
			if(qr.getEntities().size() > 1)
				WARNING("MORE THAN ONE PEER NODE FOR TREE NODE DATA IN getTreeNodeByData");

			return qr.getEntities().get(0);
		}

	}

	public Entity getTreeNodeByIdByPublicationStatus(Entity tree,String id,Object publication_status) throws PersistenceException
	{

		Query q = new Query(TREE_NODE_ENTITY);
		q.idx(IDX_TREE_NODE_BY_TREE_BY_ID_BY_PUBLICATION_STATUS);
		q.eq(q.list(tree,id,publication_status));
		QueryResult qr = QUERY(q);
		if(qr.getEntities().size() == 0)
			return null;
		else
		{
			if(qr.getEntities().size() > 1)
				WARNING("MORE THAN ONE NODE FOR ID "+id+" WITH PUBLICATION STATUS "+ publication_status+" IN getTreeNodeByIdByPublicationStatus");

			return qr.getEntities().get(0);
		}

	}


	private void update_site_trees() throws PersistenceException
	{
		set_edit_tree();
		set_published_site_root();
	}

	Map<String,Entity> site_manager_site_roots = new HashMap<String,Entity>();
	private void set_edit_tree() throws PersistenceException
	{
		site_tree = getTreeForUserByName(admin_user, "the_site_tree");
		if(site_tree == null)
			site_tree = createTree(admin_user, "the_site_tree", null, null, null);
		FILL_DEEP_AND_MASK((Entity)site_tree.getAttribute(TREE_FIELD_ROOT_NODE), FILL_ALL_FIELDS, MASK_NO_FIELDS);
		site_manager_site_roots.put(DEFAULT_LANGUAGE,translate_page_data(DEFAULT_LANGUAGE, (Entity)site_tree.getAttribute(TREE_FIELD_ROOT_NODE)));
		for(int i = 0;i < additional_languages.length;i++)
		{
			Entity translated_root = translate_page_data(additional_languages[i], (Entity)site_tree.getAttribute(TREE_FIELD_ROOT_NODE));
			site_manager_site_roots.put(additional_languages[i], translated_root);

		}
	}

	Map<String,Entity> published_site_roots = new HashMap<String,Entity>();
	private void set_published_site_root() throws PersistenceException
	{
		Entity published_site_root = calculate_published_site_tree((Entity)site_tree.getAttribute(TREE_FIELD_ROOT_NODE));
		published_site_roots.put(DEFAULT_LANGUAGE,translate_page_data(DEFAULT_LANGUAGE, published_site_root));
		for(int i = 0;i < additional_languages.length;i++)
		{
			published_site_roots.put(additional_languages[i],translate_page_data(additional_languages[i], published_site_root));
		}
	}

	private Entity get_site_manager_site_menu(String lang)
	{
		return site_manager_site_roots.get(lang);
	}

	private Entity get_published_site_menu(String lang)
	{
		return published_site_roots.get(lang);
	}

	//....TRANSLATION STUFF.....
	public static final String KEY_PERCENT_TRANSLATED = "percent_translated";
	@Export
	public List<Entity> GetTranslationSummary(UserApplicationContext uctx,String entity_type,String lang,boolean sort_least_to_most,boolean hide_translated) throws PersistenceException,WebApplicationException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(user, CAN_MANAGE_TRANSLATIONS);
		if(!isMultiLingualEntity(entity_type))
			throw new WebApplicationException(entity_type+" IS NOT A MULTILINGUAL ENTITY TYPE");

		//#stays this way
		Query q = new Query(entity_type);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		q.cacheResults(false);
		QueryResult result = QUERY(q);

		List<Entity> ret = new ArrayList<Entity>();
		for(int i = 0;i < result.size();i++)
		{
			Entity e = result.getEntities().get(i);
			double tp = get_translation_total_percent(e, lang);
			if(tp == 100 && hide_translated)
				continue;
			e.setAttribute(KEY_PERCENT_TRANSLATED,tp);
			ret.add(e);
		}
		sort_by_translation_totals(ret, sort_least_to_most);
		return ret;
	}

	private double get_translation_total_percent(Entity e,String lang)
	{
		String[] multilingual_fields 	= multilingual_entity_field_map.get(e.getType());
		if(multilingual_fields == null)
			return 100;
		int num_multilingual_fields 	= multilingual_fields.length;
		int tc 							= 0;
		for(int i = 0;i < num_multilingual_fields;i++)
		{
			String val = (String)e.getAttribute(multilingual_fields[i]+"_"+lang);
			if(val != null && !val.trim().equals(""))
				tc++;
		}
		return (tc/num_multilingual_fields) * 100;

	}

	private void sort_by_translation_totals(List<Entity> ee,final boolean least_to_most)
	{
		Collections.sort(ee, new Comparator<Entity>() {

		    public int compare(Entity e1, Entity e2)
		    {
		        double percent_translated_1 = (Double)e1.getAttribute(KEY_PERCENT_TRANSLATED);
		        double percent_translated_2 = (Double)e2.getAttribute(KEY_PERCENT_TRANSLATED);

		        if (percent_translated_1 > percent_translated_2)
		        {
		            return (least_to_most)?-1:1;
		        }else if (percent_translated_1 < percent_translated_2)
		        {
		            return (least_to_most)?1:-1;
		        }else
		        {
		            return 0;
		        }
		    }
		 });
	}




	protected void defineSite() throws PersistenceException,WebApplicationException
	{
		//do nothing by default
	}

	protected void installURLMappings() throws WebApplicationException,PersistenceException
	{
		getApplication().registerUrlMapping(".*", null, false, this);
	}

	protected void do_define_site() throws PersistenceException,InitializationException
	{
		START_TRANSACTION(getName()+" define_site");
		try{
			defineSite();
			installURLMappings();

		}catch(Exception e){
			ROLLBACK_TRANSACTION();
			e.printStackTrace();
			throw new InitializationException(e.getMessage());
		}
		COMMIT_TRANSACTION();

	}



	@Export
	public Entity GetSiteRoot(UserApplicationContext uctx) throws PersistenceException,WebApplicationException
	{
		GUARD((Entity)uctx.getUser(), CAN_MANAGE_SITE_TREE);

		return (Entity)site_tree.getAttribute(TREE_FIELD_ROOT_NODE);

		//edit_tree = getTreeForUserByName(admin_user, "the_site_tree");
		//fillNode((Entity)edit_tree.getAttribute(TREE_FIELD_ROOT_NODE), Integer.MAX_VALUE, Integer.MAX_VALUE);
		//FILL_DEEP_AND_MASK((Entity)edit_tree.getAttribute(TREE_FIELD_ROOT_NODE), FILL_ALL_FIELDS, MASK_NO_FIELDS);
		//return ((Entity)edit_tree.getAttribute(TREE_FIELD_ROOT_NODE));
	}

	@Export
	public Entity GetSiteMenu(UserApplicationContext uctx,String lang) throws PersistenceException
	{
		try{
			GUARD((Entity)uctx.getUser(), CAN_MANAGE_SITE_TREE);
			return get_site_manager_site_menu(lang);
		}catch(PermissionsException pe)
		{

			Entity e =  get_published_site_menu(lang);
			return e;
		}


	}


	//return null if the root node is unpublished//
	public Entity calculate_published_site_tree(Entity edit_site_root) throws PersistenceException
	{
		edit_site_root = CLONE_IN_MEMORY(edit_site_root, new clone_policy(){
			public int exec(Entity e,String fieldname,Entity reference_val)
			{
				if(fieldname.equals(FIELD_CREATOR))
					return NULLIFY_REFERENCE;
				else
					return CLONE_REFERENCE;
			}
		}, new HashMap<Entity,Entity>());
		Entity data = EXPAND((Entity)edit_site_root.getAttribute(TREE_NODE_FIELD_DATA));
		if(data == null)
			return get_empty_site_root();

		int publication_status = (Integer)edit_site_root.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME);
		if(publication_status == SITE_MANAGER_STATUS_UNPUBLISHED)
			return get_empty_site_root();
		set_published_children(edit_site_root);
		return edit_site_root;
	}

	private void set_published_children(Entity tn) throws PersistenceException
	{
		//mask out stuff not needed for menu//
		tn.getAttributes().remove(FIELD_CREATOR);


		List<Entity> children = (List<Entity>)tn.getAttribute(TREE_NODE_FIELD_CHILDREN);
		List<Entity> published_children = new ArrayList<Entity>();
		for(int i = 0;i < children.size();i++)
		{
			Entity c 	= EXPAND(children.get(i));
			int publication_status = (Integer)c.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME);
			if(publication_status == SITE_MANAGER_STATUS_PUBLISHED)
			{
				set_published_children(c);
				published_children.add(c);
			}
		}
		tn.setAttribute(TREE_NODE_FIELD_CHILDREN, published_children);
	}

	private Entity get_empty_site_root()
	{
		Entity e = new Entity();
		e.setType(TREE_NODE_ENTITY);
		e.setAttribute(TREE_NODE_FIELD_CHILDREN,new ArrayList<Entity>());
		Entity data = new Entity();
		data.setType(TEXT_PAGE_TYPE_ENTITY);
		data.setAttribute(PAGE_TYPE_FIELD_IN_NAV, false);
		data.setAttribute(PAGE_TYPE_FIELD_MENU_NAME, "");
		e.setAttribute(TREE_NODE_FIELD_DATA, data);
		return e;
	}

	///utility stuff//



	public P PAGE(String page_type,OBJECT data,P... children)
	{
		List<P> cc = new ArrayList<P>();
		for(int i = 0;i < children.length;i++)
			cc.add(children[i]);

		return new P(page_type,data,cc);
	}


	public void SITE(P root) throws PersistenceException,WebApplicationException
	{
		INFO("CLEARING SITE TREE");
		if(site_tree != null)
			deleteSubTree((Entity)site_tree.getAttribute(TREE_FIELD_ROOT_NODE), true, false, null);
		site_tree = createTree(admin_user, "the_site_tree", null, null, null);
		INFO("DEFINING SITE");
		setup_site_page(root,0,0);
		update_site_trees();
	}

	private void setup_site_page(P page,int depth,int cno) throws PersistenceException,WebApplicationException
	{

		INFO(make_space(depth*2)+"CREATING PAGE( "+depth+", "+cno+") "+page.page_type+" "+page.data);
		Entity tn;
		if(depth == 0)//root //id is implicit
		{
			tn = updateTreeNode((Entity)site_tree.getAttribute(TREE_FIELD_ROOT_NODE), page.page_type, "/", page.asPage());

		}
		else
		{
			tn = createTreeNode(admin_user, (Entity)page.data.get("_parent_peer_"), page.page_type, "", page.asPage());
			//set the id//
			UPDATE(tn,
					   TREE_NODE_FIELD_ID,get_node_id(tn));
		}

		Integer publication_status = (Integer)page.data.get(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME);
		if(publication_status != null)
		{
			UPDATE(tn,
					PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME,publication_status);
		}

		INFO(make_space(depth*2)+" node id is "+get_node_id(tn));

		//do the children
		List<P> cc = page.children;
		for(int i = 0;i < cc.size();i++)
		{
			P c = cc.get(i);
			c.data.put("_parent_peer_", tn);
			setup_site_page(c,depth+1,i);
		}

	}

	private String get_node_id(Entity p) throws PersistenceException
	{
		if(p.getAttribute(TREE_NODE_FIELD_PARENT_NODE) == null)//root node//
		{
			return "/";
		}

		List<String> parent_names = new ArrayList<String>();
		Entity temp = p;
		while(temp.getAttribute(TREE_NODE_FIELD_PARENT_NODE) != null)
		{
			Entity parent 		= EXPAND((Entity)temp.getAttribute(TREE_NODE_FIELD_PARENT_NODE));
			Entity parent_data 	= EXPAND((Entity)parent.getAttribute(TREE_NODE_FIELD_DATA));
			String menu_name 	= (String)parent_data.getAttribute(PAGE_TYPE_FIELD_MENU_NAME);
			parent_names.add(convert_for_id(menu_name));
			temp = parent;
		}

		//remove root//
		parent_names.remove(parent_names.size()-1);

		StringBuilder buf = new StringBuilder();
		buf.append("/");

		for(int i = parent_names.size()-1;i >=0 ;i--)
			buf.append(parent_names.get(i)+"/");


		Entity node 		= EXPAND(p);
		Entity node_data 	= EXPAND((Entity)node.getAttribute(TREE_NODE_FIELD_DATA));
		String menu_name 	= (String)node_data.getAttribute(PAGE_TYPE_FIELD_MENU_NAME);
		buf.append(convert_for_id(menu_name));
		return buf.toString();
	}

	private String convert_for_id(String s)
	{
		return gen_publishable_lookup_key(s);
	}


	private String make_space(int num)
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i < num;i++)
		{
			sb.append(' ');
		}
		return sb.toString();
	}

	protected class P
	{
		public List<P> children;
		public OBJECT data;
		public String page_type;


		public P(String page_type,OBJECT data,List<P> children)
		{
			this.page_type	= page_type;
			this.children 	= children;
			this.data 		= data;

		}

		public Entity asPage() throws PersistenceException,WebApplicationException
		{

			//using constructor that takes map of params here//
			Entity instance = NEW(page_type,
								  admin_user,data);
			return instance;

		}

	}

	@Export
	public Map<String,Object> GetPageData(UserApplicationContext uctx, String language, String url, Map<String,Object> inputs) throws PersistenceException,WebApplicationException
	{
		Map<String,Object> response 	 = new HashMap<String, Object>();
		Map<String,Object> systemdata	 = new HashMap<String, Object>();
		systemdata.put(CURRENT_DATE_KEY,new Date());

		Map<String,Object> pagemetadata  = new HashMap<String, Object>();
		Entity e = prepare_page_data(uctx, language, url, inputs, systemdata, pagemetadata);
		if(e == null)
			throw new WebApplicationException("UNABLE TO FIND DATA BY URL "+url);
		response.put(PAGEDATA_KEY,e.getAttributes());
		response.put(SYSTEMDATA_KEY,systemdata);
		response.put(PAGEMETADATA_KEY, pagemetadata);
		response.put(INPUTS_KEY, inputs);
		response.put(PAGEDATA_ENTITY, e);

		return response;
	}

	private final int[] strip_statuses_site_manager = new int[]{};
	private final int[] strip_statuses_public 	    = new int[]{SITE_MANAGER_STATUS_UNPUBLISHED};
	private Entity prepare_page_data(UserApplicationContext uctx,String lang,String url,Map<String,Object> inputs,Map<String,Object> systemdata,Map<String,Object> pagemetadata) throws WebApplicationException,PersistenceException
	{
		uctx.setProperty(CURRENT_LANG_KEY, lang);
		//strip trailing slashes
		while(url.endsWith("/") && url.length() > 1)
			url = url.substring(0,url.length()-1);

		//determine if the user gets to see published or all records//
		//if we had roles we might put the statuses in here that they could see//
		//for now it is admin only so glob will suffice
		//strip statuses are entities that are stripped from result set
		//deep.
		Object query_publication_status = Query.VAL_GLOB;
		int[] strip_statuses 			= strip_statuses_site_manager;
		try{
			GUARD((Entity)uctx.getUser(), CAN_MANAGE_SITE_TREE);
		}catch(Exception pe)
		{
			query_publication_status = SITE_MANAGER_STATUS_PUBLISHED;
			strip_statuses = strip_statuses_public;
		}

		//look for an entity than a site node//
		Entity e = lookup_entity_by_permalink(url, query_publication_status);
		if(e == null)
		{
			Entity tn = lookup_site_node_by_id( url, query_publication_status);
			if(tn != null)
			{   //put in pagetype specific stuff
				systemdata.put("selected_node",tn);
				systemdata.put("index_from_root",getIndexFromRoot(tn));
				e = (Entity)tn.getAttribute(TREE_NODE_FIELD_DATA);
			}
		}
		if(e != null)
		{
			systemdata.put("current_lang",lang);

			prune_statuses(e, strip_statuses);
			e = translate_page_data(lang, e);
			page_preparer pp = (page_preparer)page_preparers.getFirstMatch(e.getType());
			if(pp != null)
			{
				Map<String,Object> additional_page_data = new HashMap<String, Object>();
				try{
					//TODO:SHOULD PROBABALY DETERMINE IF THE PAGE PREPARER
					//IS TRANSACTION PROTECTED
					WebStoreModule.START_TRANSACTION(store,getName()+" prepare_page_data "+url);
					pp.execute(uctx,e, inputs, additional_page_data);
					WebStoreModule.COMMIT_TRANSACTION(store);
				}
				catch(Exception ee)
				{
					WebStoreModule.ROLLBACK_ALL_ACTIVE_TRANSACTIONS(store);
					throw new WebApplicationException(ee.getMessage(), ee);
				}
				catch(Throwable t)
				{
					WebStoreModule.ROLLBACK_ALL_ACTIVE_TRANSACTIONS(store);
					throw new WebApplicationException(t.getMessage(), t);
				}
				set_additional_page_data(lang,e,systemdata,additional_page_data,strip_statuses);
			}

			return e;
		}

		return null;
	}


	private static final int ERR_CODE_ENTITY_NOT_FOUND = 0x01;
	private Entity lookup_entity_by_permalink(String permalink,Object query_publication_status) throws WebApplicationException,PersistenceException
	{
		//check for direct entity link
		//get rid of leading slash so we can match /Project/blah-blah.html more easily
		String stripped_request_path = permalink;
		while(stripped_request_path.length() > 1 && stripped_request_path.charAt(0) == '/')
			stripped_request_path = stripped_request_path.substring(1);

		int slash_idx 		= stripped_request_path.indexOf('/');
		String entity_name 	= null;

		if(slash_idx != -1)
		{
			entity_name 		= publishable_entity_original_names.get(stripped_request_path.substring(0,slash_idx));
			EntityIndex idx    	= publishable_entity_index_map.get(entity_name);
			if(idx != null)
			{
				int dot_idx       = stripped_request_path.lastIndexOf('.');
				String lookup_key = null;
				if(dot_idx != -1)
					lookup_key = stripped_request_path.substring(slash_idx+1,dot_idx).toLowerCase();
				else
					lookup_key = stripped_request_path.substring(slash_idx+1).toLowerCase();//ajax/amf will be looking up by permalink which doesnt have extension//

				INFO("LOOKUP KEY IS "+lookup_key+" IDX IS "+idx);
				//#stays
				Query q = new Query(entity_name);
				q.idx(idx.getName());
				q.eq(Query.l(lookup_key,query_publication_status));
				//q.cacheResults(false);
				QueryResult r = /*super.*/QUERY(q);
				if(r.getEntities().size()==0)
				{
					//response.setStatus(404);
					//return true;
					throw new WebApplicationException("Couldn't find Entity of type "+entity_name+" by permalink "+permalink,ERR_CODE_ENTITY_NOT_FOUND);
				}
				else
				{
					//response.setContentType("text/html; charset=UTF-8");
					//PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF8"), true);

					if(r.getEntities().size() > 1)
					{
						WARNING("HEY THERE IS MORE THAN ONE PAGE WITH THE PERMALINK "+lookup_key);
						WARNING("THERE ARE "+r.getEntities().size());
						WARNING(String.valueOf(r.getEntities()));
					}

					Entity data_entity =  FILL_DEEP_AND_MASK(r.getEntities().get(0),  FILL_ALL_FIELDS, new String[]{FIELD_CREATOR});





					return data_entity;
				}
			}
		}
			return null;
	}

	public Entity getDataForURL(String url) throws WebApplicationException,PersistenceException
	{
		//look for an entity than a site node//
		Entity e = lookup_entity_by_permalink(url, Query.VAL_GLOB);
		if(e == null)
		{
			Entity tn = lookup_site_node_by_id( url, Query.VAL_GLOB);
			return (Entity)tn.getAttribute(TREE_NODE_FIELD_DATA);
		}
		return e;
	}

	private Entity lookup_site_node_by_id(String node_id,Object query_publication_status) throws WebApplicationException,PersistenceException
	{
		//check for tree node//
		Entity tn = getTreeNodeByIdByPublicationStatus(site_tree, node_id.toLowerCase(), query_publication_status);
		if(tn != null)
		{
			FILL_DEEP_AND_MASK((EXPAND((Entity)tn.getAttribute(TREE_NODE_FIELD_DATA))),FILL_ALL_FIELDS,MASK_CREATOR);
			return tn;
		}
		return null;
	}


	/* ihttp request handler */


	@Override
	public boolean handleRequest(UserApplicationContext userContext,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException, WebApplicationException {

		//we set this so we have access to it in the QUERY modify functions below
		//as well as to have it available for any module function which uses
		//application.getCallingUserContext. the module dispatch usually does
		//this but we aint going through a module here babi! this is old skool.
		getApplication().setCallingUserContext(userContext);
		String requestPath  = Util.decodeURIComponent(request.getRequestURI().substring(request.getContextPath().length()));
		String current_lang = get_current_language(request);

		INFO(getName()+" RequestPath is "+requestPath);
		INFO(getName()+" current lang is "+current_lang);
		try{
			Map<String,Object> inputs 		= prepare_page_params(request);
			Map<String,Object> systemdata 	= new HashMap<String,Object>();
			Map<String,Object> pagemetadata = new HashMap<String,Object>();

			Entity data_entity = null;
			try{
				//WebStoreModule.START_TRANSACTION(store);
				data_entity 				= prepare_page_data(userContext, current_lang, requestPath, inputs, systemdata,pagemetadata);
				//WebStoreModule.COMMIT_TRANSACTION(store);
			}catch(WebApplicationException wae)
			{
				//WebStoreModule.ROLLBACK_ALL_ACTIVE_TRANSACTIONS(store);
				if(wae.getCode() == ERR_CODE_ENTITY_NOT_FOUND)
				{
					//response.setStatus(404);
					return false;
				}
				throw wae;
			}
			catch(Exception e)
			{
				//WebStoreModule.ROLLBACK_ALL_ACTIVE_TRANSACTIONS(store);
				throw new WebApplicationException(e.getMessage(), e);
			}
			catch(Throwable t)
			{
				//WebStoreModule.ROLLBACK_ALL_ACTIVE_TRANSACTIONS(store);
				throw new WebApplicationException(t.getMessage(), t);
			}


			if(data_entity == null)
				return false;//this gateway didnt handle request//


			String data_type = data_entity.getType();
			String template_name;
			String page_title;
			if(isPageTypeEntity(data_type))
			{
				template_name = "page-types/"+page_type_template_map.get(data_type);
				page_title = String.valueOf(data_entity.getAttribute(PAGE_TYPE_FIELD_MENU_NAME));
			}
			else
			{
				template_name = "entities/"+data_entity.getType()+".fhtml";
				String lookup_field_name  = publishable_entity_field_map.get(data_type);
				page_title = String.valueOf(data_entity.getAttribute(lookup_field_name));
				if(page_title == null)
					page_title = "";


			}
			pagemetadata.put("title",Text.stripTags(page_title));
			systemdata.put("template_name",template_name);


			Map<String,Object> template_data = setup_template_context_object
			(
					userContext,
					current_lang,
					requestPath,
					request.getServerName(),
					request.getParameterMap(),
					data_entity,
					systemdata,
					pagemetadata,
					inputs
			);

			PrintWriter out 				 = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF8"), true);
			response.setContentType("text/html; charset=UTF-8");

			if(fm_renderer.templateExists(template_name))
			{
				fm_renderer.render(template_name,template_data,out);
				return true;
			}
			else
			{
				out.println("<pre>\n\n\n");
				out.println("template not found "+template_name+".fhtml\nDATA WAS:\n"+data_entity.toString());
				out.println("</pre>");
				return true;
			}



		}catch(Exception e)
		{
			//probably go to error page here
			ERROR(e);
			response.setStatus(500);
			throw new WebApplicationException("UNABLE TO EXEC URL: "+requestPath+"\n MESSAGE WAS "+e.getMessage());

		}

	}


	private void set_additional_page_data(String current_lang,Entity data,Map<String,Object> system_data,Map<String,Object> additional_data,int[] strip_statuses) throws PersistenceException
	{
		Iterator<String> keys = additional_data.keySet().iterator();
		Map<String,Object> entity_data = data.getAttributes();
		while(keys.hasNext())
		{
			String k  = keys.next();
			Object val = additional_data.get(k);
			if(k.startsWith("system."))
			{
				system_data.put(k.substring(7), val);
			}
			else if(val instanceof QueryResult)
			{
				QueryResult  rr = (QueryResult)val;
				List<Entity> translated_results = new ArrayList<Entity>();
				List<Entity> ee = rr.getEntities();
				for(int i = 0;i < ee.size();i++)
				{
					Entity e = translate_page_data(current_lang, prune_statuses(ee.get(i),strip_statuses));
					translated_results.add(e);
				}
				val = translated_results;
			}
			else if(val instanceof Entity)
			{
				Entity e 	= (Entity)val;
				val = translate_page_data(current_lang, prune_statuses(e,strip_statuses));
			}

			entity_data.put(k, val);
		}
	}

	private static final String GLOBAL_PARAM_OFFSET 	= "offset$";
	private static final String GLOBAL_PARAM_PAGESIZE 	= "pagesize$";
	private static final String GLOBAL_PARAM_PAGENO 	= "pageno$";
	private static final Object[] GLOBAL_INPUT_TYPES 	= new Object[]{GLOBAL_PARAM_OFFSET,Types.TYPE_INT,GLOBAL_PARAM_PAGESIZE,Types.TYPE_INT,GLOBAL_PARAM_PAGENO,Types.TYPE_INT};
	private PS_MATCHER_LIST input_types 				= new PS_MATCHER_LIST();

	protected void definePageInputTypes()
	{
		for(int i = 0;i < GLOBAL_INPUT_TYPES.length;i+=2)
		{
			DEFINE_PAGE_INPUT_TYPE((String)GLOBAL_INPUT_TYPES[i], (Integer)GLOBAL_INPUT_TYPES[i+1]);
		}
	}


	protected void DEFINE_PAGE_INPUT_TYPE(String regexp,int type)
	{
		FieldDefinition d;
		if(type == Types.TYPE_REFERENCE)
			d = new FieldDefinition(regexp, type,"");//ref type is unused since the type is encoded in the url//
		else
			d = new FieldDefinition(regexp, type);
		input_types.add(regexp, d, true);
	}
	protected void DEFINE_PAGE_INPUT_TYPE(String regexp,String ref_type)
	{
		FieldDefinition d = new FieldDefinition(regexp,Types.TYPE_REFERENCE,ref_type);
		input_types.add(regexp, d, true);
	}



	private Map<String,Object> prepare_page_params(HttpServletRequest request) throws PersistenceException
	{
		Map<String,Object> pp 	   = request.getParameterMap();
		Map<String,Object> ret 	   = new HashMap<String,Object>();
		Enumeration<String> keys   = request.getParameterNames();
		while(keys.hasMoreElements())
		{
			String key = keys.nextElement();
			FieldDefinition d = (FieldDefinition)input_types.getFirstMatch(key);
			if(d == null)
			{
				ret.put(key, Util.decodeURIComponent(((String)request.getParameter(key))));
				continue;
			}
			switch(d.getType())
			{
				case Types.TYPE_INT:
					ret.put(key, new Integer((String)request.getParameter(key)));
					break;
				case Types.TYPE_LONG:
					ret.put(key, new Long((String)request.getParameter(key)));
					break;
				case Types.TYPE_FLOAT:
					ret.put(key, new Float((String)request.getParameter(key)));
					break;
				case Types.TYPE_DOUBLE:
					ret.put(key, new Double((String)request.getParameter(key)));
					break;
				case Types.TYPE_STRING:

					ret.put(key,(Util.decodeURIComponent(((String)request.getParameter(key)))));

					break;
				case Types.TYPE_REFERENCE:
					String[] parts = request.getParameter(key).split(":");
					ret.put(key,FILL_DEEP(GET(parts[0],Long.parseLong(parts[1]))));
					break;
				default:
						ret.put(key,((String)request.getParameter(key)));
						break;
			}
		}
		return ret;
	}


	private PS_MATCHER_LIST page_preparers = new PS_MATCHER_LIST();
	private Class<?>[] prepare_page_params    = new Class<?>[]{UserApplicationContext.class,Entity.class,Map.class,Map.class};
	protected void definePagePreparers() throws InitializationException
	{

	}

	protected void DEFINE_PAGE_PREPARER(String regexp,String method_name) throws InitializationException
	{
		DEFINE_PAGE_PREPARER(regexp, this, method_name);
	}

	protected void DEFINE_PAGE_PREPARER(String regexp,Object ref, String method_name) throws InitializationException
	{
		Method m = LOOKUP_METHOD(ref, method_name, prepare_page_params);
		if(m == null)
			throw new InitializationException("UNABLE TO LOCATE METHOD "+method_name+" ON "+ref+" ARE YOU SURE IT HAS THE RIGHT SIGNATURE TO PREPARE A PAGE?");

		page_preparers.add(regexp,new method_page_preparer(ref, m),false);
	}

	abstract class page_preparer
	{
		abstract void execute(UserApplicationContext uctx, Entity data_entity,Map<String,Object> inputs,Map<String,Object> data) throws WebApplicationException;
	}

	class method_page_preparer extends page_preparer
	{

		private Method pp_method;
		private Object pp_ref;
		public method_page_preparer(Object ref,Method m)
		{
			this.pp_ref    = ref;
			this.pp_method = m;
		}

		public void execute(UserApplicationContext uctx,Entity data_entity,Map<String,Object> inputs,Map<String,Object> data) throws WebApplicationException
		{
			try{
				pp_method.invoke(pp_ref, uctx,data_entity,inputs,data);
			}catch(Exception e)
			{
				e.printStackTrace();
				throw new WebApplicationException("COULDNT EXECUTE PAGE PREPARER "+e.getMessage());
			}

		}
	}

	//placeholder//
	class js_page_preparer extends page_preparer
	{
		public js_page_preparer(File js)
		{

		}

		public void execute(UserApplicationContext uctx,Entity data_entity,Map<String,Object> params,Map<String,Object> data) throws WebApplicationException
		{
			try{



			}catch(Exception e)
			{
				throw new WebApplicationException("COULDNT EXECUTE PAGE PREPARER "+e.getMessage());
			}

		}
	}

	public static final String APPLICATION_KEY 		= "application";
	public static final String CONTEXT_KEY 			= "user_context";
	public static final String USER_KEY 			= "user";
	public static final String HOSTNAME_KEY			= "hostname";
	public static final String BASE_HOST_KEY		= "base_host";
	public static final String TOP_LEVEL_DOMAIN		= "top_level_domain";
	public static final String REQUEST_URL_KEY 		= "request_url";
	public static final String REQUEST_PARAMS_KEY 	= "params";

	public static final String PAGEDATA_KEY			= "data";
	public static final String PAGEMETADATA_KEY		= "page_metadata";
	public static final String PAGEDATA_ENTITY		= "entity";
	public static final String SYSTEMDATA_KEY		= "system";
	public static final String EXCEPTION_KEY 		= "exception";
	public static final String EXCEPTION_STRING_KEY = "exceptionString";
	public static final String UTIL_KEY		 		= "util";
	public static final String CURRENT_LANG_KEY		= "current_lang";
	public static final String CURRENT_DATE_KEY		= "current_date";
	public static final String USERAGENT_KEY		= "user_agent";
	public static final String INPUTS_KEY			= "inputs";


	@SuppressWarnings("unchecked")
	private Map<String, Object> setup_template_context_object(
			UserApplicationContext user_context,
			String current_lang,String request_path, String host_name,Map params,Entity pagedata,Map<String,Object> systemdata,Map<String,Object> pagemetadata,Map<String,Object> inputs)
	{
		fm_utils render_utils = new fm_utils(current_lang);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(APPLICATION_KEY, getApplication());
		data.put(CONTEXT_KEY, user_context);
		data.put(USER_KEY, user_context.getUser());
		data.put(UTIL_KEY,render_utils);
		data.put(CURRENT_LANG_KEY,current_lang);
		data.put(INPUTS_KEY, inputs);
		//
		Map<Object, Object> params0 = new HashMap<Object, Object>();
		Iterator i = params.keySet().iterator();
		while (i.hasNext())
		{
			Object k = i.next();
			Object[] o = (Object[]) params.get(k);
			if (o.length == 1)
				params0.put(k, o[0]);
			else
				params0.put(k, o);
		}
		data.put(REQUEST_PARAMS_KEY, params0);
		data.put(REQUEST_URL_KEY, request_path);
		String[] base_host_parts = host_name.split("\\.");
		String base_host 		= "localhost";
		String top_level_domain = "";
		if(base_host_parts.length > 1)
		{
			base_host 		 = base_host_parts[base_host_parts.length-2];
			top_level_domain = base_host_parts[base_host_parts.length-1];
		}
		systemdata.put(BASE_HOST_KEY,base_host);
		systemdata.put(TOP_LEVEL_DOMAIN,top_level_domain);
		systemdata.put(HOSTNAME_KEY,host_name);
		systemdata.put(CURRENT_DATE_KEY,new Date());
		systemdata.put(USERAGENT_KEY, user_context.getProperty("USER-AGENT"));

		data.put(PAGEDATA_KEY,pagedata.getAttributes());
		data.put(PAGEMETADATA_KEY,pagemetadata);
		data.put(PAGEDATA_ENTITY,pagedata);
		data.put(SYSTEMDATA_KEY,systemdata);


		return data;
	}



	//these are all bound into the util in freemarker for example ${util.LINK(project)}
	public class fm_utils
	{
		String modulename 			= resource_module.getName();
		String resource_entity 		= resource_module.getResourceEntityName();
		String resource_base_url	= null;


		private String current_lang;
		private Map<String, String> resource_url_map = new HashMap<String,String>();

		public fm_utils(String current_lang)
		{
			this.current_lang = current_lang;
			try{
				resource_base_url = resource_module.getResourceBaseURL();
			}catch(Exception e){e.printStackTrace();}
		}

		//${node.attributes.node_id}
		public String LINK(Entity e,Object...nvp)
		{
			if (e==null)
				return "UNABLE_TO_LINK_NULL";
			try{
			String query_string = get_query_string(nvp);
			if(e.getType().equals(TREE_NODE_ENTITY))
				return (String)e.getAttribute(TREE_NODE_FIELD_ID)+query_string;
			else if(isPublishableEntity(e.getType()))
				return "/"+e.getType().toLowerCase()+"/"+e.getAttribute(PUBLISHABLE_ENTITY_LOOKUP_FIELD_NAME)+".html"+query_string;

			return "UNABLE_TO_LINK_TO_"+e.getType()+":"+e.getId();
			}catch(Exception ee)
			{
				ERROR(ee);
				return "ERROR:SEE LOGS";
			}
		}

		public String LOCALIZE(String lang, String s)
		{
			return localize(lang, s);
		}

		private String get_query_string(Object...nvp) throws UnsupportedEncodingException
		{
			if(nvp.length == 0)
				return "";

			StringBuilder buf = new StringBuilder();
			buf.append("?");
			for(int i = 0;i < nvp.length;i+=2)
			{
				String key 	 = (String)nvp[i];
				Object value = nvp[i+1];
				if(value.getClass() == Entity.class)
					value = new String(((Entity)value).getType()+":"+((Entity)value).getId());

				buf.append(key+"="+Util.encodeURIComponent(String.valueOf(value)));
				buf.append("&");
			}
			buf.setLength(buf.length()-1);
			return buf.toString();
		}

		public String RESOURCE_URL(Entity resource)
		{
			if(resource == null)
				return "";
			String path_token = (String)resource.getAttribute(ResourceModule.RESOURCE_FIELD_PATH_TOKEN);
			return resource_base_url+"/"+path_token;
		}


		//TODO move to resource module and use lru cache
		public String RESOURCE_URL(Entity resource,int width,int height) throws WebApplicationException
		{
			String id = resource.getId()+"-"+width+","+height;
			String url = resource_url_map.get(id);
			if (url!=null)
				return url;
			url = resource_module.getResourcePreviewUrlWithDim(resource, width, height);
			resource_url_map.put(id,url);
			return url;
		}

		public List<Entity> GET_ALL(String type,String order_by)
		{
			try{
				Query q = new Query(type);
				q.idx(Query.PRIMARY_IDX);
				q.eq(Query.VAL_GLOB);
				q.orderBy(order_by);
				QueryResult result = SM_QUERY(q);
				return result.getEntities();
			}catch(Exception e)
			{
				ERROR(e);
				return new ArrayList<Entity>();
			}
		}

		public String STRIP_TAGS(String s)
		{
			if (s==null)
				return "";
			return stripTags(s).trim();
		}


		private String stripTags(String s)
		{
			if(s == null)
				return null;
			s = s.replaceAll("\\<(?!span).*?\\>","");//or s.replaceAll("\\<(?!td|br).*?\\>","")
			return s;
		}


		public String DUMP(Entity e)
		{
			try{
				StringBuilder buf = new StringBuilder();
				do_print_deep(e, 0, new HashMap<Entity, Entity>(), buf);
				return buf.toString();
			}catch(Exception ee)
			{

				ERROR(ee);
				return "ERROR IN DUMP SEE LOGS";
			}

		}

		public Object[] JOIN_NVP(Object[] kvp,Object[] kvp2)
		{
			return JOIN_KVP(kvp, kvp2);
		}
	}



	////////////////////////////WEBSTORE MODULE OVERRIDES////////////////////////////

	public static final String ENTITY_ATT_MANAGABLE						= "_smm_managable";
	public static final String ENTITY_ATT_PUBLISHABLE 					= "_smm_publishable";
	public static final String ENTITY_ATT_PUBLISHABLE_LOOKUP_FIELD 		= "_smm_publishable_lookup_field";
	public static final String PUBLISHABLE_ENTITY_LOOKUP_FIELD_NAME 	= "_smm_permalink";
	public static final String PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME 	= "_publication_status";

	public static final String ENTITY_ATT_MULTILINGUAL_FIELDS 			= "_smm_multilingual_fields";
	public static final String ENTITY_ATT_SEARCHABLE_FIELDS 			= "_smm_searchable_fields";

	public static final String ENTITY_ATT_IS_PAGE_TYPE					= "_smm_is_page_type";
	public static final String ENTITY_ATT_PAGE_TYPE_TEMPLATE_PATH		= "_smm_page_type_template";

	public static final int SITE_MANAGER_STATUS_UNPUBLISHED = 0x00;
	public static final int SITE_MANAGER_STATUS_PUBLISHED 	= 0x01;

	protected Map<String,EntityIndex> publishable_entity_index_map = new HashMap<String,EntityIndex>();
	protected Map<String,String> publishable_entity_original_names = new HashMap<String,String>();
	protected Map<String,String> publishable_entity_field_map      = new HashMap<String,String>();
	protected Set<String> managable_entities				       = new HashSet<String>();

	protected Map<String,String[]> multilingual_entity_field_map = new HashMap<String,String[]>();
	protected Map<String,String[]> searchable_entity_field_map   = new HashMap<String,String[]>();

	protected List<EntityDefinition> page_type_entity_list  = new ArrayList<EntityDefinition>();
	protected Map<String,String> 	 page_type_template_map = new HashMap<String,String>();

	protected boolean add_span_tag_pref = true;

	public EntityDefinition DEFINE_ENTITY(String entity_name,OBJECT atts,Object... args) throws PersistenceException,InitializationException
	{

		INFO("DEFINING "+entity_name);
		List<FieldDefinition> ff = UNFLATTEN_FIELD_DEFINITIONS(args);

		handle_managable_pre_def(atts,entity_name,ff);
		handle_publishable_pre_def(atts,entity_name,ff);
		handle_page_type_pre_def(atts,entity_name,ff);
		handle_multilingual_pre_def(atts,entity_name,ff);
		handle_searchable_pre_def(atts,entity_name,ff);
		EntityDefinition d = DEFINE_ENTITY(store,entity_name,ff);
		handle_managable_post_def(atts,d);
		handle_publishable_post_def(atts,d);
		handle_page_type_post_def(atts,d);
		handle_multilingual_post_def(atts,d);
		handle_searchable_post_def(atts,d);
		associated_entity_definitions.add(d);
		return d;

	}


	private void handle_managable_pre_def(OBJECT atts,String entity_name,List<FieldDefinition> ff) throws InitializationException,PersistenceException
	{
		Boolean p = (Boolean)atts.get(ENTITY_ATT_MANAGABLE);
		if(p != null && p)
		{
			managable_entities.add(entity_name);
		}
	}

	private void handle_publishable_pre_def(OBJECT atts,String entity_name,List<FieldDefinition> ff) throws InitializationException,PersistenceException
	{
		Boolean p = (Boolean)atts.get(ENTITY_ATT_PUBLISHABLE);
		if(p != null && p)
		{
			String f = (String)atts.get(ENTITY_ATT_PUBLISHABLE_LOOKUP_FIELD);
			ff.add(new FieldDefinition(PUBLISHABLE_ENTITY_LOOKUP_FIELD_NAME, Types.TYPE_STRING));
			FieldDefinition pf = new FieldDefinition(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME, Types.TYPE_INT);
			pf.setDefaultValue(SITE_MANAGER_STATUS_UNPUBLISHED);
			ff.add(pf);
			publishable_entity_field_map.put(entity_name,f);
		}
	}

	private void handle_multilingual_pre_def(OBJECT atts,String entity_name,List<FieldDefinition> ff) throws InitializationException,PersistenceException
	{
		String[] multilingual_fields = (String[])atts.get(ENTITY_ATT_MULTILINGUAL_FIELDS);

		if(multilingual_fields == null)
			return;
		for(int i = 0;i < multilingual_fields.length;i++)
		{
			String mf = multilingual_fields[i];
			int insert_at_idx = -1;
			for(int ii = 0;ii < ff.size();ii++)
			{
				if(mf.equals(ff.get(ii).getName()))
				{
					insert_at_idx = ii;
				}
			}
			if(insert_at_idx == -1)
				throw new InitializationException("CANT FIND FIELD "+mf+" IN MULTILINGUAL ENTITY DEFINITION.");
			FieldDefinition primary_lang_field = ff.get(insert_at_idx);
			for(int iii = 0; iii < additional_languages.length;iii++)
			{
				FieldDefinition alt_lang_field 	   = primary_lang_field.clone();
				alt_lang_field.setDefaultValue(null);
				alt_lang_field.setName(alt_lang_field.getName()+"_"+additional_languages[iii]);
				ff.add(++insert_at_idx, alt_lang_field);
			}

		}
		multilingual_entity_field_map.put(entity_name, multilingual_fields);
	}

	private void handle_searchable_pre_def(OBJECT atts,String entity_name,List<FieldDefinition> ff) throws InitializationException,PersistenceException
	{
		String[] searchable_fields = (String[])atts.get(ENTITY_ATT_SEARCHABLE_FIELDS);

		if(searchable_fields == null)
			return;

		searchable_entity_field_map.put(entity_name, searchable_fields);
	}

	private void handle_page_type_pre_def(OBJECT atts,String entity_name,List<FieldDefinition> ff) throws InitializationException,PersistenceException
	{
		Boolean p = (Boolean)atts.get(ENTITY_ATT_IS_PAGE_TYPE);
		if(p != null && p)
		{
			FieldDefinition mnf = new FieldDefinition(PAGE_TYPE_FIELD_MENU_NAME, Types.TYPE_STRING);
			mnf.setDefaultValue("");
			FieldDefinition inf = new FieldDefinition(PAGE_TYPE_FIELD_IN_NAV, Types.TYPE_BOOLEAN);
			inf.setDefaultValue(true);
			String template = (String)atts.get(ENTITY_ATT_PAGE_TYPE_TEMPLATE_PATH);
			if(template==null)
				throw new InitializationException("YOU MUST SPECIFY ATTRIBUTE "+ENTITY_ATT_PAGE_TYPE_TEMPLATE_PATH+" WHEN DEFINING A PAGE TYPE");
			FieldDefinition pf = new FieldDefinition(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME, Types.TYPE_INT);
			pf.setDefaultValue(SITE_MANAGER_STATUS_UNPUBLISHED);

			ff.add(0,inf );
			ff.add(0,mnf );
			ff.add(pf);

			String[] multilingual_fields = (String[])atts.get(ENTITY_ATT_MULTILINGUAL_FIELDS);
			String[] page_type_multilingual_fields = new String[]{PAGE_TYPE_FIELD_MENU_NAME};

			if(multilingual_fields == null)
				multilingual_fields = page_type_multilingual_fields;
			else
				multilingual_fields = CONCAT(page_type_multilingual_fields, multilingual_fields);
			atts.put(ENTITY_ATT_MULTILINGUAL_FIELDS,multilingual_fields);

		}
	}

	private void handle_managable_post_def(OBJECT atts,EntityDefinition d) throws InitializationException,PersistenceException
	{

	}

	private void handle_publishable_post_def(OBJECT atts,EntityDefinition d) throws InitializationException,PersistenceException
	{
		Boolean p = (Boolean)atts.get(ENTITY_ATT_PUBLISHABLE);
		if(p != null && p)
		{
			String entity_name = d.getName();
			//this will be used to lookup the entity from the url//
			EntityIndex ei = store.getEntityIndex(entity_name, "_smm_idx_by_"+PUBLISHABLE_ENTITY_LOOKUP_FIELD_NAME);
			if(ei == null)
				ei = store.addEntityIndex(entity_name, new String[]{PUBLISHABLE_ENTITY_LOOKUP_FIELD_NAME,PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME }, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, "_smm_idx_by_"+PUBLISHABLE_ENTITY_LOOKUP_FIELD_NAME+"_pub_status", null);

			//this will be used to join against queries//
			EntityIndex ei2 = store.getEntityIndex(entity_name, "_smm_idx_by_"+PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME);
			if(ei2 == null)
				store.addEntityIndex(entity_name, new String[]{PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME }, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, "_smm_idx_by_"+PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME, null);

			publishable_entity_original_names.put(entity_name, entity_name);
			publishable_entity_original_names.put(entity_name.toLowerCase(), entity_name);
			publishable_entity_index_map.put(entity_name,ei);

		}
	}

	private void handle_searchable_post_def(OBJECT atts,EntityDefinition d) throws InitializationException,PersistenceException
	{

	}

	private void handle_multilingual_post_def(OBJECT atts,EntityDefinition d) throws InitializationException,PersistenceException
	{
		String[] multilingual_fields = (String[])atts.get(ENTITY_ATT_MULTILINGUAL_FIELDS);
		if(multilingual_fields == null)
			return;
		INFO("MULTILINGUAL ENTITY "+d.getName()+":");
		INFO(d.toString());
	}

	private void handle_page_type_post_def(OBJECT atts,EntityDefinition d) throws InitializationException,PersistenceException
	{
		Boolean p = (Boolean)atts.get(ENTITY_ATT_IS_PAGE_TYPE);
		if(p != null && p)
		{
			page_type_entity_list.add(d);
			page_type_template_map.put(d.getName(), (String)atts.get(ENTITY_ATT_PAGE_TYPE_TEMPLATE_PATH));
		}
	}

	protected boolean isPublishableEntity(String entity_type)
	{
		return publishable_entity_field_map.containsKey(entity_type);
	}

	protected boolean isPageTypeEntity(String entity_type)
	{
		return page_type_template_map.containsKey(entity_type);
	}

	protected boolean isMultiLingualEntity(String entity_type)
	{
		return multilingual_entity_field_map.containsKey(entity_type);
	}

	protected String gen_publishable_lookup_key(String s)
	{
		s = s.trim();
		StringBuilder buf = new StringBuilder();
		char[] cc = new char[s.length()];
		s.getChars(0, s.length(), cc, 0);
		for(int i = 0;i < cc.length;i++)
		{
			char c = cc[i];
			if(Character.isLetterOrDigit(c))
				buf.append(c);
			else
				buf.append('-');
		}
		return buf.toString().toLowerCase();
	}

	protected String ensure_lookup_key_uniqueness(Entity e,String key) throws PersistenceException
	{
		EntityIndex idx = publishable_entity_index_map.get(e.getType());
		if (idx==null)
			return e.getType();
		//#stays
		Query q = new Query(e.getType());
		q.idx(idx.getName());
		q.eq(Query.l(key,Query.VAL_GLOB));
		if(QUERY(q).getEntities().size() <= 1)
			return key;
		else
			return key+"-"+e.getId();
	}


	public static final String DEFAULT_LANGUAGE = "default_lang";
	private String get_current_language(HttpServletRequest request)
	{
		String server_name = request.getServerName();
		int dot_idx 	   = server_name.indexOf('.');
		if(dot_idx == -1)//i.e. localhost
			return DEFAULT_LANGUAGE;
		String lang = server_name.substring(0,dot_idx);
		for(int i = 0;i < additional_languages.length;i++)
			if(lang.equals(additional_languages[i]))
				return lang;
		return DEFAULT_LANGUAGE;
	}

	private Entity translate_page_data(String current_lang,Entity data) throws PersistenceException
	{
		return translate_page_data(current_lang,data,add_span_tag_pref);
	}

	private Entity translate_page_data(String current_lang,Entity data,boolean add_span_tag) throws PersistenceException
	{
		if(current_lang == DEFAULT_LANGUAGE)
			return data;
		//only need to clone if not default_lang//
		//Entity cloned_data = CLONE_DEEP(data);//TODO: definitely don't want to do this!!!!!!!!
		//INFO("DATA");
		//DUMP_ENTITY(data);
		Entity cloned_data = CLONE_IN_MEMORY(data);
		//INFO("CLONED_DATA");
		//DUMP_ENTITY(cloned_data);
		do_translate(current_lang, cloned_data, new HashSet<Entity>(), add_span_tag);



		return cloned_data;
		//return data;
	}

	private void do_translate(String current_lang,Entity data,Set<Entity> seen_references,boolean add_span_tag) throws PersistenceException
	{

		if(current_lang == DEFAULT_LANGUAGE || data == null || seen_references.contains(data))
			return;
		seen_references.add(data);

		String entity_type = data.getType();

		EntityDefinition d = store.getEntityDefinition(entity_type);
		List<FieldDefinition> rf = d.getReferenceFields();
		List<FieldDefinition> ff = d.getFields();

		String[] multilingual_fields = multilingual_entity_field_map.get(entity_type);
		if(multilingual_fields != null)
		{
			for(int i = 0;i < ff.size();i++)
			{
				FieldDefinition f = ff.get(i);
				for(int ii = 0;ii < multilingual_fields.length;ii++)
				{
					if(f.getName().equals(multilingual_fields[ii]))
					{
						Object multilingual_value = data.getAttribute(f.getName()+"_"+current_lang);
						if(empty(multilingual_value))
							continue;

						Object processed_value;
						if (f.isArray())
						{
							List<Object> pvs = new ArrayList<Object>();
							for (Object o : (List)multilingual_value)
								pvs.add(process_translated_value(f,o,current_lang));
							processed_value = pvs;
						}
						else
							processed_value = process_translated_value(f,multilingual_value,current_lang);

						data.setAttribute(f.getName(), processed_value);
						break;
					}
				}
			}
		}

		for(int i=0;i < rf.size();i++)
		{
			FieldDefinition ref_field = rf.get(i);
			if(ref_field.isArray())
			{
				List<Entity> ee = (List<Entity>)data.getAttribute(rf.get(i).getName());
				if(ee == null)
					continue;
				for(int ii =0;ii < ee.size();ii++)
				{
					Entity val = ee.get(ii);
					do_translate(current_lang,val, seen_references, add_span_tag);
				}
			}
			else
			{
				Entity val = (Entity)data.getAttribute(rf.get(i).getName());
				do_translate(current_lang,val, seen_references, add_span_tag);
			}
		}

	}

	private Object process_translated_value(FieldDefinition f, Object o, String current_lang)
	{
		switch (f.getBaseType())
		{
		case Types.TYPE_STRING:
		case Types.TYPE_TEXT:
			return "<span lang=\""+current_lang+"\">"+o+"</span>";
		default:
			return o;
		}

	}

	private boolean empty(Object v)
	{
		if (v==null)
		{
			return true;
		}
		if (v instanceof String)
		{
			return v.equals("");
		}
		if (v instanceof List)
		{
			return ((List<?>) v).isEmpty();
		}
		return false;
	}

	private Entity prune_statuses(Entity data,int[] statuses) throws PersistenceException
	{
		prune_statuses(data, statuses, new HashSet<Entity>());
		return data;
	}

	private void prune_statuses(Entity data,int[] statuses,Set<Entity> seen_references) throws PersistenceException
	{

		if( data == null 					||
			seen_references.contains(data) 	||
			statuses.length == 0)
		{
			return;
		}



		seen_references.add(data);

		String entity_type = data.getType();
		//if(!isPublishableEntity(entity_type) && !isPageTypeEntity(entity_type))
		//	return;

		EntityDefinition d = store.getEntityDefinition(entity_type);
		List<FieldDefinition> rf = d.getReferenceFields();

		outer_outer:for(int i=0;i < rf.size();i++)
		{
			FieldDefinition ref_field = rf.get(i);
			if(ref_field.isArray())
			{
				List<Entity> ee = (List<Entity>)data.getAttribute(rf.get(i).getName());
				List<Entity> stripped = new ArrayList<Entity>();
				if(ee == null)
					continue;
				outer:for(int ii =0;ii < ee.size();ii++)
				{
					Entity val = ee.get(ii);
					if(val == null)
					{
						WARNING("SKIPPING!!! NULL REFERENCE INSIDE ARRAY FIELD "+ref_field.getName()+" OF "+data);
						continue;

					}
					for(int iii = 0;iii < statuses.length;iii++)
					{
						if(val.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME) != null && (Integer)val.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME) == statuses[iii])
							continue outer;
					}
					stripped.add(val);
					prune_statuses(val, statuses,seen_references);
				}
				data.setAttribute(rf.get(i).getName(), stripped);
			}
			else
			{
				Entity val = (Entity)data.getAttribute(rf.get(i).getName());
				if(val == null)
					continue outer_outer;
				for(int iii = 0;iii < statuses.length;iii++)
				{
					if(val.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME) != null && (Integer)val.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME) == statuses[iii])
					{
						data.setAttribute(rf.get(i).getName(), null);
						continue outer_outer;
					}
				}
				prune_statuses(val, statuses,seen_references);
			}
		}

	}

	public void fixPermalinks() throws WebApplicationException
	{
		try{
			Iterator<String> iter = publishable_entity_field_map.keySet().iterator();
			while(iter.hasNext())
			{
				String publishable_entity_type = iter.next();
				fix_permalinks(publishable_entity_type);

			}
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("FAILED FIXING PERMALINKS "+e.getMessage());
		}
	}

	private void fix_permalinks(String entity_type) throws PersistenceException,WebApplicationException
	{
		EntityDefinition d = store.getEntityDefinition(entity_type);
		//#stays
		Query q = new Query(entity_type);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		QueryResult result = QUERY(q);
		for(int i = 0;i < result.getEntities().size();i++)
		{
			Entity e = result.getEntities().get(i);
			String lookup_field_name  = publishable_entity_field_map.get(entity_type);
			String lookup_field_value = (String)e.getAttribute(lookup_field_name);

			set_permalink(e, lookup_field_value);

		}
	}

//	public void googleTranslate() throws PersistenceException
//	{
//		Translate.setHttpReferrer(_application.getConfig().getWebRootUrl());
//		for (String type : multilingual_entity_field_map.keySet())
//		{
//			Query q = new Query(type);
//			q.idx(Query.PRIMARY_IDX);
//			q.eq(Query.VAL_GLOB);
//			QueryResult result = QUERY(q);
//			for(int i = 0;i < result.getEntities().size();i++)
//			{
//				Entity e = result.getEntities().get(i);
//				googleTranslate(e);
//			}
//		}
//	}


//	public Entity googleTranslate(Entity e) throws PersistenceException
//	{
//
//		Map<String,Object> translated_fields = new HashMap<String,Object>();
//		String[] fields = multilingual_entity_field_map.get(e.getType());
//		for (String lang : additional_languages)
//		{
//			Language glang = lang.equals("cn") ? Language.CHINESE_SIMPLIFIED : Language.fromString(lang);
//			for (String field : fields)
//			{
//				String s = (String)e.getAttribute(field);
//				if (s==null || Text.stripTags(s).trim().equals(""))
//					continue;
//				try
//				{
//					if (s.length()>400)
//						s = s.substring(0,333);
//					String translation = Translate.execute(s, Language.ENGLISH, glang);
//					translated_fields.put(field+"_"+lang, translation);
//				} catch (Exception ex)
//				{
//					System.out.println("COULDNT TRANSLATE "+e.getType().toLowerCase()+" "+e.getId()+" // "+field+"_"+lang+" // "+ex.getMessage());
//				}
//			}
//		}
//
//		return UPDATE(e, translated_fields);
//	}



	@Override
	public void onEvent(Module src, ModuleEvent e)
			throws WebApplicationException {
		if(src.getName().equals(cms_module.getName()))
		{
			switch(e.type)
			{
				//we could have two cases here. in the update case we could listen to the pre
				//event and only update if it changed. this seems fine though
				case CmsModule.EVENT_ENTITY_PRE_UPDATE:
					Entity pre_update_instance = null;
					try{
						Entity db_instance = null;
						//maybe we should do this in the cms module//
						//package it up as an entity like this
						//and return to using candidate...who knows.
						pre_update_instance = new Entity();
						pre_update_instance.setType((String)e.getProperty("type"));
						pre_update_instance.setId((Long)e.getProperty("id"));
						pre_update_instance.setAttributes((Map<String,Object>)e.getProperty("values"));

						if(isPageTypeEntity(pre_update_instance.getType()))
						{
							Boolean in_menu   = (Boolean)pre_update_instance.getAttribute(PAGE_TYPE_FIELD_IN_NAV);
							if(in_menu == null)
							{
								db_instance = GET(pre_update_instance.getType(),pre_update_instance.getId());
								in_menu 	= (Boolean)db_instance.getAttribute(PAGE_TYPE_FIELD_IN_NAV);
								pre_update_instance.setAttribute(PAGE_TYPE_FIELD_IN_NAV, in_menu);
							}
							String menu_name  = (String)pre_update_instance.getAttribute(PAGE_TYPE_FIELD_MENU_NAME);
							if(menu_name == null)
							{
								db_instance = GET(pre_update_instance.getType(),pre_update_instance.getId());
								menu_name 	= (String)db_instance.getAttribute(PAGE_TYPE_FIELD_MENU_NAME);
								pre_update_instance.setAttribute(PAGE_TYPE_FIELD_MENU_NAME, menu_name);
							}

							if(in_menu && menu_name == null)
									throw new WebApplicationException("MENU NAME IS REQUIRED FOR THE PAGE IF "+PAGE_TYPE_FIELD_IN_NAV+" IS SET TO TRUE");
							if(menu_name != null)
								{
									//make sure the id is unique before we save anything//
									//this is in case this method is called internally without//
									//the transaction protect. we dont want to have lingering references//
								Entity db_tn = getTreeNodeByData(site_tree, pre_update_instance);
								Entity temp_tn = new Entity();
								temp_tn.setType(TREE_NODE_ENTITY);
								temp_tn.setAttribute(TREE_NODE_FIELD_PARENT_NODE, EXPAND((Entity)db_tn.getAttribute(TREE_NODE_FIELD_PARENT_NODE)));
								temp_tn.setAttribute(TREE_NODE_FIELD_DATA, pre_update_instance);
								String node_id 	  = get_node_id(temp_tn);
								Entity exisiting_node = getTreeNodeById(site_tree, node_id);

								if(exisiting_node != null && !exisiting_node.getAttribute(TREE_NODE_FIELD_DATA).equals(pre_update_instance))
									throw new WebApplicationException("NODE ID '"+node_id+"' ALREADY EXISTS.");

								}
						}
					}catch(PersistenceException pe)
					{
						throw new WebApplicationException("PERSISTENCE PROBLEM CHECKING NODE ID ON UPDATE FOR "+pre_update_instance);
					}
					break;
				case CmsModule.EVENT_ENTITY_POST_UPDATE:
				case CmsModule.EVENT_ENTITY_POST_CREATE:
					Entity instance  = (Entity)e.getProperty("instance");
					String entity_name = instance.getType();
					Map<String,Object> update_values = (Map<String,Object>)e.getProperty("values");
					//backwards compatability with old way of creating
					//which we are still using in the cms
					if(e.type == CmsModule.EVENT_ENTITY_POST_CREATE && update_values == null)
						update_values = ((Entity)e.getProperty("candidate")).getAttributes();


					if(isPublishableEntity(entity_name))
					{
						String lookup_field_name  = publishable_entity_field_map.get(entity_name);
						String lookup_field_value = (String)instance.getAttribute(lookup_field_name);
						if(e.type == CmsModule.EVENT_ENTITY_POST_UPDATE)
						{
							if(update_values.get(lookup_field_name) != null)
								set_permalink(instance, lookup_field_value);
						}
						else
							set_permalink(instance, lookup_field_value);

					}
					else if(isPageTypeEntity(entity_name))
					{
						try{
							Entity tn = getTreeNodeByData(site_tree, instance);
							String new_node_id = get_node_id(tn);
							INFO("UPDATING NODE ID FOR "+instance.getAttribute(PAGE_TYPE_FIELD_MENU_NAME)+" TO "+new_node_id);
							UPDATE(tn,
									TREE_NODE_FIELD_ID,new_node_id,
									PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME,instance.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME));
							update_site_trees();

						}catch(PersistenceException pe)
						{
							throw new WebApplicationException("BARFED TRYING TO LOOKUP PEER NODE OR UPDATE TREES FOR ENTITY "+pe.getMessage());
						}

					}

					try{
						String[] sfields = searchable_entity_field_map.get(instance.getType());
						if(sfields != null)
						{
							for(int i= 0;i < sfields.length;i++)
							{
								if(update_values.get(sfields[i]) != null)
								{
									update_search_handle(instance);
									break;
								}
							}
						}

					}catch(PersistenceException pe)
					{
						throw new WebApplicationException("BARFED TRYING TO UPDATE SEARCH HANDLE FOR ENTITY "+instance);
					}

					break;
				case CmsModule.EVENT_ENTITY_PRE_DELETE:
					Entity b4delete_instance = (Entity)e.getProperty("candidate");
					try{
						delete_old_search_handles(b4delete_instance);
					}catch(PersistenceException pe)
					{
						throw new WebApplicationException("BARFED TRYING TO DELETE SEARCH HANDLE FOR ENTITY "+b4delete_instance);
					}
					break;
				case CmsModule.EVENT_ENTITY_POST_DELETE:
					break;
			}

		}

	}

	private void set_permalink(Entity e,String permalink_value) throws WebApplicationException
	{
		try{
		String pk =  ((permalink_value == null)||"".equals(permalink_value.trim()))?String.valueOf(e.getType().toLowerCase()+"_"+e.getId()):gen_publishable_lookup_key(permalink_value);
		pk = ensure_lookup_key_uniqueness(e,pk);
		System.out.println("!! SETTING PK TO "+pk+" FOR "+e.getType()+":"+e.getId());
		UPDATE(e,
			   PUBLISHABLE_ENTITY_LOOKUP_FIELD_NAME,
			   pk);
		}catch(PersistenceException pe)
		{
			ERROR(pe);
			throw new WebApplicationException("BARFED TRYING TO UPDATE PERMALINK FOR ENTITY "+e);
		}
	}

	///QUERY OVERRIDE FOR WORKFLOW///
	public QueryResult SM_QUERY(Query q) throws PersistenceException{return super.QUERY(handle_query_modify_for_publication_status(q));}
	public QueryResult SM_QUERY_FILL(Query q) throws PersistenceException{return super.QUERY_FILL(handle_query_modify_for_publication_status(q));}
	public QueryResult SM_QUERY_FILL_DEEP(Query q) throws PersistenceException{return super.QUERY_FILL_DEEP(handle_query_modify_for_publication_status(q));}
	public QueryResult SM_QUERY_FILL(Query q,String... fill_fieldnames) throws PersistenceException{return super.QUERY_FILL(handle_query_modify_for_publication_status(q), fill_fieldnames);}
	public QueryResult SM_QUERY_FILL_DEEP(Query q,String... fill_fieldnames) throws PersistenceException{return super.QUERY_FILL_DEEP(handle_query_modify_for_publication_status(q), fill_fieldnames);}
	/* THIS FILLS ALL THE REFS BY DEFAULT. THE MASK FIELDS ARE APPLIED AFTER*/
	public QueryResult SM_QUERY_FILL_AND_MASK(Query q,String... mask_fields) throws PersistenceException{return super.QUERY_FILL_AND_MASK(handle_query_modify_for_publication_status(q), mask_fields);}
	public QueryResult SM_QUERY_FILL_DEEP_AND_MASK(Query q,String[] fill_fieldnames,String[] mask_fieldnames) throws PersistenceException{return super.QUERY_FILL_DEEP_AND_MASK(handle_query_modify_for_publication_status(q), fill_fieldnames,mask_fieldnames);}
	public PagingQueryResult SM_PAGING_QUERY(Query q) throws PersistenceException{return super.PAGING_QUERY(handle_query_modify_for_publication_status(q));}
	public PagingQueryResult SM_PAGING_QUERY_FILL(Query q) throws PersistenceException{return super.PAGING_QUERY_FILL(handle_query_modify_for_publication_status(q));}
	public PagingQueryResult SM_PAGING_QUERY_FILL_DEEP(Query q) throws PersistenceException{return super.PAGING_QUERY_FILL_DEEP(handle_query_modify_for_publication_status(q));}
	public PagingQueryResult SM_PAGING_QUERY_FILL(Query q,String... fill_fields) throws PersistenceException{return super.PAGING_QUERY_FILL(handle_query_modify_for_publication_status(q), fill_fields);}
	public PagingQueryResult SM_PAGING_QUERY_FILL_DEEP(Query q,String... fill_fields) throws PersistenceException{return super.PAGING_QUERY_FILL(handle_query_modify_for_publication_status(q), fill_fields);}
	public PagingQueryResult SM_PAGING_QUERY_FILL_AND_MASK(Query q,String... mask_fields) throws PersistenceException{return super.PAGING_QUERY_FILL_AND_MASK(handle_query_modify_for_publication_status(q), mask_fields);}
	public PagingQueryResult SM_PAGING_QUERY_FILL_DEEP_AND_MASK(Query q,String[] fill_fields,String[] mask_fields) throws PersistenceException{return super.PAGING_QUERY_FILL_DEEP_AND_MASK(handle_query_modify_for_publication_status(q),fill_fields,mask_fields);}
	public int SM_COUNT(Query q) throws PersistenceException{return super.COUNT(store,handle_query_modify_for_publication_status(q));}


	protected Query handle_query_modify_for_publication_status(Query q)
	{
		QueryNode root = q.getRootNode();
		if(publishable_entity_field_map.get(q.getReturnType()) != null || root.attributes.get(Query.ATT_INDEX_NAME) != null && root.attributes.get(Query.ATT_INDEX_NAME).equals(IDX_SEARCH_HANDLE_BY_TEXT_BY_LANG_BY_TYPE))
		{

			//optimization to not do intersect on internal site manager query//
			//if(root.attributes.get(Query.ATT_INDEX_NAME) != null && root.attributes.get(Query.ATT_INDEX_NAME).equals("_smm_idx_by__smm_permalink_pub_status"))
			//	return q;

			//System.out.println("Q WAS "+q);
			UserApplicationContext uctx = getApplication().getCallingUserContext();
			Object publication_status = Query.VAL_GLOB;
			try{
				GUARD((Entity)uctx.getUser(), CAN_MANAGE_SITE_TREE);
			}catch(Exception e)
			{
				publication_status = SITE_MANAGER_STATUS_PUBLISHED;
			}


			Query qq = new Query(q.getReturnType());
			qq.startIntersection();
					qq.insertQuery(q);
				qq.idx("_smm_idx_by_"+PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME);
				qq.eq(publication_status);
			qq.endIntersection();
			Integer order_order = (Integer)root.attributes.get(Query.ATT_ORDER_ORDER);
			if(order_order == null)
				qq.orderBy((String)root.attributes.get(Query.ATT_ORDER_FIELDNAME));
			else
				qq.orderBy((String)root.attributes.get(Query.ATT_ORDER_FIELDNAME),order_order);
			qq.offset((Integer)root.attributes.get(Query.ATT_OFFSET));
			qq.pageSize((Integer)root.attributes.get(Query.ATT_PAGE_SIZE));

			q = qq;
			//System.out.println("Q IS NOW "+q);
		}
		return q;
	}

//////SEARCH HANDLE STUFF
	//SEARCH HANDLE WRAPS ENTITIES SO WE CAN HAVE NON HOMGENOUS QUERY RESULTS
	//FROM FREETEXT STUFF


	@Export
	public PagingQueryResult FreeTextSearch(UserApplicationContext uctx,String lang,String type,List<String> query_vals,int query_op,int offset,int page_size) throws PersistenceException
	{
		//NOTE: right now we are joining against all SEARCH_RESULT_HANDLES
		//to see just the published ones(see handle_query_modify_for_publication_status)
		//as an optimization we could add two indexs on SEARCH_RESULT_HANDLE instead of one
		//
		//IDX_SEARCH_HANDLE_BY_TEXT_BY_LANG_BY_PUBLICATION_STATUS_BY_TYPE
		//IDX_SEARCH_HANDLE_BY_TEXT_BY_LANG_BY_TYPE_BY_PUBLICATION_STATUS
		//
		//in this paradigm we wouldnt do the join anymore. we would run the guard CAN_MANAGE_SITE_TREE
		//and insert the appropriate publication status and type
		//since either of those or both could be globs we need 2 indexes to handle this way
		//
		//this same logic could be applied to publishable entities as well perhaps

		Object iType = null;
		if(type.equals("*"))
			iType = Query.VAL_GLOB;
		else
			iType = type.hashCode();
		Integer iLang = lang.hashCode();

		Query q = new Query(SEARCH_HANDLE_ENTITY);
		q.idx(IDX_SEARCH_HANDLE_BY_TEXT_BY_LANG_BY_TYPE);
		switch(query_op)
		{

			case Query.FREETEXT_CONTAINS_ALL:
				q.textContainsAll(q.list(Query.FREETEXT_ALL_FIELDS,query_vals,/*start equalities*/iLang,iType));
				break;
			case Query.FREETEXT_CONTAINS_PHRASE:
				q.textContainsPhrase(q.list(Query.FREETEXT_ALL_FIELDS,query_vals,iLang,iType));
				break;
			case Query.FREETEXT_CONTAINS_ANY:
			default:
				q.textContainsAny(q.list(Query.FREETEXT_ALL_FIELDS,query_vals,iLang,iType));
				break;
		}
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(SEARCH_HANDLE_FIELD_RANK,Query.DESC);
		return SM_PAGING_QUERY_FILL_DEEP_AND_MASK(q, FILL_ALL_FIELDS, new String[]{FIELD_CREATOR});
	}


//////SEARCH HANDLE CALCULATION CALLED ON CMS CREATE/UPDATE //

	protected double calculateSearchRank(String lang,Entity e)
	{
		//System.out.println("!!!! CALCULATING SEARCH RANK FOR "+e.getType()+":"+e.getId()+" LANG "+lang);
		return 0.0;
	}

	private void update_search_handle(Entity e) throws PersistenceException
	{
		String[] searchable_fields = searchable_entity_field_map.get(e.getType());
		if(searchable_fields == null)
			return;

		//delete_old_search_handles(e);
		update_or_create_new_search_handle(e, searchable_fields);
	}

	private void delete_old_search_handles(Entity e) throws PersistenceException
	{
		Query q = new Query(SEARCH_HANDLE_ENTITY);
		q.idx(IDX_SEARCH_HANDLE_BY_DATA_BY_LANG);
		q.eq(q.list(e,Query.VAL_GLOB));
		List<Entity> result = QUERY(q).getEntities();
		for(int i = 0;i < result.size();i++)
			DELETE(result.get(i));
	}

	private Entity get_search_handle_by_data(Entity data,int lang) throws PersistenceException
	{
		Query q = new Query(SEARCH_HANDLE_ENTITY);
		q.idx(IDX_SEARCH_HANDLE_BY_DATA_BY_LANG);
		q.eq(q.list(data,lang));
		List<Entity> result = QUERY(q).getEntities();
		if(result.size() > 1)
		{
			WARNING("!!!!! SEARCH HANDLE FOR ENTITY "+data+" AND LANG "+lang+" HAS MORE THAN ONE RECORD ASSSOCIATED WITH IT");
		}
		if(result.size() > 0)
		{
			return result.get(0);
		}
		else
			return null;
	}

	private void update_or_create_new_search_handle(Entity data,String[] searchable_fields) throws PersistenceException
	{
		String text = get_search_handle_text(data, DEFAULT_LANGUAGE, searchable_fields);

		if(text.length() != 0)
		{
			Integer publication_status = (Integer)data.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME);
			if(publication_status == null)
				publication_status = SITE_MANAGER_STATUS_PUBLISHED;
			do_search_handle_update_or_create(DEFAULT_LANGUAGE, data, text, data.getType(), publication_status);
		}

		String default_lang_text = text;
		for(int i = 0;i < additional_languages.length;i++)
		{
			text = get_search_handle_text(data, additional_languages[i], searchable_fields);
			if(text.length() == 0 && default_lang_text.length() > 0)
				text = default_lang_text;

			Integer publication_status = (Integer)data.getAttribute(PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME);
			if(publication_status == null)
				publication_status = SITE_MANAGER_STATUS_PUBLISHED;
			do_search_handle_update_or_create(additional_languages[i], data, text, data.getType(), publication_status);
		}
	}

	private Entity do_search_handle_update_or_create(String lang,Entity data,String text,String type,int publication_status) throws PersistenceException
	{
		double rank 		 = calculateSearchRank(lang, data);
		Entity search_handle = get_search_handle_by_data(data, lang.hashCode());

		if(search_handle == null)
		{
			search_handle = NEW(SEARCH_HANDLE_ENTITY,
								   (Entity)data.getAttribute(FIELD_CREATOR),
								   SEARCH_HANDLE_FIELD_DATA,data,
								   SEARCH_HANDLE_FIELD_LANG,lang.hashCode(),
								   SEARCH_HANDLE_FIELD_TYPE,type.hashCode(),
								   SEARCH_HANDLE_FIELD_PUBLICATION_STATUS,publication_status,
								   SEARCH_HANDLE_FIELD_TEXT,text,
								   SEARCH_HANDLE_FIELD_RANK,rank);
		}
		else
		{
			search_handle = UPDATE(search_handle,
									SEARCH_HANDLE_FIELD_DATA,data,
									SEARCH_HANDLE_FIELD_LANG,lang.hashCode(),
									SEARCH_HANDLE_FIELD_TYPE,type.hashCode(),
									SEARCH_HANDLE_FIELD_PUBLICATION_STATUS,publication_status,
									SEARCH_HANDLE_FIELD_TEXT,text,
									SEARCH_HANDLE_FIELD_RANK,rank);
		}
		return search_handle;
	}

	private String get_search_handle_text(Entity data,String lang,String[] searchable_fields) throws PersistenceException
	{
		StringBuilder buf = new StringBuilder();
		String suffix = "_"+lang;
		if(lang == DEFAULT_LANGUAGE)
			suffix = "";
		for(int i = 0;i < searchable_fields.length;i++)
		{
			Object val = data.getAttribute(searchable_fields[i]+suffix);
			if(val == null || "".equals(val))
				continue;
			buf.append(String.valueOf(val));
			buf.append('\n');
			buf.append(('\u0000'));//break between fields//
			buf.append('\n');
		}
		//System.out.println("SEARCH HANDLE TEXT FOR "+lang+" IS "+buf.toString());
		return buf.toString();
	}


	private static final int SEARCH_RANK_THREAD_INTERVAL = (60 * 1000) * 60 * 12;//every 12 hours//
	private Thread search_rank_thread;
	private boolean search_rank_thread_running;
	private final Object SEARCH_RANK_THREAD_LOCK = new Object();
	private void start_search_rank_thread()
	{
			search_rank_thread_running = true;
			search_rank_thread = new Thread(getName()+"_search_rank_thread")
			{
				public void run()
				{
					while(search_rank_thread_running)
					{
						try{
							INFO(getName()+" UPDATING SEARCH RANKINGS");
							update_search_rankings();
							INFO(getName()+" DONE UPDATING SEARCH RANKINGS");
							Thread.sleep(SEARCH_RANK_THREAD_INTERVAL);
						}
						catch(InterruptedException ie)
						{
							//keep going
						}
						catch(Throwable t)
						{
							t.printStackTrace();
						}
					}
					INFO(getName()+" SEARCH RANK THREAD EXITED");
				}
			};
			search_rank_thread.setPriority(3);
			search_rank_thread.start();
	}

	private void update_search_rankings() throws PersistenceException,WebApplicationException,InterruptedException
	{

			List<String> current_searchable_entities = new ArrayList<String>();
			Iterator<String> it = searchable_entity_field_map.keySet().iterator();
			while(it.hasNext())
			{
				String searchable_type = it.next();
				current_searchable_entities.add(searchable_type);
				PAGE_APPLY_INTERRUPTABLE(SEARCH_RANK_THREAD_LOCK,40,searchable_type,new CALLBACK()
				{
					public Object exec(Object... args) throws Exception
					{
						Entity she = (Entity)args[0];
						update_search_handle(she);
						return CALLBACK_VOID;
					}
				});
			}

			//lets keep track of what types we indexed persistently
			String  p = GET_PROP("SMM_SEARCH_HANDLE_TYPES");
			if(p!=null)
			{
				String[] last_searchable_entities = p.split(",");
				ArrayList<String> delete_list = new ArrayList<String>();
				for(int i = 0;i < last_searchable_entities.length;i++)
				{
					if(!current_searchable_entities.contains(last_searchable_entities[i]))
						delete_list.add(last_searchable_entities[i]);
				}
				String[] d_sa = delete_list.toArray(new String[0]);
				//this does a table scan which is why
				//we pass in all the types at once.
				//if we added an index to SEARCH_HANDLE IDX_BY_TYPE
				//we could do them one at a time since we could look them
				//up by type.

				delete_search_handles_for_types(d_sa);

			}
			//ok all the search handles are updated put in some entries
			// so we can know what types are currently wrapped. we want to be
			//able to delete SEARCH_HANDLE entitiies if someone removes
			//ENTITY_ATT_SEARCHABLE fields from their entity def. In order
			//to do this we need to know what is currently indexed.
			StringBuilder buf = new StringBuilder();
			for(int ii = 0;ii < current_searchable_entities.size();ii++)
			{
				String type = current_searchable_entities.get(ii);
				buf.append(type);
				buf.append(',');
			}
			//remove last comma
			if(current_searchable_entities.size() != 0)
				buf.setLength(buf.length()-1);
			SET_PROP("SMM_SEARCH_HANDLE_TYPES", buf.toString());

	}


	private void delete_search_handles_for_types(final String... types) throws PersistenceException,WebApplicationException,InterruptedException
	{
		if(types.length == 0)
			return;
		for(int i = 0;i < types.length;i++)
			INFO("DELETING SEARCH HANDLES FOR TYPE "+types[i]);

		PAGE_APPLY_INTERRUPTABLE(SEARCH_RANK_THREAD_LOCK,40,SEARCH_HANDLE_ENTITY, new CALLBACK()
		{
			public Object exec(Object... args) throws Exception
			{
				Entity e = (Entity)args[0];
				for(int ii = 0;ii < types.length;ii++)
				{
					if(types[ii].hashCode() == (Integer)e.getAttribute(SEARCH_HANDLE_FIELD_TYPE))
						DELETE(e);
				}
				return CALLBACK_VOID;

			}
		});

	}




	@Override
	public void onDestroy() {
		super.onDestroy();
		if(search_rank_thread != null)
		{
			synchronized (SEARCH_RANK_THREAD_LOCK)
			{
				search_rank_thread_running = false;
				search_rank_thread.interrupt();
			}
		}
	}

	///////LOCALE STUFF
	//TODO:need to add a property for site-locales directory or refactor so email-templates,module-data and site-locales can use the same root
	Map<String,Map<String,String>> locale_strings = new HashMap<String, Map<String,String>>();

	@Export
	public String Localize(UserApplicationContext uctx,String lang,String s)
	{
		return localize(lang,s);
	}

	public String localize(String lang,String s)
	{
		if(lang == DEFAULT_LANGUAGE)
			return s;

		Map<String,String> dict = locale_strings.get(lang);
		if(dict == null)
			return s;
		String ret = dict.get(s);

		if(ret == null)
			return s;
		else
			return ret.trim();
	}

	protected File get_locale_base_dir()
	{
		return new File(getApplication().getConfig().getModuleDataDirectory().getParentFile(),"site-locales");
	}

	protected File get_locale_dir(String lang)
	{
		if(lang.equals(DEFAULT_LANGUAGE))
			lang = "default";
		return new File(get_locale_base_dir(),"locale."+lang);
	}

	protected File get_locale_strings_file(String lang)
	{
		return new File(get_locale_dir(lang),"strings.xml");
	}

	public void load_locale_data() throws InitializationException
	{
		try{
		File dir 		= getApplication().getConfig().getModuleDataDirectory().getParentFile();
		File locale_dir = new File(dir,"site-locales");
		locale_dir.mkdirs();

		File[] ff = locale_dir.listFiles();
		for(int i = 0;i < ff.length;i++)
		{
			File f = ff[i];
			if(f.isDirectory())
			{
				String dirname = f.getName();
				if(dirname.equals("locale.default"))
				{
					Map<String,String> dict = parseSiteDictXML( new File(f,"strings.xml"));
					locale_strings.put(DEFAULT_LANGUAGE, dict);
				}
				else if(dirname.startsWith("locale"))
				{
					int dot_idx = dirname.lastIndexOf('.');
					if(dot_idx != -1)
					{
						String lang = dirname.substring(dot_idx+1);
						if(CONTAINS(additional_languages,lang))
						{
							File sfile = new File(f,"strings.xml");
							if(!sfile.exists())
								continue;
							Map<String,String> dict = parseSiteDictXML(sfile);
							locale_strings.put(lang, dict);
						}
					}
				}
			}

		}

		if(locale_strings.get(DEFAULT_LANGUAGE) == null)
			locale_strings.put(DEFAULT_LANGUAGE, new HashMap<String,String>());
		//bootstrap additional languages with default keys if they exist
		for(int i = 0;i < additional_languages.length;i++)
		{
			Map<String,String> default_dict 		= locale_strings.get(DEFAULT_LANGUAGE);
			Map<String,String> additional_lang_map 	= locale_strings.get(additional_languages[i]);
			if(additional_lang_map == null)
			{
				locale_strings.put(additional_languages[i],default_dict);
			}
			else//merge in any new entries that are in the default lang map
			{
				Iterator<String> it = default_dict.keySet().iterator();
				while(it.hasNext())
				{
					String key = it.next();
					if(!additional_lang_map.containsKey(key))
						additional_lang_map.put(key, null);
				}
				//remove keys that aren't in the master default lang string.xml//
				//we accumulate the list to avoid a concuirrent modification exception
				List<String> remove_keys = new ArrayList<String>();
				it = additional_lang_map.keySet().iterator();
				while(it.hasNext())
				{
					String key = it.next();
					if(!default_dict.containsKey(key))
						remove_keys.add(key);
				}
				for(int r = 0;r < remove_keys.size();r++)
					additional_lang_map.remove(remove_keys.get(r));

			}
			dumpLocaleDataToDisk(additional_languages[i]);
		}

		}catch(Exception wae)
		{
			wae.printStackTrace();
			throw new InitializationException(wae.getMessage());
		}
	}


	public void dumpLocaleDataToDisk(String lang) throws WebApplicationException
	{
		try{
			Map<String,String> ls = getLocaleStringsMap(lang);
			if(ls == null)
				throw new WebApplicationException("NO LOCALE DATA EXISTS FOR LANG "+lang);

			File f = get_locale_strings_file(lang);
			if(!f.getParentFile().exists())
				f.getParentFile().mkdirs();
			emitSiteDictXML(lang, ls, f);
		}catch(Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("PROBLEM DUMPING LOCALE DATA TO DISK"+e.getMessage());
		}
	}

	public Map<String,String> getLocaleStringsMap(String lang)
	{
		return locale_strings.get(lang);
	}

	public void setLocaleStringsMap(String lang,Map<String,String> strings)
	{
		locale_strings.put(lang,strings);
	}



	public void emitSiteDictXML(String lang,Map<String,String> dict,File out) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException
	{
	       DocumentBuilderFactory factory
	         = DocumentBuilderFactory.newInstance();
	       DocumentBuilder builder = factory.newDocumentBuilder();
	       DOMImplementation impl = builder.getDOMImplementation();

	       Document doc = impl.createDocument(null,null,null);

	       Element root = doc.createElement("translation-unit");
			root.setAttribute("type", "site-dictionary");
			root.setAttribute("lang", lang);
			doc.appendChild(root);

			Iterator<String> it = dict.keySet().iterator();
			while(it.hasNext())
			{

				String key = it.next();
				String val = dict.get(key);

	    	   Element entry = doc.createElement("entry");
	    	   Element value = doc.createElement("value");

	    	   value.setTextContent(key);
	    	   entry.appendChild(value);

    		   Element translation_unit = doc.createElement("translation");
    		   translation_unit.setAttribute("lang", lang);
    		  if(val == null)
    			  val = "NULL";
    		   translation_unit.setTextContent(val);
    		   entry.appendChild(translation_unit);

    		   root.appendChild(entry);

			}

			String xml 			 = transform_document(doc);
			if(out.exists())
				out.delete();
			FileWriter fout 	 = new FileWriter(out);
			fout.write(xml);
			fout.close();

	}

	public String getSiteDictLang(File f) throws ParserConfigurationException, SAXException, IOException
	{
		 DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		 DocumentBuilder db = dbf.newDocumentBuilder();
		 Document doc = db.parse(f);
		 doc.getDocumentElement().normalize();
		 String lang = get_translation_document_language(doc);
		 return lang;
	}

	public Map<String,String> parseSiteDictXML(File f) throws ParserConfigurationException, SAXException, IOException
	{
		 Map<String,String> dict = new LinkedHashMap<String, String>();

		 DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		 DocumentBuilder db = dbf.newDocumentBuilder();
		 Document doc = db.parse(f);
		 doc.getDocumentElement().normalize();
		 Element root = doc.getDocumentElement();
		 String lang = get_translation_document_language(doc);
		 NodeList nodeLst = doc.getElementsByTagName("entry");

		for (int s = 0; s < nodeLst.getLength(); s++)
		{
			  Node fstNode = nodeLst.item(s);
			    if (fstNode.getNodeType() != Node.ELEMENT_NODE)
			    	continue;

			 Element entry_node = (Element) fstNode;
	    	 NodeList value_and_translation = entry_node.getElementsByTagName("value");
	    	 Element value_element = (Element) value_and_translation.item(0);
	    	 Element translation_element = null;

	    	 if(!lang.equals(SiteManagerModule.DEFAULT_LANGUAGE))
	    	 {
	    		 value_and_translation = entry_node.getElementsByTagName("translation");
	    		 translation_element = (Element) value_and_translation.item(0);
	    	 }

	    	 String key   = value_element.getTextContent();
	    	 String value = null;
	    	 if(!lang.equals(SiteManagerModule.DEFAULT_LANGUAGE))
	    		 value = translation_element.getTextContent();
	    	 if("NULL".equals(value) || "".equals(value))
	    		 value = null;

	    	 dict.put(key, value);
		}
		return dict;
		 //System.out.println("Root element " + doc.getDocumentElement().getNodeName());

	}

	public String transform_document(Document doc)  throws javax.xml.transform.TransformerException,javax.xml.transform.TransformerConfigurationException
	{
		DOMSource domSource = new DOMSource(doc);
       TransformerFactory tf = TransformerFactory.newInstance();
       Transformer transformer = tf.newTransformer();
       //transformer.setOutputProperty
        //   (OutputKeys.OMIT_XML_DECLARATION, "yes");
       transformer.setOutputProperty(OutputKeys.METHOD, "xml");
       transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
       transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "value translation");
       transformer.setOutputProperty
           ("{http://xml.apache.org/xslt}indent-amount", "4");
       transformer.setOutputProperty(OutputKeys.INDENT, "yes");
       java.io.StringWriter sw = new java.io.StringWriter();
       StreamResult sr = new StreamResult(sw);
       transformer.transform(domSource, sr);
       String xml = sw.toString();
       return xml;
	}

	public String get_translation_document_language(Document d)
	{
		return d.getDocumentElement().getAttribute("lang");
	}

	private static boolean CONTAINS(Object[]oo,Object o)
	{
		for(int i = 0;i < oo.length;i++)
			if(oo[i].equals(o))
				return true;
		return false;

	}

//////////////////////SITE MANAGER ENTITIES////////////////////

	public static final String PAGE_TYPE_FIELD_MENU_NAME 		 		= "menu_name";
	public static final String PAGE_TYPE_FIELD_IN_NAV 		 	 		= "in_nav";

	public static final String TEXT_PAGE_TYPE_ENTITY					= "TextPage";
	public static final String TEXT_PAGE_TYPE_ENTITY_FIELD_CONTENT		= "content";

	public static final String SEARCH_HANDLE_ENTITY						= "SMMSearchHandle";
	public static final String SEARCH_HANDLE_FIELD_TEXT					= "text";
	public static final String SEARCH_HANDLE_FIELD_LANG					= "language";
	public static final String SEARCH_HANDLE_FIELD_DATA					= "data";
	public static final String SEARCH_HANDLE_FIELD_TYPE					= "type";
	public static final String SEARCH_HANDLE_FIELD_RANK					= "rank";
	public static final String SEARCH_HANDLE_FIELD_PUBLICATION_STATUS	= PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME;
	//"_smm_idx_by_"+PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME


	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		super.defineEntities(config);

		ADD_FIELDS(TREE_NODE_ENTITY,
				   PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME,Types.TYPE_INT,SITE_MANAGER_STATUS_UNPUBLISHED);

		DEFINE_ENTITY(
				TEXT_PAGE_TYPE_ENTITY,
				OBJECT(ENTITY_ATT_IS_PAGE_TYPE,true,
						   ENTITY_ATT_PAGE_TYPE_TEMPLATE_PATH,"text-page.fhtml",
						   ENTITY_ATT_MULTILINGUAL_FIELDS,new String[]{"content"}),
			     TEXT_PAGE_TYPE_ENTITY_FIELD_CONTENT,Types.TYPE_TEXT,""
		);

		DEFINE_ENTITY(SEARCH_HANDLE_ENTITY,
					SEARCH_HANDLE_FIELD_DATA,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
					SEARCH_HANDLE_FIELD_TEXT,Types.TYPE_TEXT,null,
					SEARCH_HANDLE_FIELD_LANG,Types.TYPE_INT,null,
					SEARCH_HANDLE_FIELD_TYPE,Types.TYPE_INT,null,
					SEARCH_HANDLE_FIELD_RANK,Types.TYPE_DOUBLE,null,
					SEARCH_HANDLE_FIELD_PUBLICATION_STATUS,Types.TYPE_INT,null
		);


	}

	public static final String IDX_BY_NAME = "byName";
	public static final String IDX_TREE_NODE_BY_TREE_BY_DATA  						= "byTreeByData";
	public static final String IDX_TREE_NODE_BY_TREE_BY_ID_BY_PUBLICATION_STATUS  	= "byTreeByIdByPublicationStatus";
	public static final String IDX_SEARCH_HANDLE_BY_DATA_BY_LANG 				 	= "byDataByLang";
	public static final String IDX_SEARCH_HANDLE_BY_TEXT_BY_LANG_BY_TYPE 			= "byTextByLangByType";
	public static final String IDX_SEARCH_HANDLE_BY_PUBLICATION_STATUS 	 			= "_smm_idx_by_"+PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME;
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		super.defineIndexes(config);
		DEFINE_ENTITY_INDICES(TREE_NODE_ENTITY,
				ENTITY_INDEX(IDX_TREE_NODE_BY_TREE_BY_DATA,EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, TREE_NODE_FIELD_TREE,TREE_NODE_FIELD_DATA),
				ENTITY_INDEX(IDX_TREE_NODE_BY_TREE_BY_ID_BY_PUBLICATION_STATUS,EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, TREE_NODE_FIELD_TREE,TREE_NODE_FIELD_ID,PUBLISHABLE_ENTITY_PUBLICATION_STATUS_FIELD_NAME)

		);

		DEFINE_ENTITY_INDICES(SEARCH_HANDLE_ENTITY,
				ENTITY_INDEX(IDX_SEARCH_HANDLE_BY_DATA_BY_LANG,EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX,SEARCH_HANDLE_FIELD_DATA,SEARCH_HANDLE_FIELD_LANG),
				ENTITY_INDEX(IDX_SEARCH_HANDLE_BY_PUBLICATION_STATUS,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,SEARCH_HANDLE_FIELD_PUBLICATION_STATUS),
				ENTITY_INDEX(IDX_SEARCH_HANDLE_BY_TEXT_BY_LANG_BY_TYPE,EntityIndex.TYPE_MULTI_FIELD_FREETEXT_INDEX,SEARCH_HANDLE_FIELD_TEXT,SEARCH_HANDLE_FIELD_LANG,SEARCH_HANDLE_FIELD_TYPE)
		);
	}

}