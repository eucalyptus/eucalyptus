/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
