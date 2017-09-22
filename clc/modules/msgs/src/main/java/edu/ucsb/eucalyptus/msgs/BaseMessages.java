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
package edu.ucsb.eucalyptus.msgs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.bootstrap.BillOfMaterials;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.NamespaceMappingXMLStreamReader;
import com.eucalyptus.util.NamespaceMappingXMLStreamWriter;
import com.eucalyptus.util.OMXMLStreamWriter;
import com.eucalyptus.util.XmlDataBindingModule;
import com.eucalyptus.ws.WebServiceError;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 *
 */
public class BaseMessages {

  private static final XmlMapper xmlMapper = Json.mapper( new XmlMapper( ), Json.JsonOption.IgnoreBaseMinimalMessage );
  private static final String xmlNamespace = "http://msgs.eucalyptus.com/" + BillOfMaterials.getVersion( );
  private static final ObjectMapper mapper = new ObjectMapper( );
  static {
    xmlMapper.registerModule( new XmlDataBindingModule( ) );
    xmlMapper.addMixIn( WebServiceError.class, WebServiceErrorMixIn.class );
    xmlMapper.configure( MapperFeature.AUTO_DETECT_IS_GETTERS, false );
    xmlMapper.configure( MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true );

    mapper.addMixIn( BaseMessage.class, BaseMessageMixIn.class);
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

  public static String toString( final BaseMessage message ) throws IOException {
    return xmlMapper.writeValueAsString( message );
  }

  public static void toStream( final BaseMessage message, final OutputStream out ) throws IOException {
    xmlMapper.writeValue( out, message );
  }

  public static OMElement toOm( final BaseMessage message ) throws IOException {
    HoldMe.canHas.lock( );
    try {
      final OMFactory factory = HoldMe.getOMFactory( );
      final OMDocument document = factory.createOMDocument( );
      final XMLStreamWriter wrtr = new NamespaceMappingXMLStreamWriter(
          new OMXMLStreamWriter( factory, document ),
          Collections.singletonMap( "", xmlNamespace ) );
      xmlMapper.writeValue( wrtr, message );
      return document.getOMDocumentElement( );
    } finally {
      HoldMe.canHas.unlock( );
    }
  }

  public static <T> T fromOm( final OMElement object, final Class<T> type ) throws IOException {
    final XMLStreamReader reader = new NamespaceMappingXMLStreamReader(
        object.getXMLStreamReader( ),
        Collections.singletonMap( xmlNamespace, "" ) );
    return xmlMapper.readValue( reader, type );
  }

  @JsonIgnoreProperties( { "correlationId", "effectiveUserId", "reply", "statusMessage", "userId" } )
  private static final class BaseMessageMixIn { }

  @JsonIgnoreProperties( { "webServiceErrorCode", "webServiceErrorMessage" } )
  private static final class WebServiceErrorMixIn { }
}
