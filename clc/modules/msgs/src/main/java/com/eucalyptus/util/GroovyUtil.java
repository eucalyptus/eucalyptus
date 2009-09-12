package com.eucalyptus.util;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class GroovyUtil {
  public static ScriptEngine groovyEngine;
  static {
    ScriptEngineManager manager = new ScriptEngineManager();
    groovyEngine = manager.getEngineByName( "groovy" );
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
      groovyEngine.eval( new FileReader( SubDirectory.SCRIPTS + File.separator + fileName ) );
    } catch ( Throwable e ) {
      throw new FailScriptFailException( "Executing the requested script failed: " + fileName, e );
    }
  }

  public static Object eval( String code ) throws FailScriptFailException {
    try {
      return groovyEngine.eval( code );
    } catch ( Throwable e ) {
      throw new FailScriptFailException( "Executing the requested script failed: " + code, e );
    }
  }

}
