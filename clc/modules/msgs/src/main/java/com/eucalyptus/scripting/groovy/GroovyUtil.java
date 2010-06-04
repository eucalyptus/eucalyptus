package com.eucalyptus.scripting.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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

  public static Object evaluateScript( String fileName ) throws ScriptExecutionFailedException {
    FileReader fileReader = null;
    try {
      fileReader = new FileReader( SubDirectory.SCRIPTS + File.separator + fileName + (fileName.endsWith(".groovy")?"":".groovy") );
      return getGroovyEngine().eval( fileReader );
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
          LOG.warn( e, e );
        }
      } catch ( FileNotFoundException e ) {
        LOG.info( "-> No config file found." );
      }
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
  }

}
