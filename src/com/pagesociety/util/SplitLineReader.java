/*
 * Created on Sep 20, 2004
 *
 */
package com.pagesociety.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author duke
 */
public class SplitLineReader {
	int line;
	InputStreamReader fis;
	private char splitChar;
	private BufferedReader br;
	private static String CHARSET_NAME = "Cp1252";

	public SplitLineReader(String path) {
		this(path, '\t');
	}

	public SplitLineReader(String path, char split) {
		this.splitChar = split;
		openFile(path);
	}

	public void openFile(String path) {
		line = 0;
		try {
			fis = new InputStreamReader(new FileInputStream(path), CHARSET_NAME);
			br = new BufferedReader(fis);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String[] next() {
		if (fis == null) {
			return null;
		}
		String record;
		try {
			record = br.readLine();
		} catch (IOException e) {
			close();
			e.printStackTrace();
			return null;
		}
		if (record == null) {
			close();
			return null;
		}
		line++;
		record = fixEntities(record);
		return split(record);
	}

	public void close() {
		try {
			fis.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		fis = null;
		br = null;
	}

	private String fixEntities(String record) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < record.length(); i++) {
			int c = record.charAt(i);
			if (c > 255) {
				b.append("&#" + c + ";");
			} else {
				b.append((char) c);
			}
		}
		return b.toString();
	}

	private String[] split(String string) {
		List<String> b = new ArrayList<String>();
		int l = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == splitChar) {
				b.add(string.substring(l, i));
				l = i + 1;
			}
		}
		if (l < string.length()) {
			b.add(string.substring(l));
		}
		String[] s = new String[b.size()];
		for (int i = 0; i < b.size(); i++) {
			s[i] = (String) b.get(i);
		}
		return s;
	}
}