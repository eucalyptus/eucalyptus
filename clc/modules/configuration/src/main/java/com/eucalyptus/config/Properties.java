package com.eucalyptus.config;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurationProperties;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class Properties {
  private static Logger LOG = Logger.getLogger( Properties.class );
  
  public DescribePropertiesResponseType describeProperties( DescribePropertiesType request ) throws EucalyptusCloudException {
    if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      throw new EucalyptusCloudException( "You are not authorized to interact with this service." );
    }
    DescribePropertiesResponseType reply = request.getReply( );
    List<Property> props = reply.getProperties( );
    if ( request.getProperties( ).isEmpty( ) ) {
      for ( ConfigurableProperty entry : PropertyDirectory.getPropertyEntrySet( ) ) {
        String value = "********";
        if ( !entry.getWidgetType( ).equals( ConfigurableFieldType.KEYVALUEHIDDEN ) )
          value = entry.getValue( );
        props.add( new Property( entry.getQualifiedName( ), value, entry.getDescription( ) ) );
      }
    } else {
      deprecatedEucaOps( request, props );
      for ( ConfigurableProperty entry : PropertyDirectory.getPropertyEntrySet( ) ) {
        if ( request.getProperties( ).contains( entry.getQualifiedName( ) ) ) {
          String value = "********";
          if ( !entry.getWidgetType( ).equals( ConfigurableFieldType.KEYVALUEHIDDEN ) )
            value = entry.getValue( );
          props.add( new Property( entry.getQualifiedName( ), value, entry.getDescription( ) ) );
        }
      }
      for ( String entrySetName : PropertyDirectory.getPropertyEntrySetNames( ) ) {
        if ( request.getProperties( ).contains( entrySetName ) ) {
          String value = "********";
          for ( ConfigurableProperty entry : PropertyDirectory.getPropertyEntrySet( entrySetName ) ) {
            if ( !entry.getWidgetType( ).equals( ConfigurableFieldType.KEYVALUEHIDDEN ) ) {
              value = entry.getValue( );
            }
            props.add( new Property( entry.getQualifiedName( ), value, entry.getDescription( ) ) );
          }
        }
      }
    }
    return reply;
  }
  /**
   * DO NOT USE THIS ANYMORE.
   */
  @Deprecated
  private void deprecatedEucaOps( DescribePropertiesType request, List<Property> props ) {
    Iterable<String> eucas = Iterables.filter( request.getProperties( ), new Predicate<String>( ) {
      @Override
      public boolean apply( String arg0 ) {
        return arg0.matches( "euca=.*" );
      }
    } );
    for ( String altValue : eucas ) {
      try {
        props.add( new Property( ( altValue = altValue.replaceAll( "euca=", "" ) ), "" + Groovyness.eval( altValue ), altValue ) );
      } catch ( Exception ex ) {
        props.add( new Property( altValue, ex.getMessage( ), Exceptions.string( ex ) ) );
      }
    }
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
        try {
          entry.setValue( request.getValue( ) );
        } catch ( Exception e ) {
          entry.setValue( oldValue );
        }
        ConfigurationProperties.store( entry.getEntrySetName( ) );
        reply.setValue( entry.getValue( ) );
        reply.setName( request.getName( ) );
      } catch ( IllegalAccessException e ) {
        throw new EucalyptusCloudException( "Failed to set property: " + e.getMessage( ) );
      }
    }
    return reply;
  }
}
