/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Entity;

public class SingletonDatabasePropertyEntry extends AbstractConfigurableProperty implements ConfigurableProperty {
  private static Logger LOG = Logger.getLogger( SingletonDatabasePropertyEntry.class );
  
  public SingletonDatabasePropertyEntry( Class definingClass, String entrySetName, Field field, String description, String defaultValue,
                                         PropertyTypeParser typeParser,
                                         Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias ) {
    super( definingClass, entrySetName, field, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias );
  }
  
  @Override
  protected Object getQueryObject( ) throws Exception {
    return this.getNoArgConstructor( ).newInstance( );
  }
  
  public static class DatabasePropertyBuilder implements ConfigurablePropertyBuilder {
    
    @Override
    public ConfigurableProperty buildProperty( Class c, Field f ) throws ConfigurablePropertyException {
      if ( c.isAnnotationPresent( Entity.class ) &&
           ( ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class ) ).singleton( ) &&
           f.isAnnotationPresent( ConfigurableField.class ) ) {
        LOG.trace( "Checking field: " + c.getName( ) + "." + f.getName( ) );//REVIEW: lowered this to trace.. sorry.
        ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
        ConfigurableField annote = f.getAnnotation( ConfigurableField.class );
        String fqPrefix = classAnnote.root( );
        String alias = classAnnote.alias( );
        String description = annote.description( );
        String defaultValue = annote.initial( );
        PropertyTypeParser p = PropertyTypeParser.get( f.getType( ) );
        try {
          if ( !Modifier.isStatic( f.getModifiers( ) ) && !f.isAnnotationPresent( Transient.class ) ) {
            ConfigurableProperty prop = new SingletonDatabasePropertyEntry( c, fqPrefix, f, description, defaultValue, p, annote.readonly( ),
                                                                            annote.displayName( ), annote.type( ), alias );
            return prop;
          }
        } catch ( Exception e ) {
          LOG.debug( e, e );
          return null;
        }
      } else {
        return null;
      }
      return null;
    }
    
  }
  
  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo( ConfigurableProperty that ) {
    return this.getQualifiedName( ) != null
      ? this.getQualifiedName( ).compareTo( that.getQualifiedName( ) )
      : ( that.getQualifiedName( ) == null
        ? 0
        : -1 );
  }
  
}
