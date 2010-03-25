package com.eucalyptus.config;

import java.util.List;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurationProperties;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.DescribePropertiesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePropertiesType;
import edu.ucsb.eucalyptus.msgs.ModifyPropertyValueResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyPropertyValueType;
import edu.ucsb.eucalyptus.msgs.Property;

public class Properties {
  
  public DescribePropertiesResponseType describeProperties( DescribePropertiesType request ) throws EucalyptusCloudException {
    if( !request.isAdministrator( ) ) {
      throw new EucalyptusCloudException( "You are not authorized to interact with this service." );
    }
    DescribePropertiesResponseType reply = request.getReply( );
    List<Property> props = reply.getProperties( );
    if ( request.getProperties( ).isEmpty( ) ) {
      for ( ConfigurableProperty entry : PropertyDirectory.getPropertyEntrySet( ) ) {
        props.add( new Property( entry.getQualifiedName( ), entry.getValue( ), entry.getDescription( ) ) );
      }
    } else {
      for ( ConfigurableProperty entry : PropertyDirectory.getPropertyEntrySet( ) ) {
        if ( request.getProperties( ).contains( entry.getQualifiedName( ) ) ) {
          props.add( new Property( entry.getQualifiedName( ), entry.getValue( ), entry.getDescription( ) ) );
        }
      }
    }
    return reply;
  }

  public ModifyPropertyValueResponseType modifyProperty( ModifyPropertyValueType request ) throws EucalyptusCloudException {
    if( !request.isAdministrator( ) ) {
      throw new EucalyptusCloudException( "You are not authorized to interact with this service." );
    }
    ModifyPropertyValueResponseType reply = request.getReply( );
    try {
      ConfigurableProperty entry = PropertyDirectory.getPropertyEntry( request.getName( ) );
      String oldValue = entry.getValue( );
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
    return reply;
  }
}
