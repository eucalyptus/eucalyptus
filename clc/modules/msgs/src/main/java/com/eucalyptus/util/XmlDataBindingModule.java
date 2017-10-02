/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.util;

import java.io.IOException;
import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 */
public class XmlDataBindingModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings( "WeakerAccess" )
  public XmlDataBindingModule( final String name ) {
    super( name );
  }

  public XmlDataBindingModule( ) {
    this( "EucalyptusXmlModule" );
  }

  @Override
  public void setupModule( final SetupContext context ) {
    super.setupModule( context );
    context.appendAnnotationIntrospector( new IncludeOverridingAnnotationIntrospector( ) );
    context.addBeanDeserializerModifier( new NonNullBeanDeserializerModifier( ) );
  }

  private static final class NonNullBeanDeserializerModifier extends BeanDeserializerModifier {
    @Override
    public JsonDeserializer<?> modifyDeserializer(
        final DeserializationConfig config,
        final BeanDescription beanDesc,
        final JsonDeserializer<?> deserializer
    ) {
      JsonDeserializer<?> modifiedDeserializer = deserializer;
      if ( deserializer instanceof BeanDeserializer ) {
        modifiedDeserializer = NonNullBeanDeserializer.wrap( (BeanDeserializer) deserializer );
      }
      return super.modifyDeserializer( config, beanDesc, modifiedDeserializer );
    }
  }

  private static final class NonNullBeanDeserializer extends BeanDeserializer {
    private static final long serialVersionUID = 1L;

    NonNullBeanDeserializer( final BeanDeserializerBase src ) {
      super( src );
    }

    static NonNullBeanDeserializer wrap( final BeanDeserializer deserializer ) {
      final NonNullBeanDeserializer wrapped;
      if ( deserializer instanceof NonNullBeanDeserializer ) {
        wrapped = (NonNullBeanDeserializer) deserializer;
      } else {
        wrapped = new NonNullBeanDeserializer( deserializer );
      }
      return wrapped;
    }

    @Override
    public Object deserialize( final JsonParser p, final DeserializationContext ctxt ) throws IOException {
      Object value =  super.deserialize( p, ctxt );
      if ( value == null ) {
        value = this._valueInstantiator.createUsingDefault( ctxt );
      }
      return value;
    }
  }

  private static final class IncludeOverridingAnnotationIntrospector extends AnnotationIntrospector {
    private static final long serialVersionUID = 1L;

    @Override 
    public Version version( ) { 
      return Version.unknownVersion( ); 
    }

    @Override
    public JsonInclude.Value findPropertyInclusion( final Annotated a ) {
      // For collections, we serialize when non-empty to avoid setting to
      // null on deserialization when the collection is empty.
      // We cannot use non-empty for all as this omits zero values
      // causing nulls on deserialization
      return Collection.class.isAssignableFrom( a.getRawType( ) ) ?
          JsonInclude.Value.construct( JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY ) :
          JsonInclude.Value.construct( JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL );
    }
  }
}
