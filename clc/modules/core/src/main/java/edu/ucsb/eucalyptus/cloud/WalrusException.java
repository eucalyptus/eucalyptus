/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package edu.ucsb.eucalyptus.cloud;

import java.io.IOException;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.util.EucalyptusCloudException;

@SuppressWarnings("serial")
public class WalrusException extends EucalyptusCloudException {

	String message;
	String code;
	HttpResponseStatus errStatus;
	String resourceType;
    String resource;
    BucketLogData logData;
    
	public WalrusException()
	{
		super();
	}

	public WalrusException(String message)
	{
		super(message);
		this.message = message;
		this.code = "InternalServerError";
		this.errStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
	}

	public WalrusException(String code, String message, String resourceType, String resource, HttpResponseStatus status)
	{
		super(message);
		this.code = code;
		this.message = message;
		this.resourceType = resourceType;
		this.resource = resource;
		this.errStatus = status;
	}

	public WalrusException(String code, String message, String resourceType, String resource, HttpResponseStatus status, BucketLogData logData)
	{
		this(code, message, resourceType, resource, status);
		this.logData = logData;
	}

	public String getMessage() {
		return message;
	}
	
	public String getCode() {
		return code;
	}

	public HttpResponseStatus getStatus() {
		return errStatus;
	}
	
	public String getResourceType() {
		return resourceType;
	}
	
	public String getResource() {
		return resource;		
	}
	
	public WalrusException(String message, Throwable ex)
	{
		super(message,ex);
	}

	public BucketLogData getLogData() {
		return logData;
	}

	public void setLogData(BucketLogData logData) {
		this.logData = logData;
	}	
}
