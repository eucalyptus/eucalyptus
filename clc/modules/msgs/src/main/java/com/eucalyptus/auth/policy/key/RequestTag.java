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

import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.HasTags;
import net.sf.json.JSONException;

/**
 *
 */
public class RequestTag implements AwsKey {
  @Nonnull private final String name;
  @Nonnull private final String tagKey;

  public RequestTag( @Nonnull final String name, @Nonnull final String tagKey ) {
    this.name = Assert.notNull( name, "name" );
    this.tagKey = Assert.notNull( tagKey, "tagKey" );
  }

  @Override
  public String name( ) {
    return name;
  }

  @Override
  public String value( ) throws AuthException {
    return getTagValue( );
  }

  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( name( ) + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  private String getTagValue( ) throws AuthException {
    try {
      final BaseMessage request = Contexts.lookup( ).getRequest( );
      final String value;
      if ( request instanceof HasTags ) {
        value = ( (HasTags) request ).getTagValue(
            PolicyResourceContext.getResourceType( ),
            PolicyResourceContext.getResourceId( ),
            tagKey
        );
      } else {
        throw new AuthException( "Error getting value for request tag condition" );
      }
      return value;
    } catch ( Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for request tag condition", e );
    }
  }
}
