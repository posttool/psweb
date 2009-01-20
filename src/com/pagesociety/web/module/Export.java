package com.pagesociety.web.module;

import java.lang.annotation.Retention;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Export
{
	String[] 	ParameterNames() 	default {};
	String 		Description() 		default "";
}
