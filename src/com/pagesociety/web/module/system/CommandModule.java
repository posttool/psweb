package com.pagesociety.web.module.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;

public class CommandModule extends WebModule
{

	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);

	}

	//env is name value pairs for environmant variables. i.e. ENV_DOMAIN,www.topher.com if none is provided it inherits
	//from env from parent
	public int exec(String[] command,CommandListener listener,boolean async,String...env ) throws WebApplicationException
	{
		Process p = null;
		String[] p_env;
		
		if(env.length == 0)
			p_env = null;
		else
		{
			p_env = new String[env.length/2];
			for(int i = 0,j=0;i < env.length;i+=2,j++)
			{
				String env_var = new String(env[i]+"="+env[i+1]);
				p_env[j] = env_var;
			}
		}
		
		if(async)
		{
			final String[] f_command = command;
			final String[] f_penv = p_env;
			final CommandListener f_listener = listener;
			Thread t = new Thread()
			{
				public void run()
				{
					try {
						do_exec(f_command, f_penv, f_listener);
					} catch (WebApplicationException e) {
						ERROR(e);
					}
				}
			};
			t.start();
			//async always returns 0 really..... return value must be gotten in listener//
			return 0;
		}
		else
		{
			return do_exec(command, p_env, listener);
		}
	}
	
	/* return exit val */
	public int do_exec(String[] cmd,String[] p_env,CommandListener listener) throws WebApplicationException
	{
		StringBuilder command_string = new StringBuilder();
		for(int i = 0;i <cmd.length;i++)
		{
			command_string.append(cmd[i]);
			command_string.append(" ");
		}
		command_string.setLength(command_string.length()-1);
		String scmd = command_string.toString();
		
		Process p = null;
		try
		{
			System.out.println("ABOUT TO EXEC "+scmd+" PENV "+p_env);
			p = Runtime.getRuntime().exec(cmd,p_env);
			StreamGobbler out_g = new StreamGobbler(p.getInputStream(),STREAM_GOBBLER_OUT,listener);
			StreamGobbler err_g = new StreamGobbler(p.getErrorStream(),STREAM_GOBBLER_ERR,listener);
			out_g.start();
			err_g.start();
			int exit_val = p.waitFor();
			MODULE_LOG("CMD OK: "+scmd);
			if(listener != null)
				listener.command_complete(exit_val);
			return exit_val;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			MODULE_LOG("CMD FIALED: "+scmd+" SEE SERVER LOGS. E:"+e.getMessage());
			throw new WebApplicationException("EXEC OF "+scmd+" FAILED.",e);
		}
		finally
		{
			p.destroy();
		}	
	}
	
	/* /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/ */
	public class CommandListener
	{
		private StringBuilder stderr_buf;
		private StringBuilder stdout_buf;
		public void stdout(String msg)
		{
			//do nothing by default//
			stdout_buf.append(msg);
		}
		
		public void stderr(String msg)
		{
			//do nothing by default//
			stderr_buf.append(msg);
		}
		
		public void command_complete(int exit_code)
		{
			
		}
		
		public String getStdErrorBuf()
		{
			return stderr_buf.toString();
		}
		
		public String getStdOutBuffer()
		{
			return stdout_buf.toString();
		}
	}
	
    static final int STREAM_GOBBLER_ERR = 0x01;
    static final int STREAM_GOBBLER_OUT = 0x02;
	class StreamGobbler 
	{
	    InputStream is;
	    int type;

	    CommandListener listener;
	   
	    StreamGobbler(InputStream is, int type,CommandListener listener)
	    {
	        this.is 		= is;
	        this.type 		= type;
	        this.listener 	= listener;
	    }
	    
	    public void start()
	    {
	    	if(listener == null)
	    		run_null_listener();
	    	else if(type == STREAM_GOBBLER_ERR)
	    		run_std_err();
	    	else if(type == STREAM_GOBBLER_OUT)
	    		run_std_out();
	    }
	    
	    public void run_std_err()
	    {
	    	Thread t = new Thread()
	    	{
	    		public void run()
	    		{
			        try
			        {
			            InputStreamReader isr = new InputStreamReader(is);
			            BufferedReader br = new BufferedReader(isr);
			            String line=null;
			            while ( (line = br.readLine()) != null)
			            	listener.stderr(line+"\n");
		            } catch (IOException ioe)
		              {
		                ioe.printStackTrace();  
		              }
	    		}
	    	};
	    	t.start();
	    }
	    
	    public void run_std_out()
	    {
	    	Thread t = new Thread()
	    	{
	    		
	    		public void run()
	    		{
	    			try
	    			{
	    				InputStreamReader isr = new InputStreamReader(is);
	    				BufferedReader br = new BufferedReader(isr);
	    				String line=null;
	    				while ( (line = br.readLine()) != null)
	    					listener.stdout(line+"\n");
	    			}catch (IOException ioe)
	    			{
	    				ioe.printStackTrace();  
	    			}
	    		}
	    	};
	    	t.start();
	    }	    
	    public void run_null_listener()
	    {
	    	Thread t = new Thread()
	    	{
	    		public void run()
	    		{
			        try
			        {
			            InputStreamReader isr = new InputStreamReader(is);
			            BufferedReader br = new BufferedReader(isr);
			            while ( (br.readLine()) != null)
			            	;
		            } catch (IOException ioe)
		              {
		                ioe.printStackTrace();  
		              }
	    		}
	    	};
	    	t.start();
	    }

	
	}

}


