/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.loadbalancing.service;

import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

/**
 * Error responses for LoadBalancing service extend this class.
 *
 * <p>An example error code for load balancing is "AccessPointNotFound".</p>
 *
 * http://docs.aws.amazon.com/ElasticLoadBalancing/latest/APIReference/CommonErrors.html
 *
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class LoadBalancingException extends EucalyptusWebServiceException {
  private static final long serialVersionUID = 1L;

	public static final Role DEFAULT_ROLE = Role.Receiver;
	public static final String DEFAULT_CODE = "InternalFailure";

  public LoadBalancingException(final String message){
		this(DEFAULT_CODE, DEFAULT_ROLE, message);
	}
	public LoadBalancingException(final String message, Throwable inner){
		this(DEFAULT_CODE, DEFAULT_ROLE, message);
		this.initCause(inner);
	}
 	public LoadBalancingException( 
                               final String code, 
                               final Role role, 
                               final String message ) {
 		 super( code, role, message );
  	}


}