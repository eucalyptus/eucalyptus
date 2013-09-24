/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.system.Ats;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class PolicySpec {
  
  public static final String VERSION = "Version";
  
  public static final String STATEMENT = "Statement";
  public static final String SID = "Sid";
  public static final String EFFECT = "Effect";
  public static final String ACTION = "Action";
  public static final String NOTACTION = "NotAction";
  public static final String RESOURCE = "Resource";
  public static final String NOTRESOURCE = "NotResource";
  public static final String PRINCIPAL = "Principal";
  public static final String NOTPRINCIPAL = "NotPrincipal";
  public static final String CONDITION = "Condition";
    
  // Effect
  public static final Set<String> EFFECTS = ImmutableSet.copyOf( Iterators.transform( Iterators.forArray(EffectType.values()), new Function<EffectType,String>() {
    @Override
    public String apply( final EffectType effect ) {
      return effect.name( );
    }
  }) );
  
  // Vendor (AWS products)
  public static final String VENDOR_IAM = "iam";
  public static final String VENDOR_EC2 = "ec2";
  public static final String VENDOR_S3 = "s3";
  public static final String VENDOR_STS = "sts";
  public static final String VENDOR_AUTOSCALING = "autoscaling";
  public static final String VENDOR_CLOUDWATCH = "cloudwatch";
  public static final String VENDOR_LOADBALANCING = "elasticloadbalancing";
  
  
  public static final Set<String> VENDORS = ImmutableSet.of(
    VENDOR_IAM,
    VENDOR_EC2,
    VENDOR_S3,
    VENDOR_STS,
    VENDOR_AUTOSCALING,
    VENDOR_CLOUDWATCH,
    VENDOR_LOADBALANCING
  );

  public static final String ALL_ACTION = "*";
  
  // IAM actions, based on API version 2010-05-08
  public static final String IAM_ADDROLETOINSTANCEPROFILE = "addroletoinstanceprofile";
  public static final String IAM_ADDUSERTOGROUP = "addusertogroup";
  public static final String IAM_CHANGEPASSWORD = "changepassword";
  public static final String IAM_CREATEACCESSKEY = "createaccesskey";
  public static final String IAM_CREATEACCOUNTALIAS = "createaccountalias";
  public static final String IAM_CREATEGROUP = "creategroup";
  public static final String IAM_CREATEINSTANCEPROFILE = "createinstanceprofile";
  public static final String IAM_CREATELOGINPROFILE = "createloginprofile";
  public static final String IAM_CREATEROLE = "createrole";
  public static final String IAM_CREATEUSER = "createuser";
  public static final String IAM_CREATEVIRTUALMFADEVICE = "createvirtualmfadevice";
  public static final String IAM_DEACTIVATEMFADEVICE = "deactivatemfadevice";
  public static final String IAM_DELETEACCESSKEY = "deleteaccesskey";
  public static final String IAM_DELETEACCOUNTALIAS = "deleteaccountalias";
  public static final String IAM_DELETEACCOUNTPASSWORDPOLICY = "deleteaccountpasswordpolicy";
  public static final String IAM_DELETEGROUP = "deletegroup";
  public static final String IAM_DELETEGROUPPOLICY = "deletegrouppolicy";
  public static final String IAM_DELETEINSTANCEPROFILE = "deleteinstanceprofile";
  public static final String IAM_DELETELOGINPROFILE = "deleteloginprofile";
  public static final String IAM_DELETEROLE = "deleterole";
  public static final String IAM_DELETEROLEPOLICY = "deleterolepolicy";
  public static final String IAM_DELETESERVERCERTIFICATE = "deleteservercertificate";
  public static final String IAM_DELETESIGNINGCERTIFICATE = "deletesigningcertificate";
  public static final String IAM_DELETEUSER = "deleteuser";
  public static final String IAM_DELETEUSERPOLICY = "deleteuserpolicy";
  public static final String IAM_DELETEVIRTUALMFADEVICE = "deletevirtualmfadevice";
  public static final String IAM_ENABLEMFADEVICE = "enablemfadevice";
  public static final String IAM_GETACCOUNTPASSWORDPOLICY = "getaccountpasswordpolicy";
  public static final String IAM_GETACCOUNTSUMMARY = "getaccountsummary";
  public static final String IAM_GETGROUP = "getgroup";
  public static final String IAM_GETGROUPPOLICY = "getgrouppolicy";
  public static final String IAM_GETINSTANCEPROFILE = "getinstanceprofile";
  public static final String IAM_GETLOGINPROFILE = "getloginprofile";
  public static final String IAM_GETROLE = "getrole";
  public static final String IAM_GETROLEPOLICY = "getrolepolicy";
  public static final String IAM_GETSERVERCERTIFICATE = "getservercertificate";
  public static final String IAM_GETUSER = "getuser";
  public static final String IAM_GETUSERPOLICY = "getuserpolicy";
  public static final String IAM_LISTACCESSKEYS = "listaccesskeys";
  public static final String IAM_LISTACCOUNTALIASES = "listaccountaliases";
  public static final String IAM_LISTGROUPPOLICIES = "listgrouppolicies";
  public static final String IAM_LISTGROUPS = "listgroups";
  public static final String IAM_LISTGROUPSFORUSER = "listgroupsforuser";
  public static final String IAM_LISTINSTANCEPROFILES = "listinstanceprofiles";
  public static final String IAM_LISTINSTANCEPROFILESFORROLE = "listinstanceprofilesforrole";
  public static final String IAM_LISTMFADEVICES = "listmfadevices";
  public static final String IAM_LISTROLEPOLICIES = "listrolepolicies";
  public static final String IAM_LISTROLES = "listroles";
  public static final String IAM_LISTSERVERCERTIFICATES = "listservercertificates";
  public static final String IAM_LISTSIGNINGCERTIFICATES = "listsigningcertificates";
  public static final String IAM_LISTUSERPOLICIES = "listuserpolicies";
  public static final String IAM_LISTUSERS = "listusers";
  public static final String IAM_LISTVIRTUALMFADEVICES = "listvirtualmfadevices";
  public static final String IAM_PASSROLE = "passrole";
  public static final String IAM_PUTGROUPPOLICY = "putgrouppolicy";
  public static final String IAM_PUTROLEPOLICY = "putrolepolicy";
  public static final String IAM_PUTUSERPOLICY = "putuserpolicy";
  public static final String IAM_REMOVEROLEFROMINSTANCEPROFILE = "removerolefrominstanceprofile";
  public static final String IAM_REMOVEUSERFROMGROUP = "removeuserfromgroup";
  public static final String IAM_RESYNCMFADEVICE = "resyncmfadevice";
  public static final String IAM_UPDATEACCESSKEY = "updateaccesskey";
  public static final String IAM_UPDATEACCOUNTPASSWORDPOLICY = "updateaccountpasswordpolicy";
  public static final String IAM_UPDATEASSUMEROLEPOLICY = "updateassumerolepolicy";
  public static final String IAM_UPDATEGROUP = "updategroup";
  public static final String IAM_UPDATELOGINPROFILE = "updateloginprofile";
  public static final String IAM_UPDATESERVERCERTIFICATE = "updateservercertificate";
  public static final String IAM_UPDATESIGNINGCERTIFICATE = "updatesigningcertificate";
  public static final String IAM_UPDATEUSER = "updateuser";
  public static final String IAM_UPLOADSERVERCERTIFICATE = "uploadservercertificate";
  public static final String IAM_UPLOADSIGNINGCERTIFICATE = "uploadsigningcertificate";

  public static final Set<String> IAM_ACTIONS = new ImmutableSet.Builder<String>( )
    .add( IAM_ADDROLETOINSTANCEPROFILE )
    .add( IAM_ADDUSERTOGROUP )
    .add( IAM_CHANGEPASSWORD )
    .add( IAM_CREATEACCESSKEY )
    .add( IAM_CREATEACCOUNTALIAS )
    .add( IAM_CREATEGROUP )
    .add( IAM_CREATEINSTANCEPROFILE )
    .add( IAM_CREATELOGINPROFILE )
    .add( IAM_CREATEROLE )
    .add( IAM_CREATEUSER )
    .add( IAM_CREATEVIRTUALMFADEVICE )
    .add( IAM_DEACTIVATEMFADEVICE )
    .add( IAM_DELETEACCESSKEY )
    .add( IAM_DELETEACCOUNTALIAS )
    .add( IAM_DELETEACCOUNTPASSWORDPOLICY )
    .add( IAM_DELETEGROUP )
    .add( IAM_DELETEGROUPPOLICY )
    .add( IAM_DELETEINSTANCEPROFILE )
    .add( IAM_DELETELOGINPROFILE )
    .add( IAM_DELETEROLE )
    .add( IAM_DELETEROLEPOLICY )
    .add( IAM_DELETESERVERCERTIFICATE )
    .add( IAM_DELETESIGNINGCERTIFICATE )
    .add( IAM_DELETEUSER )
    .add( IAM_DELETEUSERPOLICY )
    .add( IAM_DELETEVIRTUALMFADEVICE )
    .add( IAM_ENABLEMFADEVICE )
    .add( IAM_GETACCOUNTPASSWORDPOLICY )
    .add( IAM_GETACCOUNTSUMMARY )
    .add( IAM_GETGROUP )
    .add( IAM_GETGROUPPOLICY )
    .add( IAM_GETINSTANCEPROFILE )
    .add( IAM_GETLOGINPROFILE )
    .add( IAM_GETROLE )
    .add( IAM_GETROLEPOLICY )
    .add( IAM_GETSERVERCERTIFICATE )
    .add( IAM_GETUSER )
    .add( IAM_GETUSERPOLICY )
    .add( IAM_LISTACCESSKEYS )
    .add( IAM_LISTACCOUNTALIASES )
    .add( IAM_LISTGROUPPOLICIES )
    .add( IAM_LISTGROUPS )
    .add( IAM_LISTGROUPSFORUSER )
    .add( IAM_LISTINSTANCEPROFILES )
    .add( IAM_LISTINSTANCEPROFILESFORROLE )
    .add( IAM_LISTMFADEVICES )
    .add( IAM_LISTROLEPOLICIES )
    .add( IAM_LISTROLES )
    .add( IAM_LISTSERVERCERTIFICATES )
    .add( IAM_LISTSIGNINGCERTIFICATES )
    .add( IAM_LISTUSERPOLICIES )
    .add( IAM_LISTUSERS )
    .add( IAM_LISTVIRTUALMFADEVICES )
    .add( IAM_PASSROLE )
    .add( IAM_PUTGROUPPOLICY )
    .add( IAM_PUTROLEPOLICY )
    .add( IAM_PUTUSERPOLICY )
    .add( IAM_REMOVEROLEFROMINSTANCEPROFILE )
    .add( IAM_REMOVEUSERFROMGROUP )
    .add( IAM_RESYNCMFADEVICE )
    .add( IAM_UPDATEACCESSKEY )
    .add( IAM_UPDATEACCOUNTPASSWORDPOLICY )
    .add( IAM_UPDATEASSUMEROLEPOLICY )
    .add( IAM_UPDATEGROUP )
    .add( IAM_UPDATELOGINPROFILE )
    .add( IAM_UPDATESERVERCERTIFICATE )
    .add( IAM_UPDATESIGNINGCERTIFICATE )
    .add( IAM_UPDATEUSER )
    .add( IAM_UPLOADSERVERCERTIFICATE )
    .add( IAM_UPLOADSIGNINGCERTIFICATE )
    .build();

  // EC2 actions, based on API version 2013-07-15
  public static final String EC2_ALLOCATEADDRESS = "allocateaddress";
  public static final String EC2_ASSIGNPRIVATEIPADDRESSES = "assignprivateipaddresses";
  public static final String EC2_ASSOCIATEADDRESS = "associateaddress";
  public static final String EC2_ASSOCIATEDHCPOPTIONS = "associatedhcpoptions";
  public static final String EC2_ASSOCIATEROUTETABLE = "associateroutetable";
  public static final String EC2_ATTACHINTERNETGATEWAY = "attachinternetgateway";
  public static final String EC2_ATTACHNETWORKINTERFACE = "attachnetworkinterface";
  public static final String EC2_ATTACHVOLUME = "attachvolume";
  public static final String EC2_ATTACHVPNGATEWAY = "attachvpngateway";
  public static final String EC2_AUTHORIZESECURITYGROUPEGRESS = "authorizesecuritygroupegress";
  public static final String EC2_AUTHORIZESECURITYGROUPINGRESS = "authorizesecuritygroupingress";
  public static final String EC2_BUNDLEINSTANCE = "bundleinstance";
  public static final String EC2_CANCELBUNDLETASK = "cancelbundletask";
  public static final String EC2_CANCELCONVERSIONTASK = "cancelconversiontask";
  public static final String EC2_CANCELEXPORTTASK = "cancelexporttask";
  public static final String EC2_CANCELRESERVEDINSTANCESLISTING = "cancelreservedinstanceslisting";
  public static final String EC2_CANCELSPOTINSTANCEREQUESTS = "cancelspotinstancerequests";
  public static final String EC2_CONFIRMPRODUCTINSTANCE = "confirmproductinstance";
  public static final String EC2_COPYIMAGE = "copyimage";
  public static final String EC2_COPYSNAPSHOT = "copysnapshot";
  public static final String EC2_CREATECUSTOMERGATEWAY = "createcustomergateway";
  public static final String EC2_CREATEDHCPOPTIONS = "createdhcpoptions";
  public static final String EC2_CREATEIMAGE = "createimage";
  public static final String EC2_CREATEINSTANCEEXPORTTASK = "createinstanceexporttask";
  public static final String EC2_CREATEINTERNETGATEWAY = "createinternetgateway";
  public static final String EC2_CREATEKEYPAIR = "createkeypair";
  public static final String EC2_CREATENETWORKACL = "createnetworkacl";
  public static final String EC2_CREATENETWORKACLENTRY = "createnetworkaclentry";
  public static final String EC2_CREATENETWORKINTERFACE = "createnetworkinterface";
  public static final String EC2_CREATEPLACEMENTGROUP = "createplacementgroup";
  public static final String EC2_CREATERESERVEDINSTANCESLISTING = "createreservedinstanceslisting";
  public static final String EC2_CREATEROUTE = "createroute";
  public static final String EC2_CREATEROUTETABLE = "createroutetable";
  public static final String EC2_CREATESECURITYGROUP = "createsecuritygroup";
  public static final String EC2_CREATESNAPSHOT = "createsnapshot";
  public static final String EC2_CREATESPOTDATAFEEDSUBSCRIPTION = "createspotdatafeedsubscription";
  public static final String EC2_CREATESUBNET = "createsubnet";
  public static final String EC2_CREATETAGS = "createtags";
  public static final String EC2_CREATEVOLUME = "createvolume";
  public static final String EC2_CREATEVPC = "createvpc";
  public static final String EC2_CREATEVPNCONNECTION = "createvpnconnection";
  public static final String EC2_CREATEVPNCONNECTIONROUTE = "createvpnconnectionroute";
  public static final String EC2_CREATEVPNGATEWAY = "createvpngateway";
  public static final String EC2_DELETECUSTOMERGATEWAY = "deletecustomergateway";
  public static final String EC2_DELETEDHCPOPTIONS = "deletedhcpoptions";
  public static final String EC2_DELETEINTERNETGATEWAY = "deleteinternetgateway";
  public static final String EC2_DELETEKEYPAIR = "deletekeypair";
  public static final String EC2_DELETENETWORKACL = "deletenetworkacl";
  public static final String EC2_DELETENETWORKACLENTRY = "deletenetworkaclentry";
  public static final String EC2_DELETENETWORKINTERFACE = "deletenetworkinterface";
  public static final String EC2_DELETEPLACEMENTGROUP = "deleteplacementgroup";
  public static final String EC2_DELETEROUTE = "deleteroute";
  public static final String EC2_DELETEROUTETABLE = "deleteroutetable";
  public static final String EC2_DELETESECURITYGROUP = "deletesecuritygroup";
  public static final String EC2_DELETESNAPSHOT = "deletesnapshot";
  public static final String EC2_DELETESPOTDATAFEEDSUBSCRIPTION = "deletespotdatafeedsubscription";
  public static final String EC2_DELETESUBNET = "deletesubnet";
  public static final String EC2_DELETETAGS = "deletetags";
  public static final String EC2_DELETEVOLUME = "deletevolume";
  public static final String EC2_DELETEVPC = "deletevpc";
  public static final String EC2_DELETEVPNCONNECTION = "deletevpnconnection";
  public static final String EC2_DELETEVPNCONNECTIONROUTE = "deletevpnconnectionroute";
  public static final String EC2_DELETEVPNGATEWAY = "deletevpngateway";
  public static final String EC2_DEREGISTERIMAGE = "deregisterimage";
  public static final String EC2_DESCRIBEACCOUNTATTRIBUTES = "describeaccountattributes";
  public static final String EC2_DESCRIBEADDRESSES = "describeaddresses";
  public static final String EC2_DESCRIBEAVAILABILITYZONES = "describeavailabilityzones";
  public static final String EC2_DESCRIBEBUNDLETASKS = "describebundletasks";
  public static final String EC2_DESCRIBECONVERSIONTASKS = "describeconversiontasks";
  public static final String EC2_DESCRIBECUSTOMERGATEWAYS = "describecustomergateways";
  public static final String EC2_DESCRIBEDHCPOPTIONS = "describedhcpoptions";
  public static final String EC2_DESCRIBEEXPORTTASKS = "describeexporttasks";
  public static final String EC2_DESCRIBEIMAGEATTRIBUTE = "describeimageattribute";
  public static final String EC2_DESCRIBEIMAGES = "describeimages";
  public static final String EC2_DESCRIBEINSTANCEATTRIBUTE = "describeinstanceattribute";
  public static final String EC2_DESCRIBEINSTANCES = "describeinstances";
  public static final String EC2_DESCRIBEINSTANCESTATUS = "describeinstancestatus";
  public static final String EC2_DESCRIBEINTERNETGATEWAYS = "describeinternetgateways";
  public static final String EC2_DESCRIBEKEYPAIRS = "describekeypairs";
  public static final String EC2_DESCRIBENETWORKACLS = "describenetworkacls";
  public static final String EC2_DESCRIBENETWORKINTERFACEATTRIBUTE = "describenetworkinterfaceattribute";
  public static final String EC2_DESCRIBENETWORKINTERFACES = "describenetworkinterfaces";
  public static final String EC2_DESCRIBEPLACEMENTGROUPS = "describeplacementgroups";
  public static final String EC2_DESCRIBEREGIONS = "describeregions";
  public static final String EC2_DESCRIBERESERVEDINSTANCES = "describereservedinstances";
  public static final String EC2_DESCRIBERESERVEDINSTANCESLISTINGS = "describereservedinstanceslistings";
  public static final String EC2_DESCRIBERESERVEDINSTANCESMODIFICATIONS = "describereservedinstancesmodifications";
  public static final String EC2_DESCRIBERESERVEDINSTANCESOFFERINGS = "describereservedinstancesofferings";
  public static final String EC2_DESCRIBEROUTETABLES = "describeroutetables";
  public static final String EC2_DESCRIBESECURITYGROUPS = "describesecuritygroups";
  public static final String EC2_DESCRIBESNAPSHOTATTRIBUTE = "describesnapshotattribute";
  public static final String EC2_DESCRIBESNAPSHOTS = "describesnapshots";
  public static final String EC2_DESCRIBESPOTDATAFEEDSUBSCRIPTION = "describespotdatafeedsubscription";
  public static final String EC2_DESCRIBESPOTINSTANCEREQUESTS = "describespotinstancerequests";
  public static final String EC2_DESCRIBESPOTPRICEHISTORY = "describespotpricehistory";
  public static final String EC2_DESCRIBESUBNETS = "describesubnets";
  public static final String EC2_DESCRIBETAGS = "describetags";
  public static final String EC2_DESCRIBEVOLUMEATTRIBUTE = "describevolumeattribute";
  public static final String EC2_DESCRIBEVOLUMES = "describevolumes";
  public static final String EC2_DESCRIBEVOLUMESTATUS = "describevolumestatus";
  public static final String EC2_DESCRIBEVPCATTRIBUTE = "describevpcattribute";
  public static final String EC2_DESCRIBEVPCS = "describevpcs";
  public static final String EC2_DESCRIBEVPNCONNECTIONS = "describevpnconnections";
  public static final String EC2_DESCRIBEVPNGATEWAYS = "describevpngateways";
  public static final String EC2_DETACHINTERNETGATEWAY = "detachinternetgateway";
  public static final String EC2_DETACHNETWORKINTERFACE = "detachnetworkinterface";
  public static final String EC2_DETACHVOLUME = "detachvolume";
  public static final String EC2_DETACHVPNGATEWAY = "detachvpngateway";
  public static final String EC2_DISABLEVGWROUTEPROPAGATION = "disablevgwroutepropagation";
  public static final String EC2_DISASSOCIATEADDRESS = "disassociateaddress";
  public static final String EC2_DISASSOCIATEROUTETABLE = "disassociateroutetable";
  public static final String EC2_ENABLEVGWROUTEPROPAGATION = "enablevgwroutepropagation";
  public static final String EC2_ENABLEVOLUMEIO = "enablevolumeio";
  public static final String EC2_GETCONSOLEOUTPUT = "getconsoleoutput";
  public static final String EC2_GETPASSWORDDATA = "getpassworddata";
  public static final String EC2_IMPORTINSTANCE = "importinstance";
  public static final String EC2_IMPORTKEYPAIR = "importkeypair";
  public static final String EC2_IMPORTVOLUME = "importvolume";
  public static final String EC2_MODIFYIMAGEATTRIBUTE = "modifyimageattribute";
  public static final String EC2_MODIFYINSTANCEATTRIBUTE = "modifyinstanceattribute";
  public static final String EC2_MODIFYNETWORKINTERFACEATTRIBUTE = "modifynetworkinterfaceattribute";
  public static final String EC2_MODIFYRESERVEDINSTANCES = "modifyreservedinstances";
  public static final String EC2_MODIFYSNAPSHOTATTRIBUTE = "modifysnapshotattribute";
  public static final String EC2_MODIFYVOLUMEATTRIBUTE = "modifyvolumeattribute";
  public static final String EC2_MODIFYVPCATTRIBUTE = "modifyvpcattribute";
  public static final String EC2_MONITORINSTANCES = "monitorinstances";
  public static final String EC2_PURCHASERESERVEDINSTANCESOFFERING = "purchasereservedinstancesoffering";
  public static final String EC2_REBOOTINSTANCES = "rebootinstances";
  public static final String EC2_REGISTERIMAGE = "registerimage";
  public static final String EC2_RELEASEADDRESS = "releaseaddress";
  public static final String EC2_REPLACENETWORKACLASSOCIATION = "replacenetworkaclassociation";
  public static final String EC2_REPLACENETWORKACLENTRY = "replacenetworkaclentry";
  public static final String EC2_REPLACEROUTE = "replaceroute";
  public static final String EC2_REPLACEROUTETABLEASSOCIATION = "replaceroutetableassociation";
  public static final String EC2_REPORTINSTANCESTATUS = "reportinstancestatus";
  public static final String EC2_REQUESTSPOTINSTANCES = "requestspotinstances";
  public static final String EC2_RESETIMAGEATTRIBUTE = "resetimageattribute";
  public static final String EC2_RESETINSTANCEATTRIBUTE = "resetinstanceattribute";
  public static final String EC2_RESETNETWORKINTERFACEATTRIBUTE = "resetnetworkinterfaceattribute";
  public static final String EC2_RESETSNAPSHOTATTRIBUTE = "resetsnapshotattribute";
  public static final String EC2_REVOKESECURITYGROUPEGRESS = "revokesecuritygroupegress";
  public static final String EC2_REVOKESECURITYGROUPINGRESS = "revokesecuritygroupingress";
  public static final String EC2_RUNINSTANCES = "runinstances";
  public static final String EC2_STARTINSTANCES = "startinstances";
  public static final String EC2_STOPINSTANCES = "stopinstances";
  public static final String EC2_TERMINATEINSTANCES = "terminateinstances";
  public static final String EC2_UNASSIGNPRIVATEIPADDRESSES = "unassignprivateipaddresses";
  public static final String EC2_UNMONITORINSTANCES = "unmonitorinstances";

  // Deprecated EC2 actions
  public static final String EC2_ACTIVATELICENSE = "activatelicense";
  public static final String EC2_DEACTIVATELICENSE = "deactivatelicense";
  public static final String EC2_DESCRIBELICENSES = "describelicenses";

  public static final Set<String> EC2_ACTIONS = new ImmutableSet.Builder<String>()
    .add( EC2_ALLOCATEADDRESS )
    .add( EC2_ASSIGNPRIVATEIPADDRESSES )
    .add( EC2_ASSOCIATEADDRESS )
    .add( EC2_ASSOCIATEDHCPOPTIONS )
    .add( EC2_ASSOCIATEROUTETABLE )
    .add( EC2_ATTACHINTERNETGATEWAY )
    .add( EC2_ATTACHNETWORKINTERFACE )
    .add( EC2_ATTACHVOLUME )
    .add( EC2_ATTACHVPNGATEWAY )
    .add( EC2_AUTHORIZESECURITYGROUPEGRESS )
    .add( EC2_AUTHORIZESECURITYGROUPINGRESS )
    .add( EC2_BUNDLEINSTANCE )
    .add( EC2_CANCELBUNDLETASK )
    .add( EC2_CANCELCONVERSIONTASK )
    .add( EC2_CANCELEXPORTTASK )
    .add( EC2_CANCELRESERVEDINSTANCESLISTING )
    .add( EC2_CANCELSPOTINSTANCEREQUESTS )
    .add( EC2_CONFIRMPRODUCTINSTANCE )
    .add( EC2_COPYIMAGE )
    .add( EC2_COPYSNAPSHOT )
    .add( EC2_CREATECUSTOMERGATEWAY )
    .add( EC2_CREATEDHCPOPTIONS )
    .add( EC2_CREATEIMAGE )
    .add( EC2_CREATEINSTANCEEXPORTTASK )
    .add( EC2_CREATEINTERNETGATEWAY )
    .add( EC2_CREATEKEYPAIR )
    .add( EC2_CREATENETWORKACL )
    .add( EC2_CREATENETWORKACLENTRY )
    .add( EC2_CREATENETWORKINTERFACE )
    .add( EC2_CREATEPLACEMENTGROUP )
    .add( EC2_CREATERESERVEDINSTANCESLISTING )
    .add( EC2_CREATEROUTE )
    .add( EC2_CREATEROUTETABLE )
    .add( EC2_CREATESECURITYGROUP )
    .add( EC2_CREATESNAPSHOT )
    .add( EC2_CREATESPOTDATAFEEDSUBSCRIPTION )
    .add( EC2_CREATESUBNET )
    .add( EC2_CREATETAGS )
    .add( EC2_CREATEVOLUME )
    .add( EC2_CREATEVPC )
    .add( EC2_CREATEVPNCONNECTION )
    .add( EC2_CREATEVPNCONNECTIONROUTE )
    .add( EC2_CREATEVPNGATEWAY )
    .add( EC2_DELETECUSTOMERGATEWAY )
    .add( EC2_DELETEDHCPOPTIONS )
    .add( EC2_DELETEINTERNETGATEWAY )
    .add( EC2_DELETEKEYPAIR )
    .add( EC2_DELETENETWORKACL )
    .add( EC2_DELETENETWORKACLENTRY )
    .add( EC2_DELETENETWORKINTERFACE )
    .add( EC2_DELETEPLACEMENTGROUP )
    .add( EC2_DELETEROUTE )
    .add( EC2_DELETEROUTETABLE )
    .add( EC2_DELETESECURITYGROUP )
    .add( EC2_DELETESNAPSHOT )
    .add( EC2_DELETESPOTDATAFEEDSUBSCRIPTION )
    .add( EC2_DELETESUBNET )
    .add( EC2_DELETETAGS )
    .add( EC2_DELETEVOLUME )
    .add( EC2_DELETEVPC )
    .add( EC2_DELETEVPNCONNECTION )
    .add( EC2_DELETEVPNCONNECTIONROUTE )
    .add( EC2_DELETEVPNGATEWAY )
    .add( EC2_DEREGISTERIMAGE )
    .add( EC2_DESCRIBEACCOUNTATTRIBUTES )
    .add( EC2_DESCRIBEADDRESSES )
    .add( EC2_DESCRIBEAVAILABILITYZONES )
    .add( EC2_DESCRIBEBUNDLETASKS )
    .add( EC2_DESCRIBECONVERSIONTASKS )
    .add( EC2_DESCRIBECUSTOMERGATEWAYS )
    .add( EC2_DESCRIBEDHCPOPTIONS )
    .add( EC2_DESCRIBEEXPORTTASKS )
    .add( EC2_DESCRIBEIMAGEATTRIBUTE )
    .add( EC2_DESCRIBEIMAGES )
    .add( EC2_DESCRIBEINSTANCEATTRIBUTE )
    .add( EC2_DESCRIBEINSTANCES )
    .add( EC2_DESCRIBEINSTANCESTATUS )
    .add( EC2_DESCRIBEINTERNETGATEWAYS )
    .add( EC2_DESCRIBEKEYPAIRS )
    .add( EC2_DESCRIBENETWORKACLS )
    .add( EC2_DESCRIBENETWORKINTERFACEATTRIBUTE )
    .add( EC2_DESCRIBENETWORKINTERFACES )
    .add( EC2_DESCRIBEPLACEMENTGROUPS )
    .add( EC2_DESCRIBEREGIONS )
    .add( EC2_DESCRIBERESERVEDINSTANCES )
    .add( EC2_DESCRIBERESERVEDINSTANCESLISTINGS )
    .add( EC2_DESCRIBERESERVEDINSTANCESMODIFICATIONS )
    .add( EC2_DESCRIBERESERVEDINSTANCESOFFERINGS )
    .add( EC2_DESCRIBEROUTETABLES )
    .add( EC2_DESCRIBESECURITYGROUPS )
    .add( EC2_DESCRIBESNAPSHOTATTRIBUTE )
    .add( EC2_DESCRIBESNAPSHOTS )
    .add( EC2_DESCRIBESPOTDATAFEEDSUBSCRIPTION )
    .add( EC2_DESCRIBESPOTINSTANCEREQUESTS )
    .add( EC2_DESCRIBESPOTPRICEHISTORY )
    .add( EC2_DESCRIBESUBNETS )
    .add( EC2_DESCRIBETAGS )
    .add( EC2_DESCRIBEVOLUMEATTRIBUTE )
    .add( EC2_DESCRIBEVOLUMES )
    .add( EC2_DESCRIBEVOLUMESTATUS )
    .add( EC2_DESCRIBEVPCATTRIBUTE )
    .add( EC2_DESCRIBEVPCS )
    .add( EC2_DESCRIBEVPNCONNECTIONS )
    .add( EC2_DESCRIBEVPNGATEWAYS )
    .add( EC2_DETACHINTERNETGATEWAY )
    .add( EC2_DETACHNETWORKINTERFACE )
    .add( EC2_DETACHVOLUME )
    .add( EC2_DETACHVPNGATEWAY )
    .add( EC2_DISABLEVGWROUTEPROPAGATION )
    .add( EC2_DISASSOCIATEADDRESS )
    .add( EC2_DISASSOCIATEROUTETABLE )
    .add( EC2_ENABLEVGWROUTEPROPAGATION )
    .add( EC2_ENABLEVOLUMEIO )
    .add( EC2_GETCONSOLEOUTPUT )
    .add( EC2_GETPASSWORDDATA )
    .add( EC2_IMPORTINSTANCE )
    .add( EC2_IMPORTKEYPAIR )
    .add( EC2_IMPORTVOLUME )
    .add( EC2_MODIFYIMAGEATTRIBUTE )
    .add( EC2_MODIFYINSTANCEATTRIBUTE )
    .add( EC2_MODIFYNETWORKINTERFACEATTRIBUTE )
    .add( EC2_MODIFYRESERVEDINSTANCES )
    .add( EC2_MODIFYSNAPSHOTATTRIBUTE )
    .add( EC2_MODIFYVOLUMEATTRIBUTE )
    .add( EC2_MODIFYVPCATTRIBUTE )
    .add( EC2_MONITORINSTANCES )
    .add( EC2_PURCHASERESERVEDINSTANCESOFFERING )
    .add( EC2_REBOOTINSTANCES )
    .add( EC2_REGISTERIMAGE )
    .add( EC2_RELEASEADDRESS )
    .add( EC2_REPLACENETWORKACLASSOCIATION )
    .add( EC2_REPLACENETWORKACLENTRY )
    .add( EC2_REPLACEROUTE )
    .add( EC2_REPLACEROUTETABLEASSOCIATION )
    .add( EC2_REPORTINSTANCESTATUS )
    .add( EC2_REQUESTSPOTINSTANCES )
    .add( EC2_RESETIMAGEATTRIBUTE )
    .add( EC2_RESETINSTANCEATTRIBUTE )
    .add( EC2_RESETNETWORKINTERFACEATTRIBUTE )
    .add( EC2_RESETSNAPSHOTATTRIBUTE )
    .add( EC2_REVOKESECURITYGROUPEGRESS )
    .add( EC2_REVOKESECURITYGROUPINGRESS )
    .add( EC2_RUNINSTANCES )
    .add( EC2_STARTINSTANCES )
    .add( EC2_STOPINSTANCES )
    .add( EC2_TERMINATEINSTANCES )
    .add( EC2_UNASSIGNPRIVATEIPADDRESSES )
    .add( EC2_UNMONITORINSTANCES )

    // Deprecated
    .add( EC2_ACTIVATELICENSE )
    .add( EC2_DEACTIVATELICENSE )
    .add( EC2_DESCRIBELICENSES )
    .build();

  // S3 actions
  public static final String S3_ABORTMULTIPARTUPLOAD = "abortmultipartupload";
  public static final String S3_CREATEBUCKET = "createbucket";
  public static final String S3_DELETEBUCKET = "deletebucket";
  public static final String S3_DELETEBUCKETPOLICY = "deletebucketpolicy";
  public static final String S3_DELETEBUCKETWEBSITE = "deletebucketwebsite";
  public static final String S3_DELETEOBJECT = "deleteobject";
  public static final String S3_DELETEOBJECTVERSION = "deleteobjectversion";
  public static final String S3_GETBUCKETACL = "getbucketacl";
  public static final String S3_GETBUCKETCORS = "getbucketcors";
  public static final String S3_GETBUCKETLOCATION = "getbucketlocation";
  public static final String S3_GETBUCKETLOGGING = "getbucketlogging";
  public static final String S3_GETBUCKETNOTIFICATION = "getbucketnotification";
  public static final String S3_GETBUCKETPOLICY = "getbucketpolicy";
  public static final String S3_GETBUCKETREQUESTPAYMENT = "getbucketrequestpayment";
  public static final String S3_GETBUCKETVERSIONING = "getbucketversioning";
  public static final String S3_GETBUCKETWEBSITE = "getbucketwebsite";
  public static final String S3_GETLIFECYCLECONFIGURATION = "getlifecycleconfiguration";
  public static final String S3_GETOBJECT = "getobject";
  public static final String S3_GETOBJECTACL = "getobjectacl";
  public static final String S3_GETOBJECTTORRENT = "getobjecttorrent";
  public static final String S3_GETOBJECTVERSION = "getobjectversion";
  public static final String S3_GETOBJECTVERSIONACL = "getobjectversionacl";
  public static final String S3_GETOBJECTVERSIONTORRENT = "getobjectversiontorrent";
  public static final String S3_LISTALLMYBUCKETS = "listallmybuckets";
  public static final String S3_LISTBUCKET = "listbucket";
  public static final String S3_LISTBUCKETMULTIPARTUPLOADS = "listbucketmultipartuploads";
  public static final String S3_LISTBUCKETVERSIONS = "listbucketversions";
  public static final String S3_LISTMULTIPARTUPLOADPARTS = "listmultipartuploadparts";
  public static final String S3_PUTBUCKETACL = "putbucketacl";
  public static final String S3_PUTBUCKETCORS = "putbucketcors";
  public static final String S3_PUTBUCKETLOGGING = "putbucketlogging";
  public static final String S3_PUTBUCKETNOTIFICATION = "putbucketnotification";
  public static final String S3_PUTBUCKETPOLICY = "putbucketpolicy";
  public static final String S3_PUTBUCKETREQUESTPAYMENT = "putbucketrequestpayment";
  public static final String S3_PUTBUCKETVERSIONING = "putbucketversioning";
  public static final String S3_PUTBUCKETWEBSITE = "putbucketwebsite";
  public static final String S3_PUTLIFECYCLECONFIGURATION = "putlifecycleconfiguration";
  public static final String S3_PUTOBJECT = "putobject";
  public static final String S3_PUTOBJECTACL = "putobjectacl";
  public static final String S3_PUTOBJECTVERSIONACL = "putobjectversionacl";
  public static final String S3_RESTOREOBJECT = "restoreobject";

  public static final Set<String> S3_ACTIONS = new ImmutableSet.Builder<String>()
    .add( S3_ABORTMULTIPARTUPLOAD )
    .add( S3_CREATEBUCKET )
    .add( S3_DELETEBUCKET )
    .add( S3_DELETEBUCKETPOLICY )
    .add( S3_DELETEBUCKETWEBSITE )
    .add( S3_DELETEOBJECT )
    .add( S3_DELETEOBJECTVERSION )
    .add( S3_GETBUCKETACL )
    .add( S3_GETBUCKETCORS )
    .add( S3_GETBUCKETLOCATION )
    .add( S3_GETBUCKETLOGGING )
    .add( S3_GETBUCKETNOTIFICATION )
    .add( S3_GETBUCKETPOLICY )
    .add( S3_GETBUCKETREQUESTPAYMENT )
    .add( S3_GETBUCKETVERSIONING )
    .add( S3_GETBUCKETWEBSITE )
    .add( S3_GETLIFECYCLECONFIGURATION )
    .add( S3_GETOBJECT )
    .add( S3_GETOBJECTACL )
    .add( S3_GETOBJECTTORRENT )
    .add( S3_GETOBJECTVERSION )
    .add( S3_GETOBJECTVERSIONACL )
    .add( S3_GETOBJECTVERSIONTORRENT )
    .add( S3_LISTALLMYBUCKETS )
    .add( S3_LISTBUCKET )
    .add( S3_LISTBUCKETMULTIPARTUPLOADS )
    .add( S3_LISTBUCKETVERSIONS )
    .add( S3_LISTMULTIPARTUPLOADPARTS )
    .add( S3_PUTBUCKETACL )
    .add( S3_PUTBUCKETCORS )
    .add( S3_PUTBUCKETLOGGING )
    .add( S3_PUTBUCKETNOTIFICATION )
    .add( S3_PUTBUCKETPOLICY )
    .add( S3_PUTBUCKETREQUESTPAYMENT )
    .add( S3_PUTBUCKETVERSIONING )
    .add( S3_PUTBUCKETWEBSITE )
    .add( S3_PUTLIFECYCLECONFIGURATION )
    .add( S3_PUTOBJECT )
    .add( S3_PUTOBJECTACL )
    .add( S3_PUTOBJECTVERSIONACL )
    .add( S3_RESTOREOBJECT )
    .build();

  // STS actions, based on IAM Using Temporary Security Credentials version 2011-06-15
  public static final String STS_ASSUMEROLE = "assumerole";
  public static final String STS_ASSUMEROLEWITHWEBIDENTITY = "assumerolewithwebidentity";
  public static final String STS_DECODEAUTHORIZATIONMESSAGE = "decodeauthorizationmessage";
  public static final String STS_GETFEDERATIONTOKEN = "getfederationtoken";
  public static final String STS_GETSESSIONTOKEN = "getsessiontoken";

  public static final Set<String> STS_ACTIONS = new ImmutableSet.Builder<String>()
      .add( STS_ASSUMEROLE )
      .add( STS_ASSUMEROLEWITHWEBIDENTITY )
      .add( STS_DECODEAUTHORIZATIONMESSAGE )
      .add( STS_GETFEDERATIONTOKEN )
      .add( STS_GETSESSIONTOKEN )
      .build();

  // Auto Scaling actions, based on API Reference (API Version 2011-01-01)
  public static final String AUTOSCALING_CREATEAUTOSCALINGGROUP = "createautoscalinggroup";
  public static final String AUTOSCALING_CREATELAUNCHCONFIGURATION = "createlaunchconfiguration";
  public static final String AUTOSCALING_CREATEORUPDATESCALINGTRIGGER = "createorupdatescalingtrigger"; // deprecated
  public static final String AUTOSCALING_CREATEORUPDATETAGS = "createorupdatetags";
  public static final String AUTOSCALING_DELETEAUTOSCALINGGROUP = "deleteautoscalinggroup";
  public static final String AUTOSCALING_DELETELAUNCHCONFIGURATION = "deletelaunchconfiguration";
  public static final String AUTOSCALING_DELETENOTIFICATIONCONFIGURATION = "deletenotificationconfiguration";
  public static final String AUTOSCALING_DELETEPOLICY = "deletepolicy";
  public static final String AUTOSCALING_DELETESCHEDULEDACTION = "deletescheduledaction";
  public static final String AUTOSCALING_DELETETAGS = "deletetags";
  public static final String AUTOSCALING_DELETETRIGGER = "deletetrigger"; // deprecated
  public static final String AUTOSCALING_DESCRIBEADJUSTMENTTYPES = "describeadjustmenttypes";
  public static final String AUTOSCALING_DESCRIBEAUTOSCALINGGROUPS = "describeautoscalinggroups";
  public static final String AUTOSCALING_DESCRIBEAUTOSCALINGINSTANCES = "describeautoscalinginstances";
  public static final String AUTOSCALING_DESCRIBEAUTOSCALINGNOTIFICATIONTYPES = "describeautoscalingnotificationtypes";
  public static final String AUTOSCALING_DESCRIBELAUNCHCONFIGURATIONS = "describelaunchconfigurations";
  public static final String AUTOSCALING_DESCRIBEMETRICCOLLECTIONTYPES = "describemetriccollectiontypes";
  public static final String AUTOSCALING_DESCRIBENOTIFICATIONCONFIGURATIONS = "describenotificationconfigurations";
  public static final String AUTOSCALING_DESCRIBEPOLICIES = "describepolicies";
  public static final String AUTOSCALING_DESCRIBESCALINGACTIVITIES = "describescalingactivities";
  public static final String AUTOSCALING_DESCRIBESCALINGPROCESSTYPES = "describescalingprocesstypes";
  public static final String AUTOSCALING_DESCRIBESCHEDULEDACTIONS = "describescheduledactions";
  public static final String AUTOSCALING_DESCRIBETAGS = "describetags";
  public static final String AUTOSCALING_DESCRIBETERMINATIONPOLICYTYPES = "describeterminationpolicytypes";
  public static final String AUTOSCALING_DESCRIBETRIGGERS = "describetriggers"; // deprecated
  public static final String AUTOSCALING_DISABLEMETRICSCOLLECTION = "disablemetricscollection";
  public static final String AUTOSCALING_ENABLEMETRICSCOLLECTION = "enablemetricscollection";
  public static final String AUTOSCALING_EXECUTEPOLICY = "executepolicy";
  public static final String AUTOSCALING_PUTNOTIFICATIONCONFIGURATION = "putnotificationconfiguration";
  public static final String AUTOSCALING_PUTSCALINGPOLICY = "putscalingpolicy";
  public static final String AUTOSCALING_PUTSCHEDULEDUPDATEGROUPACTION = "putscheduledupdategroupaction";
  public static final String AUTOSCALING_RESUMEPROCESSES = "resumeprocesses";
  public static final String AUTOSCALING_SETDESIREDCAPACITY = "setdesiredcapacity";
  public static final String AUTOSCALING_SETINSTANCEHEALTH = "setinstancehealth";
  public static final String AUTOSCALING_SUSPENDPROCESSES = "suspendprocesses";
  public static final String AUTOSCALING_TERMINATEINSTANCEINAUTOSCALINGGROUP = "terminateinstanceinautoscalinggroup";
  public static final String AUTOSCALING_UPDATEAUTOSCALINGGROUP = "updateautoscalinggroup";

  public static final Set<String> AUTOSCALING_ACTIONS = new ImmutableSet.Builder<String>()
      .add( AUTOSCALING_CREATEAUTOSCALINGGROUP )
      .add( AUTOSCALING_CREATELAUNCHCONFIGURATION )
      .add( AUTOSCALING_CREATEORUPDATESCALINGTRIGGER )
      .add( AUTOSCALING_CREATEORUPDATETAGS )
      .add( AUTOSCALING_DELETEAUTOSCALINGGROUP )
      .add( AUTOSCALING_DELETELAUNCHCONFIGURATION )
      .add( AUTOSCALING_DELETENOTIFICATIONCONFIGURATION )
      .add( AUTOSCALING_DELETEPOLICY )
      .add( AUTOSCALING_DELETESCHEDULEDACTION )
      .add( AUTOSCALING_DELETETAGS )
      .add( AUTOSCALING_DELETETRIGGER )
      .add( AUTOSCALING_DESCRIBEADJUSTMENTTYPES )
      .add( AUTOSCALING_DESCRIBEAUTOSCALINGGROUPS )
      .add( AUTOSCALING_DESCRIBEAUTOSCALINGINSTANCES )
      .add( AUTOSCALING_DESCRIBEAUTOSCALINGNOTIFICATIONTYPES )
      .add( AUTOSCALING_DESCRIBELAUNCHCONFIGURATIONS )
      .add( AUTOSCALING_DESCRIBEMETRICCOLLECTIONTYPES )
      .add( AUTOSCALING_DESCRIBENOTIFICATIONCONFIGURATIONS )
      .add( AUTOSCALING_DESCRIBEPOLICIES )
      .add( AUTOSCALING_DESCRIBESCALINGACTIVITIES )
      .add( AUTOSCALING_DESCRIBESCALINGPROCESSTYPES )
      .add( AUTOSCALING_DESCRIBESCHEDULEDACTIONS )
      .add( AUTOSCALING_DESCRIBETAGS )
      .add( AUTOSCALING_DESCRIBETERMINATIONPOLICYTYPES )
      .add( AUTOSCALING_DESCRIBETRIGGERS )
      .add( AUTOSCALING_DISABLEMETRICSCOLLECTION )
      .add( AUTOSCALING_ENABLEMETRICSCOLLECTION )
      .add( AUTOSCALING_EXECUTEPOLICY )
      .add( AUTOSCALING_PUTNOTIFICATIONCONFIGURATION )
      .add( AUTOSCALING_PUTSCALINGPOLICY )
      .add( AUTOSCALING_PUTSCHEDULEDUPDATEGROUPACTION )
      .add( AUTOSCALING_RESUMEPROCESSES )
      .add( AUTOSCALING_SETDESIREDCAPACITY )
      .add( AUTOSCALING_SETINSTANCEHEALTH )
      .add( AUTOSCALING_SUSPENDPROCESSES )
      .add( AUTOSCALING_TERMINATEINSTANCEINAUTOSCALINGGROUP )
      .add( AUTOSCALING_UPDATEAUTOSCALINGGROUP )
      .build();

  //Cloud Watch actions, based on API Reference (API Version 2010-08-01)
  public static final String CLOUDWATCH_DELETEALARMS = "deletealarms";
  public static final String CLOUDWATCH_DESCRIBEALARMHISTORY = "describealarmhistory";
  public static final String CLOUDWATCH_DESCRIBEALARMS = "describealarms";
  public static final String CLOUDWATCH_DESCRIBEALARMSFORMETRIC = "describealarmsformetric";
  public static final String CLOUDWATCH_DISABLEALARMACTIONS = "disablealarmactions";
  public static final String CLOUDWATCH_ENABLEALARMACTIONS = "enablealarmactions";
  public static final String CLOUDWATCH_GETMETRICSTATISTICS = "getmetricstatistics";
  public static final String CLOUDWATCH_LISTMETRICS = "listmetrics";
  public static final String CLOUDWATCH_PUTMETRICALARM = "putmetricalarm";
  public static final String CLOUDWATCH_PUTMETRICDATA = "putmetricdata";
  public static final String CLOUDWATCH_SETALARMSTATE = "setalarmstate";
  
  public static final Set<String> CLOUDWATCH_ACTIONS = new ImmutableSet.Builder<String>()
      .add(CLOUDWATCH_DELETEALARMS)
      .add(CLOUDWATCH_DESCRIBEALARMHISTORY)
      .add(CLOUDWATCH_DESCRIBEALARMS)
      .add(CLOUDWATCH_DESCRIBEALARMSFORMETRIC)
      .add(CLOUDWATCH_DISABLEALARMACTIONS)
      .add(CLOUDWATCH_ENABLEALARMACTIONS)
      .add(CLOUDWATCH_GETMETRICSTATISTICS)
      .add(CLOUDWATCH_LISTMETRICS)
      .add(CLOUDWATCH_PUTMETRICALARM)
      .add(CLOUDWATCH_PUTMETRICDATA)
      .add(CLOUDWATCH_SETALARMSTATE)
      .build();
  
  //Load Balancing actions, based on API Reference (API Version 2012-06-01)
  public static final String LOADBALANCING_APPLYSECURITYGROUPSTOLOADBALANCER = "applysecuritygroupstoloadbalancer";
  public static final String LOADBALANCING_ATTACHLOADBALANCERTOSUBNETS = "attachLoadbalancertosubnets";
  public static final String LOADBALANCING_CONFIGUREHEALTHCHECK = "configurehealthcheck";
  public static final String LOADBALANCING_CREATEAPPCOOKIESTICKINESSPOLICY = "createappcookiestickinesspolicy";
  public static final String LOADBALANCING_CREATELBCOOKIESTICKINESSPOLICY = "createlbcookiestickinesspolicy";
  public static final String LOADBALANCING_CREATELOADBALANCER = "createloadbalancer";
  public static final String LOADBALANCING_CREATELOADBALANCERLISTENERS = "createloadbalancerlisteners";
  public static final String LOADBALANCING_CREATELOADBALANCERPOLICY = "createloadbalancerpolicy";
  public static final String LOADBALANCING_DELETELOADBALANCER = "deleteloadbalancer";
  public static final String LOADBALANCING_DELETELOADBALANCERLISTENERS = "deleteloadbalancerlisteners";
  public static final String LOADBALANCING_DELETELOADBALANCERPOLICY = "deleteloadbalancerpolicy";
  public static final String LOADBALANCING_DEREGISTERINSTANCESFROMLOADBALANCER = "deregisterinstancesfromloadbalancer";
  public static final String LOADBALANCING_DESCRIBEINSTANCEHEALTH = "describeinstancehealth";
  public static final String LOADBALANCING_DESCRIBELOADBALANCERPOLICIES = "describeloadbalancerpolicies";
  public static final String LOADBALANCING_DESCRIBELOADBALANCERPOLICYTYPES = "describeloadbalancerpolicytypes";
  public static final String LOADBALANCING_DESCRIBELOADBALANCERS = "describeloadbalancers";
  public static final String LOADBALANCING_DETACHLOABBALANCERFROMSUBNETS = "detachloadbalancerfromsubnets";
  public static final String LOADBALANCING_DISABLEAVAILABILITYZONESFORLOADBALANCER = "disableavailabilityzonesforloadbalancer";
  public static final String LOADBALANCING_ENABLEAVAILABILITYZONESFORLOADBALANCER = "enableavailabilityzonesforloadbalancer";
  public static final String LOADBALANCING_REGISTERINSTANCESWITHLOADBALANCER = "registerinstanceswithloadbalancer";
  public static final String LOADBALANCING_SETLOADBALANCERLISTENERSSLCERTIFICATE = "setloadbalancerlistenersslcertificate";
  public static final String LOADBALANCING_SETLOADBALANCERPOLICIESFORBACKENDSERVER = "setloadbalancerpoliciesforbackendserver";
  public static final String LOADBALANCING_SETLOADBALANCERPOLICIESOFLISTENER = "setloadbalancerpoliciesoflistener";

  // Non-AWS, Euca-specific ELB operations
  public static final String LOADBALANCING_DESCRIBELOADBALANCERSBYSERVO = "describeloadbalancersbyservo";
  public static final String LOADBALANCING_PUTSERVOSTATES = "putservostates";
  
  
  public static final Set<String> LOADBALANCING_ACTIONS = new ImmutableSet.Builder<String>()
      .add(LOADBALANCING_APPLYSECURITYGROUPSTOLOADBALANCER)
      .add(LOADBALANCING_ATTACHLOADBALANCERTOSUBNETS)
      .add(LOADBALANCING_CONFIGUREHEALTHCHECK)
      .add(LOADBALANCING_CREATEAPPCOOKIESTICKINESSPOLICY)
      .add(LOADBALANCING_CREATELBCOOKIESTICKINESSPOLICY)
      .add(LOADBALANCING_CREATELOADBALANCER)
      .add(LOADBALANCING_CREATELOADBALANCERLISTENERS)
      .add(LOADBALANCING_CREATELOADBALANCERPOLICY)
      .add(LOADBALANCING_DELETELOADBALANCER)
      .add(LOADBALANCING_DELETELOADBALANCERLISTENERS)
      .add(LOADBALANCING_DELETELOADBALANCERPOLICY)
      .add(LOADBALANCING_DEREGISTERINSTANCESFROMLOADBALANCER)
      .add(LOADBALANCING_DESCRIBEINSTANCEHEALTH)
      .add(LOADBALANCING_DESCRIBELOADBALANCERPOLICIES)
      .add(LOADBALANCING_DESCRIBELOADBALANCERPOLICYTYPES)
      .add(LOADBALANCING_DESCRIBELOADBALANCERS)
      .add(LOADBALANCING_DESCRIBELOADBALANCERSBYSERVO)
      .add(LOADBALANCING_DETACHLOABBALANCERFROMSUBNETS)
      .add(LOADBALANCING_DISABLEAVAILABILITYZONESFORLOADBALANCER)
      .add(LOADBALANCING_ENABLEAVAILABILITYZONESFORLOADBALANCER)
      .add(LOADBALANCING_PUTSERVOSTATES)
      .add(LOADBALANCING_REGISTERINSTANCESWITHLOADBALANCER)
      .add(LOADBALANCING_SETLOADBALANCERLISTENERSSLCERTIFICATE)
      .add(LOADBALANCING_SETLOADBALANCERPOLICIESFORBACKENDSERVER)
      .add(LOADBALANCING_SETLOADBALANCERPOLICIESOFLISTENER)
      .build();
  
  // Map vendor to actions
  public static final Map<String, Set<String>> VENDOR_ACTIONS = new ImmutableMap.Builder<String,Set<String>>()
    .put( VENDOR_IAM, IAM_ACTIONS )
    .put( VENDOR_EC2, EC2_ACTIONS )
    .put( VENDOR_S3, S3_ACTIONS )
    .put( VENDOR_STS, STS_ACTIONS )
    .put( VENDOR_AUTOSCALING, AUTOSCALING_ACTIONS )
    .put( VENDOR_CLOUDWATCH, CLOUDWATCH_ACTIONS)
    .put( VENDOR_LOADBALANCING, LOADBALANCING_ACTIONS)
    .build();

  // Map vendors to resource vendors
  public static final Map<String, Set<String>> VENDOR_RESOURCE_VENDORS = new ImmutableMap.Builder<String,Set<String>>()
      .put( VENDOR_STS, ImmutableSet.of( VENDOR_IAM ) )
      .build();

  // Set of vendors with case sensitive resource names
  public static final Set<String> VENDORS_CASE_SENSITIVE_RESOURCES = new ImmutableSet.Builder<String>()
      .add( VENDOR_IAM )
      .build();

  // Action syntax
  public static final Pattern ACTION_PATTERN = Pattern.compile( "\\*|(?:(" + VENDOR_IAM + "|" + VENDOR_EC2 + "|" + VENDOR_S3 + "|" + VENDOR_STS  + "|" + VENDOR_LOADBALANCING + "|" + VENDOR_AUTOSCALING + "|" + VENDOR_CLOUDWATCH + "):(\\S+))" );
  
  // Wildcard
  public static final String ALL_RESOURCE = "*";
  
  // IAM resource types
  public static final String IAM_RESOURCE_GROUP = "group";
  public static final String IAM_RESOURCE_USER = "user";
  public static final String IAM_RESOURCE_ROLE = "role";
  public static final String IAM_RESOURCE_INSTANCE_PROFILE = "instance-profile";

  // EC2 resource types, extension to AWS IAM
  public static final String EC2_RESOURCE_IMAGE = "image";
  public static final String EC2_RESOURCE_SECURITYGROUP = "securitygroup";
  public static final String EC2_RESOURCE_ADDRESS = "address";
  public static final String EC2_RESOURCE_AVAILABILITYZONE = "availabilityzone";
  public static final String EC2_RESOURCE_INSTANCE = "instance";
  public static final String EC2_RESOURCE_KEYPAIR = "keypair";
  public static final String EC2_RESOURCE_VOLUME = "volume";
  public static final String EC2_RESOURCE_SNAPSHOT = "snapshot";
  public static final String EC2_RESOURCE_VMTYPE = "vmtype";
  public static final String EC2_RESOURCE_TAG = "tag";
  
  public static final Set<String> EC2_RESOURCES = new ImmutableSet.Builder<String>()
    .add( EC2_RESOURCE_IMAGE )
    .add( EC2_RESOURCE_SECURITYGROUP )
    .add( EC2_RESOURCE_ADDRESS )
    .add( EC2_RESOURCE_AVAILABILITYZONE )
    .add( EC2_RESOURCE_INSTANCE )
    .add( EC2_RESOURCE_KEYPAIR )
    .add( EC2_RESOURCE_VOLUME )
    .add( EC2_RESOURCE_SNAPSHOT )
    .add( EC2_RESOURCE_VMTYPE )
    .add( EC2_RESOURCE_TAG )
    .build();

  public static final Pattern IPV4_ADDRESS_RANGE_PATTERN = Pattern.compile( "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:-(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))?" );
  
  // S3 resource types
  public static final String S3_RESOURCE_BUCKET = "bucket";
  public static final String S3_RESOURCE_OBJECT = "object";

  // Auto scaling resource types
  public static final String AUTOSCALING_RESOURCE_TAG = "tag";

  public static String qualifiedName( String vendor, String name ) {
    return vendor + ":" + name;
  }

  public static String vendor( final String qualifiedName ) {
    int index = qualifiedName.indexOf( ':' );
    if ( index <= 0 ) {
      throw new IllegalArgumentException( "Name not qualified: " + qualifiedName );
    }
    return qualifiedName.substring( 0, index );
  }

  public static boolean isPermittedResourceVendor( final String vendor, final String resourceVendor ) {
    final Set<String> resourceVendors = VENDOR_RESOURCE_VENDORS.get( vendor );
    return resourceVendors == null ?
        vendor.equals( resourceVendor ) :
        resourceVendors.contains( resourceVendor );
  }
  
  /**
   * Map request to policy language's action string.
   * 
   * @param request The request message
   * @return The IAM ARN action string.
   */
  public static String requestToAction( BaseMessage request ) {
    if ( request != null ) {
      PolicyAction action = Ats.from( request ).get( PolicyAction.class );
      if ( action != null ) {
        return action.action( );
      }
    }
    return null;
  }
  
  public static String objectFullName( String bucketName, String objectKey ) {
    if ( objectKey.startsWith( "/" ) ) {
      return bucketName + objectKey;
    } else {
      return bucketName + "/" + objectKey;
    }
  }
  
  public static String describeAction( final String vendor, final String resource ) {
    return "describe" + resource + "s";
  }

  public static String canonicalizeResourceName( final String type,
                                                 final String name ) {
    return VENDORS_CASE_SENSITIVE_RESOURCES.contains( vendor( type ) ) ?
        name :
        name.toLowerCase();
  }
  
}
