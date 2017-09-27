/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
