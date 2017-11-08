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
