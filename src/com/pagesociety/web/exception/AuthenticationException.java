package com.pagesociety.web.exception;



@SuppressWarnings("serial")
public class AuthenticationException extends WebApplicationException {

	public AuthenticationException(String message) {
		super(message);

	}

	public AuthenticationException(String message, Throwable t) {
		super(message, t);
	}

}
