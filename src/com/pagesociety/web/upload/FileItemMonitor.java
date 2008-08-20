package com.pagesociety.web.upload;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.fileupload.disk.DiskFileItem;

public class FileItemMonitor extends DiskFileItem
{
	private static final long serialVersionUID = 9192199072043214722L;
	private UploadItemProgress observer;
	private long bytesRead;
	private boolean isFormField;

	public FileItemMonitor(String fieldName, String contentType, boolean isFormField,
			String fileName, File repository, UploadItemProgress observer)
	{
		super(fieldName, contentType, isFormField, fileName, MultipartFormConstants.DEFAULT_SIZE_THRESHOLD, repository);
		this.observer = observer;
		this.isFormField = isFormField;
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		OutputStream baseOutputStream = super.getOutputStream();
		if (!isFormField)
		{
			return new BytesCountingOutputStream(baseOutputStream);
		}
		else
		{
			return baseOutputStream;
		}
	}

	private class BytesCountingOutputStream extends OutputStream
	{
		// private long previousProgressUpdate;
		private OutputStream base;

		public BytesCountingOutputStream(OutputStream ous)
		{
			base = ous;
		}

		@Override
		public void close() throws IOException
		{
			base.close();
		}

		@Override
		public boolean equals(Object arg0)
		{
			return base.equals(arg0);
		}

		@Override
		public void flush() throws IOException
		{
			base.flush();
		}

		@Override
		public int hashCode()
		{
			return base.hashCode();
		}

		@Override
		public String toString()
		{
			return base.toString();
		}

		@Override
		public void write(byte[] bytes, int offset, int len) throws IOException
		{
			base.write(bytes, offset, len);
			fireProgressEvent(len);
		}

		@Override
		public void write(byte[] bytes) throws IOException
		{
			base.write(bytes);
			fireProgressEvent(bytes.length);
		}

		@Override
		public void write(int b) throws IOException
		{
			base.write(b);
			fireProgressEvent(1);
		}

		private void fireProgressEvent(int b)
		{
			bytesRead += b;
			observer.progress = (((double) (bytesRead)) / observer.fileSize) * 100.0;
			observer.bytesRead = bytesRead;
			observer.complete = (bytesRead == observer.fileSize);
			if (MultipartFormConstants.TESTING)
			{
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
