package com.eucalyptus.auth.policy;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;

public class ResourceTypeDiscovery extends ServiceJarDiscovery {
  
  private static Logger LOG = Logger.getLogger( ResourceTypeDiscovery.class );
  
  @SuppressWarnings( "unchecked" )
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    if ( Ats.from( candidate ).has( PolicyResourceType.class ) ) {
      PolicyResourceType type = Ats.from( candidate ).get( PolicyResourceType.class );
      LOG.debug( "Register policy resource type " + type.resource( ) + " for " + candidate.getCanonicalName( ) );
      if ( !PolicySpec.registerResourceType( candidate, type.vendor( ) + ":" + type.resource( ) ) ) {
        LOG.error( "Registration conflict for " + candidate.getCanonicalName( ) );
      }
      return true;
    }
    return false;
  }
  
  @Override
  public Double getPriority( ) {
    return 1.0d;
  }
  
}
