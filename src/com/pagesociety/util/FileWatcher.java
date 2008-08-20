package com.pagesociety.util;

import java.util.*;
import java.io.*;

public abstract class FileWatcher extends TimerTask {
	private long timeStamp;
	private File file;

	public FileWatcher(File file) {
		this.file = file;
		this.timeStamp = file.lastModified();
	}

	public final void run() {
		long timeStamp = file.lastModified();

		if (this.timeStamp != timeStamp) {
			this.timeStamp = timeStamp;
			onChange(file);
		}
	}

	protected abstract void onChange(File file);

	public static void main(String args[]) {
		// monitor a single file
		TimerTask task = new FileWatcher(new File("c:/temp/text.txt")) {
			protected void onChange(File file) {
				System.out.println("File " + file.getName() + " have change !");
			}
		};

		Timer timer = new Timer();
		// repeat the check every second
		timer.schedule(task, new Date(), 1000);
	}
}