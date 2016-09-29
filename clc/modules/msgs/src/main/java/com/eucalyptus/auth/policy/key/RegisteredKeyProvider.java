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
package com.eucalyptus.auth.policy.key;

import java.util.Map;
import com.eucalyptus.util.Ordered;

/**
 * KeyProvider implementation for static (discovered and registered) keys.
 */
public class RegisteredKeyProvider implements KeyProvider, Ordered {

  @Override
  public String getName() {
    return "Registered";
  }

  @Override
  public boolean provides( final String name ) {
    return Keys.getKeyClass( name ) != null;
  }

  @Override
  public Key getKey( final String name ) {
    return Keys.getKeyInstance( Keys.getKeyClass( name ) );
  }

  @Override
  public Map<String, Key> getKeyInstances( final Key.EvaluationConstraint constraint ) {
    return Keys.getRegisteredKeyInstances( constraint );
  }

  @Override
  public int getOrder( ) {
    return -1000;
  }
}
