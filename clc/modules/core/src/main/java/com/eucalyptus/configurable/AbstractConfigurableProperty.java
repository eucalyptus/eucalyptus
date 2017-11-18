/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;

public abstract class AbstractConfigurableProperty implements ConfigurableProperty {
  
  private static Logger                LOG = Logger.getLogger( AbstractConfigurableProperty.class );
  private final String                 entrySetName;
  private final String                 fieldName;
  private final String                 qualifiedName;
  private final String                 description;
  private final PropertyTypeParser     typeParser;
  private final String                 defaultValue;
  private final Class                  definingClass;
  private final Constructor            noArgConstructor;
  private final Boolean                readOnly;
  private final String                 displayName;
  private final ConfigurableFieldType  widgetType;
  private final String                 alias;
  private final PropertyChangeListener changeListener;
  private final Field                  field;
  private final Method                 getter;
  private final Method                 setter;
  private final Class[]                setArgs;
  private final boolean deferred;
  
  public AbstractConfigurableProperty( Class definingClass, String entrySetName, Field field, String defaultValue, String description,
                                       PropertyTypeParser typeParser, Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias ) {
    this( definingClass, entrySetName, field, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias,
          NoopEventListener.NOOP );
  }
  
  public AbstractConfigurableProperty( Class definingClass, String entrySetName, Field field, String defaultValue, String description,
                                       PropertyTypeParser typeParser, Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias,
                                       PropertyChangeListener changeListener ) {
    this.definingClass = definingClass;
    this.field = field;
    this.fieldName = this.field.getName( ).toLowerCase( );
    this.entrySetName = entrySetName.toLowerCase( );
    this.qualifiedName = this.entrySetName + "." + this.fieldName;
    this.description = description;
    this.typeParser = typeParser;
    this.defaultValue = defaultValue;
    this.readOnly = readOnly;
    this.displayName = displayName;
    this.widgetType = widgetType;
    this.alias = alias;
    this.changeListener = changeListener;
    Constructor cons = null;
    try {
      cons = this.definingClass.getConstructor( new Class[] {} );
      cons.setAccessible( true );
    } catch ( Exception ex ) {
      if ( !Modifier.isStatic( field.getModifiers( ) ) ) {
        LOG.debug( "Known declared constructors: " + this.getDefiningClass( ).getDeclaredConstructors( ) );
        LOG.debug( "Known constructors: " + this.getDefiningClass( ).getConstructors( ) );
        LOG.debug( ex, ex );
        throw new RuntimeException( ex );
      } else {
        //that a default no-arg constructor is required indicates there is too much specialized junk in here.
      }
    }
    this.noArgConstructor = cons;
    this.setArgs = new Class[] { this.field.getType( ) };
    this.getter = this.getReflectedMethod( "get", this.field );
    this.setter = this.getReflectedMethod( "set", this.field, this.setArgs );
    ConfigurableClass configurableAnnot = ( ConfigurableClass ) definingClass.getAnnotation( ConfigurableClass.class );
    this.deferred = configurableAnnot.deferred( );
  }
  
  private Method getReflectedMethod( String namePrefix, Field field, Class... setArgs2 ) {
    try {
      String name = namePrefix + this.field.getName( ).substring( 0, 1 ).toUpperCase( ) + this.field.getName( ).substring( 1 );
      Method m = definingClass.getDeclaredMethod( name, setArgs2 );
      m.setAccessible( true );
      return m;
    } catch ( Exception e ) {
      if( !Modifier.isStatic( field.getModifiers( ) ) ) {
        LOG.debug( "Known declared methods: " + this.getDefiningClass( ).getDeclaredMethods( ) );
        LOG.debug( "Known methods: " + this.getDefiningClass( ).getMethods( ) );
        LOG.debug( e, e );
      }
      return null;
    }
  }
  
  protected abstract Object getQueryObject( ) throws Exception;

  protected Object getInitialObject( ) throws Exception { return null; };

  public String getFieldName( ) {
    return this.fieldName;
  }
  
  public String getEntrySetName( ) {
    return this.entrySetName;
  }
  
  public String getQualifiedName( ) {
    return this.qualifiedName;
  }
  
  public String getDescription( ) {
    return this.description;
  }
  
  public PropertyTypeParser getTypeParser( ) {
    return this.typeParser;
  }
  
  public String getDefaultValue( ) {
    return this.defaultValue;
  }

  public String getValue( ) {	  
    try ( final TransactionResource trans = Entities.transactionFor( this.getDefiningClass( ) ) ) {
    	//Unique result gets first found value if multiple exist, should work if all are kept in sync
        Object o = Entities.uniqueResult( this.getQueryObject( ) );
        Object prop = this.getter.invoke( o );
        String result = prop != null
          ? prop.toString( )
          : "";
        trans.commit( );
        return result;
    } catch (Exception e) {
      Logs.exhaust().error(e, e);
         return "";
    }
  }
  
  public String setValue( String s ) throws ConfigurablePropertyException {
    try ( final TransactionResource trans = Entities.transactionFor( this.getDefiningClass( ) ) ) {
      //This should return all matching objects
      List<Object> resultList = Entities.query( this.getQueryObject( ) );
      Object prop = this.getTypeParser( ).apply( s );

      if(resultList == null || resultList.size() == 0) {
        Object initial = this.getInitialObject( );
        if ( initial != null ) {
          resultList = Collections.singletonList( Entities.persist( initial ) );
        } else {
          throw new EucalyptusCloudException( "Property '" + getQualifiedName( ) +
              "' is not ready to be changed. Make sure that you have all needed modules loaded." );
        }
      }

      this.fireChange( prop ); //Fire change only once
      LOG.debug("Running setters.");

      for(Object obj : resultList) {
        this.setter.invoke( obj, prop );
      }
      trans.commit( );
      return s;
    } catch ( Exception e ) {
      Logs.exhaust( ).error( e, e );
      Exceptions.findAndRethrow( e, ConfigurablePropertyException.class );
      throw new ConfigurablePropertyException( e.getMessage( ), e );
    }
  }
  
  public Class getDefiningClass( ) {
    return this.definingClass;
  }
  
  public Constructor getNoArgConstructor( ) {
    return this.noArgConstructor;
  }
  
  public String getDisplayName( ) {
    return this.displayName;
  }
  
  public ConfigurableFieldType getWidgetType( ) {
    return this.widgetType;
  }
  
  public String getAlias( ) {
    return this.alias;
  }
  
  protected void fireChange( Object newValue ) throws ConfigurablePropertyException {
    if ( !NoopEventListener.class.equals( this.changeListener.getClass( ) ) ) {
      this.changeListener.fireChange( this, newValue );
    }
  }
  
  @Override
  public Boolean getReadOnly( ) {
    return this.readOnly;
  }
  
  public PropertyChangeListener getChangeListener( ) {
    return this.changeListener;
  }
  
  public Field getField( ) {
    return this.field;
  }

  public boolean isDeferred( ) {
    return this.deferred;
  }

  @Nullable
  protected Object getInitialObjectByAnnotation( ) throws Exception {
    final Method init = findInitMethod( getDefiningClass( ) );
    if ( init == null ) {
      return null;
    }
    init.setAccessible( true );
    final Object initial = getQueryObject( );
    init.invoke( initial );
    return initial;
  }

  @Nullable
  protected static Method findInitMethod( @Nonnull final Class definingClass ) throws SecurityException {
    for ( final Class ancestor : Classes.classAncestors( definingClass ) ) {
      for ( final Method method : ancestor.getDeclaredMethods( ) ) {
        if ( method.isAnnotationPresent( ConfigurableInit.class ) ) {
          return method;
        }
      }
    }
    return null;
  }

  @Nonnull
  protected static String configurableFieldInitial( @Nonnull ConfigurableField configurableField ) {
    return
        configurableField.initialInt( ) == Integer.MIN_VALUE ?
            configurableField.initial( ) :
            String.valueOf( configurableField.initialInt( ) );
  }
}
