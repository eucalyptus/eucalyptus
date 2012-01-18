package com.eucalyptus.scripting;

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
