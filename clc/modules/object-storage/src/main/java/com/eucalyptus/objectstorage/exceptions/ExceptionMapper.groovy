/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.exceptions

import com.amazonaws.AmazonServiceException
import com.amazonaws.AmazonServiceException.ErrorType;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception

import org.jboss.netty.handler.codec.http.HttpResponseStatus

class S3ExceptionMapper {
	static S3Exception fromAWSJavaSDK(AmazonServiceException e) {
		S3Exception s3Ex= new S3Exception();
		s3Ex.setCode(e.getErrorCode());
		s3Ex.setMessage(e.getMessage());
		s3Ex.setStatus(HttpResponseStatus.valueOf(e.getStatusCode()));
		return s3Ex;
	}
		
	static AmazonServiceException toAWSJavaSDKService(S3Exception e) {
		AmazonServiceException ex = new AmazonServiceException(e.getMessage());
		ex.setStatusCode(e.getStatus().code);
		ex.setErrorCode(e.getCode());
		ex.setErrorType(ErrorType.Client);		
		return ex;		
	}
}
