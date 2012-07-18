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

package com.eucalyptus.component.id;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.ComponentId.GenerateKeys;
import com.eucalyptus.component.ComponentId.Partition;
import com.eucalyptus.component.ComponentId.PolicyVendor;
import com.eucalyptus.component.ComponentId.PublicService;
import com.eucalyptus.util.Internets;
import com.eucalyptus.ws.TransportDefinition;
import com.eucalyptus.ws.StackConfiguration.BasicTransport;
import com.google.common.collect.Lists;

@PublicService
@GenerateKeys
@PolicyVendor( "ec2" )
@Partition( Eucalyptus.class )
public class Eucalyptus extends ComponentId {
  public static final Eucalyptus INSTANCE = new Eucalyptus( );                   //NOTE: this has a silly name because it is temporary.  do not use it as an example of good form for component ids.
  private static Logger          LOG      = Logger.getLogger( Eucalyptus.class );
  
  @Override
  public String getLocalEndpointName( ) {
    return "vm://EucalyptusRequestQueue";
  }
  
  @Partition( Eucalyptus.class )
  @PublicService
  public static class Notifications extends ComponentId {}
  
  @Partition( Eucalyptus.class )
  @GenerateKeys
  public static class Database extends ComponentId {

    public Database( ) {
      super( "Db" );
    }
    
    @Override
    public Integer getPort( ) {
      return 8777;
    }
    
    @Override
    public String getLocalEndpointName( ) {
      return ServiceUris.remote( this, Internets.localHostInetAddress( ) ).toASCIIString( );
    }
    
    @Override
    public String getServicePath( String... pathParts ) {
      return Databases.getServicePath( pathParts );
    }
    
    @Override
    public String getInternalServicePath( String... pathParts ) {
      return this.getServicePath( pathParts );
    }

    @Override
    public Map<String, String> getServiceQueryParameters() {
      return Databases.getJdbcUrlQueryParameters();
    }

    @Override
    public List<? extends TransportDefinition> getTransports( ) {
      return Lists.newArrayList( BasicTransport.JDBC );
    }

  }
  
}
