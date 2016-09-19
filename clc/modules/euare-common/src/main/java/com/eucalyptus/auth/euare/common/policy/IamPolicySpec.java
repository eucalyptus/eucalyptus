/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.auth.euare.common.policy;

import com.eucalyptus.auth.policy.PolicySpec;

/**
 *
 */
public interface IamPolicySpec {

  String VENDOR_IAM = PolicySpec.VENDOR_IAM;

  // Resource types
  String IAM_RESOURCE_ACCOUNT = "account"; // eucalyptus administrative extension
  String IAM_RESOURCE_GROUP = "group";
  String IAM_RESOURCE_USER = PolicySpec.IAM_RESOURCE_USER;
  String IAM_RESOURCE_ROLE = PolicySpec.IAM_RESOURCE_ROLE;
  String IAM_RESOURCE_INSTANCE_PROFILE = PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE;
  String IAM_RESOURCE_SERVER_CERTIFICATE = PolicySpec.IAM_RESOURCE_SERVER_CERTIFICATE;
  String IAM_RESOURCE_ACCESS_KEY = "access-key";
  String IAM_RESOURCE_SIGNING_CERTIFICATE = "signing-certificate";
  String IAM_RESOURCE_OPENID_CONNECT_PROVIDER = PolicySpec.IAM_RESOURCE_OPENID_CONNECT_PROVIDER;

  // Condition keys
  String IAM_QUOTA_USER_NUMBER = "iam:quota-usernumber";
  String IAM_QUOTA_GROUP_NUMBER = "iam:quota-groupnumber";
  String IAM_QUOTA_ROLE_NUMBER = "iam:quota-rolenumber";
  String IAM_QUOTA_INSTANCE_PROFILE_NUMBER = "iam:quota-instanceprofilenumber";
  String IAM_QUOTA_SERVER_CERTIFICATE_NUMBER = "iam:quota-servercertificatenumber";
  String IAM_QUOTA_OPENID_CONNECT_PROVIDER_NUMBER = "iam:quota-openidconnectprovidernumber";

  // Actions
  String IAM_ADDCLIENTIDTOOPENIDCONNECTPROVIDER = "addclientidtoopenidconnectprovider";
  String IAM_ADDROLETOINSTANCEPROFILE = "addroletoinstanceprofile";
  String IAM_ADDUSERTOGROUP = "addusertogroup";
  String IAM_CHANGEPASSWORD = "changepassword";
  String IAM_CREATEACCESSKEY = "createaccesskey";
  String IAM_CREATEACCOUNT = "createaccount"; // eucalyptus administrative extension
  String IAM_CREATEACCOUNTALIAS = "createaccountalias";
  String IAM_CREATEGROUP = "creategroup";
  String IAM_CREATEINSTANCEPROFILE = "createinstanceprofile";
  String IAM_CREATELOGINPROFILE = "createloginprofile";
  String IAM_CREATEOPENIDCONNECTPROVIDER = "createopenidconnectprovider";
  String IAM_CREATEROLE = "createrole";
  String IAM_CREATEUSER = "createuser";
  String IAM_CREATEVIRTUALMFADEVICE = "createvirtualmfadevice";
  String IAM_DEACTIVATEMFADEVICE = "deactivatemfadevice";
  String IAM_DELETEACCESSKEY = "deleteaccesskey";
  String IAM_DELETEACCOUNT = "deleteaccount"; // eucalyptus administrative extension
  String IAM_DELETEACCOUNTALIAS = "deleteaccountalias";
  String IAM_DELETEACCOUNTPASSWORDPOLICY = "deleteaccountpasswordpolicy";
  String IAM_DELETEACCOUNTPOLICY = "deleteaccountpolicy"; // eucalyptus administrative extension
  String IAM_DELETEGROUP = "deletegroup";
  String IAM_DELETEGROUPPOLICY = "deletegrouppolicy";
  String IAM_DELETEINSTANCEPROFILE = "deleteinstanceprofile";
  String IAM_DELETELOGINPROFILE = "deleteloginprofile";
  String IAM_DELETEOPENIDCONNECTPROVIDER = "deleteopenidconnectprovider";
  String IAM_DELETEROLE = "deleterole";
  String IAM_DELETEROLEPOLICY = "deleterolepolicy";
  String IAM_DELETESERVERCERTIFICATE = "deleteservercertificate";
  String IAM_DELETESIGNINGCERTIFICATE = "deletesigningcertificate";
  String IAM_DELETEUSER = "deleteuser";
  String IAM_DELETEUSERPOLICY = "deleteuserpolicy";
  String IAM_DELETEVIRTUALMFADEVICE = "deletevirtualmfadevice";
  String IAM_ENABLEMFADEVICE = "enablemfadevice";
  String IAM_GETACCOUNTPASSWORDPOLICY = "getaccountpasswordpolicy";
  String IAM_GETACCOUNTPOLICY = "getaccountpolicy"; // eucalyptus administrative extension
  String IAM_GETACCOUNTSUMMARY = "getaccountsummary";
  String IAM_GETGROUP = "getgroup";
  String IAM_GETGROUPPOLICY = "getgrouppolicy";
  String IAM_GETINSTANCEPROFILE = "getinstanceprofile";
  String IAM_GETLOGINPROFILE = "getloginprofile";
  String IAM_GETOPENIDCONNECTPROVIDER = "getopenidconnectprovider";
  String IAM_GETROLE = "getrole";
  String IAM_GETROLEPOLICY = "getrolepolicy";
  String IAM_GETSERVERCERTIFICATE = "getservercertificate";
  String IAM_GETUSER = "getuser";
  String IAM_GETUSERPOLICY = "getuserpolicy";
  String IAM_LISTACCESSKEYS = "listaccesskeys";
  String IAM_LISTACCOUNTS = "listaccounts"; // eucalyptus administrative extension
  String IAM_LISTACCOUNTALIASES = "listaccountaliases";
  String IAM_LISTACCOUNTPOLICIES = "listaccountpolicies"; // eucalyptus administrative extension
  String IAM_LISTGROUPPOLICIES = "listgrouppolicies";
  String IAM_LISTGROUPS = "listgroups";
  String IAM_LISTGROUPSFORUSER = "listgroupsforuser";
  String IAM_LISTINSTANCEPROFILES = "listinstanceprofiles";
  String IAM_LISTINSTANCEPROFILESFORROLE = "listinstanceprofilesforrole";
  String IAM_LISTMFADEVICES = "listmfadevices";
  String IAM_LISTOPENIDCONNECTPROVIDERS = "listopenidconnectproviders";
  String IAM_LISTROLEPOLICIES = "listrolepolicies";
  String IAM_LISTROLES = "listroles";
  String IAM_LISTSERVERCERTIFICATES = "listservercertificates";
  String IAM_LISTSIGNINGCERTIFICATES = "listsigningcertificates";
  String IAM_LISTUSERPOLICIES = "listuserpolicies";
  String IAM_LISTUSERS = "listusers";
  String IAM_LISTVIRTUALMFADEVICES = "listvirtualmfadevices";
  String IAM_PASSROLE = "passrole";
  String IAM_PUTACCOUNTPOLICY = "putaccountpolicy"; // eucalyptus administrative extension
  String IAM_PUTGROUPPOLICY = "putgrouppolicy";
  String IAM_PUTROLEPOLICY = "putrolepolicy";
  String IAM_PUTUSERPOLICY = "putuserpolicy";
  String IAM_REMOVECLIENTIDFROMOPENIDCONNECTPROVIDER = "removeclientidfromopenidconnectprovider";
  String IAM_REMOVEROLEFROMINSTANCEPROFILE = "removerolefrominstanceprofile";
  String IAM_REMOVEUSERFROMGROUP = "removeuserfromgroup";
  String IAM_RESYNCMFADEVICE = "resyncmfadevice";
  String IAM_UPDATEACCESSKEY = "updateaccesskey";
  String IAM_UPDATEACCOUNTPASSWORDPOLICY = "updateaccountpasswordpolicy";
  String IAM_UPDATEASSUMEROLEPOLICY = "updateassumerolepolicy";
  String IAM_UPDATEGROUP = "updategroup";
  String IAM_UPDATELOGINPROFILE = "updateloginprofile";
  String IAM_UPDATEOPENIDCONNECTPROVIDERTHUMBPRINT = "updateopenidconnectproviderthumbprint";
  String IAM_UPDATESERVERCERTIFICATE = "updateservercertificate";
  String IAM_UPDATESIGNINGCERTIFICATE = "updatesigningcertificate";
  String IAM_UPDATEUSER = "updateuser";
  String IAM_UPLOADSERVERCERTIFICATE = "uploadservercertificate";
  String IAM_UPLOADSIGNINGCERTIFICATE = "uploadsigningcertificate";
  // IAM actions extension for internal use by eucalyptus
  String IAM_DOWNLOADSERVERCERTIFICATE = "downloadservercertificate";
  String IAM_DOWNLOADCLOUDCERTIFICATE = "downloadcloudcertificate";
}
