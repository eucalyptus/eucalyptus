/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.objectstorage.policy;

/**
 *
 */
public interface S3PolicySpec {

  // S3 vendor
  String VENDOR_S3 = "s3";

  // S3 resource types
  String S3_RESOURCE_BUCKET = "bucket";
  String S3_RESOURCE_OBJECT = "object";

  // S3 actions
  String S3_ABORTMULTIPARTUPLOAD = "abortmultipartupload";
  String S3_CREATEBUCKET = "createbucket";
  String S3_DELETEBUCKET = "deletebucket";
  String S3_DELETEBUCKETPOLICY = "deletebucketpolicy";
  String S3_DELETEBUCKETWEBSITE = "deletebucketwebsite";
  String S3_DELETEOBJECT = "deleteobject";
  String S3_DELETEOBJECTVERSION = "deleteobjectversion";
  String S3_GETBUCKETACCELERATECONFIGURATION = "getbucketaccelerateconfiguration";
  String S3_GETBUCKETACL = "getbucketacl";
  String S3_GETBUCKETCORS = "getbucketcors";
  String S3_GETBUCKETLOCATION = "getbucketlocation";
  String S3_GETBUCKETLOGGING = "getbucketlogging";
  String S3_GETBUCKETNOTIFICATION = "getbucketnotification";
  String S3_GETBUCKETNOTIFICATIONCONFIGURATION = "getbucketnotificationconfiguration";
  String S3_GETBUCKETPOLICY = "getbucketpolicy";
  String S3_GETBUCKETREQUESTPAYMENT = "getbucketrequestpayment";
  String S3_GETBUCKETTAGGING = "getbuckettagging";
  String S3_GETBUCKETVERSIONING = "getbucketversioning";
  String S3_GETBUCKETWEBSITE = "getbucketwebsite";
  String S3_GETLIFECYCLECONFIGURATION = "getlifecycleconfiguration";
  String S3_GETOBJECTACL = "getobjectacl";
  String S3_GETOBJECT = "getobject";
  String S3_GETOBJECTTORRENT = "getobjecttorrent";
  String S3_GETOBJECTVERSIONACL = "getobjectversionacl";
  String S3_GETOBJECTVERSION = "getobjectversion";
  String S3_GETOBJECTVERSIONTORRENT = "getobjectversiontorrent";
  String S3_HEADOBJECT = "headobject";
  String S3_LISTALLMYBUCKETS = "listallmybuckets";
  String S3_LISTBUCKET = "listbucket";
  String S3_LISTBUCKETMULTIPARTUPLOADS = "listbucketmultipartuploads";
  String S3_LISTBUCKETVERSIONS = "listbucketversions";
  String S3_LISTMULTIPARTUPLOADPARTS = "listmultipartuploadparts";
  String S3_PUTBUCKETACL = "putbucketacl";
  String S3_PUTBUCKETCORS = "putbucketcors";
  String S3_PUTBUCKETLOGGING = "putbucketlogging";
  String S3_PUTBUCKETNOTIFICATION = "putbucketnotification";
  String S3_PUTBUCKETPOLICY = "putbucketpolicy";
  String S3_PUTBUCKETREQUESTPAYMENT = "putbucketrequestpayment";
  String S3_PUTBUCKETTAGGING = "putbuckettagging";
  String S3_PUTBUCKETVERSIONING = "putbucketversioning";
  String S3_PUTBUCKETWEBSITE = "putbucketwebsite";
  String S3_PUTLIFECYCLECONFIGURATION = "putlifecycleconfiguration";
  String S3_PUTOBJECTACL = "putobjectacl";
  String S3_PUTOBJECT = "putobject";
  String S3_PUTOBJECTVERSIONACL = "putobjectversionacl";
  String S3_RESTOREOBJECT = "restoreobject";

}
