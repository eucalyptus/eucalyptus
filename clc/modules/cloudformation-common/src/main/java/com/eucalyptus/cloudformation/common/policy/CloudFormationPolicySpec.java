/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.policy;

/**
 *
 */
public interface CloudFormationPolicySpec {

  // Vendor
  String VENDOR_CLOUDFORMATION = "cloudformation";

  // Actions
  String CLOUDFORMATION_CANCELUPDATESTACK = "cancelupdatestack";
  String CLOUDFORMATION_CONTINUEUPDATEROLLBACK = "continueupdaterollback";
  String CLOUDFORMATION_CREATECHANGESET = "createchangeset";
  String CLOUDFORMATION_CREATESTACK = "createstack";
  String CLOUDFORMATION_CREATESTACKINSTANCES = "createstackinstances";
  String CLOUDFORMATION_CREATESTACKSET = "createstackset";
  String CLOUDFORMATION_DELETECHANGESET = "deletechangeset";
  String CLOUDFORMATION_DELETESTACK = "deletestack";
  String CLOUDFORMATION_DELETESTACKINSTANCES = "deletestackinstances";
  String CLOUDFORMATION_DELETESTACKSET = "deletestackset";
  String CLOUDFORMATION_DEREGISTERTYPE = "deregistertype";
  String CLOUDFORMATION_DESCRIBEACCOUNTLIMITS = "describeaccountlimits";
  String CLOUDFORMATION_DESCRIBECHANGESET = "describechangeset";
  String CLOUDFORMATION_DESCRIBESTACKDRIFTDETECTIONSTATUS = "describestackdriftdetectionstatus";
  String CLOUDFORMATION_DESCRIBESTACKEVENTS = "describestackevents";
  String CLOUDFORMATION_DESCRIBESTACKINSTANCE = "describestackinstance";
  String CLOUDFORMATION_DESCRIBESTACKRESOURCE = "describestackresource";
  String CLOUDFORMATION_DESCRIBESTACKRESOURCEDRIFTS = "describestackresourcedrifts";
  String CLOUDFORMATION_DESCRIBESTACKRESOURCES = "describestackresources";
  String CLOUDFORMATION_DESCRIBESTACKSET = "describestackset";
  String CLOUDFORMATION_DESCRIBESTACKSETOPERATION = "describestacksetoperation";
  String CLOUDFORMATION_DESCRIBESTACKS = "describestacks";
  String CLOUDFORMATION_DESCRIBETYPE = "describetype";
  String CLOUDFORMATION_DESCRIBETYPEREGISTRATION = "describetyperegistration";
  String CLOUDFORMATION_DETECTSTACKDRIFT = "detectstackdrift";
  String CLOUDFORMATION_DETECTSTACKRESOURCEDRIFT = "detectstackresourcedrift";
  String CLOUDFORMATION_DETECTSTACKSETDRIFT = "detectstacksetdrift";
  String CLOUDFORMATION_ESTIMATETEMPLATECOST = "estimatetemplatecost";
  String CLOUDFORMATION_EXECUTECHANGESET = "executechangeset";
  String CLOUDFORMATION_GETSTACKPOLICY = "getstackpolicy";
  String CLOUDFORMATION_GETTEMPLATE = "gettemplate";
  String CLOUDFORMATION_GETTEMPLATESUMMARY = "gettemplatesummary";
  String CLOUDFORMATION_LISTCHANGESETS = "listchangesets";
  String CLOUDFORMATION_LISTEXPORTS = "listexports";
  String CLOUDFORMATION_LISTIMPORTS = "listimports";
  String CLOUDFORMATION_LISTSTACKINSTANCES = "liststackinstances";
  String CLOUDFORMATION_LISTSTACKRESOURCES = "liststackresources";
  String CLOUDFORMATION_LISTSTACKSETOPERATIONRESULTS = "liststacksetoperationresults";
  String CLOUDFORMATION_LISTSTACKSETOPERATIONS = "liststacksetoperations";
  String CLOUDFORMATION_LISTSTACKSETS = "liststacksets";
  String CLOUDFORMATION_LISTSTACKS = "liststacks";
  String CLOUDFORMATION_LISTTYPEREGISTRATIONS = "listtyperegistrations";
  String CLOUDFORMATION_LISTTYPEVERSIONS = "listtypeversions";
  String CLOUDFORMATION_LISTTYPES = "listtypes";
  String CLOUDFORMATION_RECORDHANDLERPROGRESS = "recordhandlerprogress";
  String CLOUDFORMATION_REGISTERTYPE = "registertype";
  String CLOUDFORMATION_SETSTACKPOLICY = "setstackpolicy";
  String CLOUDFORMATION_SETTYPEDEFAULTVERSION = "settypedefaultversion";
  String CLOUDFORMATION_SIGNALRESOURCE = "signalresource";
  String CLOUDFORMATION_STOPSTACKSETOPERATION = "stopstacksetoperation";
  String CLOUDFORMATION_UPDATESTACK = "updatestack";
  String CLOUDFORMATION_UPDATESTACKINSTANCES = "updatestackinstances";
  String CLOUDFORMATION_UPDATESTACKSET = "updatestackset";
  String CLOUDFORMATION_UPDATETERMINATIONPROTECTION = "updateterminationprotection";
  String CLOUDFORMATION_VALIDATETEMPLATE = "validatetemplate";

}
