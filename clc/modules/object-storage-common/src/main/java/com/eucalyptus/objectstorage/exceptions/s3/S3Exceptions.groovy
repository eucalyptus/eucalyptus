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

class S3Exception extends ObjectStorageException {
    def S3Exception() {}

    def S3Exception(String errorCode, String description, HttpResponseStatus statusCode) {
        super();
        this.code = errorCode;
        this.message = description;
        this.status = statusCode;
    }
}

class S3ClientException extends S3Exception {
    def S3ClientException() {
        super();
    }

    def S3ClientException(String errorCode, String description, HttpResponseStatus statusCode) {
        super();
        this.code = errorCode;
        this.message = description;
        this.status = statusCode;
    }
}

class S3ServerException extends S3Exception {
    def S3ServerException() {
        super();
    }

    def S3ServerException(String errorCode, String description, HttpResponseStatus statusCode) {
        super();
        this.code = errorCode;
        this.message = description;
        this.status = statusCode;
    }
}

class AccessDeniedException extends S3ClientException {
    def AccessDeniedException() {
        super("AccessDenied", "Access Denied", HttpResponseStatus.FORBIDDEN);
    }

    def AccessDeniedException(String resource) {
        this();
        this.resource = resource;
    }
}

class AccountProblemException extends S3ClientException {
    def AccountProblemException() {
        super("AccountProblem", "There is a problem with your Eucalyptus account that prevents the operation from completing successfully. Please use Contact Us.", HttpResponseStatus.FORBIDDEN);
    }

    def AccountProblemException(String resource) {
        this();
        this.resource = resource;
    }
}

class AmbiguousGrantByEmailAddressException extends S3ClientException {
    def AmbiguousGrantByEmailAddressException() {
        super("AmbiguousGrantByEmailAddress", "The e-mail address you provided is associated with more than one account.", HttpResponseStatus.BAD_REQUEST);
    }

    def AmbiguousGrantByEmailAddressException(String resource) {
        this();
        this.resource = resource;
    }
}

class BadDigestException extends S3ClientException {
    def BadDigestException() {
        super("BadDigest", "The Content-MD5 you specified did not match what we received.", HttpResponseStatus.BAD_REQUEST);
    }

    def BadDigestException(String resource) {
        this();
        this.resource = resource;
    }
}

class BucketAlreadyExistsException extends S3ClientException {
    def BucketAlreadyExistsException() {
        super("BucketAlreadyExists", "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.", HttpResponseStatus.CONFLICT);
    }

    def BucketAlreadyExistsException(String resource) {
        this();
        this.resource = resource;
    }
}

class BucketAlreadyOwnedByYouException extends S3ClientException {
    def BucketAlreadyOwnedByYouException() {
        super("BucketAlreadyOwnedByYou", "Your previous request to create the named bucket succeeded and you already own it.", HttpResponseStatus.CONFLICT);
    }

    def BucketAlreadyOwnedByYouException(String resource) {
        this();
        this.resource = resource;
    }
}

class BucketNotEmptyException extends S3ClientException {
    def BucketNotEmptyException() {
        super("BucketNotEmpty", "The bucket you tried to delete is not empty.", HttpResponseStatus.CONFLICT);
    }

    def BucketNotEmptyException(String resource) {
        this();
        this.resource = resource;
    }
}

class CredentialsNotSupportedException extends S3ClientException {
    def CredentialsNotSupportedException() {
        super("CredentialsNotSupported", "This request does not support credentials.", HttpResponseStatus.BAD_REQUEST);
    }

    def CredentialsNotSupportedException(String resource) {
        this();
        this.resource = resource;
    }
}

class CrossLocationLoggingProhibitedException extends S3ClientException {
    def CrossLocationLoggingProhibitedException() {
        super("CrossLocationLoggingProhibited", "Cross location logging not allowed. Buckets in one geographic location cannot log information to a bucket in another location.", HttpResponseStatus.FORBIDDEN);
    }

    def CrossLocationLoggingProhibitedException(String resource) {
        this();
        this.resource = resource;
    }
}

class EntityTooSmallException extends S3ClientException {
    def EntityTooSmallException() {
        super("EntityTooSmall", "Your proposed upload is smaller than the minimum allowed object size.", HttpResponseStatus.BAD_REQUEST);
    }

    def EntityTooSmallException(String resource) {
        this();
        this.resource = resource;
    }
}

class EntityTooLargeException extends S3ClientException {
    def EntityTooLargeException() {
        super("EntityTooLarge", "Your proposed upload exceeds the maximum allowed object size.", HttpResponseStatus.BAD_REQUEST);
    }

    def EntityTooLargeException(String resource) {
        this();
        this.resource = resource;
    }
}

class ExpiredTokenException extends S3ClientException {
    def ExpiredTokenException() {
        super("ExpiredToken", "The provided token has expired.", HttpResponseStatus.BAD_REQUEST);
    }

    def ExpiredTokenException(String resource) {
        this();
        this.resource = resource;
    }
}

class IllegalVersioningConfigurationException extends S3ClientException {
    def IllegalVersioningConfigurationException() {
        super("IllegalVersioningConfigurationException", "Indicates that the Versioning configuration specified in the request is invalid.", HttpResponseStatus.BAD_REQUEST);
    }

    def IllegalVersioningConfigurationException(String resource) {
        this();
        this.resource = resource;
    }
}

class IncompleteBodyException extends S3ClientException {
    def IncompleteBodyException() {
        super("IncompleteBody", "You did not provide the number of bytes specified by the Content-Length HTTP header", HttpResponseStatus.BAD_REQUEST);
    }

    def IncompleteBodyException(String resource) {
        this();
        this.resource = resource;
    }
}

class IncorrectNumberOfFilesInPostRequestException extends S3ClientException {
    def IncorrectNumberOfFilesInPostRequestException() {
        super("IncorrectNumberOfFilesInPostRequest", "POST requires exactly one file upload per request.", HttpResponseStatus.BAD_REQUEST);
    }

    def IncorrectNumberOfFilesInPostRequestException(String resource) {
        this();
        this.resource = resource;
    }
}

class InlineDataTooLargeException extends S3ClientException {
    def InlineDataTooLargeException() {
        super("InlineDataTooLarge", "Inline data exceeds the maximum allowed size.", HttpResponseStatus.BAD_REQUEST);
    }

    def InlineDataTooLargeException(String resource) {
        this();
        this.resource = resource;
    }
}

class InternalErrorException extends S3ServerException {
    def InternalErrorException() {
        super("InternalError", "We encountered an internal error. Please try again.", HttpResponseStatus.INTERNAL_SERVER_ERROR);
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

class InvalidAccessKeyIdException extends S3ClientException {
    def InvalidAccessKeyIdException() {
        super("InvalidAccessKeyId", "The AWS Access Key Id you provided does not exist in our records.", HttpResponseStatus.FORBIDDEN);
    }

    def InvalidAccessKeyIdException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidAddressingHeaderException extends S3ClientException {
    def InvalidAddressingHeaderException() {
        super("InvalidAddressingHeader", "You must specify the Anonymous role.", null);
    }

    def InvalidAddressingHeaderException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidArgumentException extends S3ClientException {
    def InvalidArgumentException() {
        super("InvalidArgument", "Invalid Argument", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidArgumentException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidBucketNameException extends S3ClientException {
    def InvalidBucketNameException() {
        super("InvalidBucketName", "The specified bucket is not valid.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidBucketNameException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidBucketStateException extends S3ClientException {
    def InvalidBucketStateException() {
        super("InvalidBucketState", "The request is not valid with the current state of the bucket.", HttpResponseStatus.CONFLICT);
    }

    def InvalidBucketStateException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidDigestException extends S3ClientException {
    def InvalidDigestException() {
        super("InvalidDigest", "The Content-MD5 you specified was an invalid.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidDigestException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidLocationConstraintException extends S3ClientException {
    def InvalidLocationConstraintException() {
        super("InvalidLocationConstraint", "The specified location constraint is not valid. For more information about Regions, see How to Select a Region for Your Buckets.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidLocationConstraintException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidObjectStateException extends S3ClientException {
    def InvalidObjectStateException() {
        super("InvalidObjectState", "The operation is not valid for the current state of the object.", HttpResponseStatus.FORBIDDEN);
    }

    def InvalidObjectStateException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidPartException extends S3ClientException {
    def InvalidPartException() {
        super("InvalidPart", "One or more of the specified parts could not be found. The part might not have been uploaded, or the specified entity tag might not have matched the part's entity tag.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidPartException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidPartOrderException extends S3ClientException {
    def InvalidPartOrderException() {
        super("InvalidPartOrder", "The list of parts was not in ascending order.Parts list must specified in order by part number.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidPartOrderException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidPayerException extends S3ClientException {
    def InvalidPayerException() {
        super("InvalidPayer", "All access to this object has been disabled.", HttpResponseStatus.FORBIDDEN);
    }

    def InvalidPayerException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidPolicyDocumentException extends S3ClientException {
    def InvalidPolicyDocumentException() {
        super("InvalidPolicyDocument", "The content of the form does not meet the conditions specified in the policy document.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidPolicyDocumentException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidRangeException extends S3ClientException {
    def InvalidRangeException() {
        super("InvalidRange", "The requested range cannot be satisfied.", HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    def InvalidRangeException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidRequestException extends S3ClientException {
    def InvalidRequestException() {
        super("InvalidRequest", "SOAP requests must be made over an HTTPS connection.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidRequestException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidSecurityException extends S3ClientException {
    def InvalidSecurityException() {
        super("InvalidSecurity", "The provided security credentials are not valid.", HttpResponseStatus.FORBIDDEN);
    }

    def InvalidSecurityException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidSOAPRequestException extends S3ClientException {
    def InvalidSOAPRequestException() {
        super("InvalidSOAPRequest", "The SOAP request body is invalid.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidSOAPRequestException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidStorageClassException extends S3ClientException {
    def InvalidStorageClassException() {
        super("InvalidStorageClass", "The storage class you specified is not valid.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidStorageClassException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidTargetBucketForLoggingException extends S3ClientException {
    def InvalidTargetBucketForLoggingException() {
        super("InvalidTargetBucketForLogging", "The target bucket for logging does not exist, is not owned by you, or does not have the appropriate grants for the log-delivery group.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidTargetBucketForLoggingException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidTokenException extends S3ClientException {
    def InvalidTokenException() {
        super("InvalidToken", "The provided token is malformed or otherwise invalid.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidTokenException(String resource) {
        this();
        this.resource = resource;
    }
}

class InvalidURIException extends S3ClientException {
    def InvalidURIException() {
        super("InvalidURI", "Couldn't parse the specified URI.", HttpResponseStatus.BAD_REQUEST);
    }

    def InvalidURIException(String resource) {
        this();
        this.resource = resource;
    }
}

class KeyTooLongException extends S3ClientException {
    def KeyTooLongException() {
        super("KeyTooLong", "Your key is too long.", HttpResponseStatus.BAD_REQUEST);
    }

    def KeyTooLongException(String resource) {
        this();
        this.resource = resource;
    }
}

class MalformedACLErrorException extends S3ClientException {
    def MalformedACLErrorException() {
        super("MalformedACLError", "The XML you provided was not well-formed or did not validate against our published schema.", HttpResponseStatus.BAD_REQUEST);
    }

    def MalformedACLErrorException(String resource) {
        this();
        this.resource = resource;
    }
}

class MalformedPOSTRequestException extends S3ClientException {
    def MalformedPOSTRequestException() {
        super("MalformedPOSTRequest", "The body of your POST request is not well-formed multipart/form-data.", HttpResponseStatus.BAD_REQUEST);
    }

    def MalformedPOSTRequestException(String resource) {
        this();
        this.resource = resource;
    }
}

class MalformedXMLException extends S3ClientException {
    def MalformedXMLException() {
        super("MalformedXML", "The XML you provided was not well-formed or did not validate against our published schema.", HttpResponseStatus.BAD_REQUEST);
    }

    def MalformedXMLException(String resource) {
        this();
        this.resource = resource;
    }
}

class MaxMessageLengthExceededException extends S3ClientException {
    def MaxMessageLengthExceededException() {
        super("MaxMessageLengthExceeded", "Your request was too big.", HttpResponseStatus.BAD_REQUEST);
    }

    def MaxMessageLengthExceededException(String resource) {
        this();
        this.resource = resource;
    }
}

class MaxPostPreDataLengthExceededErrorException extends S3ClientException {
    def MaxPostPreDataLengthExceededErrorException() {
        super("MaxPostPreDataLengthExceededError", "Your POST request fields preceding the upload file were too large.", HttpResponseStatus.BAD_REQUEST);
    }

    def MaxPostPreDataLengthExceededErrorException(String resource) {
        this();
        this.resource = resource;
    }
}

class MetadataTooLargeException extends S3ClientException {
    def MetadataTooLargeException() {
        super("MetadataTooLarge", "Your metadata headers exceed the maximum allowed metadata size.", HttpResponseStatus.BAD_REQUEST);
    }

    def MetadataTooLargeException(String resource) {
        this();
        this.resource = resource;
    }
}

class MethodNotAllowedException extends S3ClientException {
    def MethodNotAllowedException() {
        super("MethodNotAllowed", "The specified method is not allowed against this resource.", HttpResponseStatus.METHOD_NOT_ALLOWED);
    }

    def MethodNotAllowedException(String resource) {
        this();
        this.resource = resource;
    }
}

class MissingAttachmentException extends S3ClientException {
    def MissingAttachmentException() {
        super("MissingAttachment", "A SOAP attachment was expected, but none were found.", null);
    }

    def MissingAttachmentException(String resource) {
        this();
        this.resource = resource;
    }
}

class MissingContentLengthException extends S3ClientException {
    def MissingContentLengthException() {
        super("MissingContentLength", "You must provide the Content-Length HTTP header.", HttpResponseStatus.LENGTH_REQUIRED);
    }

    def MissingContentLengthException(String resource) {
        this();
        this.resource = resource;
    }
}

class MissingRequestBodyErrorException extends S3ClientException {
    def MissingRequestBodyErrorException() {
        super("MissingRequestBodyError", "Request body is empty.", HttpResponseStatus.BAD_REQUEST);
    }

    def MissingRequestBodyErrorException(String resource) {
        this();
        this.resource = resource;
    }
}

class MissingSecurityElementException extends S3ClientException {
    def MissingSecurityElementException() {
        super("MissingSecurityElement", "The SOAP 1.1 request is missing a security element.", HttpResponseStatus.BAD_REQUEST);
    }

    def MissingSecurityElementException(String resource) {
        this();
        this.resource = resource;
    }
}

class MissingSecurityHeaderException extends S3ClientException {
    def MissingSecurityHeaderException() {
        super("MissingSecurityHeader", "Your request was missing a required header.", HttpResponseStatus.BAD_REQUEST);
    }

    def MissingSecurityHeaderException(String resource) {
        this();
        this.resource = resource;
    }
}

class NoLoggingStatusForKeyException extends S3ClientException {
    def NoLoggingStatusForKeyException() {
        super("NoLoggingStatusForKey", "There is no such thing as a logging status sub-resource for a key.", HttpResponseStatus.BAD_REQUEST);
    }

    def NoLoggingStatusForKeyException(String resource) {
        this();
        this.resource = resource;
    }
}

class NoSuchBucketException extends S3ClientException {
    def NoSuchBucketException() {
        super("NoSuchBucket", "The specified bucket does not exist.", HttpResponseStatus.NOT_FOUND);
    }

    def NoSuchBucketException(String resource) {
        this();
        this.resource = resource;
    }
}

class NoSuchKeyException extends S3ClientException {
    def NoSuchKeyException() {
        super("NoSuchKey", "The specified key does not exist.", HttpResponseStatus.NOT_FOUND);
    }

    def NoSuchKeyException(String resource) {
        this();
        this.resource = resource;
    }
}

class NoSuchLifecycleConfigurationException extends S3ClientException {
    def NoSuchLifecycleConfigurationException() {
        super("NoSuchLifecycleConfiguration", "The lifecycle configuration does not exist.", HttpResponseStatus.NOT_FOUND);
    }

    def NoSuchLifecycleConfigurationException(String resource) {
        this();
        this.resource = resource;
    }
}

class NoSuchUploadException extends S3ClientException {
    def NoSuchUploadException() {
        super("NoSuchUpload", "The specified multipart upload does not exist. The upload ID might be invalid, or the multipart upload might have been aborted or completed.", HttpResponseStatus.NOT_FOUND);
    }

    def NoSuchUploadException(String resource) {
        this();
        this.resource = resource;
    }
}

class NoSuchVersionException extends S3ClientException {
    def NoSuchVersionException() {
        super("NoSuchVersion", "Indicates that the version ID specified in the request does not match an existing version.", HttpResponseStatus.NOT_FOUND);
    }

    def NoSuchVersionException(String resource) {
        this();
        this.resource = resource;
    }
}

class NotImplementedException extends S3ClientException {
    def NotImplementedException() {
        super("NotImplemented", "A header you provided implies functionality that is not implemented.", HttpResponseStatus.NOT_IMPLEMENTED);
    }

    def NotImplementedException(String resource) {
        this();
        this.resource = resource;
    }
}

class NotSignedUpException extends S3ClientException {
    def NotSignedUpException() {
        super("NotSignedUp", "Your account is not signed up for this service. You must sign up before you can use it.", HttpResponseStatus.FORBIDDEN);
    }

    def NotSignedUpException(String resource) {
        this();
        this.resource = resource;
    }
}

class NotSuchBucketPolicyException extends S3ClientException {
    def NotSuchBucketPolicyException() {
        super("NotSuchBucketPolicy", "The specified bucket does not have a bucket policy.", HttpResponseStatus.NOT_FOUND);
    }

    def NotSuchBucketPolicyException(String resource) {
        this();
        this.resource = resource;
    }
}

class OperationAbortedException extends S3ClientException {
    def OperationAbortedException() {
        super("OperationAborted", "A conflicting conditional operation is currently in progress against this resource. Please try again.", HttpResponseStatus.CONFLICT);
    }

    def OperationAbortedException(String resource) {
        this();
        this.resource = resource;
    }
}

class PermanentRedirectException extends S3ClientException {
    def PermanentRedirectException() {
        super("PermanentRedirect", "The bucket you are attempting to access must be addressed using the specified endpoint. Please send all future requests to this endpoint.", HttpResponseStatus.MOVED_PERMANENTLY);
    }

    def PermanentRedirectException(String resource) {
        this();
        this.resource = resource;
    }
}

class PreconditionFailedException extends S3ClientException {
    def PreconditionFailedException() {
        super("PreconditionFailed", "At least one of the preconditions you specified did not hold.", HttpResponseStatus.PRECONDITION_FAILED);
    }

    def PreconditionFailedException(String resource) {
        this();
        this.resource = resource;
    }
}

class RedirectException extends S3ClientException {
    def RedirectException() {
        super("Redirect", "Temporary redirect.", HttpResponseStatus.TEMPORARY_REDIRECT);
    }

    def RedirectException(String resource) {
        this();
        this.resource = resource;
    }
}

class RestoreAlreadyInProgressException extends S3ClientException {
    def RestoreAlreadyInProgressException() {
        super("RestoreAlreadyInProgress", "Object restore is already in progress.", HttpResponseStatus.CONFLICT);
    }

    def RestoreAlreadyInProgressException(String resource) {
        this();
        this.resource = resource;
    }
}

class RequestIsNotMultiPartContentException extends S3ClientException {
    def RequestIsNotMultiPartContentException() {
        super("RequestIsNotMultiPartContent", "Bucket POST must be of the enclosure-type multipart/form-data.", HttpResponseStatus.BAD_REQUEST);
    }

    def RequestIsNotMultiPartContentException(String resource) {
        this();
        this.resource = resource;
    }
}

class RequestTimeoutException extends S3ClientException {
    def RequestTimeoutException() {
        super("RequestTimeout", "Your socket connection to the server was not read from or written to within the timeout period.", HttpResponseStatus.BAD_REQUEST);
    }

    def RequestTimeoutException(String resource) {
        this();
        this.resource = resource;
    }
}

class RequestTimeTooSkewedException extends S3ClientException {
    def RequestTimeTooSkewedException() {
        super("RequestTimeTooSkewed", "The difference between the request time and the server's time is too large.", HttpResponseStatus.FORBIDDEN);
    }

    def RequestTimeTooSkewedException(String resource) {
        this();
        this.resource = resource;
    }
}

class RequestTorrentOfBucketErrorException extends S3ClientException {
    def RequestTorrentOfBucketErrorException() {
        super("RequestTorrentOfBucketError", "Requesting the torrent file of a bucket is not permitted.", HttpResponseStatus.BAD_REQUEST);
    }

    def RequestTorrentOfBucketErrorException(String resource) {
        this();
        this.resource = resource;
    }
}

class SignatureDoesNotMatchException extends S3ClientException {
    def SignatureDoesNotMatchException() {
        super("SignatureDoesNotMatch", "The request signature we calculated does not match the signature you provided.", HttpResponseStatus.FORBIDDEN);
    }

    def SignatureDoesNotMatchException(String resource) {
        this();
        this.resource = resource;
    }
}

class ServiceUnavailableException extends S3ServerException {
    def ServiceUnavailableException() {
        super("ServiceUnavailable", "Please reduce your request rate.", HttpResponseStatus.SERVICE_UNAVAILABLE);
    }

    def ServiceUnavailableException(String resource) {
        this();
        this.resource = resource;
    }
}

class SlowDownException extends S3ServerException {
    def SlowDownException() {
        super("SlowDown", "Please reduce your request rate.", HttpResponseStatus.SERVICE_UNAVAILABLE);
    }

    def SlowDownException(String resource) {
        this();
        this.resource = resource;
    }
}

class TemporaryRedirectException extends S3ClientException {
    def TemporaryRedirectException() {
        super("TemporaryRedirect", "You are being redirected to the bucket while DNS updates.", HttpResponseStatus.TEMPORARY_REDIRECT);
    }

    def TemporaryRedirectException(String resource) {
        this();
        this.resource = resource;
    }
}

class TokenRefreshRequiredException extends S3ClientException {
    def TokenRefreshRequiredException() {
        super("TokenRefreshRequired", "The provided token must be refreshed.", HttpResponseStatus.BAD_REQUEST);
    }

    def TokenRefreshRequiredException(String resource) {
        this();
        this.resource = resource;
    }
}

class TooManyBucketsException extends S3ClientException {
    def TooManyBucketsException() {
        super("TooManyBuckets", "You have attempted to create more buckets than allowed.", HttpResponseStatus.BAD_REQUEST);
    }

    def TooManyBucketsException(String resource) {
        this();
        this.resource = resource;
    }
}

class UnexpectedContentException extends S3ClientException {
    def UnexpectedContentException() {
        super("UnexpectedContent", "This request does not support content.", HttpResponseStatus.BAD_REQUEST);
    }

    def UnexpectedContentException(String resource) {
        this();
        this.resource = resource;
    }
}

class UnresolvableGrantByEmailAddressException extends S3ClientException {
    def UnresolvableGrantByEmailAddressException() {
        super("UnresolvableGrantByEmailAddress", "The e-mail address you provided does not match any account on record.", HttpResponseStatus.BAD_REQUEST);
    }

    def UnresolvableGrantByEmailAddressException(String resource) {
        this();
        this.resource = resource;
    }
}

class UserKeyMustBeSpecifiedException extends S3ClientException {
    def UserKeyMustBeSpecifiedException() {
        super("UserKeyMustBeSpecified", "The bucket POST must contain the specified field name. If it is specified, please check the order of the fields.", HttpResponseStatus.BAD_REQUEST);
    }

    def UserKeyMustBeSpecifiedException(String resource) {
        this();
        this.resource = resource;
    }
}