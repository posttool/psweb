package com.pagesociety.cmd;

import java.util.ArrayList;
import java.util.List;

public class CmdWorker
{

	public static void doWork(CmdWork work) throws Exception
	{
		List<ProcessReader> process_readers = new ArrayList<ProcessReader>();
		work.isWorking = true;
		String[][] cmds = work.cmds;
		String[] env = work.env;
		long id = work.id;
		CmdWorkListener ob = work.observer;
		boolean _noerror = true;
		
		ob.sigstart(id);
		_noerror = true;
		for (int cc = 0; cc < cmds.length; cc++)
		{
			int exit_val = 0;
			String[] cmd = cmds[cc];
			String scmd = null;
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < cmd.length; i++)
				sb.append(cmd[i] + " ");
			scmd = sb.toString();
			
			//System.out.println(">>>>>"+scmd);
			Process p =null;
			ProcessReader r_err =null;
			ProcessReader r_out =null;
			try
			{
				p = Runtime.getRuntime().exec(cmd, env);
				work.process = p;
				r_err = new ProcessReader(p.getErrorStream(), ob, id, ProcessReader.ERR);
				r_out = new ProcessReader(p.getInputStream(), ob, id, ProcessReader.OUT);
				process_readers.add(r_err);
				process_readers.add(r_out);
				r_err.start();
				r_out.start();
				exit_val = p.waitFor();
				//System.out.println("!!! exit val is "+exit_val);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				if (p!=null)
				{
				
					p.destroy();
				
				}
				throw e;
			}
			if (exit_val != 0)
			{
				System.err.println( " failed(exit code:" + exit_val + "):"+scmd);
				ob.sigerr(id, " failed(exit code:" + exit_val + "):"+scmd);
				_noerror = false;
				break;
			}
		}
		// this bit loops until it sees that all std_err & all std_out have
		// exhauted their input streams
		int c = -1;
		while (c != 0)
		{
			c = 0;
			for (ProcessReader r : process_readers)
			{
				if (r.isReading())
				{
					c++;
				}
			}
			try
			{
				Thread.sleep(20);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		//
		if(_noerror)
			ob.sigcomplete(id);
		work.isWorking = false;
	}


}
