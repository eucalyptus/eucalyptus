package com.eucalyptus.entities;

import java.io.File;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.collect.Multimap;

public class PersistenceContextDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( PersistenceContextDiscovery.class );
  
  public PersistenceContextDiscovery( ) {}
  
  @Override
  public Double getPriority( ) {
    return 0.91d;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    if ( Ats.from( candidate ).has( Entity.class ) ) {
      if ( !Ats.from( candidate ).has( PersistenceContext.class ) ) {
        throw BootstrapException.throwFatal( "Database entity does not have required @PersistenceContext annotation: " + candidate.getCanonicalName( ) );
      } else {
        PersistenceContexts.addEntity( candidate );
        return true;
      }
    } else if ( Ats.from( candidate ).has( MappedSuperclass.class ) || Ats.from( candidate ).has( Embeddable.class ) ) {
      PersistenceContexts.addSharedEntity( candidate );
      return true;
    } else {
      return false;
    }
  }
    
}
