/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare.principal;

import java.util.Date;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.type.RestrictedType;

/**
 *
 */
@PolicyVendor( IamPolicySpec.VENDOR_IAM )
@PolicyResourceType( IamPolicySpec.IAM_RESOURCE_POLICY )
public interface EuareManagedPolicy extends Policy, RestrictedType {

  String getAccountNumber( );

  EuareAccount getAccount( ) throws AuthException;

  String getPolicyId( );

  Integer getPolicyVersion( );

  void setPolicyVersion( Integer version ) throws AuthException;

  String getName( );

  String getPath( );

  String getDescription( );

  String getText( );

  Date getCreateDate( );

  Date getUpdateDate( );

  Integer getAttachmentCount( );

  EuareManagedPolicyVersion addPolicyVersion( String text, boolean setAsDefault ) throws AuthException;

  void deletePolicyVersion( Integer versionId ) throws AuthException;

  List<EuareManagedPolicyVersion> getVersions( ) throws AuthException;

  List<EuareGroup> getGroups( ) throws AuthException;

  List<EuareRole> getRoles( ) throws AuthException;

  List<EuareUser> getUsers( ) throws AuthException;
}
