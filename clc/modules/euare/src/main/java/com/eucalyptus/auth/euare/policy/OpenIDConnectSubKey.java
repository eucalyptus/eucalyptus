/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.auth.euare.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyEvaluationWriteContextKey;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes.RoleWithWebIdSecurityTokenAttributes;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypedKey;

/**
 * @see OpenIDConnectKeyProvider
 */
public class OpenIDConnectSubKey extends OpenIDConnectProviderKeySupport {

  private static final TypedKey<Pair<String,String>> OIDC_SUB_KEY = TypedKey.create( "OpenIDConnectSub" );
  public static final PolicyEvaluationWriteContextKey<Pair<String,String>> CONTEXT_KEY =
      PolicyEvaluationWriteContextKey.create( OIDC_SUB_KEY );

  public static final String SUFFIX = ":sub";

  public OpenIDConnectSubKey( final String name ) {
    super( name, SUFFIX );
  }

  @Override
  public String value( ) throws AuthException {
    return getValue( OIDC_SUB_KEY, RoleWithWebIdSecurityTokenAttributes::getSub );
  }
}
