package com.pagesociety.transcode;

import java.io.File;

import com.pagesociety.cmd.CmdWork;
import com.pagesociety.cmd.CmdWorkListener;
import com.pagesociety.cmd.CmdWorker;

public class ImageMagick extends TranscodeWorkImpl implements CmdWorkListener
{
	private static String EXEC_PATH = "convert";
	private static final boolean DEBUG = false;

	public static void setRuntimeExecPath(String path)
	{
		EXEC_PATH = path;
		// System.out.println("ImageMagick runtime exec path = " + path);
	}

	private int width;
	private int height;
	private boolean gray;

	public ImageMagick(File input, File output)
	{
		super(input, output);
	}

	public void setSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	public void setGrayScale(boolean b)
	{
		this.gray = b;
	}

	public void exec()
	{
		String[] cmds = new String[] { EXEC_PATH, // convert
				input.getAbsolutePath(), // input
				"-resize", width + "x" + height + ">", // -resize
				"-quality", "86", // -quality
				"-colorspace", gray ? "Gray" : "RGB", // -colorspace
				output.getAbsolutePath() };
		CmdWork w = new CmdWork(this, cmds);
		CmdWorker.doWork(w);
	}

	public void sigstart(long id)
	{
		if (DEBUG)
			System.out.println("sigstart received for " + id);
		fireWorkStart();
	}

	public void sigcomplete(long id)
	{
		if (DEBUG)
			System.out.println("sigcomplete received for " + id);
		fireWorkComplete(output);
	}

	public void stdout(long id, String s)
	{
		if (DEBUG)
			System.out.println("stdout received for " + id + "\n\t" + s);
		fireWorkProgress(s);
	}

	public void stderr(long id, String err)
	{
		if (DEBUG)
			System.out.println("stderr received for " + id + ".\n\tError: " + err);
	}

	public void sigerr(long id, String err)
	{
		if (DEBUG)
			System.out.println("sigerr received for " + id + ".\n\tError: " + err);
		fireWorkError(err);
	}

	//
	public static void main(String[] args)
	{
		String bp1 = "C:\\Users\\Public\\Pictures\\Sample Pictures\\VAIO Sample Pictures\\";
		File f1 = new File(bp1 + "2007VAIO_SS04.jpg");
		File f2 = new File(bp1 + "2007VAIO_SS04.png");
		ImageMagick
				.setRuntimeExecPath("C:\\Program Files\\ImageMagick-6.3.5-Q16\\convert.exe");
		ImageMagick i = new ImageMagick(f1, f2);
		i.setSize(150, 150);
		i.exec();
	}
}
