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
import com.eucalyptus.auth.policy.key.Key.EvaluationConstraint;

/**
 * KeyProvider is implemented by providers of dynamic condition keys.
 *
 * KeyProviders can implement Ordered to control priority.
 *
 * KeyProviders must have a public no argument constructor.
 *
 * Static condition keys need only implement Key and annotate with @PolicyKey
 *
 * @see Key
 * @see PolicyKey
 * @see com.eucalyptus.util.Ordered
 */
public interface KeyProvider {

  /**
   * Unique name for the provider
   */
  String getName( );

  /**
   * Does this provider support keys with the given name.
   *
   * @param name The condition key name
   * @return True if supported
   */
  boolean provides( String name );

  /**
   * Get the key for the given name.
   *
   * NOTE: this can be at policy parse time so should not rely on any
   * particular context.
   *
   * @param name The condition key name
   * @return The key implementation
   */
  Key getKey( String name );

  /**
   * Get instances for keys implementing the given constraint.
   *
   * This will be called within the context of a request.
   *
   * @param constraint The key constraint.
   * @return The key instances, may be empty, never null
   */
  Map<String,Key> getKeyInstances( final EvaluationConstraint constraint );
}
