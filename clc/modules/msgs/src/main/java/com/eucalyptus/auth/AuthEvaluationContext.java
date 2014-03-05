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
package com.eucalyptus.auth;

import java.util.Map;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.auth.principal.User;

/**
 * Context for a policy evaluation.
 *
 * <p>The context can cache information between evaluations. A new context
 * should be created for each evaluation if caching is not desired.</p>
 */
public interface AuthEvaluationContext {
  String getResourceType();
  String getAction();
  User getRequestUser();
  Map<String,String> getEvaluatedKeys();
  @Nullable
  Principal.PrincipalType getPrincipalType();
  @Nullable
  String getPrincipalName();
  String describe( String resourceAccountNumber, String resourceName );
  String describe( String resourceName, Long quantity );
}
