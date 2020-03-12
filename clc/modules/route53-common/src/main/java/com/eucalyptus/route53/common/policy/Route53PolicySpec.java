/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.policy;

/**
 *
 */
public interface Route53PolicySpec {

  // Vendor
  String VENDOR_ROUTE53 = "route53";

  // Actions
  String ROUTE53_ASSOCIATEVPCWITHHOSTEDZONE = "associatevpcwithhostedzone";
  String ROUTE53_CHANGERESOURCERECORDSETS = "changeresourcerecordsets";
  String ROUTE53_CHANGETAGSFORRESOURCE = "changetagsforresource";
  String ROUTE53_CREATEHEALTHCHECK = "createhealthcheck";
  String ROUTE53_CREATEHOSTEDZONE = "createhostedzone";
  String ROUTE53_CREATEQUERYLOGGINGCONFIG = "createqueryloggingconfig";
  String ROUTE53_CREATEREUSABLEDELEGATIONSET = "createreusabledelegationset";
  String ROUTE53_CREATETRAFFICPOLICY = "createtrafficpolicy";
  String ROUTE53_CREATETRAFFICPOLICYINSTANCE = "createtrafficpolicyinstance";
  String ROUTE53_CREATETRAFFICPOLICYVERSION = "createtrafficpolicyversion";
  String ROUTE53_CREATEVPCASSOCIATIONAUTHORIZATION = "createvpcassociationauthorization";
  String ROUTE53_DELETEHEALTHCHECK = "deletehealthcheck";
  String ROUTE53_DELETEHOSTEDZONE = "deletehostedzone";
  String ROUTE53_DELETEQUERYLOGGINGCONFIG = "deletequeryloggingconfig";
  String ROUTE53_DELETEREUSABLEDELEGATIONSET = "deletereusabledelegationset";
  String ROUTE53_DELETETRAFFICPOLICY = "deletetrafficpolicy";
  String ROUTE53_DELETETRAFFICPOLICYINSTANCE = "deletetrafficpolicyinstance";
  String ROUTE53_DELETEVPCASSOCIATIONAUTHORIZATION = "deletevpcassociationauthorization";
  String ROUTE53_DISASSOCIATEVPCFROMHOSTEDZONE = "disassociatevpcfromhostedzone";
  String ROUTE53_GETACCOUNTLIMIT = "getaccountlimit";
  String ROUTE53_GETCHANGE = "getchange";
  String ROUTE53_GETCHECKERIPRANGES = "getcheckeripranges";
  String ROUTE53_GETGEOLOCATION = "getgeolocation";
  String ROUTE53_GETHEALTHCHECK = "gethealthcheck";
  String ROUTE53_GETHEALTHCHECKCOUNT = "gethealthcheckcount";
  String ROUTE53_GETHEALTHCHECKLASTFAILUREREASON = "gethealthchecklastfailurereason";
  String ROUTE53_GETHEALTHCHECKSTATUS = "gethealthcheckstatus";
  String ROUTE53_GETHOSTEDZONE = "gethostedzone";
  String ROUTE53_GETHOSTEDZONECOUNT = "gethostedzonecount";
  String ROUTE53_GETHOSTEDZONELIMIT = "gethostedzonelimit";
  String ROUTE53_GETQUERYLOGGINGCONFIG = "getqueryloggingconfig";
  String ROUTE53_GETREUSABLEDELEGATIONSET = "getreusabledelegationset";
  String ROUTE53_GETREUSABLEDELEGATIONSETLIMIT = "getreusabledelegationsetlimit";
  String ROUTE53_GETTRAFFICPOLICY = "gettrafficpolicy";
  String ROUTE53_GETTRAFFICPOLICYINSTANCE = "gettrafficpolicyinstance";
  String ROUTE53_GETTRAFFICPOLICYINSTANCECOUNT = "gettrafficpolicyinstancecount";
  String ROUTE53_LISTGEOLOCATIONS = "listgeolocations";
  String ROUTE53_LISTHEALTHCHECKS = "listhealthchecks";
  String ROUTE53_LISTHOSTEDZONES = "listhostedzones";
  String ROUTE53_LISTHOSTEDZONESBYNAME = "listhostedzonesbyname";
  String ROUTE53_LISTQUERYLOGGINGCONFIGS = "listqueryloggingconfigs";
  String ROUTE53_LISTRESOURCERECORDSETS = "listresourcerecordsets";
  String ROUTE53_LISTREUSABLEDELEGATIONSETS = "listreusabledelegationsets";
  String ROUTE53_LISTTAGSFORRESOURCE = "listtagsforresource";
  String ROUTE53_LISTTAGSFORRESOURCES = "listtagsforresources";
  String ROUTE53_LISTTRAFFICPOLICIES = "listtrafficpolicies";
  String ROUTE53_LISTTRAFFICPOLICYINSTANCES = "listtrafficpolicyinstances";
  String ROUTE53_LISTTRAFFICPOLICYINSTANCESBYHOSTEDZONE = "listtrafficpolicyinstancesbyhostedzone";
  String ROUTE53_LISTTRAFFICPOLICYINSTANCESBYPOLICY = "listtrafficpolicyinstancesbypolicy";
  String ROUTE53_LISTTRAFFICPOLICYVERSIONS = "listtrafficpolicyversions";
  String ROUTE53_LISTVPCASSOCIATIONAUTHORIZATIONS = "listvpcassociationauthorizations";
  String ROUTE53_TESTDNSANSWER = "testdnsanswer";
  String ROUTE53_UPDATEHEALTHCHECK = "updatehealthcheck";
  String ROUTE53_UPDATEHOSTEDZONECOMMENT = "updatehostedzonecomment";
  String ROUTE53_UPDATETRAFFICPOLICYCOMMENT = "updatetrafficpolicycomment";
  String ROUTE53_UPDATETRAFFICPOLICYINSTANCE = "updatetrafficpolicyinstance";

}
