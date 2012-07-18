/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.auth.api;

import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.Contract.Type;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;

public interface PolicyEngine {
  
  /**
   * Evaluate authorizations for a request to access a resource.
   * 
   * @param resourceType
   * @param resourceName
   * @param resourceAccount
   * @param action
   * @param requestUser
   * @param contracts For output collected contracts
   * @throws AuthException
   */
  public void evaluateAuthorization( String resourceType, String resourceName, Account resourceAccount, String action, User requestUser, Map<Type, Contract> contracts ) throws AuthException;
  
  /**
   * Evaluate quota for a request to allocate a resource.
   * 
   * @param resourceType
   * @param resourceName
   * @param action
   * @param requestUser
   * @param quantity
   * @throws AuthException
   */
  public void evaluateQuota( String resourceType, String resourceName, String action, User requestUser, Long quantity) throws AuthException;

}
