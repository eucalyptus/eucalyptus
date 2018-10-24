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

package com.eucalyptus.config;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.component.annotation.ServiceOperation;
import com.eucalyptus.config.msgs.DescribePropertiesResponseType;
import com.eucalyptus.config.msgs.DescribePropertiesType;
import com.eucalyptus.config.msgs.ModifyPropertyValueResponseType;
import com.eucalyptus.config.msgs.ModifyPropertyValueType;
import com.eucalyptus.config.msgs.Property;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

@ComponentNamed
public class PropertiesManager {
  private static Logger LOG = Logger.getLogger( PropertiesManager.class );

  @ServiceOperation( hostDispatch = true )
  public enum ModifyProperty implements Function<ModifyPropertyValueType, ModifyPropertyValueResponseType> {
    INSTANCE;

    @Override
    public ModifyPropertyValueResponseType apply( final ModifyPropertyValueType input ) {
      try {
        return PropertiesManager.modifyProperty( input );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

  }


  @ServiceOperation( hostDispatch = true )
  public enum DescribeProperties implements Function<DescribePropertiesType, DescribePropertiesResponseType> {
    INSTANCE;

    @Override
    public DescribePropertiesResponseType apply( final DescribePropertiesType input ) {
      try {
        return PropertiesManager.describeProperties( input );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

  }


  public static DescribePropertiesResponseType describeProperties( final DescribePropertiesType request ) throws EucalyptusCloudException {
    DescribePropertiesResponseType reply = request.getReply( );
    List<Property> props = reply.getProperties( );
    final Predicate<ConfigurableProperty> filter = new Predicate<ConfigurableProperty>( ) {
      public boolean apply( final ConfigurableProperty input ) {
        if ( request.getProperties( ).isEmpty( ) ) {
          return true;
        } else if ( request.getProperties( ).contains( input.getQualifiedName( ) ) ) {
          return true;
        } else {
          for ( String propRequest : request.getProperties( ) ) {
            if ( input.getQualifiedName( ).startsWith( propRequest ) ) {
              return true;
            }
          }
        }
        return false;
      }
    };
    for ( ConfigurableProperty entry : Iterables.filter( PropertyDirectory.getPropertyEntrySet( ), filter ) ) {
      if ( filter.apply( entry ) ) {
        String value = "********";
        if ( !entry.getWidgetType( ).equals( ConfigurableFieldType.KEYVALUEHIDDEN ) )
          value = entry.getValue( );
        props.add( new Property( entry.getQualifiedName( ), value, entry.getDescription( ), entry.getDefaultValue(), entry.getReadOnly() ) );
      }
    }
    return reply;
  }
  private static final String INTERNAL_OP = "euca";
  public static ModifyPropertyValueResponseType modifyProperty( ModifyPropertyValueType request ) throws EucalyptusCloudException {
    ModifyPropertyValueResponseType reply = request.getReply( );
    if( INTERNAL_OP.equals( request.getName( ) ) ) {
      if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
        throw new EucalyptusCloudException( "You are not authorized to interact with this service." );
      }
      LOG.debug( "Performing euca operation: \n" + request.getValue( ) );
      try {
        reply.setName( INTERNAL_OP );
        reply.setValue( "" + Groovyness.eval( request.getValue( ) ) );
        reply.setOldValue( "executed successfully." );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
        reply.setName( INTERNAL_OP );
        reply.setOldValue( "euca operation failed because of: " + ex.getMessage( ) );
        reply.setValue( Exceptions.string( ex ) );
      }
    } else {
      try {
        ConfigurableProperty entry = PropertyDirectory.getPropertyEntry( request.getName( ) );
        String oldValue = "********";
        if ( !entry.getWidgetType( ).equals( ConfigurableFieldType.KEYVALUEHIDDEN ) ) {
          oldValue = entry.getValue( );
        }
        reply.setOldValue( oldValue );
        Boolean reset = request.getReset( );
        if ( MoreObjects.firstNonNull( request.getReset( ), Boolean.FALSE ) ) {
          entry.setValue(entry.getDefaultValue());
        } else {
          // if property is ReadOnly it should not be set by user
          if ( !entry.getReadOnly() ) {
            try {
              String inValue = request.getValue( );
              entry.setValue( ( inValue == null ) ? "" : inValue );
            } catch ( Exception e ) {
              entry.setValue( oldValue );
              Exceptions.findAndRethrow( e, EucalyptusCloudException.class );
              throw e;
            }
          }
        }
        reply.setValue( entry.getValue( ) );
        reply.setName( request.getName( ) );
      } catch ( IllegalAccessException e ) {
        throw new EucalyptusCloudException( "Failed to set property: " + e.getMessage( ) );
      } catch (EucalyptusCloudException e) {
        throw e;
      } catch (Throwable e) {
        throw new EucalyptusCloudException(e);
      }
    }
    return reply;
  }
}
