/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.persistence.Entity;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Classes;

public class MultiDatabasePropertyEntry extends AbstractConfigurableProperty implements ConfigurableProperty {
  private static Logger LOG = Logger.getLogger( MultiDatabasePropertyEntry.class );
  private Method        setIdentifier;
  private Field         identifierField;
  private String        identifierValue;
  private String        identifiedMethodName;
  
  public MultiDatabasePropertyEntry( Class definingClass, String entrySetName, Field field, Field identifierField, String description, String defaultValue,
                                     PropertyTypeParser typeParser,
                                     Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias, String identifierValue,
                                     PropertyChangeListener changeListener ) {
    super( definingClass, entrySetName, field, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias, changeListener );
    this.identifierField = identifierField;
    this.identifiedMethodName = identifierField.getName( ).substring( 0, 1 ).toUpperCase( ) + identifierField.getName( ).substring( 1 );
    this.identifierValue = identifierValue;
  }
  
  private Method lookupSetIdentifierMethod( ) {
    try {
      Method setMethod = this.getDefiningClass( ).getMethod( "set" + this.identifiedMethodName, this.identifierField.getType( ) );
      setMethod.setAccessible( true );
      return setMethod;
    } catch ( Exception ex ) {
      throw new RuntimeException( "Failed to obtain reference to method for setting the identifier field: " + this.identifierField.getName( ) + "/"
                                  + this.identifiedMethodName + " in type " + this.getDefiningClass( ).getSimpleName( ) );
    }
  }
  
  protected Object getQueryObject( ) throws Exception {
    Object queryObject = super.getDefiningClass( ).newInstance( );
    try {
      setIdentifier = ( setIdentifier != null )
        ? setIdentifier
          : this.lookupSetIdentifierMethod( );
      setIdentifier.invoke( queryObject, identifierValue );
    } catch ( Exception e1 ) {
      try {
        this.lookupSetIdentifierMethod( ).invoke( queryObject, identifierValue );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return ex.getMessage( );
      }
    }
    return queryObject;
  }

  @Override
  protected Object getInitialObject( ) throws Exception {
    return getInitialObjectByAnnotation( );
  }

  public static class DatabasePropertyBuilder implements ConfigurablePropertyBuilder {
    
    @Override
    public ConfigurableProperty buildProperty( Class c, Field f ) throws ConfigurablePropertyException {
      if ( c.isAnnotationPresent( Entity.class ) &&
           !( ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class ) ).singleton( ) ) {
        ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
        Field identifierField = findIdentifierField( c );//GRZE:NOTE: had to refactor to also look in class hierarchy
        if ( identifierField == null ) {
          return null;
        }
        if ( f.isAnnotationPresent( ConfigurableField.class ) ) {
          LOG.trace( "Checking field: " + c.getName( ) + "." + f.getName( ) );
          ConfigurableField annote = f.getAnnotation( ConfigurableField.class );
          String fqPrefix = classAnnote.root( );
          String alias = classAnnote.alias( );
          String description = annote.description( );
          String defaultValue = configurableFieldInitial( annote );
          PropertyTypeParser p = PropertyTypeParser.get( f.getType( ) );
          PropertyChangeListener listener = PropertyChangeListeners.getListenerFromClass( annote.changeListener( ) );
          try {
            if ( !Modifier.isStatic( f.getModifiers( ) ) && !f.isAnnotationPresent( Transient.class ) ) {
              boolean readOnly = Modifier.isFinal( f.getModifiers( ) ) || annote.readonly( );
              ConfigurableProperty prop = new MultiDatabasePropertyEntry( c, fqPrefix, f, identifierField, description, defaultValue, p, readOnly,
                                                                          annote.displayName( ), annote.type( ), alias, null, listener );
              return prop;
            }
          } catch ( Exception e ) {
            LOG.debug( e, e );
            return null;
          }
        }
      } else {
        return null;
      }
      return null;
    }

    private Field findIdentifierField( Class c ) throws SecurityException {
      for ( Class ancestor : Classes.classAncestors( c ) ) {
        for ( Field field : ancestor.getDeclaredFields( ) ) {
          if ( field.isAnnotationPresent( ConfigurableIdentifier.class ) ) {
            return field;
          }
        }
      }
      return null;
    }
    
  }
  
  public String getIdentifiedMethodName( ) {
    return this.identifiedMethodName;
  }
  
  public void setIdentifiedMethodName( String identifiedMethodName ) {
    this.identifiedMethodName = identifiedMethodName;
  }
  
  public void setIdentifierValue( String value ) {
    identifierValue = value;
  }
  
  @Override
  public String getEntrySetName( ) {
    if ( identifierValue != null )
      return identifierValue + "." + super.getEntrySetName( );
    else return super.getEntrySetName( );
  }
  
  @Override
  public String getQualifiedName( ) {
    if ( identifierValue != null )
      return identifierValue + "." + super.getQualifiedName( );
    else return super.getQualifiedName( );
  }
  
  public MultiDatabasePropertyEntry getClone( String idValue ) {
    return new MultiDatabasePropertyEntry( this.getDefiningClass( ), this.getEntrySetName( ), this.getField( ), identifierField, this.getDescription( ),
                                           this.getDefaultValue( ), this.getTypeParser( ), this.getReadOnly( ),
                                           this.getDisplayName( ), this.getWidgetType( ), this.getAlias( ), idValue, this.getChangeListener( ) );
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
  
  @Override
  public int hashCode( ) {
    return getQualifiedName( ).hashCode( );
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj )
      return true;
    if ( obj == null )
      return false;
    if ( getClass( ) != obj.getClass( ) )
      return false;
    MultiDatabasePropertyEntry other = ( MultiDatabasePropertyEntry ) obj;
    return this.compareTo( other ) == 0;
  }
  
}
