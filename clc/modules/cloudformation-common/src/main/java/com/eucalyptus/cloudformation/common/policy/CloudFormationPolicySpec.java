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
package com.eucalyptus.cloudformation.common.policy;

/**
 *
 */
public interface CloudFormationPolicySpec {

  String VENDOR_CLOUDFORMATION = "cloudformation";

  //Cloud Formation actions, based on API Reference (API Version 2010-05-15)
  String CLOUDFORMATION_CANCELUPDATESTACK = "cancelupdatestack";
  String CLOUDFORMATION_CREATESTACK = "createstack";
  String CLOUDFORMATION_DELETESTACK = "deletestack";
  String CLOUDFORMATION_DESCRIBESTACKEVENTS = "describestackevents";
  String CLOUDFORMATION_DESCRIBESTACKRESOURCE = "describestackresource";
  String CLOUDFORMATION_DESCRIBESTACKRESOURCES = "describestackresources";
  String CLOUDFORMATION_DESCRIBESTACKS = "describestacks";
  String CLOUDFORMATION_ESTIMATETEMPLATECOST = "estimatetemplatecost";
  String CLOUDFORMATION_GETSTACKPOLICY = "getstackpolicy";
  String CLOUDFORMATION_GETTEMPLATE = "gettemplate";
  String CLOUDFORMATION_GETTEMPLATESUMMARY = "gettemplatesummary";
  String CLOUDFORMATION_LISTSTACKRESOURCES = "liststackresources";
  String CLOUDFORMATION_LISTSTACKS = "liststacks";
  String CLOUDFORMATION_SETSTACKPOLICY = "setstackpolicy";
  String CLOUDFORMATION_UPDATESTACK = "updatestack";
  String CLOUDFORMATION_VALIDATETEMPLATE = "validatetemplate";  
}
