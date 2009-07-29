package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.cluster.*;
import org.apache.log4j.Logger;

public class VmMetadata {
  private static Logger LOG = Logger.getLogger( VmMetadata.class );

  public String handle( String path ) {
    String vmIp = path.split( ":" )[ 0 ];
    String url = path.split( ":" )[ 1 ];
    for ( VmInstance vm : VmInstances.getInstance().listValues() )
      if ( vmIp.equals( vm.getNetworkConfig().getIpAddress() ) || vmIp.equals( vm.getNetworkConfig().getIgnoredPublicIp() ) ) {

        if ( url.equals( "user-data" ) )
          return vm.getUserData();
        else if ( !url.startsWith( "meta-data" ) )
          return null;
        url = url.replaceAll("meta-data/?","");
        return vm.getByKey( url );
      }
    return null;
  }
}


