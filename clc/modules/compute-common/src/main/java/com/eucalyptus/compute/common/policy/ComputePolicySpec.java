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
package com.eucalyptus.compute.common.policy;

import com.eucalyptus.auth.policy.PolicySpec;

/**
 *
 */
public interface ComputePolicySpec {

  String VENDOR_EC2 = PolicySpec.VENDOR_EC2;

  String EC2_ACCEPTVPCPEERINGCONNECTION = "acceptvpcpeeringconnection";
  String EC2_ASSOCIATEIAMINSTANCEPROFILE = "associateiaminstanceprofile";
  String EC2_ATTACHCLASSICLINKVPC = "attachclassiclinkvpc";
  String EC2_ATTACHVOLUME = PolicySpec.EC2_ATTACHVOLUME;
  String EC2_AUTHORIZESECURITYGROUPEGRESS = PolicySpec.EC2_AUTHORIZESECURITYGROUPEGRESS;
  String EC2_AUTHORIZESECURITYGROUPINGRESS = PolicySpec.EC2_AUTHORIZESECURITYGROUPINGRESS;
  String EC2_CREATETAGS = PolicySpec.EC2_CREATETAGS;
  String EC2_CREATEVOLUME = PolicySpec.EC2_CREATEVOLUME;
  String EC2_CREATEVPCPEERINGCONNECTION = "createvpcpeeringconnection";
  String EC2_DELETEDHCPOPTIONS = PolicySpec.EC2_DELETEDHCPOPTIONS;
  String EC2_DELETEINTERNETGATEWAY = PolicySpec.EC2_DELETEINTERNETGATEWAY;
  String EC2_DELETENETWORKACLENTRY = PolicySpec.EC2_DELETENETWORKACLENTRY;
  String EC2_DELETENETWORKACL = PolicySpec.EC2_DELETENETWORKACL;
  String EC2_DELETEROUTE = PolicySpec.EC2_DELETEROUTE;
  String EC2_DELETEROUTETABLE  = PolicySpec.EC2_DELETEROUTETABLE;
  String EC2_DELETESECURITYGROUP = PolicySpec.EC2_DELETESECURITYGROUP;
  String EC2_DELETETAGS = PolicySpec.EC2_DELETETAGS;
  String EC2_DELETEVOLUME = PolicySpec.EC2_DELETEVOLUME;
  String EC2_DELETEVPCPEERINGCONNECTION = "deletevpcpeeringconnection";
  String EC2_DETACHCLASSICLINKVPC = "detachclassiclinkvpc";
  String EC2_DETACHVOLUME = PolicySpec.EC2_DETACHVOLUME;
  String EC2_DISABLEVPCCLASSICLINK = "disablevpcclassiclink";
  String EC2_DISASSOCIATEIAMINSTANCEPROFILE = "disassociateiaminstanceprofile";
  String EC2_ENABLEVPCCLASSICLINK = "enablevpcclassiclink";
  String EC2_GETCONSOLESCREENSHOT = "getconsolescreenshot";
  String EC2_REBOOTINSTANCES = PolicySpec.EC2_REBOOTINSTANCES;
  String EC2_REJECTVPCPEERINGCONNECTION = "rejectvpcpeeringconnection";
  String EC2_REPLACEIAMINSTANCEPROFILEASSOCIATION = "replaceiaminstanceprofileassociation";
  String EC2_REVOKESECURITYGROUPEGRESS = PolicySpec.EC2_REVOKESECURITYGROUPEGRESS;
  String EC2_REVOKESECURITYGROUPINGRESS = PolicySpec.EC2_REVOKESECURITYGROUPINGRESS;
  String EC2_RUNINSTANCES = PolicySpec.EC2_RUNINSTANCES;
  String EC2_STARTINSTANCES = PolicySpec.EC2_STARTINSTANCES;
  String EC2_STOPINSTANCES = PolicySpec.EC2_STOPINSTANCES;
  String EC2_TERMINATEINSTANCES = PolicySpec.EC2_TERMINATEINSTANCES;

}
