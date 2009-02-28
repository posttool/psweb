package com.pagesociety.web.module.persistence;

import java.util.List;

import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.module.WebStoreModule;

public interface IEvolutionProvider 
{

	public void evolveEntity(WebStoreModule.schema_receiver resolver,EntityDefinition old_def,EntityDefinition proposed_def) throws SyncException;
	public void evolveIndexes(WebStoreModule.schema_receiver resolver,String entity_name,List<EntityIndex> existing_indices,List<EntityIndex> proposed_indices) throws SyncException;
}
