package com.eucalyptus.scripting.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.ReadOnlyPropertyException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.log4j.Logger;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.system.SubDirectory;

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

  public static <T> T newInstance( String fileName ) throws ScriptExecutionFailedException {
    GroovyObject groovyObject = null;
    try {
      ClassLoader parent = ClassLoader.getSystemClassLoader( );
      GroovyClassLoader loader = new GroovyClassLoader( parent );
      File f = new File( fileName );
      if( !f.exists( ) ) {
        f = new File( SubDirectory.SCRIPTS + File.separator + fileName + (fileName.endsWith(".groovy")?"":".groovy") );
      }
      Class groovyClass = loader.parseClass( f );  
      groovyObject = ( GroovyObject ) groovyClass.newInstance();
    }
    catch ( Exception e ) {
      throw new ScriptExecutionFailedException( e );
    }
    try {
      return ( T ) groovyObject;
    } catch ( ClassCastException e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( e.getMessage( ), e );
    }
  }

  public static <T> T evaluateScript( SubDirectory dir, String fileName ) throws ScriptExecutionFailedException {
    fileName = dir + File.separator + fileName;
    String fileNameWExt = fileName + ".groovy";
    if( !new File( fileName ).exists( ) && new File( fileNameWExt ).exists( ) ) {
      fileName = fileNameWExt;
    }
    FileReader fileReader = null;
    try {
      fileReader = new FileReader( fileName );
      T ret = ( T ) getGroovyEngine().eval( fileReader );
      return ret;
    } catch ( Throwable e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( "Executing the requested script failed: " + fileName, e );
    } finally {
      if(fileReader != null)
      try {
        fileReader.close();
      } catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  public static Object evaluateScript( String fileName ) throws ScriptExecutionFailedException {
    return evaluateScript( SubDirectory.SCRIPTS, fileName );
  }

  public static int exec( final String code ) throws ScriptExecutionFailedException {
    try {
      return (Integer) getGroovyEngine().eval( "p=hi.execute();p.waitFor();System.out.print(p.in.text);System.err.print(p.err.text);p.exitValue()", new SimpleScriptContext() {{
        setAttribute( "hi", code, ENGINE_SCOPE );
      }});
    } catch ( Throwable e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( "Executing the requested script failed: " + code, e );
    }
  }

  public static Object eval( String code, Map context ) throws ScriptExecutionFailedException {
    try {
      Bindings bindings = new SimpleBindings(context);
      SimpleScriptContext scriptContext = new SimpleScriptContext();
      scriptContext.setBindings( bindings, SimpleScriptContext.ENGINE_SCOPE );
      return getGroovyEngine().eval( code, scriptContext );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( "Executing the requested script failed: " + code, e );
    }
  }

  public static Object eval( String code ) throws ScriptExecutionFailedException {
    try {
      return getGroovyEngine().eval( code );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
      throw new ScriptExecutionFailedException( "Executing the requested script failed: " + code, e );
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
        BufferedReader fileReader = new BufferedReader( new FileReader( confFile ) );
        for(;
            (line = fileReader.readLine( ))!=null;
            conf += !line.matches("\\s*\\w+\\s*=[\\s\\.\\w*\"']*;{0,1}")?"":"\n"+className+"."+line);
        LOG.debug( conf );
        fileReader.close();
        try {
          getGroovyEngine( ).eval( conf );
        } catch ( ScriptException e ) {
          if( !(e.getCause( ) instanceof ReadOnlyPropertyException ) ) {
            LOG.warn( e, e );
          } else {
            LOG.warn( e.getMessage( ) );
          }
        }
      } catch ( FileNotFoundException e ) {
        LOG.info( "-> No config file found." );
      }
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
  }

}
