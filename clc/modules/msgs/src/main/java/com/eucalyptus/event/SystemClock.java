package com.eucalyptus.event;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class SystemClock extends TimerTask {
  private static Logger LOG = Logger.getLogger( SystemClock.class );
  private static SystemClock clock;
  private static Timer timer;
  private int phase = 0;
  
  public SystemClock( ) {
    super( );
  }

  public static void setupTimer( ) {
    synchronized(SystemClock.class) {
      if( timer == null ) {
        timer = new Timer("SystemClockTimer");
        clock = new SystemClock();
        ListenerRegistry.getInstance( ).register( ClockTick.class, new Dummy() );
        timer.scheduleAtFixedRate( clock, 10000, 5000 );
      }
    }
  }

  @Override
  public void run( ) {
    try {
      long sign = (long) (Math.pow(-1f,(float)(++phase%2)));
      ListenerRegistry.getInstance( ).fireEvent( new ClockTick().setMessage( sign * System.currentTimeMillis( ) ) );
    } catch ( EventVetoedException e ) {}
  }

  public static class Dummy implements EventListener{
    @Override
    public void advertiseEvent( Event event ) {}
    @Override
    public void fireEvent( Event event ) {
      LOG.debug( "-> Timer tick." );
    }
  }
  
}
