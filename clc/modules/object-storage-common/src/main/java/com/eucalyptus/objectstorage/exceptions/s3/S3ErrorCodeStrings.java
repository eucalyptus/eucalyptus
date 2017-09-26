/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.objectstorage.exceptions.s3;

/**
 * S3 Error codes. See http://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html
 */
public interface S3ErrorCodeStrings {

  String AccessDenied = "AccessDenied";
  String AccessForbidden = "AccessForbidden";
  String AccountProblem = "AccountProblem";
  String AmbiguousGrantByEmailAddress = "AmbiguousGrantByEmailAddress";
  String BadDigest = "BadDigest";
  String BadRequest = "BadRequest";
  String BucketAlreadyExists = "BucketAlreadyExists";
  String BucketAlreadyOwnedByYou = "BucketAlreadyOwnedByYou";
  String BucketNotEmpty = "BucketNotEmpty";
  String CredentialsNotSupported = "CredentialsNotSupported";
  String CrossLocationLoggingProhibited = "CrossLocationLoggingProhibited";
  String EntityTooSmall = "EntityTooSmall";
  String EntityTooLarge = "EntityTooLarge";
  String ExpiredToken = "ExpiredToken";
  String IllegalVersioningConfigurationException = "IllegalVersioningConfigurationException";
  String IncompleteBody = "IncompleteBody";
  String IncorrectNumberOfFilesInPostRequest = "IncorrectNumberOfFilesInPostRequest";
  String InlineDataTooLarge = "InlineDataTooLarge";
  String InternalError = "InternalError";
  String InvalidAccessKeyId = "InvalidAccessKeyId";
  String InvalidAddressingHeader = "InvalidAddressingHeader";
  String InvalidArgument = "InvalidArgument";
  String InvalidBucketName = "InvalidBucketName";
  String InvalidBucketState = "InvalidBucketState";
  String InvalidDigest = "InvalidDigest";
  String InvalidLocationConstraint = "InvalidLocationConstraint";
  String InvalidObjectState = "InvalidObjectState";
  String InvalidPart = "InvalidPart";
  String InvalidPartOrder = "InvalidPartOrder";
  String InvalidPayer = "InvalidPayer";
  String InvalidPolicyDocument = "InvalidPolicyDocument";
  String InvalidRange = "InvalidRange";
  String InvalidRequest = "InvalidRequest";
  String InvalidSecurity = "InvalidSecurity";
  String InvalidSOAPRequest = "InvalidSOAPRequest";
  String InvalidStorageClass = "InvalidStorageClass";
  String InvalidTagError = "InvalidTagError";
  String InvalidTargetBucketForLogging = "InvalidTargetBucketForLogging";
  String InvalidToken = "InvalidToken";
  String InvalidURI = "InvalidURI";
  String KeyTooLong = "KeyTooLong";
  String MalformedACLError = "MalformedACLError";
  String MalformedPOSTRequest = "MalformedPOSTRequest";
  String MalformedXML = "MalformedXML";
  String MaxMessageLengthExceeded = "MaxMessageLengthExceeded";
  String MaxPostPreDataLengthExceededError = "MaxPostPreDataLengthExceededError";
  String MetadataTooLarge = "MetadataTooLarge";
  String MethodNotAllowed = "MethodNotAllowed";
  String MissingAttachment = "MissingAttachment";
  String MissingContentLength = "MissingContentLength";
  String MissingRequestBodyError = "MissingRequestBodyError";
  String MissingSecurityElement = "MissingSecurityElement";
  String MissingSecurityHeader = "MissingSecurityHeader";
  String NoLoggingStatusForKey = "NoLoggingStatusForKey";
  String NoSuchBucket = "NoSuchBucket";
  String NoSuchCORSConfiguration = "NoSuchCORSConfiguration";
  String NoSuchKey = "NoSuchKey";
  String NoSuchLifecycleConfiguration = "NoSuchLifecycleConfiguration";
  String NoSuchUpload = "NoSuchUpload";
  String NoSuchVersion = "NoSuchVersion";
  String NotImplemented = "NotImplemented";
  String NotSignedUp = "NotSignedUp";
  String NoSuchBucketPolicy = "NoSuchBucketPolicy";
  String NoSuchTagSet = "NoSuchTagSet";
  String OperationAborted = "OperationAborted";
  String PermanentRedirect = "PermanentRedirect";
  String PreconditionFailed = "PreconditionFailed";
  String Redirect = "Redirect";
  String RestoreAlreadyInProgress = "RestoreAlreadyInProgress";
  String RequestIsNotMultiPartContent = "RequestIsNotMultiPartContent";
  String RequestTimeout = "RequestTimeout";
  String RequestTimeTooSkewed = "RequestTimeTooSkewed";
  String RequestTorrentOfBucketError = "RequestTorrentOfBucketError";
  String SignatureDoesNotMatch = "SignatureDoesNotMatch";
  String ServiceUnavailable = "ServiceUnavailable";
  String SlowDown = "SlowDown";
  String TemporaryRedirect = "TemporaryRedirect";
  String TokenRefreshRequired = "TokenRefreshRequired";
  String TooManyBuckets = "TooManyBuckets";
  String UnexpectedContent = "UnexpectedContent";
  String UnresolvableGrantByEmailAddress = "UnresolvableGrantByEmailAddress";
  String UserKeyMustBeSpecified = "UserKeyMustBeSpecified";
  String XAmzContentSHA256Mismatch = "XAmzContentSHA256Mismatch";
}
