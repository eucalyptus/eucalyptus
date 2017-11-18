/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.policy.key;

import java.util.Collections;
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
  default Map<String,Key> getKeyInstances( final EvaluationConstraint constraint ) {
    return Collections.emptyMap( );
  }
}
