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
