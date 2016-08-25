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

package com.eucalyptus.objectstorage.exceptions.s3

import com.eucalyptus.objectstorage.exceptions.ObjectStorageException
import org.jboss.netty.handler.codec.http.HttpResponseStatus

/*
 * S3 Error codes. See http://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html
 */

class S3ErrorCodeStrings {
  public static final String AccessDenied = "AccessDenied"
  public static final String AccessForbidden = "AccessForbidden"
  public static final String AccountProblem = "AccountProblem"
  public static final String AmbiguousGrantByEmailAddress = "AmbiguousGrantByEmailAddress"
  public static final String BadDigest = "BadDigest"
  public static final String BadRequest = "BadRequest"
  public static final String BucketAlreadyExists = "BucketAlreadyExists"
  public static final String BucketAlreadyOwnedByYou = "BucketAlreadyOwnedByYou"
  public static final String BucketNotEmpty = "BucketNotEmpty"
  public static final String CredentialsNotSupported = "CredentialsNotSupported"
  public static final String CrossLocationLoggingProhibited = "CrossLocationLoggingProhibited"
  public static final String EntityTooSmall = "EntityTooSmall"
  public static final String EntityTooLarge = "EntityTooLarge"
  public static final String ExpiredToken = "ExpiredToken"
  public static final String IllegalVersioningConfigurationException = "IllegalVersioningConfigurationException"
  public static final String IncompleteBody = "IncompleteBody"
  public static final String IncorrectNumberOfFilesInPostRequest = "IncorrectNumberOfFilesInPostRequest"
  public static final String InlineDataTooLarge = "InlineDataTooLarge"
  public static final String InternalError = "InternalError"
  public static final String InvalidAccessKeyId = "InvalidAccessKeyId"
  public static final String InvalidAddressingHeader = "InvalidAddressingHeader"
  public static final String InvalidArgument = "InvalidArgument"
  public static final String InvalidBucketName = "InvalidBucketName"
  public static final String InvalidBucketState = "InvalidBucketState"
  public static final String InvalidDigest = "InvalidDigest"
  public static final String InvalidLocationConstraint = "InvalidLocationConstraint"
  public static final String InvalidObjectState = "InvalidObjectState"
  public static final String InvalidPart = "InvalidPart"
  public static final String InvalidPartOrder = "InvalidPartOrder"
  public static final String InvalidPayer = "InvalidPayer"
  public static final String InvalidPolicyDocument = "InvalidPolicyDocument"
  public static final String InvalidRange = "InvalidRange"
  public static final String InvalidRequest = "InvalidRequest"
  public static final String InvalidSecurity = "InvalidSecurity"
  public static final String InvalidSOAPRequest = "InvalidSOAPRequest"
  public static final String InvalidStorageClass = "InvalidStorageClass"
  public static final String InvalidTagError = "InvalidTagError"
  public static final String InvalidTargetBucketForLogging = "InvalidTargetBucketForLogging"
  public static final String InvalidToken = "InvalidToken"
  public static final String InvalidURI = "InvalidURI"
  public static final String KeyTooLong = "KeyTooLong"
  public static final String MalformedACLError = "MalformedACLError"
  public static final String MalformedPOSTRequest = "MalformedPOSTRequest"
  public static final String MalformedXML = "MalformedXML"
  public static final String MaxMessageLengthExceeded = "MaxMessageLengthExceeded"
  public static final String MaxPostPreDataLengthExceededError = "MaxPostPreDataLengthExceededError"
  public static final String MetadataTooLarge = "MetadataTooLarge"
  public static final String MethodNotAllowed = "MethodNotAllowed"
  public static final String MissingAttachment = "MissingAttachment"
  public static final String MissingContentLength = "MissingContentLength"
  public static final String MissingRequestBodyError = "MissingRequestBodyError"
  public static final String MissingSecurityElement = "MissingSecurityElement"
  public static final String MissingSecurityHeader = "MissingSecurityHeader"
  public static final String NoLoggingStatusForKey = "NoLoggingStatusForKey"
  public static final String NoSuchBucket = "NoSuchBucket"
  public static final String NoSuchCORSConfiguration = "NoSuchCORSConfiguration"
  public static final String NoSuchKey = "NoSuchKey"
  public static final String NoSuchLifecycleConfiguration = "NoSuchLifecycleConfiguration"
  public static final String NoSuchUpload = "NoSuchUpload"
  public static final String NoSuchVersion = "NoSuchVersion"
  public static final String NotImplemented = "NotImplemented"
  public static final String NotSignedUp = "NotSignedUp"
  public static final String NotSuchBucketPolicy = "NotSuchBucketPolicy"
  public static final String NoSuchTagSet = "NoSuchTagSet"
  public static final String OperationAborted = "OperationAborted"
  public static final String PermanentRedirect = "PermanentRedirect"
  public static final String PreconditionFailed = "PreconditionFailed"
  public static final String Redirect = "Redirect"
  public static final String RestoreAlreadyInProgress = "RestoreAlreadyInProgress"
  public static final String RequestIsNotMultiPartContent = "RequestIsNotMultiPartContent"
  public static final String RequestTimeout = "RequestTimeout"
  public static final String RequestTimeTooSkewed = "RequestTimeTooSkewed"
  public static final String RequestTorrentOfBucketError = "RequestTorrentOfBucketError"
  public static final String SignatureDoesNotMatch = "SignatureDoesNotMatch"
  public static final String ServiceUnavailable = "ServiceUnavailable"
  public static final String SlowDown = "SlowDown"
  public static final String TemporaryRedirect = "TemporaryRedirect"
  public static final String TokenRefreshRequired = "TokenRefreshRequired"
  public static final String TooManyBuckets = "TooManyBuckets"
  public static final String UnexpectedContent = "UnexpectedContent"
  public static final String UnresolvableGrantByEmailAddress = "UnresolvableGrantByEmailAddress"
  public static final String UserKeyMustBeSpecified = "UserKeyMustBeSpecified"
}

class S3Exception extends ObjectStorageException {
  def S3Exception() {}

  def S3Exception(String errorCode, String description, HttpResponseStatus statusCode) {
    super();
    this.code = errorCode;
    this.message = description;
    this.status = statusCode;
  }
}

class S3ExtendedException extends S3Exception {
  String requestMethod;
  
  def S3ExtendedException() {}
  
  def S3ExtendedException(String errorCode, String description, HttpResponseStatus statusCode) {
    super(errorCode, description, statusCode);
  }
  
  public void setRequestMethod(String requestMethod) {
    this.requestMethod = requestMethod;
  }

  public String getRequestMethod() {
    return requestMethod;
  }

}

class AccessDeniedException extends S3Exception {
  def AccessDeniedException() {
    super(S3ErrorCodeStrings.AccessDenied, "Access Denied", HttpResponseStatus.FORBIDDEN);
  }

  def AccessDeniedException(String resource) {
    this();
    this.resource = resource;
  }
}

class AccountProblemException extends S3Exception {
  def AccountProblemException() {
    super(S3ErrorCodeStrings.AccountProblem, "There is a problem with your Eucalyptus account that prevents the operation from completing successfully. Please use Contact Us.", HttpResponseStatus.FORBIDDEN);
  }

  def AccountProblemException(String resource) {
    this();
    this.resource = resource;
  }
}

class AmbiguousGrantByEmailAddressException extends S3Exception {
  def AmbiguousGrantByEmailAddressException() {
    super(S3ErrorCodeStrings.AmbiguousGrantByEmailAddress, "The e-mail address you provided is associated with more than one account.", HttpResponseStatus.BAD_REQUEST);
  }

  def AmbiguousGrantByEmailAddressException(String resource) {
    this();
    this.resource = resource;
  }
}

class BadDigestException extends S3Exception {
  def BadDigestException() {
    super(S3ErrorCodeStrings.BadDigest, "The Content-MD5 you specified did not match what we received.", HttpResponseStatus.BAD_REQUEST);
  }

  def BadDigestException(String resource) {
    this();
    this.resource = resource;
  }
}

class BucketAlreadyExistsException extends S3Exception {
  def BucketAlreadyExistsException() {
    super(S3ErrorCodeStrings.BucketAlreadyExists, "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.", HttpResponseStatus.CONFLICT);
  }

  def BucketAlreadyExistsException(String resource) {
    this();
    this.resource = resource;
  }
}

class BucketAlreadyOwnedByYouException extends S3Exception {
  def BucketAlreadyOwnedByYouException() {
    super(S3ErrorCodeStrings.BucketAlreadyOwnedByYou, "Your previous request to create the named bucket succeeded and you already own it.", HttpResponseStatus.CONFLICT);
  }

  def BucketAlreadyOwnedByYouException(String resource) {
    this();
    this.resource = resource;
  }
}

class BucketNotEmptyException extends S3Exception {
  def BucketNotEmptyException() {
    super(S3ErrorCodeStrings.BucketNotEmpty, "The bucket you tried to delete is not empty.", HttpResponseStatus.CONFLICT);
  }

  def BucketNotEmptyException(String resource) {
    this();
    this.resource = resource;
  }
}

// On a CORS PUT of a new CORS configuration, if a bad HTTP verb (e.g. "YUCK")
// or an unsupported HTTP verb (e.g. "OPTIONS") is provided,
// to match AWS's response, return 400 Bad Request with this error code
// and message.
class CorsConfigUnsupportedMethodException extends S3Exception {
  def CorsConfigUnsupportedMethodException(String method) {
    super(S3ErrorCodeStrings.InvalidRequest,
    "Found unsupported HTTP method in CORS config. Unsupported method is " + method,
    HttpResponseStatus.BAD_REQUEST);
  }
}

// On a CORS preflight OPTIONS request, if no HTTP verb (method) is provided,
// or if a bad HTTP verb (e.g. "YUCK") or an unsupported HTTP verb
// (e.g. "OPTIONS") is provided,
// to match AWS's response, return 400 Bad Request with this error code
// and message.
class CorsPreflightInvalidMethodException extends S3Exception {
  def CorsPreflightInvalidMethodException(String method) {
    super(S3ErrorCodeStrings.BadRequest,
    "Invalid Access-Control-Request-Method: " + method,
    HttpResponseStatus.BAD_REQUEST);
  }
}

// On a CORS preflight OPTIONS request, when no CORS configuration exists,
// to match AWS's response, return 403 Forbidden with this error code
// and message.
class CorsPreflightNoConfigException extends S3ExtendedException {
  def CorsPreflightNoConfigException(String requestMethod, String resourceType) {
    super(S3ErrorCodeStrings.AccessForbidden,
    "CORSResponse: CORS is not enabled for this bucket.",
    HttpResponseStatus.FORBIDDEN);
    this.setResourceType(resourceType);
    this.setRequestMethod(requestMethod);
  }
}

// On a CORS preflight OPTIONS request, if no origin is provided,
// to match AWS's response, return 400 Bad Request with this error code
// and message.
class CorsPreflightNoOriginException extends S3Exception {
  def CorsPreflightNoOriginException() {
    super(S3ErrorCodeStrings.BadRequest,
    "Insufficient information. Origin request header needed.",
    HttpResponseStatus.BAD_REQUEST);
  }
}

// On a CORS preflight OPTIONS request, if no CORS rule matches the
// requested origin and HTTP verb (method),
// to match AWS's response, return 403 Forbidden with this error code
// and message.
class CorsPreflightNotAllowedException extends S3ExtendedException {
  def CorsPreflightNotAllowedException(String requestMethod, String resourceType) {
    super(S3ErrorCodeStrings.AccessForbidden,
    "CORSResponse: This CORS request is not allowed. " +
    "This is usually because the evaluation of Origin, request method / " +
    "Access-Control-Request-Method or Access-Control-Request-Headers are " +
    "not whitelisted by the resource's CORS spec.",
    HttpResponseStatus.FORBIDDEN);
    this.setResourceType(resourceType);
    this.setRequestMethod(requestMethod);
  }
}


class CredentialsNotSupportedException extends S3Exception {
  def CredentialsNotSupportedException() {
    super(S3ErrorCodeStrings.CredentialsNotSupported, "This request does not support credentials.", HttpResponseStatus.BAD_REQUEST);
  }

  def CredentialsNotSupportedException(String resource) {
    this();
    this.resource = resource;
  }
}

class CrossLocationLoggingProhibitedException extends S3Exception {
  def CrossLocationLoggingProhibitedException() {
    super(S3ErrorCodeStrings.CrossLocationLoggingProhibited, "Cross location logging not allowed. Buckets in one geographic location cannot log information to a bucket in another location.", HttpResponseStatus.FORBIDDEN);
  }

  def CrossLocationLoggingProhibitedException(String resource) {
    this();
    this.resource = resource;
  }
}

class EntityTooSmallException extends S3Exception {
  def EntityTooSmallException() {
    super(S3ErrorCodeStrings.EntityTooSmall, "Your proposed upload is smaller than the minimum allowed object size.", HttpResponseStatus.BAD_REQUEST);
  }

  def EntityTooSmallException(String resource) {
    this();
    this.resource = resource;
  }
}

class EntityTooLargeException extends S3Exception {
  def EntityTooLargeException() {
    super(S3ErrorCodeStrings.EntityTooLarge, "Your proposed upload exceeds the maximum allowed object size.", HttpResponseStatus.BAD_REQUEST);
  }

  def EntityTooLargeException(String resource) {
    this();
    this.resource = resource;
  }
}

class ExpiredTokenException extends S3Exception {
  def ExpiredTokenException() {
    super(S3ErrorCodeStrings.ExpiredToken, "The provided token has expired.", HttpResponseStatus.BAD_REQUEST);
  }

  def ExpiredTokenException(String resource) {
    this();
    this.resource = resource;
  }
}

class IllegalVersioningConfigurationException extends S3Exception {
  def IllegalVersioningConfigurationException() {
    super(S3ErrorCodeStrings.IllegalVersioningConfigurationException, "Indicates that the Versioning configuration specified in the request is invalid.", HttpResponseStatus.BAD_REQUEST);
  }

  def IllegalVersioningConfigurationException(String resource) {
    this();
    this.resource = resource;
  }
}

class IncompleteBodyException extends S3Exception {
  def IncompleteBodyException() {
    super(S3ErrorCodeStrings.IncompleteBody, "You did not provide the number of bytes specified by the Content-Length HTTP header", HttpResponseStatus.BAD_REQUEST);
  }

  def IncompleteBodyException(String resource) {
    this();
    this.resource = resource;
  }
}

class IncorrectNumberOfFilesInPostRequestException extends S3Exception {
  def IncorrectNumberOfFilesInPostRequestException() {
    super(S3ErrorCodeStrings.IncorrectNumberOfFilesInPostRequest, "POST requires exactly one file upload per request.", HttpResponseStatus.BAD_REQUEST);
  }

  def IncorrectNumberOfFilesInPostRequestException(String resource) {
    this();
    this.resource = resource;
  }
}

class InlineDataTooLargeException extends S3Exception {
  def InlineDataTooLargeException() {
    super(S3ErrorCodeStrings.InlineDataTooLarge, "Inline data exceeds the maximum allowed size.", HttpResponseStatus.BAD_REQUEST);
  }

  def InlineDataTooLargeException(String resource) {
    this();
    this.resource = resource;
  }
}

class InternalErrorException extends S3Exception {
  def InternalErrorException() {
    super(S3ErrorCodeStrings.InternalError, "We encountered an internal error. Please try again.", HttpResponseStatus.INTERNAL_SERVER_ERROR);
  }

  def InternalErrorException(String resource) {
    this();
    this.resource = resource;
  }

  def InternalErrorException(Throwable cause) {
    this();
    super.initCause(cause);
  }

  def InternalErrorException(String resource, Throwable cause) {
    this();
    this.resource = resource;
    super.initCause(cause);
  }
}

class InvalidAccessKeyIdException extends S3Exception {
  def InvalidAccessKeyIdException() {
    super(S3ErrorCodeStrings.InvalidAccessKeyId, "The AWS Access Key Id you provided does not exist in our records.", HttpResponseStatus.FORBIDDEN);
  }

  def InvalidAccessKeyIdException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidAddressingHeaderException extends S3Exception {
  def InvalidAddressingHeaderException() {
    super(S3ErrorCodeStrings.InvalidAddressingHeader, "You must specify the Anonymous role.", null);
  }

  def InvalidAddressingHeaderException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidArgumentException extends S3Exception {
  def InvalidArgumentException() {
    super(S3ErrorCodeStrings.InvalidArgument, "Argument format not recognized", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidArgumentException(String resource) {
    this();
    this.resource = resource;
  }

  def InvalidArgumentException(String resource, String message) {
    this();
    this.resource = resource;
    this.message = message;
  }

  def InvalidArgumentException withArgumentName(String argumentName) {
    this.argumentName = argumentName;
    return this;
  }

  def InvalidArgumentException withArgumentValue(String argumentValue) {
    this.argumentValue = argumentValue;
    return this;
  }

  String argumentValue;
  String argumentName;
}

class InvalidBucketNameException extends S3Exception {
  def InvalidBucketNameException() {
    super(S3ErrorCodeStrings.InvalidBucketName, "The specified bucket is not valid.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidBucketNameException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidBucketStateException extends S3Exception {
  def InvalidBucketStateException() {
    super(S3ErrorCodeStrings.InvalidBucketState, "The request is not valid with the current state of the bucket.", HttpResponseStatus.CONFLICT);
  }

  def InvalidBucketStateException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidDigestException extends S3Exception {
  def InvalidDigestException() {
    super(S3ErrorCodeStrings.InvalidDigest, "The Content-MD5 you specified was an invalid.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidDigestException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidLocationConstraintException extends S3Exception {
  def InvalidLocationConstraintException() {
    super(S3ErrorCodeStrings.InvalidLocationConstraint, "The specified location constraint is not valid. For more information about Regions, see How to Select a Region for Your Buckets.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidLocationConstraintException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidObjectStateException extends S3Exception {
  def InvalidObjectStateException() {
    super(S3ErrorCodeStrings.InvalidObjectState, "The operation is not valid for the current state of the object.", HttpResponseStatus.FORBIDDEN);
  }

  def InvalidObjectStateException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidPartException extends S3Exception {
  def InvalidPartException() {
    super(S3ErrorCodeStrings.InvalidPart, "One or more of the specified parts could not be found. The part might not have been uploaded, or the specified entity tag might not have matched the part's entity tag.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidPartException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidPartOrderException extends S3Exception {
  def InvalidPartOrderException() {
    super(S3ErrorCodeStrings.InvalidPartOrder, "The list of parts was not in ascending order.Parts list must specified in order by part number.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidPartOrderException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidPayerException extends S3Exception {
  def InvalidPayerException() {
    super(S3ErrorCodeStrings.InvalidPayer, "All access to this object has been disabled.", HttpResponseStatus.FORBIDDEN);
  }

  def InvalidPayerException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidPolicyDocumentException extends S3Exception {
  def InvalidPolicyDocumentException() {
    super(S3ErrorCodeStrings.InvalidPolicyDocument, "The content of the form does not meet the conditions specified in the policy document.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidPolicyDocumentException(String resource) {
    this();
    this.resource = resource;
  }

  def InvalidPolicyDocumentException(String resource, String detailMessage) {
    this();
    this.resource = resource;
    this.message = this.message + " Detail: " + detailMessage
  }
}

class InvalidRangeException extends S3Exception {
  def InvalidRangeException() {
    super(S3ErrorCodeStrings.InvalidRange, "The requested range is not satisfiable.", HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
  }

  def InvalidRangeException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidRequestException extends S3Exception {
  def InvalidRequestException() {
    super(S3ErrorCodeStrings.InvalidRequest, "SOAP requests must be made over an HTTPS connection.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidRequestException(String resource) {
    this();
    this.resource = resource;
  }

  def InvalidRequestException(String resource, String message) {
    this();
    this.resource = resource;
    this.message = message;
  }
}

class InvalidSecurityException extends S3Exception {
  def InvalidSecurityException() {
    super(S3ErrorCodeStrings.InvalidSecurity, "The provided security credentials are not valid.", HttpResponseStatus.FORBIDDEN);
  }

  def InvalidSecurityException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidSOAPRequestException extends S3Exception {
  def InvalidSOAPRequestException() {
    super(S3ErrorCodeStrings.InvalidSOAPRequest, "The SOAP request body is invalid.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidSOAPRequestException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidStorageClassException extends S3Exception {
  def InvalidStorageClassException() {
    super(S3ErrorCodeStrings.InvalidStorageClass, "The storage class you specified is not valid.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidStorageClassException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidTargetBucketForLoggingException extends S3Exception {
  def InvalidTargetBucketForLoggingException() {
    super(S3ErrorCodeStrings.InvalidTargetBucketForLogging, "The target bucket for logging does not exist, is not owned by you, or does not have the appropriate grants for the log-delivery group.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidTargetBucketForLoggingException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidTokenException extends S3Exception {
  def InvalidTokenException() {
    super(S3ErrorCodeStrings.InvalidToken, "The provided token is malformed or otherwise invalid.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidTokenException(String resource) {
    this();
    this.resource = resource;
  }
}

class InvalidURIException extends S3Exception {
  def InvalidURIException() {
    super(S3ErrorCodeStrings.InvalidURI, "Couldn't parse the specified URI.", HttpResponseStatus.BAD_REQUEST);
  }

  def InvalidURIException(String resource) {
    this();
    this.resource = resource;
  }
}

class KeyTooLongException extends S3Exception {
  def KeyTooLongException() {
    super(S3ErrorCodeStrings.KeyTooLong, "Your key is too long.", HttpResponseStatus.BAD_REQUEST);
  }

  def KeyTooLongException(String resource) {
    this();
    this.resource = resource;
  }
}

class MalformedACLErrorException extends S3Exception {
  def MalformedACLErrorException() {
    super(S3ErrorCodeStrings.MalformedACLError, "The XML you provided was not well-formed or did not validate against our published schema.", HttpResponseStatus.BAD_REQUEST);
  }

  def MalformedACLErrorException(String resource) {
    this();
    this.resource = resource;
  }
}

class MalformedPOSTRequestException extends S3Exception {
  def MalformedPOSTRequestException() {
    super(S3ErrorCodeStrings.MalformedPOSTRequest, "The body of your POST request is not well-formed multipart/form-data.", HttpResponseStatus.BAD_REQUEST);
  }

  def MalformedPOSTRequestException(String resource) {
    this();
    this.resource = resource;
  }
}

class MalformedXMLException extends S3Exception {
  def MalformedXMLException() {
    super(S3ErrorCodeStrings.MalformedXML, "The XML you provided was not well-formed or did not validate against our published schema.", HttpResponseStatus.BAD_REQUEST);
  }

  def MalformedXMLException(String resource) {
    this();
    this.resource = resource;
  }
}

class MaxMessageLengthExceededException extends S3Exception {
  def MaxMessageLengthExceededException() {
    super(S3ErrorCodeStrings.MaxMessageLengthExceeded, "Your request was too big.", HttpResponseStatus.BAD_REQUEST);
  }

  def MaxMessageLengthExceededException(String resource) {
    this();
    this.resource = resource;
  }
}

class MaxPostPreDataLengthExceededErrorException extends S3Exception {
  def MaxPostPreDataLengthExceededErrorException() {
    super(S3ErrorCodeStrings.MaxPostPreDataLengthExceededError, "Your POST request fields preceding the upload file were too large.", HttpResponseStatus.BAD_REQUEST);
  }

  def MaxPostPreDataLengthExceededErrorException(String resource) {
    this();
    this.resource = resource;
  }
}

class MetadataTooLargeException extends S3Exception {
  def MetadataTooLargeException() {
    super(S3ErrorCodeStrings.MetadataTooLarge, "Your metadata headers exceed the maximum allowed metadata size.", HttpResponseStatus.BAD_REQUEST);
  }

  def MetadataTooLargeException(String resource) {
    this();
    this.resource = resource;
  }
}

class MethodNotAllowedException extends S3Exception {
  def MethodNotAllowedException() {
    super(S3ErrorCodeStrings.MethodNotAllowed, "The specified method is not allowed against this resource.", HttpResponseStatus.METHOD_NOT_ALLOWED);
  }

  def MethodNotAllowedException(String resource) {
    this();
    this.resource = resource;
  }
}

class MissingAttachmentException extends S3Exception {
  def MissingAttachmentException() {
    super(S3ErrorCodeStrings.MissingAttachment, "A SOAP attachment was expected, but none were found.", null);
  }

  def MissingAttachmentException(String resource) {
    this();
    this.resource = resource;
  }
}

class MissingContentLengthException extends S3Exception {
  def MissingContentLengthException() {
    super(S3ErrorCodeStrings.MissingContentLength, "You must provide the Content-Length HTTP header.", HttpResponseStatus.LENGTH_REQUIRED);
  }

  def MissingContentLengthException(String resource) {
    this();
    this.resource = resource;
  }
}

class MissingRequestBodyErrorException extends S3Exception {
  def MissingRequestBodyErrorException() {
    super(S3ErrorCodeStrings.MissingRequestBodyError, "Request body is empty.", HttpResponseStatus.BAD_REQUEST);
  }

  def MissingRequestBodyErrorException(String resource) {
    this();
    this.resource = resource;
  }
}

class MissingSecurityElementException extends S3Exception {
  def MissingSecurityElementException() {
    super(S3ErrorCodeStrings.MissingSecurityElement, "The SOAP 1.1 request is missing a security element.", HttpResponseStatus.BAD_REQUEST);
  }

  def MissingSecurityElementException(String resource) {
    this();
    this.resource = resource;
  }
}

class MissingSecurityHeaderException extends S3Exception {
  def MissingSecurityHeaderException() {
    super(S3ErrorCodeStrings.MissingSecurityHeader, "Your request was missing a required header.", HttpResponseStatus.BAD_REQUEST);
  }

  def MissingSecurityHeaderException(String resource) {
    this();
    this.resource = resource;
  }
}

class NoLoggingStatusForKeyException extends S3Exception {
  def NoLoggingStatusForKeyException() {
    super(S3ErrorCodeStrings.NoLoggingStatusForKey, "There is no such thing as a logging status sub-resource for a key.", HttpResponseStatus.BAD_REQUEST);
  }

  def NoLoggingStatusForKeyException(String resource) {
    this();
    this.resource = resource;
  }
}

class NoSuchBucketException extends S3Exception {
  def NoSuchBucketException() {
    super(S3ErrorCodeStrings.NoSuchBucket, "The specified bucket does not exist.", HttpResponseStatus.NOT_FOUND);
  }

  def NoSuchBucketException(String resource) {
    this();
    this.resource = resource;
  }
}

class NoSuchKeyException extends S3Exception {
  def NoSuchKeyException() {
    super(S3ErrorCodeStrings.NoSuchKey, "The specified key does not exist.", HttpResponseStatus.NOT_FOUND);
  }

  def NoSuchKeyException(String resource) {
    this();
    this.resource = resource;
  }
}

class NoSuchLifecycleConfigurationException extends S3Exception {
  def NoSuchLifecycleConfigurationException() {
    super(S3ErrorCodeStrings.NoSuchLifecycleConfiguration, "The lifecycle configuration does not exist.", HttpResponseStatus.NOT_FOUND);
  }

  def NoSuchLifecycleConfigurationException(String resource) {
    this();
    this.resource = resource;
  }
}

// When no CORS configuration exists, to match AWS's response, return 404 NotFound with
// this error code and message.
class NoSuchCorsConfigurationException extends S3Exception {
  def NoSuchCorsConfigurationException() {
    super(S3ErrorCodeStrings.NoSuchCORSConfiguration, "The CORS configuration does not exist.", HttpResponseStatus.NOT_FOUND);
  }

  def NoSuchCorsConfigurationException(String resource) {
    this();
    this.resource = resource;
  }
}

class NoSuchUploadException extends S3Exception {
  def NoSuchUploadException() {
    super(S3ErrorCodeStrings.NoSuchUpload, "The specified multipart upload does not exist. The upload ID might be invalid, or the multipart upload might have been aborted or completed.", HttpResponseStatus.NOT_FOUND);
  }

  def NoSuchUploadException(String resource) {
    this();
    this.resource = resource;
  }
}

class NoSuchVersionException extends S3Exception {
  def NoSuchVersionException() {
    super(S3ErrorCodeStrings.NoSuchVersion, "Indicates that the version ID specified in the request does not match an existing version.", HttpResponseStatus.NOT_FOUND);
  }

  def NoSuchVersionException(String resource) {
    this();
    this.resource = resource;
  }
}

class NotImplementedException extends S3Exception {
  def NotImplementedException() {
    super(S3ErrorCodeStrings.NotImplemented, "A header you provided implies functionality that is not implemented.", HttpResponseStatus.NOT_IMPLEMENTED);
  }

  def NotImplementedException(String resource) {
    this();
    this.resource = resource;
  }
}

class NotSignedUpException extends S3Exception {
  def NotSignedUpException() {
    super(S3ErrorCodeStrings.NotSignedUp, "Your account is not signed up for this service. You must sign up before you can use it.", HttpResponseStatus.FORBIDDEN);
  }

  def NotSignedUpException(String resource) {
    this();
    this.resource = resource;
  }
}

class NotSuchBucketPolicyException extends S3Exception {
  def NotSuchBucketPolicyException() {
    super(S3ErrorCodeStrings.NotSuchBucketPolicy, "The specified bucket does not have a bucket policy.", HttpResponseStatus.NOT_FOUND);
  }

  def NotSuchBucketPolicyException(String resource) {
    this();
    this.resource = resource;
  }
}

class NoSuchTagSetException extends S3Exception {
  def NoSuchTagSetException( ) {
    super( S3ErrorCodeStrings.NoSuchTagSet, "The TagSet does not exist", HttpResponseStatus.NOT_FOUND )
  }

  def NoSuchTagSetException( String resource ) {
    this( );
    this.resource = resource;
  }
}

class InvalidTagErrorException extends S3Exception {
  def InvalidTagErrorException( ) {
    super( S3ErrorCodeStrings.InvalidTagError, "The tag provided was not a valid tag", HttpResponseStatus.BAD_REQUEST )
  }

  def InvalidTagErrorException( String resource ) {
    this( );
    this.resource = resource;
  }
}

class OperationAbortedException extends S3Exception {
  def OperationAbortedException() {
    super(S3ErrorCodeStrings.OperationAborted, "A conflicting conditional operation is currently in progress against this resource. Please try again.", HttpResponseStatus.CONFLICT);
  }

  def OperationAbortedException(String resource) {
    this();
    this.resource = resource;
  }
}

class PermanentRedirectException extends S3Exception {
  def PermanentRedirectException() {
    super(S3ErrorCodeStrings.PermanentRedirect, "The bucket you are attempting to access must be addressed using the specified endpoint. Please send all future requests to this endpoint.", HttpResponseStatus.MOVED_PERMANENTLY);
  }

  def PermanentRedirectException(String resource) {
    this();
    this.resource = resource;
  }
}

class PreconditionFailedException extends S3Exception {
  def PreconditionFailedException() {
    super(S3ErrorCodeStrings.PreconditionFailed, "At least one of the preconditions you specified did not hold.", HttpResponseStatus.PRECONDITION_FAILED);
  }

  def PreconditionFailedException(String resource) {
    this();
    this.resource = resource;
  }
}

class RedirectException extends S3Exception {
  def RedirectException() {
    super(S3ErrorCodeStrings.Redirect, "Temporary redirect.", HttpResponseStatus.TEMPORARY_REDIRECT);
  }

  def RedirectException(String resource) {
    this();
    this.resource = resource;
  }
}

class RestoreAlreadyInProgressException extends S3Exception {
  def RestoreAlreadyInProgressException() {
    super(S3ErrorCodeStrings.RestoreAlreadyInProgress, "Object restore is already in progress.", HttpResponseStatus.CONFLICT);
  }

  def RestoreAlreadyInProgressException(String resource) {
    this();
    this.resource = resource;
  }
}

class RequestIsNotMultiPartContentException extends S3Exception {
  def RequestIsNotMultiPartContentException() {
    super(S3ErrorCodeStrings.RequestIsNotMultiPartContent, "Bucket POST must be of the enclosure-type multipart/form-data.", HttpResponseStatus.BAD_REQUEST);
  }

  def RequestIsNotMultiPartContentException(String resource) {
    this();
    this.resource = resource;
  }
}

class RequestTimeoutException extends S3Exception {
  def RequestTimeoutException() {
    super(S3ErrorCodeStrings.RequestTimeout, "Your socket connection to the server was not read from or written to within the timeout period.", HttpResponseStatus.BAD_REQUEST);
  }

  def RequestTimeoutException(String resource) {
    this();
    this.resource = resource;
  }
}

class RequestTimeTooSkewedException extends S3Exception {
  def RequestTimeTooSkewedException() {
    super(S3ErrorCodeStrings.RequestTimeTooSkewed, "The difference between the request time and the server's time is too large.", HttpResponseStatus.FORBIDDEN);
  }

  def RequestTimeTooSkewedException(String resource) {
    this();
    this.resource = resource;
  }
}

class RequestTorrentOfBucketErrorException extends S3Exception {
  def RequestTorrentOfBucketErrorException() {
    super(S3ErrorCodeStrings.RequestTorrentOfBucketError, "Requesting the torrent file of a bucket is not permitted.", HttpResponseStatus.BAD_REQUEST);
  }

  def RequestTorrentOfBucketErrorException(String resource) {
    this();
    this.resource = resource;
  }
}

class SignatureDoesNotMatchException extends S3Exception {
  def SignatureDoesNotMatchException() {
    super(S3ErrorCodeStrings.SignatureDoesNotMatch, "The request signature we calculated does not match the signature you provided.", HttpResponseStatus.FORBIDDEN);
  }

  def SignatureDoesNotMatchException(String resource) {
    this();
    this.resource = resource;
  }
}

class ServiceUnavailableException extends S3Exception {
  def ServiceUnavailableException() {
    super(S3ErrorCodeStrings.ServiceUnavailable, "Please reduce your request rate.", HttpResponseStatus.SERVICE_UNAVAILABLE);
  }

  def ServiceUnavailableException(String resource) {
    this();
    this.resource = resource;
  }
}

class SlowDownException extends S3Exception {
  def SlowDownException() {
    super(S3ErrorCodeStrings.SlowDown, "Please reduce your request rate.", HttpResponseStatus.SERVICE_UNAVAILABLE);
  }

  def SlowDownException(String resource) {
    this();
    this.resource = resource;
  }
}

class TemporaryRedirectException extends S3Exception {
  def TemporaryRedirectException() {
    super(S3ErrorCodeStrings.TemporaryRedirect, "You are being redirected to the bucket while DNS updates.", HttpResponseStatus.TEMPORARY_REDIRECT);
  }

  def TemporaryRedirectException(String resource) {
    this();
    this.resource = resource;
  }
}

class TokenRefreshRequiredException extends S3Exception {
  def TokenRefreshRequiredException() {
    super(S3ErrorCodeStrings.TokenRefreshRequired, "The provided token must be refreshed.", HttpResponseStatus.BAD_REQUEST);
  }

  def TokenRefreshRequiredException(String resource) {
    this();
    this.resource = resource;
  }
}

class TooManyBucketsException extends S3Exception {
  def TooManyBucketsException() {
    super(S3ErrorCodeStrings.TooManyBuckets, "You have attempted to create more buckets than allowed.", HttpResponseStatus.BAD_REQUEST);
  }

  def TooManyBucketsException(String resource) {
    this();
    this.resource = resource;
  }
}

class UnexpectedContentException extends S3Exception {
  def UnexpectedContentException() {
    super(S3ErrorCodeStrings.UnexpectedContent, "This request does not support content.", HttpResponseStatus.BAD_REQUEST);
  }

  def UnexpectedContentException(String resource) {
    this();
    this.resource = resource;
  }
}

class UnresolvableGrantByEmailAddressException extends S3Exception {
  def UnresolvableGrantByEmailAddressException() {
    super(S3ErrorCodeStrings.UnresolvableGrantByEmailAddress, "The e-mail address you provided does not match any account on record.", HttpResponseStatus.BAD_REQUEST);
  }

  def UnresolvableGrantByEmailAddressException(String resource) {
    this();
    this.resource = resource;
  }

  String emailAddress;
}

class UserKeyMustBeSpecifiedException extends S3Exception {
  def UserKeyMustBeSpecifiedException() {
    super(S3ErrorCodeStrings.UserKeyMustBeSpecified, "The bucket POST must contain the specified field name. If it is specified, please check the order of the fields.", HttpResponseStatus.BAD_REQUEST);
  }

  def UserKeyMustBeSpecifiedException(String resource) {
    this();
    this.resource = resource;
  }
}

// Currently not used. Would not work anyway with newly added CORS exceptions that are
// many-to-one of string-to-exception.class.
// TODO: Remove?
public enum S3Exceptions {
  INSTANCE;
  public static final Map<String, Class<? extends S3Exception>> exceptionCodeToClassMap;

  static {
    exceptionCodeToClassMap = new HashMap<String, Class<? extends S3Exception>>();
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.AccessDenied, AccessDeniedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.AccountProblem, AccountProblemException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.AmbiguousGrantByEmailAddress, AmbiguousGrantByEmailAddressException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.BadDigest, BadDigestException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.BucketAlreadyExists, BucketAlreadyExistsException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.BucketAlreadyOwnedByYou, BucketAlreadyOwnedByYouException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.CredentialsNotSupported, CredentialsNotSupportedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.CrossLocationLoggingProhibited, CrossLocationLoggingProhibitedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.EntityTooLarge, EntityTooLargeException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.EntityTooSmall, EntityTooSmallException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.ExpiredToken, ExpiredTokenException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.IllegalVersioningConfigurationException, IllegalVersioningConfigurationException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.IncompleteBody, IncompleteBodyException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.IncorrectNumberOfFilesInPostRequest, IncorrectNumberOfFilesInPostRequestException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InlineDataTooLarge, InlineDataTooLargeException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InternalError, InternalErrorException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidAccessKeyId, InvalidAccessKeyIdException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidArgument, InvalidArgumentException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidBucketName, InvalidBucketNameException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidBucketState, InvalidBucketStateException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidDigest, InvalidDigestException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidLocationConstraint, InvalidLocationConstraintException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidObjectState, InvalidObjectStateException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidPart, InvalidPartException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidPartOrder, InvalidPartOrderException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidPayer, InvalidPayerException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidPolicyDocument, InvalidPolicyDocumentException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidRange, InvalidRangeException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidRequest, InvalidRequestException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidSecurity, InvalidSecurityException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidSOAPRequest, InvalidSOAPRequestException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidStorageClass, InvalidStorageClassException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidTagError, InvalidTagErrorException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidTargetBucketForLogging, InvalidTargetBucketForLoggingException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidToken, InvalidTokenException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.InvalidURI, InvalidURIException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.KeyTooLong, KeyTooLongException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MalformedACLError, MalformedACLErrorException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MalformedPOSTRequest, MalformedPOSTRequestException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MalformedXML, MalformedXMLException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MaxMessageLengthExceeded, MaxMessageLengthExceededException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MaxPostPreDataLengthExceededError, MaxPostPreDataLengthExceededErrorException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MetadataTooLarge, MetadataTooLargeException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MethodNotAllowed, MethodNotAllowedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MissingAttachment, MissingAttachmentException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MissingContentLength, MissingContentLengthException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MissingRequestBodyError, MissingRequestBodyErrorException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.MissingSecurityElement, MissingSecurityElementException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NoLoggingStatusForKey, NoLoggingStatusForKeyException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NoSuchBucket, NoSuchBucketException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NoSuchKey, NoSuchKeyException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NoSuchLifecycleConfiguration, NoSuchLifecycleConfigurationException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NoSuchUpload, NoSuchUploadException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NoSuchVersion, NoSuchVersionException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NotImplemented, NotImplementedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NotSignedUp, NotSignedUpException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NotSuchBucketPolicy, NotSuchBucketPolicyException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.NoSuchTagSet, NoSuchTagSetException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.OperationAborted, OperationAbortedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.PermanentRedirect, PermanentRedirectException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.PreconditionFailed, PreconditionFailedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.Redirect, RedirectException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.RequestIsNotMultiPartContent, RequestIsNotMultiPartContentException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.RequestTimeout, RequestTimeoutException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.RequestTimeTooSkewed, RequestTimeTooSkewedException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.RequestTorrentOfBucketError, RequestTorrentOfBucketErrorException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.RestoreAlreadyInProgress, RestoreAlreadyInProgressException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.ServiceUnavailable, ServiceUnavailableException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.SignatureDoesNotMatch, SignatureDoesNotMatchException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.SlowDown, SlowDownException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.TemporaryRedirect, TemporaryRedirectException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.TokenRefreshRequired, TokenRefreshRequiredException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.TooManyBuckets, TooManyBucketsException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.UnexpectedContent, UnexpectedContentException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.UnresolvableGrantByEmailAddress, UnresolvableGrantByEmailAddressException.class)
    exceptionCodeToClassMap.put(S3ErrorCodeStrings.UserKeyMustBeSpecified, UserKeyMustBeSpecifiedException.class)
  }
}