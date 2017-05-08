/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
