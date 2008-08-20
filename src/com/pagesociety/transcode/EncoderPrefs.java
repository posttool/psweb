package com.pagesociety.transcode;



public class EncoderPrefs extends FFMpeg
{
	public EncoderPrefs()
	{
		super(null, null);
	}

	// FLV
	public static EncoderPrefs FLV = new EncoderPrefs();
	static
	{
		FLV.setAudioBitrate(56);
		FLV.setAudioResolution(22050);
		FLV.setQscale(6);
		FLV.setFramerate(20);
		FLV.setSize(320, 240);
	}
	/**
	 * iPod
	 * 
	 * ffmpeg -i input.avi -acodec aac -ab 128 -vcodec mpeg4 -b 1200 -mbd 2
	 * -flags +4mv+trell -aic 2 -cmp 2 -subcmp 2 -s 320x240 -title X output.mp4
	 * 
	 */
	public static EncoderPrefs IPOD = new EncoderPrefs();
	static
	{
		IPOD.setAudioCodec("aac");
		IPOD.setAudioBitrate(128);
		IPOD.setVideoCodec("mpeg4");
		IPOD.setBitrate(600);
		IPOD.setMacroblockDecision(2);
		IPOD.setFlags("+4mv+trell");
		IPOD.setEnableAdvancedIntraCoding(2);
		IPOD.setComparisonForMotionEstimation(2);
		IPOD.setSubComparisonForMotionEstimation(2);
		IPOD.setSize(320, 240);
	}
	/**
	 * PSP
	 * 
	 * 
	 * OLD FIRMWARE (2.0)
	 * 
	 * ffmpeg -i $1 -acodec aac -ab 128 -vcodec h264 -b 1200 -ar 48000 -mbd 2
	 * -coder 1 -cmp 2 -subcmp 2 -s 368x192 -r 30000/1001 -title X -f psp -flags
	 * loop -trellis 2 -partitions parti4x4+parti8x8+partp4x4+partp8x8+partb8x8
	 * -t 20 $2
	 * 
	 * FIRMWARE (2.7)
	 * 
	 * ffmpeg -i input -acodec aac -ab 128 -vcodec mpeg4 -b 1200 -ar 24000 -mbd
	 * 2 -flags +4mv+trell -aic 2 -cmp 2 -subcmp 2 -s 368x192 -r 30000/1001
	 * -title X -f psp output.mp4
	 * 
	 * 
	 * 
	 * ffmpeg -i
	 * /usr/lib/apache-tomcat-5.5.17/webapps/ROOT/resources/Media/2waco.-.the.rules.of.engagement.-.extra.-.02.-.complete.911.calls.-.docdvdrip.avi
	 * -ab 24000 -ar 128 -b 600 -r 30000/1001 -s 368x192 -aic 2 -cmp 2 -subcmp 2
	 * -mbd 2 -flags +4mv+trell -vcodec mpeg4 -acodec aac -f psp -title Waco 911
	 * calls -timestamp 2006-07-01 00:28:47
	 * /usr/lib/apache-tomcat-5.5.17/webapps/ROOT/resources/Media/psp/2waco.-.the.rules.of.engagement.-.extra.-.02.-.complete.911.calls.-.docdvdrip.mp4
	 * 
	 * 
	 * 
	 */
	public static EncoderPrefs PSP = new EncoderPrefs();
	static
	{
		PSP.setAudioCodec("aac");
		PSP.setAudioResolution(24000);
		PSP.setAudioBitrate(128);
		PSP.setVideoCodec("mpeg4");
		PSP.setBitrate(600);
		PSP.setMacroblockDecision(2);
		PSP.setFlags("+4mv+trell");
		PSP.setEnableAdvancedIntraCoding(2);
		PSP.setComparisonForMotionEstimation(2);
		PSP.setSubComparisonForMotionEstimation(2);
		PSP.setSize(368, 192);
		PSP.setFramerate(30000f/1001f);
		PSP.setFormat("psp");
		// PSP.setCoder(1);
		// PSP.setTrellis(2);
		// PSP.setPartitions("parti4x4+parti8x8+partp4x4+partp8x8+partb8x8");
		// PSP.setEnableAdvancedIntraCoding(2);
	}
	/**
	 * Razr V3
	 * 
	 * ffmpeg -i $1 -acodec amr_nb -ar 8000 -ac 1 -ab 32 -vcodec h263 -s qcif -r
	 * 10 -t 20 -b 40 $2
	 */
	public static EncoderPrefs V3 = new EncoderPrefs();
	static
	{
		V3.setAudioCodec("amr_nb");
		V3.setAudioResolution(8000);
		V3.setAudioChannels(1);
		V3.setAudioBitrate(32);
		V3.setVideoCodec("h263");
		V3.setFramerate(5);
		V3.setBitrate(65);
		// V3.setSize("qcif");
		V3.setSize(176, 144);
	}
	/**
	 * Thumbnails
	 * 
	 * 
	 * ffmpeg -y -i $1 -f singlejpeg -ss 5 -vframes 1 -s 160x120 -an $2
	 * 
	 * 
	 */
	public static EncoderPrefs PSPTHUMB = new EncoderPrefs();
	static
	{
		// PSPTHUMB.setFormat("singlejpeg");
		PSPTHUMB.setSeek(35);
		PSPTHUMB.setTotalVideoFrames(1);
		PSPTHUMB.setSize(160, 120);
		PSPTHUMB.setAudioDisabled(true);
	}
	/**
	 * !!!!!!!!!!!!!!!!!!!!!! remember %d in the output filename !!!
	 * 
	 * ffmpeg -i video.flv -an -r 1 -y -s 320x240 video%d.jpg
	 */
	public static EncoderPrefs JPGS = new EncoderPrefs();
	static
	{
		// JPGS.setFormat("singlejpeg");
		JPGS.setSeek(56);
		JPGS.setTotalVideoFrames(5);
		JPGS.setFramerate(1);
		JPGS.setSize(320, 240);
		JPGS.setAudioDisabled(true);
	}
}
