package com.pagesociety.web.module;

import com.pagesociety.web.exception.WebApplicationException;




public interface IEventListener 
{
	public void onEvent(Module src,ModuleEvent e) throws WebApplicationException;
}
