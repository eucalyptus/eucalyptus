package com.eucalyptus.event;

import org.apache.log4j.Logger;

public class ClockTick extends GenericEvent<Long>{
  private static Logger LOG = Logger.getLogger( ClockTick.class );
  @Override
  public Exception getFail( ) {
    if( super.getFail( ) != null ) {
      LOG.warn("An innfallible event has failed: " + this + " " + super.getFail( ) );      
    }
    return null;
  }
  
  @Override
  public void setFail( Exception fail ) {
    LOG.debug(fail,fail);
  }

  @Override
  public boolean isVetoed( ) {
    if( super.isVetoed( ) ) {
      LOG.warn("An unvetoable event was vetoed: " + this + " " + super.getCause( )!=null?super.getCause( ):"");
    }
    return false;
  }

  @Override
  public Long getMessage( ) {
    return Math.abs( super.getMessage( ) );
  }

  public boolean isBackEdge() {
    return super.getMessage( ) > 0;
  }
  
  
  
  
  
}
