package com.eucalyptus.system;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;

@Provides(Component.bootstrap)
@RunDuring(Bootstrap.Stage.UnprivilegedConfiguration)
public class DirectoryBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DirectoryBootstrapper.class );
  public DirectoryBootstrapper( ) {}
  @Override
  public boolean load( Stage current ) throws Exception {
    for( BaseDirectory b : BaseDirectory.values( ) ) {
      EventRecord.here( DirectoryBootstrapper.class, EventType.SYSTEM_DIR_CHECK, b.name(), b.toString( ) ).info( );
      b.check( );
    }
    for( SubDirectory s : SubDirectory.values( ) ) {
      EventRecord.here( DirectoryBootstrapper.class, EventType.SYSTEM_DIR_CHECK, s.name(), s.toString( ) ).info( );
      s.check( );
    }
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    return true;
  }
  
}
