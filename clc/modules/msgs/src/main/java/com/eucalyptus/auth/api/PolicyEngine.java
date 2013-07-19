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

package com.eucalyptus.auth.api;

import static com.eucalyptus.auth.principal.Principal.PrincipalType;
import java.util.Map;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.Contract.Type;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;

public interface PolicyEngine {
  
  /**
   * Evaluate authorizations for a request to access a resource.
   * 
   * @param context Context for evaluation
   * @param resourceAccountNumber The account number for the resource
   * @param resourceName The name for the resource
   * @param contracts For output collected contracts
   * @throws AuthException If not authorized
   */
  void evaluateAuthorization( EvaluationContext context,
                              String resourceAccountNumber,
                              String resourceName,
                              Map<Type, Contract> contracts ) throws AuthException;

  /**
   * Evaluate authorizations for a request to access a resource.
   *
   * <p>This method is for resources that support attached policies.</p>
   *
   * @param context Context for evaluation
   * @param resourcePolicy The policy for the resource
   * @param resourceAccountNumber The account number for the resource
   * @param resourceName The name for the resource
   * @param contracts For output collected contracts
   * @throws AuthException If not authorized
   */
  void evaluateAuthorization( EvaluationContext context,
                              Policy resourcePolicy,
                              String resourceAccountNumber,
                              String resourceName,
                              Map<Type, Contract> contracts ) throws AuthException;

  /**
   * Evaluate quota for a request to allocate a resource.
   *
   * @param context Context for evaluation
   * @param resourceName The name of the resource
   * @param quantity The requested quantity
   * @throws AuthException If quota exceeded
   */
  void evaluateQuota( EvaluationContext context, String resourceName, Long quantity) throws AuthException;

  /**
   * Create a context for use in an evaluation.
   *
   * @param resourceType The type of the target resource
   * @param action The associated action
   * @param requestUser The user making the request
   * @return The context
   */
  EvaluationContext createEvaluationContext( String resourceType, String action, User requestUser );

  /**
   * Create a context for use in an evaluation.
   *
   * @param resourceType The type of the target resource
   * @param action The associated action
   * @param requestUser The user making the request
   * @param principalType The type of the principal
   * @param principalName The principal name
   * @return The context
   */
  EvaluationContext createEvaluationContext( String resourceType,
                                             String action,
                                             User requestUser,
                                             PrincipalType principalType,
                                             String principalName );

  /**
   * Context for a policy evaluation.
   *
   * <p>The context can cached information between requests. A new context
   * should be created for each evaluation if caching is not desired.</p>
   */
  interface EvaluationContext {
    String getResourceType( );
    String getAction( );
    User getRequestUser( );
    @Nullable
    PrincipalType getPrincipalType( );
    @Nullable
    String getPrincipalName( );
    String describe( String resourceAccountNumber, String resourceName );
    String describe( String resourceName, Long quantity );
  }    
}
