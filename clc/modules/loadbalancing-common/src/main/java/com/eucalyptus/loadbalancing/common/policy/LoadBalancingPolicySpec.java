/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.common.policy;

/**
 *
 */
public interface LoadBalancingPolicySpec {

  String VENDOR_LOADBALANCING = "elasticloadbalancing";

  //Load Balancing actions, based on API Reference (API Version 2012-06-01)
  String LOADBALANCING_APPLYSECURITYGROUPSTOLOADBALANCER = "applysecuritygroupstoloadbalancer";
  String LOADBALANCING_ATTACHLOADBALANCERTOSUBNETS = "attachLoadbalancertosubnets";
  String LOADBALANCING_CONFIGUREHEALTHCHECK = "configurehealthcheck";
  String LOADBALANCING_CREATEAPPCOOKIESTICKINESSPOLICY = "createappcookiestickinesspolicy";
  String LOADBALANCING_CREATELBCOOKIESTICKINESSPOLICY = "createlbcookiestickinesspolicy";
  String LOADBALANCING_CREATELOADBALANCER = "createloadbalancer";
  String LOADBALANCING_CREATELOADBALANCERLISTENERS = "createloadbalancerlisteners";
  String LOADBALANCING_CREATELOADBALANCERPOLICY = "createloadbalancerpolicy";
  String LOADBALANCING_DELETELOADBALANCER = "deleteloadbalancer";
  String LOADBALANCING_DELETELOADBALANCERLISTENERS = "deleteloadbalancerlisteners";
  String LOADBALANCING_DELETELOADBALANCERPOLICY = "deleteloadbalancerpolicy";
  String LOADBALANCING_DEREGISTERINSTANCESFROMLOADBALANCER = "deregisterinstancesfromloadbalancer";
  String LOADBALANCING_DESCRIBEINSTANCEHEALTH = "describeinstancehealth";
  String LOADBALANCING_DESCRIBELOADBALANCERPOLICIES = "describeloadbalancerpolicies";
  String LOADBALANCING_DESCRIBELOADBALANCERPOLICYTYPES = "describeloadbalancerpolicytypes";
  String LOADBALANCING_DESCRIBELOADBALANCERS = "describeloadbalancers";
  String LOADBALANCING_DETACHLOABBALANCERFROMSUBNETS = "detachloadbalancerfromsubnets";
  String LOADBALANCING_DISABLEAVAILABILITYZONESFORLOADBALANCER = "disableavailabilityzonesforloadbalancer";
  String LOADBALANCING_ENABLEAVAILABILITYZONESFORLOADBALANCER = "enableavailabilityzonesforloadbalancer";
  String LOADBALANCING_REGISTERINSTANCESWITHLOADBALANCER = "registerinstanceswithloadbalancer";
  String LOADBALANCING_SETLOADBALANCERLISTENERSSLCERTIFICATE = "setloadbalancerlistenersslcertificate";
  String LOADBALANCING_SETLOADBALANCERPOLICIESFORBACKENDSERVER = "setloadbalancerpoliciesforbackendserver";
  String LOADBALANCING_SETLOADBALANCERPOLICIESOFLISTENER = "setloadbalancerpoliciesoflistener";

  // Non-AWS, Euca-specific ELB operations
  String LOADBALANCING_DESCRIBELOADBALANCERSBYSERVO = "describeloadbalancersbyservo";
  String LOADBALANCING_PUTSERVOSTATES = "putservostates";

}
