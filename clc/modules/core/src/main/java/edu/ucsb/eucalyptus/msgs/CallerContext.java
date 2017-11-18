/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package edu.ucsb.eucalyptus.msgs;

import java.util.Map;
import java.util.stream.Collectors;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.context.Context;
import com.google.common.collect.Lists;

/**
 * Context for propagation of identity / authorization parameters
 */
public class CallerContext {

  private final String identity; // user, role, etc
  private final boolean privileged;
  private final Map<String,String> evaluatedKeys;

  public CallerContext( final Context context ) throws AuthException {
    identity = context.getUser( ).getAuthenticatedId( );
    privileged = context.isPrivileged( );
    evaluatedKeys = context.evaluateKeys( );
  }

  public void apply( final BaseMessage message ) {
    message.setUserId( identity );
    if ( privileged ) {
      message.markPrivileged( );
    }
    message.setCallerContext( new BaseCallerContext(
        evaluatedKeys.entrySet( ).stream( )
            .filter( entry -> entry.getValue( ) != null )
            .map( entry -> new EvaluatedIamConditionKey( entry.getKey( ), entry.getValue() ) )
            .collect( Collectors.toCollection( Lists::newArrayList ) ) ) );
  }
}
