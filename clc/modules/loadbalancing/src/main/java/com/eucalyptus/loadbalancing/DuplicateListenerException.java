package com.eucalyptus.loadbalancing;

import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 400 )
public class DuplicateListenerException extends LoadBalancingException {
	private static final long serialVersionUID = 1L;
	public DuplicateListenerException(){
		super("A Listener already exists");
	}
}
