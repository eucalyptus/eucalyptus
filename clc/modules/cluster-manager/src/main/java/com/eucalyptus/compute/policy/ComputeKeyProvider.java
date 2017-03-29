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
package com.eucalyptus.compute.policy;

import java.util.Collections;
import java.util.Map;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.KeyProvider;

/**
 * IAM policy condition key provider for dynamic compute keys.
 */
public class ComputeKeyProvider implements KeyProvider {

  private static final String RESOURCE_TAG_CONDITION_PREFIX = "ec2:resourcetag/";

  @Override
  public String getName( ) {
    return "Compute";
  }

  @Override
  public boolean provides( final String name ) {
    return name != null && name.toLowerCase( ).startsWith( RESOURCE_TAG_CONDITION_PREFIX );
  }

  @Override
  public Key getKey( final String name ) {
    return new ResourceTagKey( name.toLowerCase( ), name.substring( RESOURCE_TAG_CONDITION_PREFIX.length( ) ) );
  }

  @Override
  public Map<String, Key> getKeyInstances( final Key.EvaluationConstraint constraint ) {
    return Collections.emptyMap( );
  }
}
