package com.pagesociety.transcode;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.pagesociety.cmd.CmdWork;
import com.pagesociety.cmd.CmdWorkListener;
import com.pagesociety.cmd.CmdWorker;
import com.pagesociety.util.DateTime;
import com.pagesociety.util.Text;

/**
 * Ideally, there would be a file with metadata about all the FFMPEG resources
 * so that we don't need to
 * 
 * @author david
 * 
 */
public class FFMpeg extends TranscodeWorkImpl implements CmdWorkListener
{
	private static final Logger logger = Logger.getLogger(FFMpeg.class);

	public static void main(String[] args)
	{
		String bp1 = "C:\\Users\\david\\Desktop\\";
		File f1 = new File(bp1 + "Goodness_LRV1115.mov");
		File f2 = new File(bp1 + "Goodness_LRV1115.flv");
		do1(f1, f2);
	}

	// private static void do2(File f1, File f2)
	// {
	// FFMpeg f = new FFMpeg(f1, f2);
	// f.setSeek(56);
	// f.setTotalVideoFrames(5);
	// f.setFramerate(1);
	// f.setSize(320, 240);
	// f.setAudioDisabled(true);
	// f.exec();
	// }
	public static void do1(File f1, File f2)
	{
		FFMpeg f = new FFMpeg(f1, f2);
		f.setAudioBitrate(56);
		f.setAudioResolution(22050);
		f.setQscale(5);
		f.setFramerate(30);
		f.setSize(1004, 504);
		f.exec();
	}

	private static String EXEC_PATH = "ffmpeg";

	public static void setRuntimeExecPath(String path)
	{
		EXEC_PATH = path;
		if (logger.isDebugEnabled())
			logger.debug("setting runtime exec path = " + path);
	}

	// ffmpeg properties
	private String title;
	public int index;
	public String name;
	public boolean system = false;
	private int seek = -1;
	private int length = -1;
	private int width = -1;
	private int height = -1;
	private float framerate = -1;
	private int totalVideoFrames = -1;
	private int bitrate = -1;
	private int audioBitrate = -1;
	private int audioRes = -1;
	private int audioChannels = -1;
	private boolean audioDisabled = false;
	private int cmp = -1;
	private int subcmp = -1;
	private int mbd = -1;
	private int aic = -1;
	private String flags;
	private String vcodec;
	private String acodec;
	private String format;
	private String partitions;
	private int trellis = -1;
	private int coder = -1;
	private boolean sameQuality;
	private float qscale = -1f;
	//
	// read from the ffpeg output stream
	private FFMpeg details;
	private boolean init;

	public FFMpeg(File input, File output)
	{
		super(input, output);
		init = false;
	}

	private FFMpeg(File input)
	{
		super(input, null);
		init = true;
	}

	public synchronized void exec()
	{
		if (!init)
			init();
		else
			exec2();
	}

	private synchronized void init()
	{
		final FFMpeg scope = this;
		details = new FFMpeg(input);
		TranscodeWorkListener listener = new TranscodeWorkListener()
		{
			public void onFileWorkComplete(File output)
			{
				durationInSeconds = details.durationInSeconds;
				original_width = details.original_width;
				original_height = details.original_height;
				scope.exec2();
			}

			public void onFileWorkError(String err_msg)
			{
				scope.fireWorkError(err_msg);
			}

			public void onFileWorkProgress(Object progress)
			{
			}

			public void onFileWorkStart()
			{
			}
		};
		details.addTranscodeWorkListener(listener);
		details.exec();
	}

	public synchronized void exec2()
	{
		String outputFile = output == null ? null : output.getPath();
		String[] cmd = getCommandArgs(input.getPath(), outputFile, title);
		CmdWork work = new CmdWork(this, cmd);
		CmdWorker.doWork(work);
	}

	public void sigstart(long id)
	{
		// System.out.println("sigstart " + id);
		fireWorkStart();
	}

	public void sigcomplete(long id)
	{
		// System.out.println("sigcomplete " + id + " " + output);
		// still work was completed in the progress method
		boolean isProducingStillImages = (totalVideoFrames != -1);
		if (!isProducingStillImages)
		{
			fireWorkComplete(output);
		}
		else
		{
			String numberedStill = output.getName().replaceFirst("%d", "1");
			File stillFrame = new File(output.getParentFile(), numberedStill);
			fireWorkComplete(stillFrame);
		}
	}

	public void stdout(long id, String s)
	{
		logger.debug("sigout(long, String) - sigout " + id + "nt" + s);
		fireWorkProgress(s);
	}

	public void stderr(long id, String err)
	{
		// System.out.println("sigerr " + id + ".\n\tError: " + err);
		process(err);
	}

	public void sigerr(long id, String err_msg)
	{
		fireWorkError(err_msg);
	}

	private static final int INIT = 0;
	private static final int GETTING_INPUT = 1;
	private static final int DESCRIBING_OUTPUT = 2;
	private static final int CONVERTING = 3;
	private int state = INIT;
	private int stillFileCount = 0;
	private float durationInSeconds = -1;
	private float original_framerate;
	private int original_width;
	private int original_height;

	private void process(String line)
	{
		if (line.startsWith("Input #0"))
		{
			state = GETTING_INPUT;
			return;
		}
		else if (line.startsWith("Output #0"))
		{
			state = DESCRIBING_OUTPUT;
			return;
		}
		else if (line.equals("Press [q] to stop encoding"))
		{
			state = CONVERTING;
			return;
		}
		//
		switch (state)
		{
		case INIT:
			break;
		case GETTING_INPUT:
			getInputInfo(line);
			break;
		case DESCRIBING_OUTPUT:
			break;
		case CONVERTING:
			getProgressInfo(line);
			break;
		default:
			break;
		}
	}

	private void getInputInfo(String line)
	{
		if (line.startsWith("  Duration:"))
		{
			int o = 12;
			int frames = Text.toInt(line.substring(o + 9, o + 10));
			int seconds = Text.toInt(line.substring(o + 6, o + 8));
			int minutes = Text.toInt(line.substring(o + 3, o + 5));
			int hours = Text.toInt(line.substring(o, o + 2));
			durationInSeconds = hours * 60 * 60 + minutes * 60 + seconds + frames / 30f;
			logger.debug("getInputInfo(String) - DURATION " + hours + ":" + minutes + ":"
					+ seconds);
		}
		else if (line.substring(15, 20).equals("Video"))
		{
			// mpeg4
			// yuv420p
			// 512x384
			// 29.97 fps(r)
			String[] videoProps = line.substring(22).split(", ");
			original_framerate = Text.toFloat(videoProps[3].split(" ")[0]);
			String[] size = videoProps[2].split("x");
			original_width = Text.toInt(size[0]);
			original_height = Text.toInt(size[1]);
			logger.debug("getInputInfo() - ORIGINAL FRAMERATE " + framerate);
			logger.debug("getInputInfo() - ORIGINAL SIZE " + original_width + ","
					+ original_height);
		}
	}

	private void getProgressInfo(String line)
	{
		if (line.length() < 11)
			return;
		int frame = Text.toInt(line.substring(6, 11).trim());
		float totalFrames = durationInSeconds * framerate;
		float percentComplete = frame / totalFrames;
		// a video, not stills
		if (totalVideoFrames == -1)
		{
			String s = (frame + " of " + totalFrames + " ("
					+ Math.floor(percentComplete * 100) + "%)");
			logger.debug("getProgressInfo(String) - " + s);
			fireWorkProgress(s);
		}
		else
		{
			String numberedStill = output.getName().replaceFirst("%d",
					Integer.toString(stillFileCount));
			File stillFrame = new File(output.getParentFile(), numberedStill);
			fireWorkProgress(stillFrame);
			stillFileCount++;
		}
	}

	public float getTotalFrames()
	{
		return durationInSeconds * framerate;
	}

	public float getDurationInSeconds()
	{
		return durationInSeconds;
	}

	public float getFPS()
	{
		return framerate == -1 ? original_framerate : framerate;
	}

	// ///////////////////////////////////////////////////////////////////
	public void setSeek(int s)
	{
		this.seek = s;
	}

	public void setQscale(int i)
	{
		this.qscale = i;
	}

	public void sameQuality(boolean b)
	{
		this.sameQuality = b;
	}

	public void setLength(int t)
	{
		this.length = t;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

	// -s
	public void setSize(int w, int h)
	{
		this.width = w;
		this.height = h;
	}

	// -r
	public void setFramerate(float f)
	{
		this.framerate = f;
	}

	// -b
	public void setBitrate(int b)
	{
		this.bitrate = b;
	}

	// -ab
	public void setAudioBitrate(int ab)
	{
		this.audioBitrate = ab;
	}

	// -ar
	public void setAudioResolution(int ar)
	{
		this.audioRes = ar;
	}

	// -ar
	public void setAudioChannels(int ac)
	{
		this.audioChannels = ac;
	}

	// -an
	public void setAudioDisabled(boolean b)
	{
		this.audioDisabled = b;
	}

	public void setTotalVideoFrames(int vf)
	{
		this.totalVideoFrames = vf;
	}

	// IPOD
	// -cmp
	// http://www.mplayerhq.hu/DOCS/HTML/en/menc-feat-enc-libavcodec.html
	public void setComparisonForMotionEstimation(int cmp)
	{
		this.cmp = cmp;
	}

	// -subcmp
	public void setSubComparisonForMotionEstimation(int subsmp)
	{
		this.subcmp = subsmp;
	}

	// -aic
	public void setEnableAdvancedIntraCoding(int aic)
	{
		this.aic = aic;
	}

	// -mbd
	public void setMacroblockDecision(int mbd)
	{
		this.mbd = mbd;
	}

	// -flags
	public void setFlags(String flags)
	{
		this.flags = flags;
	}

	// -vcodec
	public void setVideoCodec(String vcodec)
	{
		this.vcodec = vcodec;
	}

	// -acodec
	public void setAudioCodec(String acodec)
	{
		this.acodec = acodec;
	}

	/**
	 * PSP x264 specific?
	 * 
	 */
	public void setPartitions(String p)
	{
		this.partitions = p;
	}

	public void setTrellis(int t)
	{
		this.trellis = t;
	}

	public void setCoder(int c)
	{
		this.coder = c;
	}

	/**
	 * 
	 */
	public String[] getCommandArgs(String input, String output, String title)
	{
		if (title == null || title.equals(""))
		{
			title = "Untitled";
		}
		List<String> command = new ArrayList<String>();
		command.add(EXEC_PATH);
		command.add("-i");
		command.add(input);
		if (audioBitrate != -1)
		{
			command.add("-ab");
			command.add(Integer.toString(audioBitrate));
		}
		if (audioRes != -1)
		{
			command.add("-ar");
			command.add(Integer.toString(audioRes));
		}
		if (audioChannels != -1)
		{
			command.add("-ac");
			command.add(Integer.toString(audioChannels));
		}
		if (audioDisabled)
		{
			command.add("-an");
		}
		if (bitrate != -1)
		{
			command.add("-b");
			command.add(Integer.toString(bitrate));
		}
		if (framerate != -1)
		{
			command.add("-r");
			command.add(Float.toString(framerate));
		}
		if (width != -1 || height != -1)
		{
			float wratio = (float) width / (float) original_width;
			float hratio = (float) height / (float) original_height;
			if (width < 0)
			{
				wratio = hratio;
			}
			if (height < 0)
			{
				hratio = wratio;
			}
			int s_w = Math.round(wratio * original_width);
			int s_h = Math.round(hratio * original_height);
			command.add("-s");
			command.add(s_w + "x" + s_h);
		}
		if (seek != -1)
		{
			command.add("-ss");
			command.add(Integer.toString(seek));
		}
		if (length != -1)
		{
			command.add("-t");
			command.add(Integer.toString(length));
		}
		if (totalVideoFrames != -1)
		{
			command.add("-vframes");
			command.add(Integer.toString(totalVideoFrames));
		}
		if (aic != -1)
		{
			command.add("-aic");
			command.add(Integer.toString(aic));
		}
		if (cmp != -1)
		{
			command.add("-cmp");
			command.add(Integer.toString(cmp));
		}
		if (subcmp != -1)
		{
			command.add("-subcmp");
			command.add(Integer.toString(subcmp));
		}
		if (mbd != -1)
		{
			command.add("-mbd");
			command.add(Integer.toString(mbd));
		}
		if (flags != null)
		{
			command.add("-flags");
			command.add(flags);
		}
		if (vcodec != null)
		{
			command.add("-vcodec");
			command.add(vcodec);
		}
		if (acodec != null)
		{
			command.add("-acodec");
			command.add(acodec);
		}
		if (format != null)
		{
			command.add("-f");
			command.add(format);
		}
		if (partitions != null)
		{
			command.add("-partitions");
			command.add(partitions);
		}
		if (trellis != -1)
		{
			command.add("-trellis");
			command.add(Integer.toString(trellis));
		}
		if (coder != -1)
		{
			command.add("-coder");
			command.add(Integer.toString(coder));
		}
		if (sameQuality)
		{
			command.add("-sameq");
		}
		if (qscale != -1f)
		{
			command.add("-qscale");
			command.add(Float.toString(qscale));
		}
		if (output != null)
		{
			command.add("-title");
			command.add(title);
			command.add("-timestamp");
			command.add(DateTime.formatDB(new Date()));
			command.add(output);
		}
		logger.debug(">" + command);
		System.out.println(command);
		return command.toArray(new String[command.size()]);
	}
}
