package com.pagesociety.web.module.cms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.IEventListener;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleEvent;

public class CmsModuleWithDeletePolicyA extends CmsModule
{
	@Override
	public void init(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		super.init(app, config);
		addEventListener(new IEventListener()
		{
			@Override
			public void onEvent(Module src, ModuleEvent e) throws WebApplicationException
			{
				switch (e.type)
				{
				case EVENT_ENTITY_PRE_DELETE:
	                Entity instance  = (Entity)e.getProperty("candidate");
	                try
					{
						remove_deleted(instance);
					} catch (PersistenceException ex)
					{
						ex.printStackTrace();
						throw new WebApplicationException("cant delete references to "+instance,ex);
					}
				}
			}
		});
		
	}
	
	@Override
	public void loadbang(WebApplication app, Map<String, Object> config)
		throws InitializationException
	{
		super.loadbang(app, config);
		try
		{
			fix_data();
		} catch (PersistenceException ex)
		{
			ex.printStackTrace();
			throw new InitializationException("cant fix data",ex);
		}
	}
	
	
	private void fix_data() throws PersistenceException
	{
		List<EntityDefinition> defs = store.getEntityDefinitions();
		for (EntityDefinition d : defs)
		{
			fix_data(d);
		}

	}
	
	
	private void fix_data(final EntityDefinition def) 
    {

        try{
            START_TRANSACTION("cms");

            PAGE_APPLY(def.getName(), new CALLBACK(){
                
                public Object exec(Object... args) throws Exception
                {
                    Entity e = (Entity)args[0];
                    List<FieldDefinition> refs = def.getReferenceFields();
                    for (FieldDefinition r : refs)
                    {
                    	if (r.isArray())
                    	{
		                    List<Entity> refval =(List<Entity>) e.getAttribute(r.getName());
		                    if(refval != null)
		                    {
		                        for(int i = 0;i < refval.size();i++)
		                        {
		                            Entity v = refval.get(i);
		                            if (v == null)
		                            	continue;
		                            try{
		                                GET(r.getReferenceType(),v.getId());
		                            }
		                            catch(Exception ex)
		                            {
		                                refval.remove(v);
		                                i--;
		                                UPDATE(e,r.getName(),refval);
		                                INFO("deleted reference to "+v+" from "+e);
		                            }
		                        }
		                    }
                    	}
                    	else
                    	{
		                    Entity refval =(Entity) e.getAttribute(r.getName());
		                    if (refval == null)
		                    	continue;
		                    try{
                                GET(r.getReferenceType(),refval.getId());
                            }
                            catch(Exception ex)
                            {
                                UPDATE(e,r.getName(),null);
                                INFO("deleted reference to "+refval+" from "+e);
                            }
                    	}
                    }
                    return CALLBACK_VOID;
                }
            });
            COMMIT_TRANSACTION();
        }catch(Exception e)
        {
            try{ROLLBACK_TRANSACTION();}catch(Exception ee){ee.printStackTrace();}
            e.printStackTrace();
        }
    }
	
	
	private void remove_deleted(Entity deleted) throws PersistenceException
	{
		List<EntityDefinition> defs = store.getEntityDefinitions();
		for (EntityDefinition d : defs)
		{
			List<FieldDefinition> merefs = new ArrayList<FieldDefinition>();
            List<FieldDefinition> refs = d.getReferenceFields();
            for (FieldDefinition r : refs)
            {
            	if (r.getReferenceType().equals(deleted.getType()))
            		merefs.add(r);
            }
            if (!merefs.isEmpty())
            	remove_deleted(d,deleted,merefs);
		}
	}
	
	private void remove_deleted(final EntityDefinition def, final Entity deleted, List<FieldDefinition> refs)
	{
		try{
            START_TRANSACTION("cms");
            PAGE_APPLY(def.getName(), new CALLBACK(){
                
                public Object exec(Object... args) throws Exception
                {
                    Entity e = (Entity)args[0];
                    List<FieldDefinition> refs = def.getReferenceFields();
                    for (FieldDefinition r : refs)
                    {
                    	if (r.isArray())
                    	{
                    		List<Entity> refvalue =(List<Entity>) e.getAttribute(r.getName());
		                    if(refvalue != null && refvalue.contains(deleted))
		                    {
		                        refvalue.remove(deleted);
		                        UPDATE(e,r.getName(),refvalue);
                                INFO("deleted reference to "+deleted+" from "+e);
		                    }
                    	} else
                    	{
                    		Entity refval = (Entity) e.getAttribute(r.getName());
                    		if (refval==deleted)
                    		{
		                        UPDATE(e,r.getName(),null);
                                INFO("deleted reference to "+deleted+" from "+e);
                    		}
                    	}
                    }
                    return CALLBACK_VOID;
                }
            });
            COMMIT_TRANSACTION();
        }catch(Exception e)
        {
            try{ROLLBACK_TRANSACTION();}catch(Exception ee){ee.printStackTrace();}
            e.printStackTrace();
        }
    }

	
}