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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.bootstrap;

import groovy.lang.ExpandoMetaClass;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.Security;
import java.util.Map;
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
  
  private static SystemBootstrapper singleton;
  private static ThreadGroup        singletonGroup;
  public static final PrintStream   out = System.out;
  public static final PrintStream   err = System.err;
  
  public static SystemBootstrapper getInstance( ) {
    synchronized ( SystemBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new SystemBootstrapper( );
        LOG.info( "Creating Bootstrapper instance." );
      } else {
        LOG.info( "Returning Bootstrapper instance." );
      }
    }
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
            System.out.println( "All threads:\n" );
            for ( Map.Entry<Thread, StackTraceElement[]> ent : Thread.getAllStackTraces( ).entrySet( ) ) {

            }
          }
        }
      }
    } );
    OrderedShutdown.initialize( );
    BootstrapArgs.init( );
    if ( Security.getProvider( BouncyCastleProvider.PROVIDER_NAME ) == null ) {
      if ( Security.getProviders().length > 4 ) {
        Security.insertProviderAt( new BouncyCastleProvider( ), 4 ); // EUCA-5833
      } else {
        Security.addProvider( new BouncyCastleProvider( ) );
      }
    }
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
//    String prefix = "\n[8m-----------------------------------------------------[0;10m[1m";
    String prefix = "\n\t";
    String headerHeader = "\n[8m-----------------[0;10m[1m_________________________________________________________[0;10m\n[8m-----------------[0;10m[1m|";
    String headerFormat = "  %-53.53s";
    String headerFooter = "|\n[8m-----------------[0;10m[1m|#######################################################|[0;10m\n";
    String banner = "[8m-----------------[0;10m[1m._______________________________________________________.[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#######################################################|[0;10m\n"
                    +
                    "[8m----------------.[0;10m[1m|#[0;10m[8m                                                  [0;10m[1m.____[0;10m,[8m+[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m  -------------------------------[0;10m_[1m.._.[0;10m_____[8m +--[0;10m[1m..[0;10mvH[1m~[0;10m[8mn[0;10m===[8m+[0;10m\n"
                    +
                    "[8m-[0;10m_,[8m +------------[0;10m[1m|)[0;10m[8m|.-----------------------------[0;10m[1m._[0;10mggQgp;;;;;[1m.[0;10m[8mI[0;10m_[1m_[0;10mgI\"\"`[8m:[0;10m`[8m-[0;10m\n"
                    +
                    "[8m [0;10m-!M08mQgggggggggwwg[1m_[0;10mgww[1m_[0;10mg[1m_[0;10mw[1m_[0;10mw[1m_________.[0;10m[8m |----[0;10m_[1m__vvvvn[0;10m0[1m+[0;10m([1m.[0;10mn|<[1m._[0;10mV[1m+[0;10m\"[8m|::=:-[0;10m[1m#|[0;10m\n"
                    +
                    "[8m+iv[0;10m-\"\"![1m\"{{vnvvvvvvvvvvvvvvs%%iiiiii[0;10mQ[1mii[0;10mg[1mi,[0;10m[8mW---.[0;10mj[1mivvvvvl`:[0;10m<o^^wg2\"'[8m-:-----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m---:+ii:   [0;10m-\"\"[1m~~\"+[0;10mMMMMMMM[1m{IIvvnvvvvvnvnnnn[0;10mg[1m_[0;10mj[1mvvnvvl[0;10mHF|[8mQ[0;10m|s[1m.;~`[0;10m[8mI[0;10mL[8m|:-------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-------.  :+|+=-[0;10m-^\"\"\"\"\"\"![1m~~^\"\"[0;10mM[1m{Ivvvnvnvvnl[0;10mg[1mvvIvII[0;10mF\"'=[1m.[0;10mgwg\"[8mv:-[0;10mL[8m=--------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m----------------.[0;10m[1m|)[8m -+iI=. ..-    [0;10m-\"[1m~~~~[0;10mn[1m_unvvI[0;10mM!|[1m._[0;10m%<[1m~[0;10m\")|[8mi:--[0;10m<[8m+--------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m----------------.[0;10m[1m|#[0;10m[8m  ..-...---.   --    [0;10mg[1mvvvvl~[0;10m\"-:[1m=[0;10mT^\"[8m |[0;10m][8mn---.[0;10m-[8m#.-------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m   ..------------. [0;10m[1m_invv[0;10mME[8mI[0;10m_[8m|[0;10m_\"`[8m :=|:[0;10m[1m;[0;10m[8m|----[0;10m-m[1m_[0;10m[8m.------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|)[0;10m[8m-  ..-----[0;10m_[1m.[0;10m,[8m---- [0;10m[1m_vvnI[0;10mN\"'[1m.,[0;10m[8mi[0;10m-[8m+:----[0;10m[1m.[0;10mf[8m+----Q[0;10m[1mv[0;10mg[1m_[0;10m[8m:-----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m--[0;10m,______[1m...[0;10m____[1m..______[0;10mwg[1mi%vvvvi[0;10m6[1m_[0;10mq[1m%v%[0;10mME[8m.[0;10m_[1m.)[0;10mT`[8m+------[0;10m[1m.[0;10mH`[8m--- [0;10m[1m_vlnl[0;10mc[8m.----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m--[0;10m<qggggggggggwwg[1miii[0;10mg[1ms[0;10mQ[1mvvvvvvvvI+~[0;10mq[1minv[0;10mF\"'[1m.;[0;10mg[1m~[0;10m[8mi:----- [0;10m[1m_[0;10mp^[8m  --[0;10m][1mivvvv[0;10mL[8m=----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m--+=[0;10m\"\"[1m^[0;10mMM[1m*[0;10mM[1mI[0;10mM[1m*[0;10mHNMMMM[1m*[0;10mMMM[1mllIl[0;10mMN[1m`[0;10m[8mQ[0;10m[1m_[0;10mw[1m}[0;10mM[1m~`[0;10m[8mv[0;10m[1m.[0;10m]\"}[8mn[0;10m_[1m._[0;10m<v[1m=[0;10mqg[1m%I[0;10mF[8m-..-.[0;10m[1m)vvnvn[0;10mE[8m=----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----| [0;10m(\"\"\"!\"\"\"\"\"\"\"\"\"\"\"\"[1m~~~~[0;10m1w,j[1m>+~[0;10m{=`_a[1m;[0;10m[8mX[0;10m`[1m.[0;10mgI\"\"\"n[1minvv[0;10m[[8m:-.-.[0;10m[1mvvnvnv[0;10mF[8m|----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[8m---------:.[0;10m4C_[1m...._[0;10mW#==)[1m~[0;10m'[8m==+[0;10m[1m.vvnvv[0;10m`[8m.---.[0;10m[1mvvvnvv[0;10mf[8m+----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[8m ..--------[0;10m-^-|!{g[1mv[0;10mQ!^^^[8mn+:--[0;10mj[1mnvnv[0;10mM[8m .--..[0;10m[1mvvnvnv[0;10m[[8m-----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m----------------.[0;10m[1m|#[0;10m[8m   --------+|||[0;10m[1m_)nv}[0;10m[8mn=::--- [0;10mg[1mnnvv}[0;10m[8m+..--..[0;10m[1mvvnvvv[0;10m`[8m+----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m   .----------[0;10m_[1m%vvv[0;10mF[8m ------[0;10m][1mIvnvv[0;10mF[8m -----.[0;10m[1mvvnvnI[0;10m[8m i----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m    --------[0;10m_j[1m%nv}+[0;10mc[8m.-----[0;10m[1m.[0;10mQ[1mlnv}+[0;10m[8m  -----.[0;10m[1mvvnvv[0;10mT[8m=:----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m    .------ [0;10mj[1mvvvI[0;10m!>C[8m+----[0;10m_g[1mvIvn[0;10m@-[8m-.-----.[0;10m[1m{vnvv[0;10m+[8m=-----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m     -----[0;10m_q[1mvvI[0;10mN[1m'[0;10m[8m [0;10m\"[1m`[0;10m[8m:---.[0;10mj[1mIvvv+[0;10m`[8m -------.[0;10m[1mvnvv}[0;10m[8mQ:-----[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m     ----[0;10m[1m.[0;10mq[1mvn}[0;10mF^'[8mn+:----.[0;10mj[1mnvn}[0;10m^[8m  -------.[0;10m[1mvvvn%[0;10m[8m.------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m     . [0;10m[1m_[0;10mq[1mvn}+[0;10m'[8m=i+:----- [0;10mw[1mvnvI`[0;10m[8m  .-------.[0;10m[1mvvnv[0;10mf[8m=------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m     [0;10m_[1m_vvv}~[0;10m'[8mi---------[0;10m<[1mvvnv[0;10mT[8m =..-------.[0;10m[1mvvnv`[0;10m[8m-------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m   [0;10m_[1m_%v}[0;10mM[1m`[0;10m'[8m:::--------[0;10m[1m.)vnv[0;10m@[8m-=:---------.[0;10m[1mvvn[0;10mH[8m..------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m   [0;10mw[1m%vn~`[0;10m`[8mI:--------- [0;10mg[1mnnvv`[0;10m[8m ..---------.[0;10m[1mvvn[0;10m![8m.-------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m [0;10m_q[1mn}+~[0;10m[8m =+:----------[0;10mj[1mnnv}[0;10m[8mi  ..---------.[0;10m[1mvvv[0;10m=[8m -------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|)[0;10m[8m-[0;10mj[1mu}~[0;10m^[8m :-------------[0;10m[1m%vvv[0;10mF[8m=....---------.[0;10m[1mvvv[0;10m[8mQ--------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m----------------  [0;10m_q[1m}+[0;10m[8m --=:------------X[0;10m[1mnvn[0;10mM[1m`[0;10m[8m=-------------.[0;10m[1mvn[0;10mE[8m.--------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m--------------.. [0;10m/g[1m%`[0;10m'[8mi:---------------[0;10m[1m_nv}[0;10m\"[8m I-------------[0;10m[1m.nI`[0;10m[8m --------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m--------------.[0;10m/_)[1m`[0;10m`[8m=+:---------------Q[0;10m[1mvnv[0;10m[[8m+I=------------ [0;10m[1mi[0;10mN\"[8m----------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m---------------[0;10m-\"[8m X:------------------[0;10m<[1mvn[0;10mM`[8m---------------Q[0;10mT^[8m|----------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|)[8m-------------------[0;10mj[1mn}[0;10m[8m=.--------------- v::----------[0;10m[1m#|[0;10m\n"
                    +
                    "[8m----------------.[0;10m[1m|#[0;10m[8m  .---------------.[0;10mj[1mv[0;10mF[8m[0;10m                               [1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#[0;10m[8m                   [0;10mj[1mn[0;10mf^[8m     [0;10m[1mStarted Eucalyptus       [0;10m[1m#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m|#___________________[0;10m [1mN[0;10m'[8m [0;10m[1m______________________________#|[0;10m\n"
                    +
                    "[8m-----------------[0;10m[1m(####################[0;10m [1mv[0;10m'[8m [0;10m[1m###############################)[0;10m\n"
                    +
                    "[8m----------------     .--------------:I [0;10m^[8m   . .. .. .. .. ..... .. .. ...=v[0;10m\n"
                    +
                    "[8m---------------..     ------------------  ................................[0;10m\n";
    
    banner += "\n[8m-----------------[0;10m[1m Version: " + singleton.getVersion( )
              + "\n";
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
