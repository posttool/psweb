package com.pagesociety.web.module.raw;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.RawUIModule;


public class RawUIAggregator extends RawUIModule
{

	private List<Module> raw_ui_modules;
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	
		raw_ui_modules 	 = new ArrayList<Module>();
		List<Module> application_modules = app.getModules();
		for(int i = 0;i < application_modules.size();i++)
		{
			Module m = application_modules.get(i);
			if(m instanceof RawUIModule)
			{
				if(m.getName().equals(getName()))
					continue;
				raw_ui_modules.add(m);
				LOG("DISCOVERED RAWUI MODULE "+m.getName());
			}
		}
	}
		
	public void exec(UserApplicationContext uctx,Map<String,Object> params) throws WebApplicationException,FileNotFoundException,IOException
	{
		String call_module = (String)params.get("call_module");
		if(call_module != null)
		{
			CALL(uctx,call_module,RAW_SUBMODE_DEFAULT,RAW_SUBMODE_DEFAULT);
		}
		else
		{
			DOCUMENT_START(uctx, getName(), RAW_UI_BACKGROUND_COLOR, RAW_UI_FONT_FAMILY, RAW_UI_FONT_COLOR, 24,RAW_UI_LINK_COLOR,RAW_UI_LINK_HOVER_COLOR);
			SPAN(uctx,getApplication().getConfig().getName()+" RAW UI OVERVIEW",18);
			DISPLAY_ERROR(uctx,params);
			DISPLAY_INFO(uctx,params);
			P(uctx);
			for(int i = 0;i < raw_ui_modules.size();i++)
			{
				Module m = raw_ui_modules.get(i);
				A(uctx, getName(),RAW_SUBMODE_DEFAULT, m.getName(),"call_module",m.getName());
				P(uctx);
	
			}
			DOCUMENT_END(uctx);
			}
	}
		
}
