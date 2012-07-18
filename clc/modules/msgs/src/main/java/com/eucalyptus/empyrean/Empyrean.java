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

package com.eucalyptus.empyrean;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.AdminService;
import com.eucalyptus.component.ComponentId.GenerateKeys;
import com.eucalyptus.component.ComponentId.Partition;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.Internets;
import com.eucalyptus.ws.WebServices;
import com.google.common.base.Predicate;

@Partition( Empyrean.class )
@GenerateKeys
@AdminService
public class Empyrean extends ComponentId {
  private static final long    serialVersionUID = 1L;
  private static Logger        LOG              = Logger.getLogger( Empyrean.class );
  public static final Empyrean INSTANCE         = new Empyrean( );                   //NOTE: this has a silly name because it is temporary.  do not use it as an example of good form for component ids.
                                                                                      
  @Partition( value = { Empyrean.class },
              manyToOne = true )
//  @InternalService
  public static class Arbitrator extends ComponentId {

    @Override
    public boolean isDistributedService( ) {
      return false;
    }
    
  }
  
  @Partition( Empyrean.class )
  @AdminService
  public static class PropertiesService extends ComponentId {
    
    private static final long serialVersionUID = 1L;
    
    public PropertiesService( ) {
      super( "Properties" );
    }
    
    @Override
    public String getLocalEndpointName( ) {
      return "vm://PropertiesInternal";
    }
    
  }
  
  public Empyrean( ) {
    super( "Bootstrap" );
  }
  
  @Override
  public String getServiceModelFileName( ) {
    return "eucalyptus-bootstrap.xml";
  }
  
  @Override
  public String getInternalServicePath( final String... pathParts ) {
    return "/internal/Empyrean";
  }
  
  @Override
  public String getServicePath( final String... pathParts ) {
    return "/services/Empyrean";
  }
}
