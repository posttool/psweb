package com.pagesociety.web.exception;



@SuppressWarnings("serial")
public class LoginFailedException extends WebApplicationException {

	public LoginFailedException(String message) {
		super(message);

	}

	public LoginFailedException(String message,int code) {
		super(message,code);

	}

	public LoginFailedException(String message, Throwable t) {
		super(message, t);
	}

}
