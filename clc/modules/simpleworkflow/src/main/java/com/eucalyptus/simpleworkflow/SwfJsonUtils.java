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
package com.eucalyptus.simpleworkflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;
import com.eucalyptus.util.Exceptions;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

/**
 *
 */
public class SwfJsonUtils {

  private static final ObjectMapper mapper = new ObjectMapper( );
  static {
    final SimpleModule module = new SimpleModule( "SwfModule", new Version(1, 0, 0, null, null, null) )
              .addSerializer( Date.class, new EpochSecondsDateSerializer( )  )
              .addDeserializer( Date.class, new EpochSecondsDateDeserializer( ) );
    mapper.registerModule( module );
    mapper.setDateFormat( new EpochSecondsDateFormat() );
    mapper.addMixInAnnotations( SimpleWorkflowMessage.class, BindingMixIn.class );
    mapper.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );
    mapper.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );
    mapper.setVisibilityChecker(new VisibilityChecker.Std(VisibilityChecker.Std.class.getAnnotation(JsonAutoDetect.class)){
      private static final long serialVersionUID = 1L;
      @Override
      public boolean isSetterVisible( final Method m ) {
        return !( m.getParameterCount( ) == 1 && m.getParameterTypes( )[ 0 ].isEnum( ) ) && super.isSetterVisible( m );
      }
    });
    mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
  }

  public static String writeObjectAsString( final Object object ) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream( 512 );
    try {
      mapper.writeValue( out, object );
    } catch ( IOException ioe ) {
      throw Exceptions.toUndeclared( ioe );
    }
    return new String( out.toByteArray( ), Charsets.UTF_8 );
  }

  public static void writeObject( final OutputStream out, final Object object ) throws IOException {
    mapper.writeValue( out, object );
  }

  public static <T> T readObject( final String in, final Class<T> type ) throws IOException {
    final JsonParser parser = mapper.getFactory( ).createJsonParser( new StringReader( in ) {
      @Override public String toString() { return "message"; } // overridden for better source in error message
    } );
    final T result = mapper.readValue( parser, type );
    boolean trailingContent;
    try {
      trailingContent = parser.nextToken( ) != null;
    } catch ( IOException e ) {
      trailingContent = true;
    }
    if ( trailingContent ) {
      throw new IOException( "Unexpected trailing content at " + parser.getCurrentLocation( ) );
    }
    return result;
  }

  // TODO:STEVE: add unit test to ensure we don't start using unexpected properties from BaseMessage
  // ignore properties of BaseMessage
  @JsonIgnoreProperties( { "correlationId", "effectiveUserId", "reply", "statusMessage", "userId",
      "_disabledServices", "_notreadyServices", "_stoppedServices", "_epoch", "_services", "_return",
      "callerContext" } )
  private interface BindingMixIn {
  }

  private static final class EpochSecondsDateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize( final JsonParser jsonParser,
                             final DeserializationContext deserializationContext ) throws IOException {
      final JsonToken token = jsonParser.getCurrentToken( );
      switch ( token ) {
        case VALUE_NUMBER_FLOAT:
          return new Date( jsonParser.getDecimalValue( ).movePointRight( 3 ).longValue( ) );
        case VALUE_NUMBER_INT:
          return new Date( jsonParser.getLongValue( ) * 1000L );
        default:
          return new DateDeserializers.DateDeserializer( ).deserialize( jsonParser, deserializationContext );
      }
    }
  }

  private static final class EpochSecondsDateSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize( final Date date,
                           final JsonGenerator jsonGenerator,
                           final SerializerProvider serializerProvider ) throws IOException {
      jsonGenerator.writeRawValue( String.valueOf( date.getTime( ) / 1000 ) + "." + Strings.padStart( Long.toString( date.getTime( ) % 1000 ), 3, '0' ) );
    }
  }

  private static final class EpochSecondsDateFormat extends DateFormat implements Cloneable {
    private static final long serialVersionUID = 1L;

    @Override
    public StringBuffer format( final Date date, final StringBuffer toAppendTo, final FieldPosition fieldPosition ) {
      StringBuffer out = toAppendTo == null ? new StringBuffer( ) : toAppendTo;
      if ( date != null ) {
        out.append( date.getTime( ) / 1000 );
        out.append( '.' );
        out.append( Strings.padStart( Long.toString( date.getTime( ) % 1000 ), 3, '0' ) );
      }
      return out;
    }

    @Override
    public Date parse( final String source, final ParsePosition pos ) {
      if ( source != null ) try {
        Number number = DecimalFormat.getInstance( new Locale( "en" ) ).parse( source );
        pos.setIndex( source.length( ) ) ;
        return new Date( (long)(number.doubleValue() * 1000d) );
      } catch ( ParseException ignore ) {
      }
      return null;
    }

    @SuppressWarnings( "CloneDoesntCallSuperClone" )
    @Override
    public Object clone( ) {
      return this;
    }
  }
}
