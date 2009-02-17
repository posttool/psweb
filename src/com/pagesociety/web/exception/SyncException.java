package com.pagesociety.web.exception;



@SuppressWarnings("serial")
public class SyncException extends InitializationException {

	public SyncException(String message) {
		super(message);

	}

	public SyncException(String message, Throwable t) {
		super(message, t);
	}

}
