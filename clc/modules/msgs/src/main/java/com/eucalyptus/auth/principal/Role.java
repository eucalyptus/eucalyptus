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
 ************************************************************************/
package com.eucalyptus.auth.principal;

import java.util.Date;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;

/**
 *
 */
public interface Role extends AuthorizedPrincipal {

  String getRoleId( );

  String getPath( );

  String getSecret( );

  Policy getAssumeRolePolicy( ) throws AuthException;
  Policy setAssumeRolePolicy( String policy ) throws AuthException, PolicyParseException;

  List<InstanceProfile> getInstanceProfiles() throws AuthException;

  Date getCreationTimestamp();
}
