/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.common.msgs;

import java.lang.reflect.Method;
import java.util.Map;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.util.MessageValidation;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( LoadBalancing.class )
public class LoadBalancingMessage extends BaseMessage implements MessageValidation.ValidatableMessage {

  public static ResponseMetadata getResponseMetadata( final BaseMessage message ) {
    try {
      Method responseMetadataMethod = message.getClass( ).getMethod( "getResponseMetadata" );
      return ( (ResponseMetadata) responseMetadataMethod.invoke( message ) );
    } catch ( Exception e ) {
      return null;
    }

  }

  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    final TYPE type = super.getReply( );
    final ResponseMetadata responseMetadata = getResponseMetadata( type );
    if ( responseMetadata != null ) {
      responseMetadata.setRequestId( type.getCorrelationId( ) );
    }
    return type;
  }

  public Map<String, String> validate( ) {
    return MessageValidation.validateRecursively(
        Maps.newTreeMap( ),
        new LoadBalancingMessageValidation.LoadBalancingMessageValidationAssistant( ),
        "",
        this );
  }
}
