package com.pagesociety.web.exception;



@SuppressWarnings("serial")
public class AccountLockedException extends LoginFailedException {

	public AccountLockedException(String message) {
		super(message);

	}

	public AccountLockedException(String message, Throwable t) {
		super(message, t);
	}

}
