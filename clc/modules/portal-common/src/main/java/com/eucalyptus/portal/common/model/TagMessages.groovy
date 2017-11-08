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
@GroovyAddClassUUID
package com.eucalyptus.portal.common.model

import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.portal.common.Tag
import com.eucalyptus.ws.WebServiceError
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import static com.eucalyptus.util.MessageValidation.validateRecursively

@ComponentMessage(Tag)
class TagMessage extends PortalBaseMessage {

  public Map<String,String> validate( ) {
    return validateRecursively(
        Maps.newTreeMap( ),
        new PortalBaseMessage.PortalBaseMessageValidationAssistant( ),
        "",
        this );
  }
}

class GetTagKeysType extends TagMessage {
}

class GetTagKeysResult extends EucalyptusData {
  ArrayList<String> keys = Lists.newArrayList( )
}

class GetTagKeysResponseType extends TagMessage {
  @JsonUnwrapped
  GetTagKeysResult result = new GetTagKeysResult()
}

class TagError extends EucalyptusData {
  String type
  String code
  String message
}

class TagErrorResponse extends TagMessage implements WebServiceError {
  String requestId
  ArrayList<TagError> error = new ArrayList<TagError>( )

  TagErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @Override
  String getWebServiceErrorCode( ) {
    error?.getAt(0)?.code
  }

  @Override
  String getWebServiceErrorMessage( ) {
    error?.getAt(0)?.message
  }
}
