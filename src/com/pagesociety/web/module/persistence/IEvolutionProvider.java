package com.pagesociety.web.module.persistence;

import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;

public interface IEvolutionProvider 
{

	public void evolveEntity(String source,EntityDefinition old_def,EntityDefinition proposed_def) throws SyncException;
	/* this tells the evolution system to ignore certain fields on certain entities */
	/* this is useful if you have some code which dynamically is adding fields to different */
	/* entities for its own private use */
	public void evolveIgnore(String entity,String field);
}
