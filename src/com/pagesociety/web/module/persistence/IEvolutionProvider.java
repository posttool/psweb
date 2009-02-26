package com.pagesociety.web.module.persistence;

import java.util.List;

import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;

public interface IEvolutionProvider 
{

	public void evolveEntity(String source,EntityDefinition old_def,EntityDefinition proposed_def) throws SyncException;
	/* this tells the evolution system to ignore certain fields on certain entities */
	/* this is useful if you have some code which dynamically is adding fields to different */
	/* entities for its own private use */
	public void evolveIgnoreField(String entity,String field);
	public void evolveIgnoreIndex(String entity,String index);
	public void evolveIndexes(String source,String entity_name,List<EntityIndex> existing_indices,List<EntityIndex> proposed_indices) throws SyncException;
}
