/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

/**
 * NOTE: Please do not add service specific IAM policy details here.
 */
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
	// Do not add vendors here (use modules)
  public static final String VENDOR_IAM = "iam";
  public static final String VENDOR_EC2 = "ec2";
  public static final String VENDOR_S3 = "s3";
  public static final String VENDOR_STS = "sts";
  public static final String VENDOR_IMAGINGSERVICE = "eucaimaging";
	// Do not add vendors here (use modules)

  public static final String ALL_PRINCIPALS = "*";

  public static final String ALL_ACTION = "*";

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
  public static final String EC2_DESCRIBEINSTANCETYPES = "describeinstancetypes";
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
  public static final String EC2_MIGRATEINSTANCES = "migrateinstances";  // eucalyptus administrative extension
  public static final String EC2_MODIFYIMAGEATTRIBUTE = "modifyimageattribute";
  public static final String EC2_MODIFYINSTANCEATTRIBUTE = "modifyinstanceattribute";
  public static final String EC2_MODIFYNETWORKINTERFACEATTRIBUTE = "modifynetworkinterfaceattribute";
  public static final String EC2_MODIFYRESERVEDINSTANCES = "modifyreservedinstances";
  public static final String EC2_MODIFYSNAPSHOTATTRIBUTE = "modifysnapshotattribute";
  public static final String EC2_MODIFYVOLUMEATTRIBUTE = "modifyvolumeattribute";
  public static final String EC2_MODIFYVMTYPE = "modifyvmtype";  // eucalyptus administrative extension
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
  public static final String S3_GETBUCKETTAGGING = "getbuckettagging";
  public static final String S3_GETLIFECYCLECONFIGURATION = "getlifecycleconfiguration";
  public static final String S3_GETOBJECT = "getobject";
  public static final String S3_GETOBJECTACL = "getobjectacl";
  public static final String S3_GETOBJECTTORRENT = "getobjecttorrent";
  public static final String S3_GETOBJECTVERSION = "getobjectversion";
  public static final String S3_GETOBJECTVERSIONACL = "getobjectversionacl";
  public static final String S3_GETOBJECTVERSIONTORRENT = "getobjectversiontorrent";
  public static final String S3_HEADOBJECT = "headobject";
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
  public static final String S3_PUTBUCKETTAGGING = "putbuckettagging";
  public static final String S3_PUTLIFECYCLECONFIGURATION = "putlifecycleconfiguration";
  public static final String S3_PUTOBJECT = "putobject";
  public static final String S3_PUTOBJECTACL = "putobjectacl";
  public static final String S3_PUTOBJECTVERSIONACL = "putobjectversionacl";
  public static final String S3_RESTOREOBJECT = "restoreobject";

  // STS actions, based on IAM Using Temporary Security Credentials version 2011-06-15
  public static final String STS_ASSUMEROLE = "assumerole";
  public static final String STS_ASSUMEROLEWITHWEBIDENTITY = "assumerolewithwebidentity";
  public static final String STS_DECODEAUTHORIZATIONMESSAGE = "decodeauthorizationmessage";
  public static final String STS_GETACCESSTOKEN = "getaccesstoken"; // eucalyptus extension
  public static final String STS_GETCALLERIDENTITY = "getcalleridentity";
  public static final String STS_GETFEDERATIONTOKEN = "getfederationtoken";
  public static final String STS_GETIMPERSONATIONTOKEN = "getimpersonationtoken"; // eucalyptus extension
  public static final String STS_GETSESSIONTOKEN = "getsessiontoken";

  // Euca-specific Imaging Service operations
  public static final String IMAGINGSERVICE_PUTINSTANCEIMPORTTASKSTATUS = "putinstanceimporttaskstatus";
  public static final String IMAGINGSERVICE_GETINSTANCEIMPORTTASK = "getinstanceimporttask";

  // Map vendors to resource vendors
  public static final Map<String, Set<String>> VENDOR_RESOURCE_VENDORS = new ImmutableMap.Builder<String,Set<String>>()
      .put( VENDOR_STS, ImmutableSet.of( VENDOR_IAM ) )
      .build();

  // Set of vendors with case insensitive resource names
  public static final Set<String> VENDORS_CASE_INSENSITIVE_RESOURCES = new ImmutableSet.Builder<String>()
      .add( VENDOR_EC2 )
      .build();

  // Action syntax
  public static final Pattern ACTION_PATTERN = Pattern.compile( "\\*|(?:([a-z0-9]+):(\\S+))" );

  // Wildcard
  public static final String ALL_RESOURCE = "*";

  // IAM resource types (see IamPolicySpec for all resources)
  public static final String IAM_RESOURCE_USER = "user";
  public static final String IAM_RESOURCE_ROLE = "role";
  public static final String IAM_RESOURCE_INSTANCE_PROFILE = "instance-profile";
  public static final String IAM_RESOURCE_SERVER_CERTIFICATE = "server-certificate";
  public static final String IAM_RESOURCE_OPENID_CONNECT_PROVIDER = "oidc-provider";

  // EC2 resource types, extension to AWS IAM
  public static final String EC2_RESOURCE_IMAGE = "image";
  public static final String EC2_RESOURCE_SECURITYGROUP = "security-group";
  public static final String EC2_RESOURCE_ADDRESS = "address";
  public static final String EC2_RESOURCE_AVAILABILITYZONE = "availabilityzone";
  public static final String EC2_RESOURCE_INSTANCE = "instance";
  public static final String EC2_RESOURCE_KEYPAIR = "key-pair";
  public static final String EC2_RESOURCE_VOLUME = "volume";
  public static final String EC2_RESOURCE_SNAPSHOT = "snapshot";
  public static final String EC2_RESOURCE_VMTYPE = "vmtype";
  public static final String EC2_RESOURCE_TAG = "tag";
  public static final String EC2_RESOURCE_PLACEMENTGROUP = "placement-group";
  public static final String EC2_RESOURCE_CUSTOMERGATEWAY = "customer-gateway";
  public static final String EC2_RESOURCE_DHCPOPTIONS = "dhcp-options";
  public static final String EC2_RESOURCE_INTERNETGATEWAY = "internet-gateway";
  public static final String EC2_RESOURCE_NETWORKACL = "network-acl";
  public static final String EC2_RESOURCE_NETWORKINTERFACE = "network-interface";
  public static final String EC2_RESOURCE_ROUTETABLE = "route-table";
  public static final String EC2_RESOURCE_SUBNET = "subnet";
  public static final String EC2_RESOURCE_VPCPEERINGCONNECTION = "vpc-peering-connection";
  public static final String EC2_RESOURCE_VPC = "vpc";


  public static final Set<String> EC2_RESOURCES = new ImmutableSet.Builder<String>()
    .add( EC2_RESOURCE_IMAGE )
    .add( EC2_RESOURCE_SECURITYGROUP )
    .add( EC2_RESOURCE_SECURITYGROUP.replace( "-", "" ) ) // no '-' until v4.1
    .add( EC2_RESOURCE_ADDRESS )
    .add( EC2_RESOURCE_AVAILABILITYZONE )
    .add( EC2_RESOURCE_INSTANCE )
    .add( EC2_RESOURCE_KEYPAIR )
    .add( EC2_RESOURCE_KEYPAIR.replace( "-", "" ) ) // no '-' until v4.1
    .add( EC2_RESOURCE_VOLUME )
    .add( EC2_RESOURCE_SNAPSHOT )
    .add( EC2_RESOURCE_VMTYPE )
    .add( EC2_RESOURCE_TAG )
    .add( EC2_RESOURCE_PLACEMENTGROUP )
    .add( EC2_RESOURCE_CUSTOMERGATEWAY )
    .add( EC2_RESOURCE_DHCPOPTIONS )
    .add( EC2_RESOURCE_INTERNETGATEWAY )
    .add( EC2_RESOURCE_NETWORKACL )
    .add( EC2_RESOURCE_NETWORKINTERFACE )
    .add( EC2_RESOURCE_ROUTETABLE )
    .add( EC2_RESOURCE_SUBNET )
    .add( EC2_RESOURCE_VPCPEERINGCONNECTION )
    .add( EC2_RESOURCE_VPC )
    .build();

  public static final Pattern IPV4_ADDRESS_RANGE_PATTERN = Pattern.compile( "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:-(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))?" );

  // S3 resource types
  public static final String S3_RESOURCE_BUCKET = "bucket";
  public static final String S3_RESOURCE_OBJECT = "object";

  public static String qualifiedName( String vendor, String name ) {
    return name == null ? null : vendor + ":" + name;
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

  public static String describeAction( final String vendor, final String resource ) {
    return "describe" + resource + "s";
  }

  public static String canonicalizeResourceName( final String type,
                                                 final String name ) {
    return type == null || !VENDORS_CASE_INSENSITIVE_RESOURCES.contains( vendor( type ) ) ?
        name :
        name.toLowerCase();
  }

}
