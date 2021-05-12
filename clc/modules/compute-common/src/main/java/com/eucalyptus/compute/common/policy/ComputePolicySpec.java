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

import java.util.Set;
import com.eucalyptus.auth.policy.PolicySpec;
import com.google.common.collect.ImmutableSet;

/**
 *
 */
public interface ComputePolicySpec {

  String VENDOR_EC2 = PolicySpec.VENDOR_EC2;

  String EC2_ACCEPTVPCPEERINGCONNECTION = "acceptvpcpeeringconnection";
  String EC2_ALLOCATEADDRESS = "allocateaddress";
  String EC2_ASSOCIATEIAMINSTANCEPROFILE = "associateiaminstanceprofile";
  String EC2_ATTACHCLASSICLINKVPC = "attachclassiclinkvpc";
  String EC2_ATTACHVOLUME = "attachvolume";
  String EC2_AUTHORIZESECURITYGROUPEGRESS = "authorizesecuritygroupegress";
  String EC2_AUTHORIZESECURITYGROUPINGRESS = "authorizesecuritygroupingress";
  String EC2_CANCELCONVERSIONTASK = "cancelconversiontask";
  String EC2_CREATEINTERNETGATEWAY = "createinternetgateway";
  String EC2_CREATELAUNCHTEMPLATE = "createlaunchtemplate";
  String EC2_CREATESECURITYGROUP = "createsecuritygroup";
  String EC2_CREATESNAPSHOT = "createsnapshot";
  String EC2_CREATETAGS = "createtags";
  String EC2_CREATEVOLUME = "createvolume";
  String EC2_CREATEVPC = "createvpc";
  String EC2_CREATEVPCPEERINGCONNECTION = "createvpcpeeringconnection";
  String EC2_DELETEDHCPOPTIONS = "deletedhcpoptions";
  String EC2_DELETEINTERNETGATEWAY = "deleteinternetgateway";
  String EC2_DELETELAUNCHTEMPLATE = "deletelaunchtemplate";
  String EC2_DELETENATGATEWAY = "deletenatgateway";
  String EC2_DELETENETWORKACL = "deletenetworkacl";
  String EC2_DELETENETWORKACLENTRY = "deletenetworkaclentry";
  String EC2_DELETEROUTE = "deleteroute";
  String EC2_DELETEROUTETABLE  = "deleteroutetable";
  String EC2_DELETESECURITYGROUP = "deletesecuritygroup";
  String EC2_DELETESNAPSHOT = "deletesnapshot";
  String EC2_DELETETAGS = "deletetags";
  String EC2_DELETEVOLUME = "deletevolume";
  String EC2_DELETEVPCPEERINGCONNECTION = "deletevpcpeeringconnection";
  String EC2_DESCRIBECONVERSIONTASKS = "describeconversiontasks";
  String EC2_DETACHCLASSICLINKVPC = "detachclassiclinkvpc";
  String EC2_DETACHVOLUME = "detachvolume";
  String EC2_DISABLEVPCCLASSICLINK = "disablevpcclassiclink";
  String EC2_DISASSOCIATEIAMINSTANCEPROFILE = "disassociateiaminstanceprofile";
  String EC2_ENABLEVPCCLASSICLINK = "enablevpcclassiclink";
  String EC2_GETCONSOLESCREENSHOT = "getconsolescreenshot";
  String EC2_IMPORTINSTANCE = "importinstance";
  String EC2_IMPORTVOLUME = "importvolume";
  String EC2_REBOOTINSTANCES = "rebootinstances";
  String EC2_REGISTERIMAGE = "registerimage";
  String EC2_REJECTVPCPEERINGCONNECTION = "rejectvpcpeeringconnection";
  String EC2_REPLACEIAMINSTANCEPROFILEASSOCIATION = "replaceiaminstanceprofileassociation";
  String EC2_REVOKESECURITYGROUPEGRESS = "revokesecuritygroupegress";
  String EC2_REVOKESECURITYGROUPINGRESS = "revokesecuritygroupingress";
  String EC2_RUNINSTANCES = "runinstances";
  String EC2_STARTINSTANCES = "startinstances";
  String EC2_STOPINSTANCES = "stopinstances";
  String EC2_TERMINATEINSTANCES = "terminateinstances";

  // EC2 eucalyptus actions
  String EC2_MIGRATEINSTANCES = "migrateinstances";

  // EC2 resource types, extension to AWS IAM
  String EC2_RESOURCE_IMAGE = "image";
  String EC2_RESOURCE_SECURITYGROUP = "security-group";
  String EC2_RESOURCE_ADDRESS = "address";
  String EC2_RESOURCE_ELASTICIP = "elastic-ip";
  String EC2_RESOURCE_AVAILABILITYZONE = "availabilityzone";
  String EC2_RESOURCE_INSTANCE = "instance";
  String EC2_RESOURCE_KEYPAIR = "key-pair";
  String EC2_RESOURCE_LAUNCHTEMPLATE = "launch-template";
  String EC2_RESOURCE_VOLUME = "volume";
  String EC2_RESOURCE_SNAPSHOT = "snapshot";
  String EC2_RESOURCE_VMTYPE = "vmtype";
  String EC2_RESOURCE_TAG = "tag";
  String EC2_RESOURCE_PLACEMENTGROUP = "placement-group";
  String EC2_RESOURCE_CUSTOMERGATEWAY = "customer-gateway";
  String EC2_RESOURCE_DHCPOPTIONS = "dhcp-options";
  String EC2_RESOURCE_INTERNETGATEWAY = "internet-gateway";
  String EC2_RESOURCE_NATGATEWAY = "nat-gateway";
  String EC2_RESOURCE_NETWORKACL = "network-acl";
  String EC2_RESOURCE_NETWORKINTERFACE = "network-interface";
  String EC2_RESOURCE_ROUTETABLE = "route-table";
  String EC2_RESOURCE_SUBNET = "subnet";
  String EC2_RESOURCE_VPCPEERINGCONNECTION = "vpc-peering-connection";
  String EC2_RESOURCE_VPC = "vpc";

  Set<String> EC2_RESOURCES = new ImmutableSet.Builder<String>()
      .add( EC2_RESOURCE_IMAGE )
      .add( EC2_RESOURCE_SECURITYGROUP )
      .add( EC2_RESOURCE_SECURITYGROUP.replace( "-", "" ) ) // no '-' until v4.1
      .add( EC2_RESOURCE_ADDRESS )
      .add( EC2_RESOURCE_ELASTICIP )
      .add( EC2_RESOURCE_AVAILABILITYZONE )
      .add( EC2_RESOURCE_INSTANCE )
      .add( EC2_RESOURCE_KEYPAIR )
      .add( EC2_RESOURCE_KEYPAIR.replace( "-", "" ) ) // no '-' until v4.1
      .add( EC2_RESOURCE_LAUNCHTEMPLATE )
      .add( EC2_RESOURCE_VOLUME )
      .add( EC2_RESOURCE_SNAPSHOT )
      .add( EC2_RESOURCE_VMTYPE )
      .add( EC2_RESOURCE_TAG )
      .add( EC2_RESOURCE_PLACEMENTGROUP )
      .add( EC2_RESOURCE_CUSTOMERGATEWAY )
      .add( EC2_RESOURCE_DHCPOPTIONS )
      .add( EC2_RESOURCE_INTERNETGATEWAY )
      .add( EC2_RESOURCE_NATGATEWAY )
      .add( EC2_RESOURCE_NETWORKACL )
      .add( EC2_RESOURCE_NETWORKINTERFACE )
      .add( EC2_RESOURCE_ROUTETABLE )
      .add( EC2_RESOURCE_SUBNET )
      .add( EC2_RESOURCE_VPCPEERINGCONNECTION )
      .add( EC2_RESOURCE_VPC )
      .build();
}
