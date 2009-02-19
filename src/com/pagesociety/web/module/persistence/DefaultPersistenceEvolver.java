package com.pagesociety.web.module.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebStoreModule;

public class DefaultPersistenceEvolver extends WebStoreModule implements IEvolutionProvider
{

	@Override
	public void evolveEntity(String source,EntityDefinition old_def,
			EntityDefinition proposed_def) throws SyncException
	{
		try{
			List<FieldDefinition> delete_fields = new ArrayList<FieldDefinition>();
			List<FieldDefinition> add_fields	= new ArrayList<FieldDefinition>();
			calc_added_and_deleted_fields(old_def, proposed_def, delete_fields, add_fields);
			if(delete_fields.size() == 0)
			{
				for(int i = 0;i < add_fields.size();i++)
				{
					FieldDefinition f;
					f = add_fields.get(i);
					try{
						boolean go_on = confirm("ADD FIELD "+f.getName()+" TO "+old_def.getName()+"?");
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
						boolean go_on = confirm("DELETE FIELD "+f.getName()+" IN "+old_def.getName()+"?");
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
		
					INFO(" WAS UNABLE TO AUTOMATICALLY RESOLVE THE EVOLUTION\n OF THE "+old_def.getName()+" ENTITY IN "+source+" PLEASE ASSIST.");
					for(int i = 0;i < add_fields.size();i++)
					{
				
						FieldDefinition f;
						f = add_fields.get(i);
						System.out.println("PROBLEM EVOLVING "+old_def.getName()+"."+f.getName());
						StringBuilder question = new StringBuilder("Do you mean to:\n\t[0] Add field "+f.getName()+" to "+source+"."+old_def.getName());
						for(int j = 0;j < delete_fields.size();j++)
						{						
							question.append("\n\t["+(j+1)+"] Rename "+delete_fields.get(j).getName()+" to "+f.getName()+" in "+source+"."+old_def.getName());						
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
									boolean go_on = confirm("ADD FIELD "+f.getName()+" TO "+old_def.getName()+"? ");
									if(go_on)
										store.addEntityField(old_def.getName(), f);
									else
										continue;
									INFO(getName()+": ADDED FIELD "+f.getName()+" TO "+source+"."+old_def.getName());
								}catch(PersistenceException p)
								{
									ERROR(p);
									throw new InitializationException(getName()+": FAILED ADDING FIELD "+f+" TO "+source+"."+old_def.getName());
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
										boolean go_on = confirm("RENAME FIELD "+renamed_field+" TO "+f.getName()+"? ");
										if(go_on)
											store.renameEntityField(old_def.getName(), renamed_field.getName(), f.getName());
										else
											continue;
										delete_fields.remove(delete_idx);
										INFO(getName()+": RENAMED FIELD "+renamed_field.getName()+" TO "+f.getName()+" IN "+source+"."+old_def.getName());
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
							store.deleteEntityField(old_def.getName(), f.getName());
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
	
	private void calc_added_and_deleted_fields(EntityDefinition old_def,EntityDefinition new_def,List<FieldDefinition> delete_fields,List<FieldDefinition> add_fields)
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
				//System.out.println("EVOLVE IGNORE MAP FOR "+old_def.getName()+old_field.getName()+" IS "+evolve_ignore_map.get(old_def.getName()+old_field.getName()));
				if(evolve_ignore_map.get(old_def.getName()+old_field.getName()) == null)
					delete_fields.add(old_field);
			}
		}
		
		for(int i = 0; i < new_fields.size();i++)
		{
			FieldDefinition new_field = new_fields.get(i);
			if(old_fields.contains(new_field))
				continue;
			else
			{
				//System.out.println("EVOLVE IGNORE MAP FOR "+old_def.getName()+new_field.getName()+" IS "+evolve_ignore_map.get(old_def.getName()+new_field.getName()));
				if(evolve_ignore_map.get(old_def.getName()+new_field.getName()) == null)
					add_fields.add(new_field);
			}
		}
		
	}

	private static  Map<String,String> evolve_ignore_map = new HashMap<String,String>();
	public void evolveIgnore(String entity, String fieldname) 
	{
		evolve_ignore_map.put(new String(entity+fieldname),fieldname);
	}

}
