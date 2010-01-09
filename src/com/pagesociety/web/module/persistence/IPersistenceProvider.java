package com.pagesociety.web.module.persistence;


import java.io.File;
import java.util.List;

import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.PersistentStore;

public interface IPersistenceProvider 
{
	public PersistentStore getStore();
	public IEvolutionProvider getEvolutionProvider();
	public String getName();
	
	public List<String> getBackupIdentifiers() throws PersistenceException;
	public String 		doFullBackup() throws PersistenceException;
	public String 		doIncrementalBackup(String fullbackup_token) throws PersistenceException;
	public void 		restoreFromBackup(String fullbackup_token) throws PersistenceException;
	public void 		deleteBackup(String fullbackup_token) throws PersistenceException;
	public File 		getBackupAsZipFile(String backup_identifier) throws PersistenceException;
	public String       getStatistics();


	
}
