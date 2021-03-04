/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.policy;

/**
 *
 */
public interface Loadbalancingv2PolicySpec {

  // Vendor
  String VENDOR_LOADBALANCINGV2 = "elasticloadbalancing";

  // Actions
  String LOADBALANCINGV2_ADDLISTENERCERTIFICATES = "addlistenercertificates";
  String LOADBALANCINGV2_ADDTAGS = "addtags";
  String LOADBALANCINGV2_CREATELISTENER = "createlistener";
  String LOADBALANCINGV2_CREATELOADBALANCER = "createloadbalancer";
  String LOADBALANCINGV2_CREATERULE = "createrule";
  String LOADBALANCINGV2_CREATETARGETGROUP = "createtargetgroup";
  String LOADBALANCINGV2_DELETELISTENER = "deletelistener";
  String LOADBALANCINGV2_DELETELOADBALANCER = "deleteloadbalancer";
  String LOADBALANCINGV2_DELETERULE = "deleterule";
  String LOADBALANCINGV2_DELETETARGETGROUP = "deletetargetgroup";
  String LOADBALANCINGV2_DEREGISTERTARGETS = "deregistertargets";
  String LOADBALANCINGV2_DESCRIBEACCOUNTLIMITS = "describeaccountlimits";
  String LOADBALANCINGV2_DESCRIBELISTENERCERTIFICATES = "describelistenercertificates";
  String LOADBALANCINGV2_DESCRIBELISTENERS = "describelisteners";
  String LOADBALANCINGV2_DESCRIBELOADBALANCERATTRIBUTES = "describeloadbalancerattributes";
  String LOADBALANCINGV2_DESCRIBELOADBALANCERS = "describeloadbalancers";
  String LOADBALANCINGV2_DESCRIBERULES = "describerules";
  String LOADBALANCINGV2_DESCRIBESSLPOLICIES = "describesslpolicies";
  String LOADBALANCINGV2_DESCRIBETAGS = "describetags";
  String LOADBALANCINGV2_DESCRIBETARGETGROUPATTRIBUTES = "describetargetgroupattributes";
  String LOADBALANCINGV2_DESCRIBETARGETGROUPS = "describetargetgroups";
  String LOADBALANCINGV2_DESCRIBETARGETHEALTH = "describetargethealth";
  String LOADBALANCINGV2_MODIFYLISTENER = "modifylistener";
  String LOADBALANCINGV2_MODIFYLOADBALANCERATTRIBUTES = "modifyloadbalancerattributes";
  String LOADBALANCINGV2_MODIFYRULE = "modifyrule";
  String LOADBALANCINGV2_MODIFYTARGETGROUP = "modifytargetgroup";
  String LOADBALANCINGV2_MODIFYTARGETGROUPATTRIBUTES = "modifytargetgroupattributes";
  String LOADBALANCINGV2_REGISTERTARGETS = "registertargets";
  String LOADBALANCINGV2_REMOVELISTENERCERTIFICATES = "removelistenercertificates";
  String LOADBALANCINGV2_REMOVETAGS = "removetags";
  String LOADBALANCINGV2_SETIPADDRESSTYPE = "setipaddresstype";
  String LOADBALANCINGV2_SETRULEPRIORITIES = "setrulepriorities";
  String LOADBALANCINGV2_SETSECURITYGROUPS = "setsecuritygroups";
  String LOADBALANCINGV2_SETSUBNETS = "setsubnets";

}
