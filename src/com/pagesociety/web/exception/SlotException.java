package com.pagesociety.web.exception;



@SuppressWarnings("serial")
public class SlotException extends WebApplicationException {

	public SlotException(String message) {
		super(message);

	}

	public SlotException(String message, Throwable t) {
		super(message, t);
	}

}
