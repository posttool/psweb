package com.pagesociety.web;


@SuppressWarnings("serial")
public class SlotException extends WebApplicationException {

	public SlotException(String message) {
		super(message);

	}

	public SlotException(String message, Throwable t) {
		super(message, t);
	}

}
