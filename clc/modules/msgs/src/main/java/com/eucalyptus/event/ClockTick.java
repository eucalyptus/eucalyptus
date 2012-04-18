package com.eucalyptus.event;

import org.apache.log4j.Logger;

public class ClockTick extends GenericEvent<Long>{
  private static Logger LOG = Logger.getLogger( ClockTick.class );

  @Override
  public Long getMessage( ) {
    return Math.abs( super.getMessage( ) );
  }

  public boolean isBackEdge() {
    return super.getMessage( ) > 0;
  }
  
  
  
  
  
}
