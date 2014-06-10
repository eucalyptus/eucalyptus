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

package com.eucalyptus.scripting;

import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.ExpandoMetaClassCreationHandle;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.ReadOnlyPropertyException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Maps;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

public class Groovyness {
  private static Logger      LOG          = Logger.getLogger( Groovyness.class );
  public static ScriptEngine groovyEngine = getGroovyEngine( );
  
  public static <T extends GroovyObject> T expandoMetaClass( T obj ) {
    ExpandoMetaClass emc = new ExpandoMetaClass( obj.getClass( ), false );
    emc.initialize( );
    obj.setMetaClass( emc );
    return obj;
  }
  
  private static GroovyClassLoader getGroovyClassLoader( ) {
    GroovySystem.getMetaClassRegistry( ).setMetaClassCreationHandle( new ExpandoMetaClassCreationHandle( ) );
    CompilerConfiguration config = new CompilerConfiguration( );
    config.setDebug( true );
    config.setVerbose( true );
    ClassLoader parent = ClassLoader.getSystemClassLoader( );
    GroovyClassLoader loader = new GroovyClassLoader( parent );
    loader.setShouldRecompile( true );
    return loader;
  }

  /**
   * Periodically free the groovy engine to workaround perm-gen leak
   */
  public static class FreeTheGroovyEngine implements EventListener<ClockTick> {
    public static void register() {
      Listeners.register( ClockTick.class, new FreeTheGroovyEngine() );
    }

    private static void logJmx() {
      ClassLoadingMXBean classLoadingMx = ManagementFactory.getClassLoadingMXBean();
      MemoryPoolMXBean permGen = Iterables.find( ManagementFactory.getMemoryPoolMXBeans(), new Predicate<MemoryPoolMXBean>() {
        @Override
        public boolean apply( MemoryPoolMXBean memoryPoolMXBean ) {
          return memoryPoolMXBean.getName().contains( "Perm Gen" );
        }
      } );
      MemoryUsage permGcUsage = permGen.getCollectionUsage();
      MemoryUsage permPeakUsage = permGen.getPeakUsage();
      MemoryUsage permUsage = permGen.getUsage();
      LOG.debug("EUCA-8917: loaded/unloaded/total=" + classLoadingMx.getLoadedClassCount() + "/" + classLoadingMx.getUnloadedClassCount() + "/" + classLoadingMx.getTotalLoadedClassCount() +
                " used=" + permUsage.getUsed()/1024.0 + "/" + permUsage.getMax()/1024.0 +
                " last-gc="+ permGcUsage.getUsed()/1024.0 + "/" + permGcUsage.getMax()/1024.0 +
                " peak="+ permPeakUsage.getUsed()/1024.0 + "/" + permPeakUsage.getMax()/1024.0 );
    }

    @Override
    public void fireEvent( ClockTick event ) {
      synchronized( Groovyness.class ) {
        if ( Groovyness.groovyEngine != null ) {
          GroovyScriptEngineImpl groovyEngine = ( GroovyScriptEngineImpl ) Groovyness.groovyEngine;
          groovyEngine.getClassLoader().clearCache();
          Groovyness.groovyEngine = null;
        }
      }
      System.gc();
      logJmx();
    }
  }

  private static ScriptEngine getGroovyEngine( ) {
    synchronized ( Groovyness.class ) {
      if ( groovyEngine == null ) {
        GroovySystem.getMetaClassRegistry( ).setMetaClassCreationHandle( new ExpandoMetaClassCreationHandle( ) );
        ScriptEngineManager manager = new ScriptEngineManager( getGroovyClassLoader( ) );
        groovyEngine = manager.getEngineByName( "groovy" );
      }
      return groovyEngine;
    }
  }
  
  public static <T> T newInstance( String fileName ) throws ScriptExecutionFailedException {
    GroovyObject groovyObject = null;
    try {
      File f = new File( fileName );
      if ( !f.exists( ) ) {
        f = new File( SubDirectory.SCRIPTS + File.separator + fileName + ( fileName.endsWith( ".groovy" )
                                                                                                         ? ""
                                                                                                         : ".groovy" ) );
      }
      GroovyClassLoader loader = getGroovyClassLoader( );
      Class groovyClass = loader.parseClass( f );
      groovyObject = ( GroovyObject ) groovyClass.newInstance( );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new ScriptExecutionFailedException( e );
    }
    try {
      return ( T ) groovyObject;
    } catch ( ClassCastException e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( e.getMessage( ), e );
    }
  }
  
  public static <T> T run( SubDirectory dir, String fileName, Map context ) {
    fileName = dir + File.separator + fileName;
    String fileNameWExt = fileName + ".groovy";
    if ( !new File( fileName ).exists( ) && new File( fileNameWExt ).exists( ) ) {
      fileName = fileNameWExt;
    }
    FileReader fileReader = null;
    try {
      fileReader = new FileReader( fileName );
      Bindings bindings = new SimpleBindings( context );
      SimpleScriptContext scriptContext = new SimpleScriptContext( );
      scriptContext.setBindings( bindings, SimpleScriptContext.ENGINE_SCOPE );
      T ret = ( T ) getGroovyEngine( ).eval( fileReader, scriptContext );
      return ret;
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new RuntimeException( "Executing the requested script failed: " + fileName, e );
    } finally {
      if ( fileReader != null ) {
        try {
          fileReader.close( );
        } catch ( IOException e ) {
          LOG.error( e );
        }
      }
    }
  }
  
  public static <T> T run( SubDirectory dir, String fileName ) {
    return run( dir, fileName, Maps.newHashMap( ) );
  }
  
  public static <T> T run( String fileName ) {
    return ( T ) run( SubDirectory.SCRIPTS, fileName );
  }
  
  public static int exec( final String code ) throws ScriptExecutionFailedException {
    try {
      return ( Integer ) getGroovyEngine( ).eval( "p=hi.execute();p.waitFor();System.out.print(p.in.text);System.err.print(p.err.text);p.exitValue()",
                                                  new SimpleScriptContext( ) {
                                                    {
                                                      setAttribute( "hi", code, ENGINE_SCOPE );
                                                    }
                                                  } );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( "Executing the requested script failed: " + code, e );
    }
  }
  
  public static <T> T eval( String code, Map context ) throws ScriptExecutionFailedException {
    try {
      Bindings bindings = new SimpleBindings( context );
      SimpleScriptContext scriptContext = new SimpleScriptContext( );
      scriptContext.setBindings( bindings, SimpleScriptContext.ENGINE_SCOPE );
      return ( T ) getGroovyEngine( ).eval( code, scriptContext );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( "Executing the requested script failed:\n"
                                                + "============================\n"
                                                + code
                                                + "============================\n"
                                                + "\nbecause of:\n" + Exceptions.causeString( e ), e );
    }
  }
  
  public static <T> T eval( String code ) throws ScriptExecutionFailedException {
    try {
      return ( T ) getGroovyEngine( ).eval( code );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( "Executing the requested script failed:\n"
                                                + "============================\n"
                                                + code
                                                + "============================\n"
                                                + "\nbecause of:\n" + Exceptions.causeString( e ), e );
    }
  }
  
  public static void loadConfig( String confFile ) {
    try {
      confFile = SubDirectory.SCRIPTS + File.separator + confFile;
      String className = Thread.currentThread( ).getStackTrace( )[2].getClassName( );
      LOG.info( "Trying to load config for " + className + " from " + confFile );
      String conf = "import " + className;
      String line = null;
      try {
        BufferedReader fileReader = null;
        try {
          fileReader = new BufferedReader( new FileReader( confFile ) );
          for ( ; ( line = fileReader.readLine( ) ) != null; conf += !line.matches( "\\s*\\w+\\s*=[\\s\\.\\w*\"']*;{0,1}" )
                                                                                                                           ? ""
                                                                                                                           : "\n" + className + "." + line );
          LOG.debug( conf );
          try {
            getGroovyEngine( ).eval( conf );
          } catch ( ScriptException e ) {
            if ( !( e.getCause( ) instanceof ReadOnlyPropertyException ) ) {
              LOG.warn( e, e );
            } else {
              LOG.warn( e.getMessage( ) );
            }
          }
        } finally {
          if ( fileReader != null ) {
            fileReader.close( );
          }
        }
      } catch ( FileNotFoundException e ) {
        LOG.info( "-> No config file found." );
      }
    } catch ( Exception e ) {
      LOG.debug( e, e );
    }
  }
  
}
