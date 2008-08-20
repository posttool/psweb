package com.pagesociety.cmd;

public class CmdWork {
	public long id = -1;
	public String[][] cmds = null;
	public String[] env = null;
	public CmdWorkListener observer = null;
	protected boolean isWorking = false;
	public Process process;
	//
	private static int WORK_ID = 100000;

	public CmdWork(CmdWorkListener observer, long work_id, String[] cmd) {
		String[][] cmds = new String[1][];
		cmds[0] = cmd;
		this.observer = observer;
		this.id = work_id;
		this.cmds = cmds;
	}

	public CmdWork(CmdWorkListener observer, String[] cmd) {
		this(observer, WORK_ID++, cmd);
	}

	public String toString() {
		String cmdStr = "";
		for (String cmd : this.cmds[0])
			cmdStr += cmd + " ";
		return id + " " + cmdStr;
	}

	public boolean isWorking() {
		return isWorking;
	}

	public boolean isWaiting() {
		return !isWorking;
	}
}
