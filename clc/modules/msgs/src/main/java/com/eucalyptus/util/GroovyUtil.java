package com.eucalyptus.util;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

public class GroovyUtil {
  private static Logger LOG = Logger.getLogger( GroovyUtil.class );
  public static ScriptEngine groovyEngine = getGroovyEngine();
  public static ScriptEngine getGroovyEngine() {
    synchronized( GroovyUtil.class ) {
      if( groovyEngine == null ) {
        ScriptEngineManager manager = new ScriptEngineManager();
        groovyEngine = manager.getEngineByName( "groovy" );        
      }
      return groovyEngine;      
    }
  }
  
  public static Object newInstance( String fileName ) throws FailScriptFailException {
    GroovyObject groovyObject = null;
    try {
      ClassLoader parent = ClassLoader.getSystemClassLoader( );
      GroovyClassLoader loader = new GroovyClassLoader( parent );
      Class groovyClass = loader.parseClass( new File( fileName ) );  
      groovyObject = ( GroovyObject ) groovyClass.newInstance();
    }
    catch ( Exception e ) {
      throw new FailScriptFailException( e );
    }
    return groovyObject;
  }

  public static void evaluateScript( String fileName ) throws FailScriptFailException {
    try {
      getGroovyEngine().eval( new FileReader( SubDirectory.SCRIPTS + File.separator + fileName ) );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
      throw new FailScriptFailException( "Executing the requested script failed: " + fileName, e );
    }
  }

  public static Object eval( String code ) throws FailScriptFailException {
    try {
      return getGroovyEngine().eval( code );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
      throw new FailScriptFailException( "Executing the requested script failed: " + code, e );
    }
  }

}
