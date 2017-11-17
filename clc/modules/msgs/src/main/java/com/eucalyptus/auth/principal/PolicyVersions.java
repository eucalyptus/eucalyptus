/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.principal;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.util.CompatFunction;
import com.google.common.base.MoreObjects;
import com.google.common.io.BaseEncoding;

/**
 *
 */
public class PolicyVersions {

  public static PolicyVersion getAdministratorPolicy( ) {
    return ADMINISTRATOR;
  }

  public static CompatFunction<Policy, PolicyVersion> policyVersion( final PolicyScope scope, final String scopeArn ) {
    return sourcePolicy -> new PolicyVersionImpl(
        scopeArn + "/policy/" + sourcePolicy.getName( ) + "," + String.valueOf( sourcePolicy.getPolicyVersion( ) ),
        sourcePolicy.getName( ),
        scope,
        sourcePolicy.getText( ),
        hash( sourcePolicy.getText( ) ) );
  }

  /**
   * Policy version for a managed policy.
   */
  public static PolicyVersion policyVersion( final Policy sourcePolicy, final String policyArn ) {
    return new PolicyVersionImpl(
        policyArn + "," + String.valueOf( sourcePolicy.getPolicyVersion( ) ),
        sourcePolicy.getName( ),
        PolicyScope.Managed,
        sourcePolicy.getText( ),
        hash( sourcePolicy.getText( ) ) );
  }

  public static String hash( String text ) {
    return BaseEncoding.base64( ).encode( Digest.SHA256.digestBinary( text.getBytes( StandardCharsets.UTF_8 ) ) );
  }

  private static final String ADMINISTRATOR_POLICY =
      "{\n" +
      "  \"Version\": \"2012-10-17\",\n" +
      "  \"Statement\": [{\n" +
      "    \"Effect\": \"Allow\",\n" +
      "    \"Action\": [\"*\"],\n" +
      "    \"Resource\": [\"*\"]\n" +
      "  }]\n" +
      "}";
  private static final PolicyVersion ADMINISTRATOR = new PolicyVersionImpl(
      "arn:aws:iam::eucalyptus:policy/Administrator,1",
      "Administrator",
      PolicyScope.User,
      ADMINISTRATOR_POLICY,
      hash( ADMINISTRATOR_POLICY ) );

  private static class PolicyVersionImpl implements PolicyVersion {

    PolicyVersionImpl(
        final String policyVersionId,
        final String policyName,
        final PolicyScope policyScope,
        final String policy,
        final String policyHash
    ) {
      this.policyVersionId = policyVersionId;
      this.policyName = policyName;
      this.policyScope = policyScope;
      this.policy = policy;
      this.policyHash = policyHash;
    }

    public String getPolicyVersionId( ) {
      return policyVersionId;
    }

    public String getPolicyName( ) {
      return policyName;
    }

    public PolicyScope getPolicyScope( ) {
      return policyScope;
    }

    public String getPolicy( ) {
      return policy;
    }

    public String getPolicyHash( ) {
      return policyHash;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final PolicyVersionImpl that = (PolicyVersionImpl) o;
      return Objects.equals( getPolicyVersionId( ), that.getPolicyVersionId( ) ) &&
          Objects.equals( getPolicyName( ), that.getPolicyName( ) ) &&
          getPolicyScope( ) == that.getPolicyScope( ) &&
          Objects.equals( getPolicy( ), that.getPolicy( ) ) &&
          Objects.equals( getPolicyHash( ), that.getPolicyHash( ) );
    }

    @Override
    public int hashCode( ) {
      return Objects.hash( getPolicyVersionId( ), getPolicyName( ), getPolicyScope( ), getPolicy( ), getPolicyHash( ) );
    }

    @Override
    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "policyVersionId", policyVersionId )
          .add( "policyName", policyName )
          .add( "policyScope", policyScope )
          .add( "policy", policy )
          .add( "policyHash", policyHash )
          .toString( );
    }

    private final String policyVersionId;
    private final String policyName;
    private final PolicyScope policyScope;
    private final String policy;
    private final String policyHash;
  }
}
