/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.auth.policy.key;

import java.util.Collections;
import java.util.Map;
import com.eucalyptus.auth.policy.variable.PolicyVariables;

/**
 * Key provider that exposes policy variables as condition keys.
 */
public class PolicyVariableKeyProvider implements KeyProvider {

  @Override
  public String getName( ) {
    return "PolicyVariable";
  }

  @Override
  public boolean provides( final String name ) {
    return name != null && name.contains( ":" ) && name.length( ) > 2 && PolicyVariables.isValid( name );
  }

  @Override
  public Key getKey( final String name ) {
    return new PolicyVariableKey( name );
  }

  @Override
  public Map<String, Key> getKeyInstances( final Key.EvaluationConstraint constraint ) {
    return Collections.emptyMap( );
  }
}
