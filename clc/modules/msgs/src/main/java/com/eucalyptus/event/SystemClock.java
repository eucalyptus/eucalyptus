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

package com.eucalyptus.event;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.empyrean.Empyrean;

@ConfigurableClass( root = "bootstrap.timer",
                    description = "Parameters controlling the system timer." )
public class SystemClock extends TimerTask implements UncaughtExceptionHandler {
  private static Logger      LOG   = Logger.getLogger( SystemClock.class );
  
  @ConfigurableField( description = "Amount of time (in milliseconds) before a previously running instance which is not reported will be marked as terminated.",
                      initial = "60", changeListener=ClockRateChangeListener.class )
  public static Long         RATE  = 10000L;
  
  private static SystemClock clock;
  private static Timer       timer;
  private static Timer       hzTimer;
  private static HzClock     hertz;
  private int                phase = 0;
  
  public static class ClockRateChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        RATE = Long.parseLong( ( String ) newValue );
        hzTimer.cancel( );
        timer.cancel( );
        setupTimer( );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
    }
  }
  
  public SystemClock( ) {
    super( );
  }
  
  public static long getRate( ) {
    return RATE;
  }
  
  public static void setupTimer( ) {
    synchronized ( SystemClock.class ) {
      if ( timer == null ) {
        timer = new Timer( "SystemClockTimer" );
        hzTimer = new Timer( "SystemHzTimer" );
        clock = new SystemClock( );
        hertz = new HzClock( );
        ListenerRegistry.getInstance( ).register( ClockTick.class, new Dummy( ) );
        ListenerRegistry.getInstance( ).register( Hertz.class, new Dummy( ) );
        timer.scheduleAtFixedRate( clock, 0, RATE );//TODO: make configurable
        hzTimer.scheduleAtFixedRate( hertz, 0, 1000 );
        OrderedShutdown.registerPreShutdownHook( new Runnable( ) {
          @Override
          public void run( ) {
            timer.cancel( );
          }
        } );
        OrderedShutdown.registerPreShutdownHook( new Runnable( ) {
          
          @Override
          public void run( ) {
            hzTimer.cancel( );
          }
        } );
      }
    }
  }
  
  @Override
  public void run( ) {
    Thread.currentThread( ).setUncaughtExceptionHandler( ( UncaughtExceptionHandler ) this );
    if ( !Databases.isVolatile( ) ) {
      try {
        long sign = ( long ) ( Math.pow( -1f, ( float ) ( ++phase % 2 ) ) );
        ListenerRegistry.getInstance( ).fireEvent( new ClockTick( ).setMessage( sign * System.currentTimeMillis( ) ) );
      } catch ( EventFailedException e ) {} catch ( Exception t ) {
        LOG.error( t, t );
      }
    }
  }
  
  public static class Dummy implements EventListener {
    @Override
    public void fireEvent( Event event ) {}
  }
  
  @Override
  public void uncaughtException( Thread t, Throwable e ) {
    LOG.fatal( e, e );
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.Final )
  public static class SystemClockBootstrapper extends Bootstrapper {
    
    @Override
    public boolean load( ) throws Exception {
      return true;
    }
    
    @Override
    public boolean start( ) throws Exception {
      setupTimer( );
      return true;
    }
    
    @Override
    public boolean enable( ) throws Exception {
      return true;
    }
    
    @Override
    public boolean stop( ) throws Exception {
      //ASAP:FIXME:GRZE restarting the timer
      return true;
    }
    
    @Override
    public void destroy( ) throws Exception {}
    
    @Override
    public boolean disable( ) throws Exception {
      return true;
    }
    
    @Override
    public boolean check( ) throws Exception {
      return true;
    }
    
  }
  
  public static class HzClock extends TimerTask implements UncaughtExceptionHandler {
    private int phase = 0;
    
    @Override
    public void uncaughtException( Thread thread, Throwable t ) {
      LOG.error( t, t );
    }
    
    public void run( ) {
      if ( !Databases.isVolatile( ) ) {
        Thread.currentThread( ).setUncaughtExceptionHandler( ( UncaughtExceptionHandler ) this );
        try {
          ListenerRegistry.getInstance( ).fireEvent( new Hertz( ) );
        } catch ( EventFailedException e ) {} catch ( Exception t ) {
          LOG.error( t, t );
        }
      }
    }
  }
  
}
