/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
