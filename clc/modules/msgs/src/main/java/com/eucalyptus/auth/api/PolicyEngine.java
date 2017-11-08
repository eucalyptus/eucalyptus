/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.api;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthEvaluationContext;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.Contract.Type;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.TypedPrincipal;
import com.eucalyptus.auth.principal.User;

public interface PolicyEngine {

  /**
   * Evaluate authorizations for a request to access a resource.
   *
   * @param context Context for evaluation
   * @param authorizationMatch The authorization matching to perform
   * @param resourceAccountNumber The account number for the resource
   * @param resourceName The name for the resource
   * @param contracts For output collected contracts
   * @throws AuthException If not authorized
   */
  void evaluateAuthorization( @Nonnull  AuthEvaluationContext context,
                              @Nonnull  AuthorizationMatch authorizationMatch,
                              @Nullable String resourceAccountNumber,
                              @Nonnull  String resourceName,
                              @Nonnull  Map<Type, Contract> contracts ) throws AuthException;

  /**
   * Evaluate authorizations for a request to access a resource.
   *
   * <p>This method is for resources that support attached policies.</p>
   *
   * @param context Context for evaluation
   * @param requestAccountDefaultAllow If requesting account has permission via external mechanism
   * @param resourcePolicy The policy for the resource
   * @param resourceAccountNumber The account number for the resource
   * @param resourceName The name for the resource
   * @param contracts For output collected contracts
   * @throws AuthException If not authorized
   */
  void evaluateAuthorization( AuthEvaluationContext context,
                              boolean requestAccountDefaultAllow,
                              PolicyVersion resourcePolicy,
                              String resourcePolicyAccountNumber,
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
  void evaluateQuota( AuthEvaluationContext context,
                      String resourceName,
                      Long quantity) throws AuthException;

  /**
   * Create a context for use in an evaluation.
   *
   * @param resourceType The type of the target resource
   * @param action The associated action
   * @param requestUser The user making the request
   * @param evaluatedKeys Evaluated IAM condition keys
   * @param policies The (non-resource) policies to use for authorization
   * @return The context
   */
  AuthEvaluationContext createEvaluationContext( String resourceType,
                                                 String action,
                                                 User requestUser,
                                                 Map<String,String> evaluatedKeys,
                                                 Iterable<PolicyVersion> policies );

  /**
   * Create a context for use in an evaluation.
   *
   * @param resourceType The type of the target resource
   * @param action The associated action
   * @param requestUser The user making the request
   * @param evaluatedKeys Evaluated IAM condition keys
   * @param policies The (non-resource) policies to use for authorization
   * @param principals The typed principals making the request
   * @return The context
   */
  AuthEvaluationContext createEvaluationContext( String resourceType,
                                                 String action,
                                                 User requestUser,
                                                 Map<String,String> evaluatedKeys,
                                                 Iterable<PolicyVersion> policies,
                                                 Set<TypedPrincipal> principals );

  enum AuthorizationMatch {
    /**
     * Full authorization matching, action, principal, resource and conditions.
     */
    All,

    /**
     * Authorization matching excluding resource and conditions.
     *
     * <p>This is used for checking some permission for an action.</p>
     *
     * <p>WARNING! With an unconditional match, authorization indicates that the
     * user MAY have permission to perform an action but a full authorization
     * check is still required to verify authorization.</p>
     */
    Unconditional,
  }

}
