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
