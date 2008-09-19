package com.pagesociety.web;


@SuppressWarnings("serial")
public class SyncException extends WebApplicationException {

	public SyncException(String message) {
		super(message);

	}

	public SyncException(String message, Throwable t) {
		super(message, t);
	}

}
