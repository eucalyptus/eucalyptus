/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.loadbalancing;

import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class LoadBalancingException extends EucalyptusWebServiceException {
  
	public static final String DUPLICATE_LOADBALANCER_EXCEPTION = "Duplicate loadbalancer name is found";
	public static final String LOADBALANCER_CREATE_EXCEPTION = "Failed to create a new loadbalancer";
	
	public static final Role DEFAULT_ROLE = Role.Receiver;
	public static final String DEFAULT_CODE = "500";  //  TODO: SPARK: Figure out the use of code
	
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