package com.pagesociety.web.module.permissions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.module.user.UserModule;


public class DefaultPermissionsModule extends PermissionsModule 
{
	
	//role->modulename->permission->permevaluator//
	private Map<Integer,Map<String,Map<String,PermissionEvaluator>>> role_module_perm_pf_map;
	
	public static final int ROLE_PUBLIC = 0x3000000;
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		role_module_perm_pf_map =  new HashMap<Integer,Map<String,Map<String,PermissionEvaluator>>>();
		
		definePermissions();
	}
	

	public static final String GLOB="*";
	
	private Map<String,List<String>> module_permission_map = new HashMap<String,List<String>>();
	public void definePermission(String namespace,String permission)
	{
		List<String> pp = module_permission_map.get(namespace);
		if(pp == null)
		{
			pp = new ArrayList<String>();
			module_permission_map.put(namespace, pp);
		}
		pp.add(permission);
	}
	
	public List<String> getExportedPermissions(String module_name)
	{
		return module_permission_map.get(module_name);
	}
	
	public void bindPermission(int role,String namespace,String permission,PermissionEvaluator pf)
	{
		if(namespace == GLOB)
			do_role_glob(role,pf);
		else if(permission == GLOB)
			do_namespace_glob(role, namespace,pf);
		else
			do_bind_permission(role, namespace, permission, pf);

	}
	
	private void do_role_glob(int role,PermissionEvaluator pf) 
	{
		Iterator<String> it = module_permission_map.keySet().iterator();
		while(it.hasNext())
		{
			String namespace = it.next();
			do_namespace_glob(role, namespace,pf);
		}
		
	}
	
	private void do_namespace_glob(int role, String namespace,PermissionEvaluator pf) 
	{
		List<String> pids = module_permission_map.get(namespace);
		int s = pids.size();
		for(int i = 0;i < s;i++)
		{
			String perm_id = pids.get(i);
			do_bind_permission(role, namespace, perm_id, pf);
		}
	}



	private void do_bind_permission(int role,String namespace,String permission,PermissionEvaluator pf)
	{

		Map<String,Map<String,PermissionEvaluator>> module_to_perm_map = role_module_perm_pf_map.get(role);
		if(module_to_perm_map == null)
		{
			module_to_perm_map = new HashMap<String,Map<String,PermissionEvaluator>>();
			role_module_perm_pf_map.put(role, module_to_perm_map);
		}
		
		Map<String,PermissionEvaluator> perm_to_pf_map = module_to_perm_map.get(namespace);
		if(perm_to_pf_map == null)
		{
			perm_to_pf_map = new HashMap<String,PermissionEvaluator>();
			module_to_perm_map.put(namespace, perm_to_pf_map);
		}
		perm_to_pf_map.put(permission, pf);		
	}
	

	public boolean checkPermission(Entity user,String namespace,String permission_id,Map<String,Object> context) throws PersistenceException
	{
		List<Integer> roles = (List<Integer>)user.getAttribute(UserModule.FIELD_ROLES);
		if(user == null || roles == null || roles.size() == 0)
		{
			try{
				PermissionEvaluator pf = role_module_perm_pf_map.get(ROLE_PUBLIC).get(namespace).get(permission_id);
				return pf.exec(user, namespace,permission_id, context);
			}catch(NullPointerException npe)
			{
				
				return false;
			}			
		}

		for(int i = 0;i < roles.size();i++)
		{
			int role = roles.get(i);
			try{
				PermissionEvaluator pf = role_module_perm_pf_map.get(role).get(namespace).get(permission_id);
				
				if(pf.exec(user, namespace,permission_id, context))
					return true;
			}catch(NullPointerException npe)
			{
				continue;
			}			
		}
		
		return false;	
	}
	
	
	//override this one//
	public void definePermissions()
	{
		ROLE_PERMISSIONS(UserModule.USER_ROLE_WHEEL,GLOB,true);
		ROLE_PERMISSIONS(ROLE_PUBLIC,GLOB,false);
	}

		
	//PERMISSION MACROS////////////////////////////////////////////////
	protected class PermissionDescriptor
	{
		public String permission_id;
		public PermissionEvaluator pf;
	}
	
	protected class ModuleDescriptor
	{
		public String namespace;
		public PermissionDescriptor[] ppd;
	}
	
	protected class RoleDescriptor
	{
		public int role;
		public ModuleDescriptor[] mdf;
	}
	

	public void ROLE_PERMISSIONS(int role,ModuleDescriptor... mm)
	{
		for(int i = 0;i < mm.length;i++)
		{
			ModuleDescriptor md = mm[i];
			PermissionDescriptor[] ppd = mm[i].ppd;
			for(int ii = 0; ii < ppd.length;ii++)
				bindPermission(role,md.namespace, ppd[ii].permission_id, ppd[ii].pf);	
		}
	}
	
	public void ROLE_PERMISSIONS(int role,String glob)
	{
		ROLE_PERMISSIONS(role, glob, true);
	}
	
	public void ROLE_PERMISSIONS(int role,String glob,boolean enable)
	{
		if(enable)
			ROLE_PERMISSIONS(role,glob,PermissionEvaluator.ALWAYS_TRUE_EVALUATOR);
		else
			ROLE_PERMISSIONS(role,glob,PermissionEvaluator.ALWAYS_FALSE_EVALUATOR);
	}
	
	public void ROLE_PERMISSIONS(int role,String glob,PermissionEvaluator pf)
	{
		bindPermission(role, glob, null, pf);
	}
	
	public ModuleDescriptor MODULE_PERMISSIONS(String module_name,PermissionDescriptor... pp)
	{
		ModuleDescriptor md = new ModuleDescriptor();
		md.namespace = module_name;
		md.ppd = pp;
		return md;
	}
	
	public ModuleDescriptor MODULE_PERMISSIONS(String module_name,String glob,boolean enable)
	{
		if(enable)
			return MODULE_PERMISSIONS(module_name, glob, PermissionEvaluator.ALWAYS_TRUE_EVALUATOR);
		else
			return MODULE_PERMISSIONS(module_name, glob, PermissionEvaluator.ALWAYS_FALSE_EVALUATOR);

	}
	
	public ModuleDescriptor MODULE_PERMISSIONS(String module_name,String glob,PermissionEvaluator pf)
	{
		ModuleDescriptor md = new ModuleDescriptor();
		md.namespace = module_name;
		PermissionDescriptor pd = new PermissionDescriptor();
		pd.permission_id = GLOB;
		pd.pf = pf;
		md.ppd = new PermissionDescriptor[]{pd};
		return md;
	}
	
		
	public  PermissionDescriptor PERMISSION(String permission_id)
	{
		return PERMISSION(permission_id,true);
	}
	
	public  PermissionDescriptor PERMISSION(String permission_id,boolean enable)
	{
		if(enable)
			return PERMISSION(permission_id, PermissionEvaluator.ALWAYS_TRUE_EVALUATOR);
		else
			return PERMISSION(permission_id, PermissionEvaluator.ALWAYS_FALSE_EVALUATOR);

	}

	public  PermissionDescriptor PERMISSION(String permission_id,PermissionEvaluator pf)
	{
		PermissionDescriptor d = new PermissionDescriptor();
		d.permission_id = permission_id;
		d.pf = pf;
		return d;
	}
	
}
