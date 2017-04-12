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

/**
 * IAM policy condition key provider for dynamic aws keys.
 */
public class AwsKeyProvider implements KeyProvider {

  private static final String REQUEST_TAG_CONDITION_PREFIX = "aws:requesttag/";

  @Override
  public String getName( ) {
    return "Aws";
  }

  @Override
  public boolean provides( final String name ) {
    return name != null && name.toLowerCase( ).startsWith( REQUEST_TAG_CONDITION_PREFIX );
  }

  @Override
  public Key getKey( final String name ) {
    final String tagKey = name.substring( REQUEST_TAG_CONDITION_PREFIX.length( ) );
    return new RequestTag( REQUEST_TAG_CONDITION_PREFIX + tagKey, tagKey );
  }

  @Override
  public Map<String, Key> getKeyInstances( final Key.EvaluationConstraint constraint ) {
    return Collections.emptyMap( );
  }
}
