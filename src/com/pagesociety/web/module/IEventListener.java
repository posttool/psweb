package com.pagesociety.web.module;




public interface IEventListener 
{
	public void onEvent(Module src,int event,Object val);
}
