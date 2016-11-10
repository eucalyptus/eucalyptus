/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package edu.ucsb.eucalyptus.msgs;

import java.io.IOException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 *
 */
public class BaseMessages {

  private static final ObjectMapper mapper = new ObjectMapper( );
  static {
    mapper.addMixInAnnotations( BaseMessage.class, BaseMessageMixIn.class);
    mapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );
  }

  @SuppressWarnings( "unchecked" )
  public static <T extends BaseMessage> T deepCopy( final T message ) throws IOException {
    return (T) deepCopy( message,  message.getClass( ) );
  }

  public static <T extends BaseMessage, R extends BaseMessage> R deepCopy(
      final T message,
      final Class<R> resultType
  ) throws IOException {
    return (R) mapper.treeToValue( mapper.valueToTree( message ), resultType );
  }

  @JsonIgnoreProperties( { "correlationId", "effectiveUserId", "reply", "statusMessage", "userId" } )
  private static final class BaseMessageMixIn { }
}
