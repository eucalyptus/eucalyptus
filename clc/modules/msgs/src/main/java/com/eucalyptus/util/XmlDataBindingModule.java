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

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 */
public class XmlDataBindingModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

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
  }
  
  private static final class IncludeOverridingAnnotationIntrospector extends AnnotationIntrospector {
    private static final long serialVersionUID = 1L;

    @Override 
    public Version version( ) { 
      return Version.unknownVersion( ); 
    }
    
    @Override 
    public JsonInclude.Include findSerializationInclusion(
      final Annotated a,
      final JsonInclude.Include defValue
    ) {
      // For collections, we serialize when non-empty to avoid setting to
      // null on deserialization when the collection is empty.
      // We cannot use non-empty for all as this omits zero values
      // causing nulls on deserialization
      return Collection.class.isAssignableFrom( a.getRawType( ) ) ?
          JsonInclude.Include.NON_EMPTY :
          JsonInclude.Include.NON_NULL;
    }
  }
}
