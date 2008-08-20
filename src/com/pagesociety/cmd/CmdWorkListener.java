package com.pagesociety.cmd;
public interface CmdWorkListener
{
    public void sigstart(long id);
    public void sigcomplete(long id);
    public void stderr(long id,String std_err_msg);
    public void stdout(long id,String std_out_msg);
    public void sigerr(long id,String err_msg);
}
