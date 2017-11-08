/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare.principal;

import java.util.Date;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.BaseRole;

/**
 *
 */
public interface EuareRole extends BaseRole, EuareAccountScopedPrincipal {

  EuareAccount getAccount( ) throws AuthException;

  Policy getAssumeRolePolicy( ) throws AuthException;
  Policy setAssumeRolePolicy( String policy ) throws AuthException, PolicyParseException;

  List<EuareInstanceProfile> getInstanceProfiles() throws AuthException;

  Date getCreationTimestamp( );

  List<Policy> getPolicies( ) throws AuthException;

  /**
   * Add a policy, fail if exists.
   */
  Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException;

  /**
   * Add or update the named policy.
   */
  Policy putPolicy( String name, String policy ) throws AuthException, PolicyParseException;
  void removePolicy( String name ) throws AuthException;


  List<EuareManagedPolicy> getAttachedPolicies() throws AuthException;
  void attachPolicy( EuareManagedPolicy policy ) throws AuthException;
  void detachPolicy( EuareManagedPolicy policy ) throws AuthException;
}
