package com.eucalyptus.walrus.exceptions;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

@SuppressWarnings("serial")
public class MethodNotAllowedException extends WalrusException {
	
	public MethodNotAllowedException() {
		super("MethodNotAllowed");
	}

	public MethodNotAllowedException(String message, String resourceType, String resource) {
		super("MethodNotAllowed", message, resourceType, resource, HttpResponseStatus.METHOD_NOT_ALLOWED);
	}
}
