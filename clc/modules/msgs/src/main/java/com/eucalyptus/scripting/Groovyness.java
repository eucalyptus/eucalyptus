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

package com.eucalyptus.scripting;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.ExpandoMetaClassCreationHandle;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Maps;

public class Groovyness {
  private static Logger      LOG          = Logger.getLogger( Groovyness.class );
  private static List<Function<String,String>> fileMappers = new CopyOnWriteArrayList<>( );
  private static ScriptEngine groovyEngine = getGroovyEngine( );

  static {
    registerFileMapper( fileName ->
        SubDirectory.SCRIPTS + File.separator + fileName + ( fileName.endsWith( ".groovy" ) ?
            "" :
            ".groovy" ) );
  }

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
  
  public static <T> T newInstance( final String fileName ) throws ScriptExecutionFailedException {
    GroovyObject groovyObject;
    try {
      final File f = new File( mapScriptName( fileName ) );
      final GroovyClassLoader loader = getGroovyClassLoader( );
      final Class groovyClass = loader.parseClass( f );
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
  
  public static <T> T run( final String fileName, final Map<String,Object> context ) {
    final File f = new File( mapScriptName( fileName ) );
    try ( final FileReader fileReader = new FileReader( f ) ) {
      final Bindings bindings = new SimpleBindings( context );
      final SimpleScriptContext scriptContext = new SimpleScriptContext( );
      scriptContext.setBindings( bindings, SimpleScriptContext.ENGINE_SCOPE );
      T ret = ( T ) getGroovyEngine( ).eval( fileReader, scriptContext );
      return ret;
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new RuntimeException( "Executing the requested script failed: " + fileName, e );
    }
  }
  
  public static <T> T run( final String fileName ) {
    return run( fileName, Maps.newHashMap( ) );
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
  
  public static <T> T eval( final String code, final Map<String,Object> context ) throws ScriptExecutionFailedException {
    try {
      final Bindings bindings = new SimpleBindings( context );
      final SimpleScriptContext scriptContext = new SimpleScriptContext( );
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
  
  public static <T> T eval( final String code ) throws ScriptExecutionFailedException {
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

  public static String mapScriptName( String fileName ) {
    if ( !new File( fileName ).exists( ) ) {
      for ( final Function<String,String> fileNameMapper : fileMappers ) {
        final String mappedFileName = fileNameMapper.apply( fileName );
        if ( new File( mappedFileName ).exists( ) ) {
          fileName = mappedFileName;
          break;
        }
      }
    }
    return fileName;
  }

  public static void registerFileMapper( final Function<String,String> fileMapper ) {
    fileMappers.add( fileMapper );
  }
}
