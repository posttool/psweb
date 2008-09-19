package com.pagesociety.web;


@SuppressWarnings("serial")
public class AuthenticationException extends WebApplicationException {

	public AuthenticationException(String message) {
		super(message);

	}

	public AuthenticationException(String message, Throwable t) {
		super(message, t);
	}

}
