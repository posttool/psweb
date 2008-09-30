package com.pagesociety.web;


@SuppressWarnings("serial")
public class AccountLockedException extends LoginFailedException {

	public AccountLockedException(String message) {
		super(message);

	}

	public AccountLockedException(String message, Throwable t) {
		super(message, t);
	}

}
