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
package com.eucalyptus.cloudformation.common.policy;

/**
 *
 */
public interface CloudFormationPolicySpec {

  String VENDOR_CLOUDFORMATION = "cloudformation";

  //Cloud Formation actions, based on API Reference (API Version 2010-05-15)
  String CLOUDFORMATION_CANCELUPDATESTACK = "cancelupdatestack";
  String CLOUDFORMATION_CREATESTACK = "createstack";
  String CLOUDFORMATION_DELETESTACK = "deletestack";
  String CLOUDFORMATION_DESCRIBESTACKEVENTS = "describestackevents";
  String CLOUDFORMATION_DESCRIBESTACKRESOURCE = "describestackresource";
  String CLOUDFORMATION_DESCRIBESTACKRESOURCES = "describestackresources";
  String CLOUDFORMATION_DESCRIBESTACKS = "describestacks";
  String CLOUDFORMATION_ESTIMATETEMPLATECOST = "estimatetemplatecost";
  String CLOUDFORMATION_GETSTACKPOLICY = "getstackpolicy";
  String CLOUDFORMATION_GETTEMPLATE = "gettemplate";
  String CLOUDFORMATION_GETTEMPLATESUMMARY = "gettemplatesummary";
  String CLOUDFORMATION_LISTSTACKRESOURCES = "liststackresources";
  String CLOUDFORMATION_LISTSTACKS = "liststacks";
  String CLOUDFORMATION_SETSTACKPOLICY = "setstackpolicy";
  String CLOUDFORMATION_UPDATESTACK = "updatestack";
  String CLOUDFORMATION_VALIDATETEMPLATE = "validatetemplate";  
}
