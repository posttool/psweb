package com.pagesociety.web.module.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebStoreModule;

public class DefaultPersistenceEvolver extends WebStoreModule implements IEvolutionProvider
{

	public String getName()
	{
		return "DefaultPersistenceEvolver";
	}
	
	@Override
	public void evolveIndexes(WebStoreModule.schema_receiver resolver, String entity_name,List<EntityIndex> existing_indices,List<EntityIndex> proposed_indices) throws SyncException
	{
		List<EntityIndex> delete_indices = new ArrayList<EntityIndex>();
		List<EntityIndex> add_indices	= new ArrayList<EntityIndex>();
	try{
				calc_added_and_deleted_indices(entity_name,existing_indices, proposed_indices, delete_indices, add_indices);
				if(delete_indices.size() == 0)
				{
					for(int i = 0;i < add_indices.size();i++)
					{
						EntityIndex idx;
						idx = add_indices.get(i);
						try{
							boolean go_on = confirm("ADD INDEX "+idx.getName()+" DECLARED BY "+resolver.getDeclaringModuleForIndex(entity_name,idx.getName() ) +" TO "+entity_name+"?");
							if(go_on)
								do_define_entity_index(entity_name, idx);
							else
								continue;
							INFO(getName()+": ADDING INDEX "+idx.getName()+" TO "+entity_name);
						}catch(PersistenceException p)
						{
							ERROR(p);
							throw new SyncException(getName()+": FAILED ADDING INDEX "+idx+" TO "+entity_name);
						}
					}
				}
				else if(add_indices.size() == 0)
				{
					for(int i = 0;i < delete_indices.size();i++)
					{
						EntityIndex idx;
						idx = delete_indices.get(i);
						try{
							boolean go_on = confirm("DELETE INDEX "+idx.getName()+" IN "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name+"?");
							if(go_on)
								store.deleteEntityIndex(entity_name, idx.getName());
							else
								continue;
							INFO(getName()+": DELETING INDEX "+idx.getName()+" FROM "+entity_name);
						}catch(PersistenceException p)
						{
							ERROR(p);
							throw new SyncException(getName()+": FAILED DELETING INDEX "+idx+" TO "+entity_name);
						}
					}
				}
				else
				{
			
					INFO(" WAS UNABLE TO AUTOMATICALLY RESOLVE THE EVOLUTION\n OF THE "+entity_name+" ENTITY IN "+resolver.getDeclaringModuleForEntity(entity_name)+" PLEASE ASSIST.");
					for(int i = 0;i < add_indices.size();i++)
					{
				
						EntityIndex idx;
						idx = add_indices.get(i);
						System.out.println("PROBLEM EVOLVING "+entity_name+" INDEX "+idx.getName()+" DECLARED BY "+resolver.getDeclaringModuleForIndex(entity_name, idx.getName()));
						StringBuilder question = new StringBuilder("Do you mean to:\n\t[0] Add index "+idx.getName()+" to "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);
						for(int j = 0;j < delete_indices.size();j++)
						{						
							question.append("\n\t["+(j+1)+"] Rename "+delete_indices.get(j).getName()+" to "+idx.getName()+" in "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);						
						}
						question.append("?\n"); 
						String answer = null;
						while(answer == null)
						{
							answer = GET_CONSOLE_INPUT(question.toString());
							answer = answer.toLowerCase();
							if(answer.equals("0"))
							{
								try{
									boolean go_on = confirm("ADD INDEX "+idx.getName()+" DECLARED BY "+resolver.getDeclaringModuleForIndex(entity_name,idx.getName())+" TO "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name+"? ");
									if(go_on)
										do_define_entity_index(entity_name, idx);
									else
										continue;
									INFO(getName()+": ADDED INDEX "+idx.getName()+" TO "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);
								}catch(PersistenceException p)
								{
									ERROR(p);
									throw new InitializationException(getName()+": FAILED ADDING INDEX "+idx+" TO "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);
								}
							}
							else
							{
								try{
									int delete_idx = Integer.parseInt(answer) - 1;
									EntityIndex renamed_index = delete_indices.get(delete_idx);
									if(delete_idx > delete_indices.size()-1)
										answer = null;
									else
									{
										//rename field...update delete list.
										boolean go_on = confirm("RENAME INDEX "+renamed_index+" TO "+idx.getName()+"? ");
										if(go_on)
											store.renameEntityIndex(entity_name, renamed_index.getName(), idx.getName());
										else
											continue;
										delete_indices.remove(delete_idx);
										INFO(getName()+": RENAMED INDEX "+renamed_index.getName()+" TO "+idx.getName()+" IN "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);
									}
								}catch(Exception e)
								{
									answer = null;
								}
							}
						}
					}
					for(int i = 0;i < delete_indices.size();i++)
					{
						EntityIndex idx = delete_indices.get(i);
						try{
							boolean go_on = confirm("DELETE INDEX "+idx.getName()+" IN "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name+"?");
							if(go_on)
								store.deleteEntityIndex(entity_name, idx.getName());
							else
								continue;
							INFO(getName()+": DELETED INDEX "+idx.getName()+" FROM "+entity_name);
						}catch(PersistenceException p)
						{
							ERROR(p);
							throw new InitializationException(getName()+": FAILED DELETING INDEX "+idx+" FROM "+entity_name);
						}
					}
				}
				
		}
		catch (SyncException se)
		{
			throw se;
		}
		catch(Exception e)
		{
			System.out.println("@@@@@@@@@@@@BAH!!!");
			e.printStackTrace();
			ERROR(e);
			throw new SyncException("FAILED EVOLVING ENTITY INDEXES FOR "+entity_name);
		}
	}
	
	@Override
	public void evolveEntity(WebStoreModule.schema_receiver resolver,EntityDefinition old_def,
			EntityDefinition proposed_def) throws SyncException
	{
		
		try{
			boolean go_on = confirm("REALLY EVOLVE "+proposed_def.getName());
			if(!go_on)
				return;
			List<FieldDefinition> delete_fields 	= new ArrayList<FieldDefinition>();
			List<FieldDefinition> add_fields		= new ArrayList<FieldDefinition>();
			List<FieldDefinition[]> changed_fields	= new ArrayList<FieldDefinition[]>();
			calc_added_and_deleted_and_changed_fields(old_def, proposed_def, delete_fields, add_fields,changed_fields);
			System.out.println("ADDED CHANGED AND DELETED FIELDS");
			System.out.println("ADD FIELDS "+add_fields);
			System.out.println("DELETE FIELDS "+delete_fields);
			System.out.println("CHANGE FIELDS "+changed_fields);
			if(changed_fields.size() != 0)
			{
				StringBuilder incompatible_buf = new StringBuilder();
				for(int i = 0;i < changed_fields.size();i++)
				{
					FieldDefinition of = changed_fields.get(i)[0];
					FieldDefinition nf = changed_fields.get(i)[1];
					boolean handled = handle_field_def_change(old_def,proposed_def,of,nf);
					
					if(!handled)
						incompatible_buf.append("FIELD "+of.getName()+" default_values: "+of.getDefaultValue()+" -> "+nf.getDefaultValue()+" types:"+FieldDefinition.typeAsString(of.getType())+" -> "+FieldDefinition.typeAsString(nf.getType())+"\n");
				}
				if(incompatible_buf.length() > 0)
				{

					force_abort("INCOMPATIBLE CHANGES FOR FIELD EVOLUTION:\n"+incompatible_buf.toString());
				}
			}
			
			if(delete_fields.size() == 0)
			{
				for(int i = 0;i < add_fields.size();i++)
				{
					FieldDefinition f;
					f = add_fields.get(i);
					try{
						go_on = confirm("ADD FIELD "+f.getName()+" DEFINED BY "+resolver.getDeclaringModuleForEntityField(old_def.getName(), f.getName()) +" TO "+resolver.getDeclaringModuleForEntity(old_def.getName())+"."+old_def.getName()+"?");
						if(go_on)
							store.addEntityField(old_def.getName(), f);
						else
							continue;
						INFO(getName()+": ADDING FIELD "+f.getName()+" TO "+old_def.getName());
					}catch(PersistenceException p)
					{
						ERROR(p);
						throw new SyncException(getName()+": FAILED ADDING FIELD "+f+" TO "+old_def.getName());
					}
				}
			}
			else if(add_fields.size() == 0)
			{
				for(int i = 0;i < delete_fields.size();i++)
				{
					FieldDefinition f;
					f = delete_fields.get(i);
					try{
						go_on = confirm("DELETE FIELD "+f.getName()+" IN "+old_def.getName()+"?");
						if(go_on)
							store.deleteEntityField(old_def.getName(), f.getName());
						else
							continue;
						INFO(getName()+": DELETING FIELD "+f.getName()+" FROM "+old_def.getName());
					}catch(PersistenceException p)
					{
						ERROR(p);
						throw new SyncException(getName()+": FAILED ADDING FIELD "+f+" TO "+old_def.getName());
					}
				}
			}
			else
			{
		
					String entity_name = old_def.getName();
					INFO(" WAS UNABLE TO AUTOMATICALLY RESOLVE THE EVOLUTION\n OF THE "+old_def.getName()+" ENTITY IN "+resolver.getDeclaringModuleForEntity(entity_name)+" PLEASE ASSIST.");
					for(int i = 0;i < add_fields.size();i++)
					{
				
						FieldDefinition f;
						f = add_fields.get(i);
						System.out.println("PROBLEM EVOLVING "+entity_name+"."+f.getName()+" DECLARED BY "+resolver.getDeclaringModuleForEntityField(entity_name, f.getName()));
						StringBuilder question = new StringBuilder("Do you mean to:\n\t[0] Add field "+f.getName()+" to "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);
						for(int j = 0;j < delete_fields.size();j++)
						{						
							question.append("\n\t["+(j+1)+"] Rename "+delete_fields.get(j).getName()+" to "+f.getName()+" in "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);						
						}
						question.append("?\n"); 
						String answer = null;
						while(answer == null)
						{
							answer = GET_CONSOLE_INPUT(question.toString());
							answer = answer.toLowerCase();
							if(answer.equals("0"))
							{
								try{
									 go_on = confirm("ADD FIELD "+f.getName()+" DECLARED BY "+resolver.getDeclaringModuleForEntityField(entity_name, f.getName())+" TO "+resolver.getDeclaringModuleForEntity(old_def.getName())+"."+old_def.getName()+"? ");
									if(go_on)
										store.addEntityField(old_def.getName(), f);
									else
										continue;
									INFO(getName()+": ADDED FIELD "+f.getName()+" DECLARED BY "+resolver.getDeclaringModuleForEntityField(entity_name, f.getName())+" TO "+resolver.getDeclaringModuleForEntity(entity_name)+"."+entity_name);
								}catch(PersistenceException p)
								{
									ERROR(p);
									throw new InitializationException(getName()+": FAILED ADDING FIELD "+f.getName()+" DECLARED BY "+resolver.getDeclaringModuleForEntityField(entity_name, f.getName())+" TO "+resolver.getDeclaringModuleForEntity(old_def.getName())+"."+old_def.getName());
								}
							}
							else
							{
								try{
									int delete_idx = Integer.parseInt(answer) - 1;
									FieldDefinition renamed_field = delete_fields.get(delete_idx);
									if(delete_idx > delete_fields.size()-1)
										answer = null;
									else
									{
										//rename field...update delete list.
										go_on = confirm("RENAME FIELD "+renamed_field+" TO "+f.getName()+"? ");
										if(go_on)
											store.renameEntityField(old_def.getName(), renamed_field.getName(), f.getName());
										else
											continue;
										delete_fields.remove(delete_idx);
										INFO(getName()+": RENAMED FIELD "+renamed_field.getName()+" TO "+f.getName()+" IN "+resolver.getDeclaringModuleForEntity(old_def.getName())+"."+old_def.getName());
									}
								}catch(Exception e)
								{
									answer = null;
								}
							}
						}
					}
					for(int i = 0;i < delete_fields.size();i++)
					{
						FieldDefinition f = delete_fields.get(i);
						try{
							go_on = confirm("DELETE FIELD "+f.getName()+" IN "+resolver.getDeclaringModuleForEntity(old_def.getName())+"."+old_def.getName()+"?");
							if(go_on)
								store.deleteEntityField(old_def.getName(), f.getName());
							else
								continue;
							INFO(getName()+": DELETED FIELD "+f.getName()+" FROM "+old_def.getName());
						}catch(PersistenceException p)
						{
							ERROR(p);
							throw new InitializationException(getName()+": FAILED DELETING FIELD "+f+" TO "+old_def.getName());
						}
					}

		}
		}catch(Exception e)
		{
			System.out.println("BAH@@ ");
			e.printStackTrace();
			ERROR(e);
			throw new SyncException("FAILED EVOLVING ENTITY DEF.");
		}
	}
	
	private boolean confirm(String message) throws InitializationException
	{
		String answer = null;
		while(answer == null)
		{
			try{
				answer = GET_CONSOLE_INPUT("ARE YOU SURE YOU WANT TO "+message+"\n\t[Y] YES\n\t[N] NO \n\t[A] Abort\n");
			}catch(WebApplicationException wae)
			{
				ERROR(wae);
				throw new InitializationException("BARFED IN EVO!!!");
			}
			answer = answer.toLowerCase();
			
			if(answer.equals("y"))
				return true;
			if(answer.equals("n"))
				return false;
			if(answer.equals("a"))
				throw new InitializationException("USER ABORTED APP STARTUP IN SYNC.");
			else
				answer = null;
		}
		return false;
	}
	
	private void force_abort(String message) throws InitializationException
	{
		String answer = null;
		while(answer == null)
		{
			try{
				answer = GET_CONSOLE_INPUT(message+"\n\t[A] Abort\n");
			}catch(WebApplicationException wae)
			{
				ERROR(wae);
				throw new InitializationException("BARFED IN EVO!!!");
			}
			answer = answer.toLowerCase();
			if(answer.equals("a"))
				throw new InitializationException("USER WAS FORCED TO ABORT.");
			else
				answer = null;
		}

	}
	
	private boolean changed_fields_contains(List<FieldDefinition[]> changed_fields,FieldDefinition field)
	{
		for(int i = 0;i < changed_fields.size();i++)
		{
			FieldDefinition[] ffdd = changed_fields.get(i);
			if(ffdd[0].equals(field) || ffdd[1].equals(field))
				return true;
		}
		return false;
	}
	
	private void calc_added_and_deleted_and_changed_fields(EntityDefinition old_def,EntityDefinition new_def,List<FieldDefinition> delete_fields,List<FieldDefinition> add_fields,List<FieldDefinition[]> changed_fields)
	{
		List<FieldDefinition> old_fields = old_def.getFields();
		List<FieldDefinition> new_fields = new_def.getFields();
		
		for(int i = 0; i < old_fields.size();i++)
		{
			FieldDefinition old_field = old_fields.get(i);
			if(new_fields.contains(old_field))
				continue;
			else
			{
				for(int ii = 0;ii < new_fields.size();ii++)
				{
					if(old_field.getName().equals(new_fields.get(ii).getName()))
					{
						if(evolve_ignore_map_fields.get(old_def.getName()+old_field.getName()) == null)
						{
							changed_fields.add(new FieldDefinition[]{old_field,new_fields.get(ii)});
							break;
						}
					}
				}

				//System.out.println("EVOLVE IGNORE MAP FOR "+old_def.getName()+old_field.getName()+" IS "+evolve_ignore_map.get(old_def.getName()+old_field.getName()));
				if(!changed_fields_contains(changed_fields, old_field) &&
				   evolve_ignore_map_fields.get(old_def.getName()+old_field.getName()) == null)
				{
					delete_fields.add(old_field);
					break;
				}

			}
		}
		
		for(int i = 0; i < new_fields.size();i++)
		{
			FieldDefinition new_field = new_fields.get(i);
			if(old_fields.contains(new_field) || changed_fields_contains(changed_fields,new_field))
				continue;
			else
			{
				//System.out.println("EVOLVE IGNORE MAP FOR "+old_def.getName()+new_field.getName()+" IS "+evolve_ignore_map.get(old_def.getName()+new_field.getName()));
				if(evolve_ignore_map_fields.get(old_def.getName()+new_field.getName()) == null)
					add_fields.add(new_field);
			}
		}
		
	}
	
	private void calc_added_and_deleted_indices(String entity_name,List<EntityIndex> old_indices,List<EntityIndex> new_indices,List<EntityIndex> delete_indices,List<EntityIndex> add_indices)
	{
		for(int i = 0; i < old_indices.size();i++)
		{
			EntityIndex old_index = old_indices.get(i);
			if(new_indices.contains(old_index))
				continue;
			else
			{
				//System.out.println("EVOLVE IGNORE MAP FOR "+old_def.getName()+old_field.getName()+" IS "+evolve_ignore_map.get(old_def.getName()+old_field.getName()));
				if(evolve_ignore_map_indices.get(entity_name+old_index.getName()) == null)
					delete_indices.add(old_index);
			}
		}
		
		for(int i = 0; i < new_indices.size();i++)
		{
			EntityIndex new_index = new_indices.get(i);
			if(old_indices.contains(new_index))
				continue;
			else
			{
				//System.out.println("EVOLVE IGNORE MAP FOR "+old_def.getName()+new_field.getName()+" IS "+evolve_ignore_map.get(old_def.getName()+new_field.getName()));
				if(evolve_ignore_map_indices.get(entity_name+new_index.getName()) == null)
					add_indices.add(new_index);
			}
		}
		
	}

	private static  Map<String,String> evolve_ignore_map_fields = new HashMap<String,String>();
	private static  Map<String,String> evolve_ignore_map_indices = new HashMap<String,String>();
	public void evolveIgnoreField(String entity, String fieldname) 
	{
		evolve_ignore_map_fields.put(new String(entity+fieldname),fieldname);
	}
	
	public void evolveIgnoreIndex(String entity, String index_name) 
	{
		evolve_ignore_map_indices.put(new String(entity+index_name),index_name);
	}

	private EntityIndex do_define_entity_index(String entity_name,EntityIndex index) throws PersistenceException,InitializationException
	{

		String index_name    = index.getName();
		int index_type       = index.getType();
		Map<String,Object> index_atts  = index.getAttributes();
		String[] field_names = new String[index.getFields().size()];
		for(int i=0;i < index.getFields().size();i++)
			field_names[i] = index.getFields().get(i).getName();
		
		List<EntityIndex> idxs = store.getEntityIndices(entity_name);
		for(int i = 0;i < idxs.size();i++) 
		{
			EntityIndex idx = idxs.get(i);
			if(idx.getName().equals(index_name))
			{
				List<FieldDefinition> fields = idx.getFields();
				
				if(fields.size() != field_names.length)
					throw new SyncException("ENTITY INDEX "+index_name+" ALREADY EXISTS ON ENTITY "+entity_name+" BUT THE DATABASE VERSION HAS A DIFFERENT NUMBER OF FIELDS dbVersion is "+idx);
				
				for(int j = 0;j < fields.size();j++)
				{
					FieldDefinition f = fields.get(j);
					if(field_names[j].equals(f.getName()))
						continue;
					else
						throw new SyncException("ENTITY INDEX "+index_name+" ALREADY EXISTS ON ENTITY "+entity_name+" BUT THE DATABASE VERSION DIFFERS ON FIELD "+field_names[j]+" dbVersion "+idx);
				}
				return idx;//the index exists and it checked out
			}
		}
		//the index didnt exist..create it 
		return store.addEntityIndex(entity_name, field_names, index_type, index_name,index_atts);	
	}


	
	private boolean handle_field_def_change(EntityDefinition old_def,EntityDefinition new_def,final FieldDefinition of,final FieldDefinition nf) throws InitializationException,PersistenceException,WebApplicationException
	{
		
		if(handle_same_type_different_defaults(old_def, new_def, of, nf))
			return true;
		return false;
	}

	private boolean handle_same_type_different_defaults(EntityDefinition old_def,EntityDefinition new_def,final FieldDefinition of,final FieldDefinition nf) throws InitializationException,PersistenceException,WebApplicationException
	{
		
		if(of.getType()==nf.getType() &&
			((of.getDefaultValue() == null && nf.getDefaultValue() != null) ||
			  !of.getDefaultValue().equals(nf.getDefaultValue())))
		{
			boolean go_on = confirm("CHANGE DEFAULT VALUE OF "+old_def.getName()+" "+of.getName()+" FROM "+of.getDefaultValue()+" TO "+nf.getDefaultValue());
			if(go_on)
			{			
				FieldDefinition cnf = nf.clone();//dont want to muck with field definition in the store//
				
				String entity_name = old_def.getName();	
				final String tempname = of.getName()+String.valueOf(new Date().getTime());
				//add new field to store
				INFO("ADDING "+tempname+" TO "+entity_name);
				cnf.setName(tempname);
				store.addEntityField(entity_name, cnf);
				//copy old values into temp field name
				PAGE_APPLY(entity_name, new CALLBACK(){
					public Object exec(Object... args)
					{
						Entity e = (Entity)args[0];
						e.setAttribute(tempname, e.getAttribute(of.getName()));
						return CALLBACK_VOID;
					}		
				});
				List<EntityIndex> idxs = delete_indexes_for_field(old_def, of);
				INFO("REMOVING "+of.getName());
				store.deleteEntityField(entity_name, of.getName());
				INFO("RENAMING "+tempname+" TO "+of.getName());
				store.renameEntityField(entity_name, tempname, of.getName() );
				INFO("RESTORING INDEXES FOR "+of.getName());
				restore_indexes(old_def, idxs);
				return true;
			}
		}
		return false;
	}
	
	private List<EntityIndex> delete_indexes_for_field(EntityDefinition def, FieldDefinition f) throws PersistenceException
	{
		List<EntityIndex> idxs = store.getEntityIndices(def.getName());
		List<EntityIndex> delete_idxs = new ArrayList<EntityIndex>();
		for(int i = 0;i < idxs.size();i++)
		{
			EntityIndex idx = idxs.get(i);
			if(idx.getFields().contains(f))
			{
				store.deleteEntityIndex(def.getName(), idx.getName());
				delete_idxs.add(idx);
				INFO("DELETED IDX "+def.getName()+" "+idx.getName());
			}
		}		
		return delete_idxs;
	}
	
	private void restore_indexes(EntityDefinition def,List<EntityIndex> idxs) throws PersistenceException
	{
		
		for(int i = 0;i < idxs.size();i++)
		{
			EntityIndex idx = idxs.get(i);
			List<FieldDefinition> fields = idx.getFields();
			List<String> fieldnames = new ArrayList<String>();
			for(int ii = 0;ii < fields.size();ii++)
				fieldnames.add(fields.get(i).getName());
			
			store.addEntityIndex(def.getName(), fieldnames.toArray(new String[0]), idx.getType(), idx.getName(), idx.getAttributes());
			INFO("CREATED IDX "+def.getName()+" "+idx.getName());
		}
		
	
	}

}
