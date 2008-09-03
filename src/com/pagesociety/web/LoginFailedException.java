package com.pagesociety.web;


@SuppressWarnings("serial")
public class LoginFailedException extends WebApplicationException {

	public LoginFailedException(String message) {
		super(message);

	}

	public LoginFailedException(String message, Throwable t) {
		super(message, t);
	}

}
