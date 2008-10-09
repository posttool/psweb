package com.pagesociety.web.module.persistence;


import com.pagesociety.persistence.PersistentStore;

public interface IPersistenceProvider 
{
	public PersistentStore getStore();
}
