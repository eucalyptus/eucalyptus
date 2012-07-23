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
import java.util.Date;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Fields;

public class StaticPropertyEntry extends AbstractConfigurableProperty {
  static Logger LOG = Logger.getLogger( StaticPropertyEntry.class );
  private Field field;
  private Date  lastLoad;
  
  private StaticPropertyEntry( Class definingClass, String entrySetName, Field field, String description, String defaultValue,
                                              PropertyTypeParser typeParser,
                                              Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias,
                                              PropertyChangeListener changeListener ) {
    super( definingClass, entrySetName, field, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias, changeListener );
    this.field = field;
  }
  
  private String getFieldCanonicalName( ) {
    return this.getField( ).getDeclaringClass( ).getCanonicalName( ) + "." + this.getFieldName( );
//    if ( this.field.getType( ).isPrimitive( ) ) {
//      throw new CoderMalfunctionError( new UnsupportedDataTypeException( "Unsupported usage of @Configurable on a primitive field: "
//                                                                         + field.getDeclaringClass( ) + "." + field.getName( ) ) );
//    }
  }
  
  public Field getField( ) {
    return this.field;
  }
  
  @Override
  public String getValue( ) {
    try {
      StaticDatabasePropertyEntry dbEntry = StaticDatabasePropertyEntry.lookup( this.getFieldCanonicalName(),
                                                                                this.getQualifiedName( ),
                                                                                this.safeGetFieldValue( )
                                                                       );
      if ( this.lastLoad == null || this.lastLoad.before( dbEntry.getLastUpdateTimestamp( ) ) ) {
        this.lastLoad = dbEntry.getLastUpdateTimestamp( );
        String fieldValue = this.safeGetFieldValue( );
        if ( fieldValue.equals( dbEntry.getValue( ) ) ) {
          return fieldValue;
        } else {
          Object o = super.getTypeParser( ).apply( dbEntry.getValue( ) );
          if ( !Modifier.isFinal( this.field.getModifiers( ) ) ) {
            this.fireChange( dbEntry.getValue( ) );
            this.field.set( null, o );
            Logs.extreme( ).trace( "--> Set property value:  " + super.getQualifiedName( ) + " to " + dbEntry.getValue( ) );
          }
        }
      }
      return dbEntry.getValue( );
    } catch ( IllegalAccessException e ) {
      Logs.exhaust( ).trace( e, e );
      return super.getDefaultValue( );
    } catch ( Exception e ) {
      LOG.warn( "Failed to get property: " + super.getQualifiedName( ) + " because of " + e.getMessage( ) );
      Logs.extreme( ).debug( e, e );
      return super.getDefaultValue( );
    }
  }
  
  private String safeGetFieldValue( ) {
    try {
      Object o = this.field.get( null );
      if ( o == null ) {
        return super.getDefaultValue( );
      } else {
        return o.toString( );
      }
    } catch ( Exception ex ) {
      return super.getDefaultValue( );
    }
  }
  
  @Override
  public String setValue( String s ) {
    if ( Modifier.isFinal( this.field.getModifiers( ) ) ) {
      return "failed to assign final field: " + super.getQualifiedName( );
    } else if ( Bootstrap.isFinished( ) ) {
      try {
        Object o = super.getTypeParser( ).apply( s );
        this.fireChange( s );
        StaticDatabasePropertyEntry.update( this.getFieldCanonicalName( ), this.getQualifiedName( ), s );
        this.field.set( null, o );
        Logs.extreme( ).trace( "--> Set property value:  " + super.getQualifiedName( ) + " to " + s );
      } catch ( Exception e ) {
        LOG.warn( "Failed to set property: " + super.getQualifiedName( ) + " because of " + e.getMessage( ) );
        Logs.extreme( ).debug( e, e );
      }
      return this.getValue( );
    } else {
      return super.getDefaultValue( );
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
  
  public static class StaticPropertyBuilder implements ConfigurablePropertyBuilder {
    
    @Override
    public ConfigurableProperty buildProperty( Class c, Field field ) throws ConfigurablePropertyException {
      Ats classAts = Ats.from( c );
      Ats fieldAts = Ats.from( field );
      if ( classAts.has( ConfigurableClass.class ) && fieldAts.has( ConfigurableField.class ) ) {
        ConfigurableClass classAnnote = classAts.get( ConfigurableClass.class );
        ConfigurableField annote = fieldAts.get( ConfigurableField.class );
        String description = annote.description( );
        String defaultValue = annote.initial( );
        String fqPrefix = classAnnote.root( );
        String fq = fqPrefix + "." + field.getName( ).toLowerCase( );
        String alias = classAnnote.alias( );
        PropertyTypeParser p = PropertyTypeParser.get( field.getType( ) );
        ConfigurableProperty entry = null;
        Class<? extends PropertyChangeListener> changeListenerClass = annote.changeListener( );
        PropertyChangeListener changeListener;
        if ( !changeListenerClass.equals( NoopEventListener.class ) ) {
          if ( changeListenerClass.isEnum( ) ) {
            changeListener = changeListenerClass.getEnumConstants( )[0];
          }
          try {
            changeListener = changeListenerClass.newInstance( );
          } catch ( Throwable e ) {
            changeListener = NoopEventListener.NOOP;
          }
        } else {
          changeListener = NoopEventListener.NOOP;
        }
        int modifiers = field.getModifiers( );
        if ( Modifier.isPublic( modifiers ) && Modifier.isStatic( modifiers ) ) {
          entry = new StaticPropertyEntry( c, fqPrefix, field, description, defaultValue, p, annote.readonly( ), annote.displayName( ), annote.type( ), alias,
                                           changeListener );
          return entry;
        }
      }
      return null;
    }
  }
  
  /**
   * @see com.eucalyptus.configurable.AbstractConfigurableProperty#getQueryObject()
   */
  @Override
  protected Object getQueryObject( ) throws Exception {
    return null;
  }
  
  @Override
  public boolean isDeferred( ) {
    return true;
  }
  
}
