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

package com.eucalyptus.config;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class Properties {
  private static Logger LOG = Logger.getLogger( Properties.class );
  
  
  public DescribePropertiesResponseType describeProperties( final DescribePropertiesType request ) throws EucalyptusCloudException {
    if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      throw new EucalyptusCloudException( "You are not authorized to interact with this service." );
    }
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
        props.add( new Property( entry.getQualifiedName( ), value, entry.getDescription( ) ) );
      }
    }
    return reply;
  }
  private static final String INTERNAL_OP = "euca";
  public ModifyPropertyValueResponseType modifyProperty( ModifyPropertyValueType request ) throws EucalyptusCloudException {
    if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      throw new EucalyptusCloudException( "You are not authorized to interact with this service." );
    }
    ModifyPropertyValueResponseType reply = request.getReply( );
    if( INTERNAL_OP.equals( request.getName( ) ) ) {
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
        if (reset != null) {
          if (Boolean.TRUE.equals( reset )) {
        	entry.setValue(entry.getDefaultValue());	
          }
        } else { 
        try {
          entry.setValue( request.getValue( ) );
        } catch ( Exception e ) {
          entry.setValue( oldValue );
        }
        }
        reply.setValue( entry.getValue( ) );
        reply.setName( request.getName( ) );
      } catch ( IllegalAccessException e ) {
        throw new EucalyptusCloudException( "Failed to set property: " + e.getMessage( ) );
      }
    }
    return reply;
  }
}
