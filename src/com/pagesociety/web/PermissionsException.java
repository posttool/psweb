package com.pagesociety.web;


@SuppressWarnings("serial")
public class PermissionsException extends WebApplicationException {

	public PermissionsException(String message) {
		super(message);

	}

	public PermissionsException(String message, Throwable t) {
		super(message, t);
	}

}
