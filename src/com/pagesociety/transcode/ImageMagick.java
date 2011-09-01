package com.pagesociety.transcode;

import java.io.File;
import java.util.Map;

import com.pagesociety.util.FileInfo;
import com.pagesociety.util.Text;

public class ImageMagick extends TranscodeWorkImpl
{
	private static String EXEC_PATH = "convert";

	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String QUALITY = "quality";
	public static final String GRAYSCALE = "grayscale";

	private static final Object OUTPUT_TYPE = "type";
	

	public static void setRuntimeExecPath(String path)
	{
		EXEC_PATH = path;
	}
	
	private int 	width;
	private int 	height;
	private int 	quality = 78;
	private boolean gray;
	private String  output_type;


	public ImageMagick(File input, File output)
	{
		super(input, output);
	}

	public void setSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	public void setQuality(int q)
	{
		this.quality = q;
	}
	
	public void setGrayScale(boolean b)
	{
		this.gray = b;
	}
	
	public void setOptions(Map<String, String> options)
	{
		if (options.containsKey(WIDTH))
			width = Integer.parseInt( options.get(WIDTH) );
		if (options.containsKey(HEIGHT))
			height = Integer.parseInt( options.get(HEIGHT));
		if (options.containsKey(QUALITY))
			quality = Integer.parseInt( options.get(QUALITY));
		if (options.containsKey(GRAYSCALE))
			gray =  options.get(GRAYSCALE).equals("true");
		if (options.containsKey(OUTPUT_TYPE))
			output_type =  options.get(OUTPUT_TYPE);
	}

	public void exec() throws Exception 
	{
		throw new Exception("ImageMagick.exec should not be called");
	}
	
	public String getCmd()
	{
		return EXEC_PATH;
	}
	
	public String[] getArgs()
	{
		String output_path = output.getAbsolutePath();
		if (output_type!=null)
		{
			if (FileInfo.getSimpleType(output_path) == FileInfo.SIMPLE_TYPE_UNKNOWN)
			{
				output_path += "."+output_type;
			}
			else
			{
				output_path = output_path.substring(0, output_path.lastIndexOf('.')+1) + output_type;
			}
		}
		return new String[]{
				input.getAbsolutePath(), // input
				"-resize", get_resize_geometry_string(), // -resize 
				"-quality", String.valueOf(quality), 	// -quality
				"-colorspace", gray ? "Gray" : "RGB", 	// -colorspace
				output_path
			};
	}
	
	//TODO: This is a placeholder for now.
	//We need to refactor preview logic throughout
	//the app stack.
	//if width is 0 it means fit to height
	//if height is 0 it means fit to width
	//if width and height are provided we use them
	//and append an '>' which tells image magick:
	//	Change as per widthxheight but only if an image dimension exceeds a specified dimension. 
	//At some point getImagePreviewURL should take a map of paramters
	//instead of width and height. These parameters might change depending
	//on the simple type.
	public String get_resize_geometry_string()
	{
		StringBuilder buf = new StringBuilder();
		if(width != 0 )
			buf.append(String.valueOf(width));
		if(height != 0 )
		{
			buf.append('x');
			buf.append(String.valueOf(height));
		}
		if(width != 0 || height != 0)
			buf.append(">");
		return buf.toString();
	}
	
	

	//
	public static void main(String[] args)
	{
		String bp1 = "/Users/posttool/Pictures";
		File f1 = new File(bp1 , "daya.jpg");
		File f2 = new File(bp1 , "daya_1.jpg");
		ImageMagick.setRuntimeExecPath("/Applications/ImageMagick-6.6.3/bin/convert");
		ImageMagick i = new ImageMagick(f1, f2);
		i.setSize(150, 150);
		try{
		i.exec();
		}catch(Exception e )
		{
			e.printStackTrace();
		}
	}

	
}
