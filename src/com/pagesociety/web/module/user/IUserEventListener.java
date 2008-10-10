package com.pagesociety.web.module.user;

import com.pagesociety.persistence.Entity;

public interface IUserEventListener 
{
	public void userRegistered(Entity user);
	public void userLoggedIn(Entity user);
	public void userLoggedOut(Entity user);
}
