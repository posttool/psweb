package com.pagesociety.web.exception;



@SuppressWarnings("serial")
public class PermissionsException extends WebApplicationException {

	public PermissionsException(String message) {
		super(message);

	}

	public PermissionsException(String message, Throwable t) {
		super(message, t);
	}

}
