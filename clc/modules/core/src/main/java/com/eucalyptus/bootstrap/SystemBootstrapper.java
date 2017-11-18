/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.bootstrap;

import groovy.lang.ExpandoMetaClass;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.Security;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.Capabilities;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Internets;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Java entry point from eucalyptus-bootstrap. See {@link Bootstrap} for a detailed explanation of
 * the system startup procedure.
 * 
 * <b>IMPORTANT:</b> See {@link #init()} regarding exceptin handling.
 * 
 * @see Bootstrap
 * @see SystemBootstrapper#init()
 */
public class SystemBootstrapper {
  private static final String       SEP = " -- ";

  static Logger                     LOG = Logger.getLogger( SystemBootstrapper.class );

  private static volatile SystemBootstrapper singleton;
  public static final PrintStream   out = System.out;
  public static final PrintStream   err = System.err;

  public static SystemBootstrapper getInstance( ) {
    if (singleton == null) {
      synchronized(SystemBootstrapper.class) {
        if (singleton == null) {
          LOG.info( "Creating Bootstrapper instance." );
          singleton = new SystemBootstrapper( );
        }
      }
    }

    LOG.info( "Returning Bootstrapper instance." );
    return singleton;
  }
  
  public SystemBootstrapper( ) {}
  
  /**
   * <b>IMPORTANT NOTE:</b> this is the <b><i>only</i></b> class for which it is acceptable to
   * {@code catch} or {@code throw} the {@link Throwable} type. See {@link ThreadDeath} for an
   * explanation of the constraints on
   * handling {@link Throwable} propagation.
   * 
   * @see java.lang.ThreadDeath
   * @param t
   * @throws Throwable
   */
  private static void handleException( Throwable t ) throws Throwable {
    if ( t instanceof BootstrapException ) {
      t.printStackTrace( );
      LOG.fatal( t, t );
      throw t;
    } else {
      t.printStackTrace( );
      LOG.fatal( t, t );
      System.exit( 123 );
    }
  }
  
  /**
   * {@inheritDoc #handleException(Throwable)}
   * 
   * @return
   * @throws Throwable
   */
  public boolean init( ) throws Throwable {
    ExpandoMetaClass.enableGlobally( );
    Logs.init( );
    Thread.setDefaultUncaughtExceptionHandler( new UncaughtExceptionHandler( ) {
      
      @Override
      public void uncaughtException( Thread t, Throwable e ) {
        try {
          String stack = Joiner.on( "\t\n" ).join( Thread.currentThread( ).getStackTrace( ) );
          LOG.error( stack );
          LOG.error( e, e );
        } catch ( Exception ex ) {
          try {
            System.out.println( Joiner.on( "\t\n" ).join( Thread.currentThread( ).getStackTrace( ) ) );
            e.printStackTrace( );
            ex.printStackTrace( );
          } catch ( Exception ex1 ) {
            System.out.println( "Failed because of badness in uncaught exception path." );
            System.out.println( "Thread:      " + t.toString( ) );
            System.out.println( "Exception:   " + e.getClass( ) );
            System.out.println( "Message:     " + e.getMessage( ) );
          }
        }
      }
    } );
    OrderedShutdown.initialize( );
    BootstrapArgs.init( );
    Security.addProvider( new BouncyCastleProvider( ) );
    try {//GRZE:HACK: need to remove the nss add-on in deb based distros as it breaks ssl.
      Groovyness.eval( "import sun.security.jca.*; Providers.setProviderList( ProviderList.remove( Providers.@providerList,\"SunPKCS11-NSS\") );" );
    } catch ( Exception ex ) {
      LOG.error( ex , ex );
    }
    try {
      if ( !BootstrapArgs.isInitializeSystem( ) ) {
        Bootstrap.init( );
        Bootstrap.Stage stage = Bootstrap.transition( );
        stage.load( );
      }
      return true;
    } catch ( Throwable t ) {
      SystemBootstrapper.handleException( t );
      return false;
    }
  }
  
  /**
   * {@inheritDoc #handleException(Throwable)}
   * 
   * @return
   * @throws Throwable
   */
  public boolean load( ) throws Throwable {
    if ( BootstrapArgs.isInitializeSystem( ) ) {
      try {
        Bootstrap.initializeSystem( );
        System.exit( 0 );
      } catch ( Throwable t ) {
        LOG.error( t, t );
        System.exit( 1 );
        throw t;
      }
    } else {
      SystemBootstrapper.runSystemStages( new Predicate<Stage>( ) {
        
        @Override
        public boolean apply( Stage input ) {
          input.load( );
          return true;
        }
      } );
      Bootstrap.applyTransition( Component.State.LOADED, Components.whichCanLoad( ) );
    }
    return true;
  }
  
  /**
   * NOTE: this is the /only/ class for which it is acceptable to {@code catch} or {@code throw} the
   * {@link Throwable} type. See {@link ThreadDeath} for an explanation of the constraints on
   * handling {@link Throwable} propagation.
   * 
   * @see java.lang.ThreadDeath
   * @return
   * @throws Throwable
   */
  public boolean start( ) throws Throwable {
    Capabilities.initialize( );
    SystemBootstrapper.runSystemStages( new Predicate<Stage>( ) {
      
      @Override
      public boolean apply( Stage input ) {
        input.start( );
        return true;
      }
    } );
    Bootstrap.applyTransition( Component.State.LOADED, Components.whichCanLoad( ) );
    Threads.enqueue( Empyrean.class, SystemBootstrapper.class, new Callable<Boolean>( ) {
                       @Override
                       public Boolean call( ) {
                         try {
                           Bootstrap.applyTransition( Component.State.DISABLED, Components.whichCanLoad( ) );
                           Bootstrap.applyTransition( Component.State.ENABLED, Components.whichCanEnable( ) );
                         } catch ( Exception ex ) {
                           LOG.error( ex, ex );
                         }
                         return Boolean.TRUE;
                       }
                     } ).get( );
    try {
      SystemBootstrapper.printBanner( );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
    return true;
  }
  
  private static void runSystemStages( Predicate<Bootstrap.Stage> exec ) throws Throwable {
    try {
      // TODO: validation-api
      /** @NotNull */
      Bootstrap.Stage stage = Bootstrap.transition( );
      do {
        exec.apply( stage );
      } while ( ( stage = Bootstrap.transition( ) ) != null );
    } catch ( Throwable t ) {
      LOG.error( t );
      Logs.extreme( ).error( t, t );
      System.exit( 123 );
    }
  }
  
  public String getVersion( ) {
    return BillOfMaterials.RequiredFields.VERSION.getValue( );
  }
  
  public boolean check( ) {
    return true;
  }
  
  public boolean destroy( ) {
    return true;
  }
  
  public boolean stop( ) throws Exception {
    LOG.warn( "Shutting down Eucalyptus." );
    EventRecord.here( SystemBootstrapper.class, EventClass.SYSTEM, EventType.SYSTEM_STOP, "SHUT DOWN" ).info( );
    return true;
  }
  
  public static void restart( ) { System.exit( 123 ); }
  
  private static native void shutdown( boolean reload );
  
  public static native void hello( );
  
  private static String printBanner( ) {
    String prefix = "\n\t";
    String headerHeader = "\n_________________________________________________________\n";
    String headerFormat = "  %-53.53s";
    String headerFooter = "\n_________________________________________________________\n";

    
    String banner = "Started Eucalyptus Version: " + singleton.getVersion( ) + "\n";
    banner += headerHeader + String.format( headerFormat, "System Bootstrap Configuration" )
              + headerFooter;
    for ( Bootstrap.Stage stage : Bootstrap.Stage.values( ) ) {
      banner += prefix + stage.name( )
                + SEP
                + stage.describe( ).replaceAll( "(\\w*)\\w\n", "\1\n" + prefix
                                                               + stage.name( )
                                                               + SEP ).replaceAll( "^\\w* ", "" );
    }
    banner += headerHeader + String.format( headerFormat, "Component Bootstrap Configuration" )
              + headerFooter;
    for ( Component c : Components.list( ) ) {
      if ( c.getComponentId( ).isAvailableLocally( ) ) {
        banner += c.getBootstrapper( );
      }
    }
    banner += headerHeader + String.format( headerFormat, "Local Services" )
              + headerFooter;
    for ( Component c : Components.list( ) ) {
      if ( c.hasLocalService( ) ) {
        ServiceConfiguration localConfig = c.getLocalServiceConfiguration( );
        banner += prefix + c.getName( )
                  + SEP
                  + localConfig.toString( );
        banner += prefix + c.getName( )
                  + SEP
                  + localConfig.getComponentId( ).toString( );
        banner += prefix + c.getName( )
                  + SEP
                  + localConfig.lookupState( ).toString( );
      }
    }
    banner += headerHeader + String.format( headerFormat, "Detected Interfaces" )
              + headerFooter;
    for ( NetworkInterface iface : Internets.getNetworkInterfaces( ) ) {
      banner += prefix + iface.getDisplayName( )
                + SEP
                + Lists.transform( iface.getInterfaceAddresses( ), Functions.toStringFunction( ) );
      for ( InetAddress addr : Lists.newArrayList( Iterators.forEnumeration( iface.getInetAddresses( ) ) ) ) {
        banner += prefix + iface.getDisplayName( )
                  + SEP
                  + addr;
      }
    }
    LOG.info( banner );
    return banner;
  }
  
}
