package com.eucalyptus.binding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.jibx.binding.Compile;

public class BuildBindings extends Task {
  private List<FileSet> classFileSets   = null;
  
  @Override
  public void init( ) throws BuildException {
    super.init( );
    this.classFileSets = new ArrayList<FileSet>( );
  }
  
  public void addClassFileSet( FileSet classFiles ) {
    this.classFileSets.add( classFiles );
  }
  
  private String[] paths( ) {
    Set<String> dirs = new HashSet<String>( );
    for ( FileSet fs : this.classFileSets ) {
      final String dirName = fs.getDir( getProject( ) ).getAbsolutePath( );
      for ( String d : fs.getDirectoryScanner( getProject( ) ).getIncludedFiles( ) ) {
        final String buildDir = dirName + File.separator + d.replaceAll( "build/.*", "build" );
        if ( !dirs.contains( buildDir ) ) {
          log( "Found class directory: " + buildDir );
          dirs.add( buildDir );
        }
      }
    }
    return dirs.toArray( new String[] {} );
  }
  
  PrintStream oldOut = System.out, oldErr = System.err;
  public void error( Throwable e ) {
    e.printStackTrace( System.err );
    System.setOut( this.oldOut );
    System.setErr( this.oldErr );
    e.printStackTrace( System.err );
    log( "ERROR See clc/bind.log for additional information: " + e.getMessage( ) );
    System.exit( -1 );
  }
  
  public void execute( ) {
    PrintStream buildLog;
    try {
      buildLog = new PrintStream( new FileOutputStream( "bind.log", false ) );
      System.setOut( buildLog );
      System.setErr( buildLog );
      if ( this.classFileSets.isEmpty( ) ) {
        throw new BuildException( "No classes were provided to bind." );
      } else {
        try {
          System.setProperty( "java.class.path", ( ( AntClassLoader ) BuildBindings.class.getClassLoader( ) ).getClasspath( ) );
        } catch ( Exception e ) {
          System.err.println( "Failed setting classpath from Ant task" );
        }
        Path path = new Path( getProject( ) );
        for ( String p : paths( ) ) {
          path.add( new Path( getProject( ), p ) );
        }
        for ( File f : new File( "lib" ).listFiles( new FilenameFilter( ) {
          @Override
          public boolean accept( File dir, String name ) {
            return name.endsWith( ".jar" );
          }
        } ) ) {
          path.add( new Path( getProject( ), f.getAbsolutePath( ) ) );
        }
        runPreBindingGenerators( path );
      }
    } catch ( FileNotFoundException e2 ) {
      System.setOut( this.oldOut );
      System.setErr( this.oldErr );
    } finally {
      System.setOut( this.oldOut );
      System.setErr( this.oldErr );
    }
    
  }  
  private void runPreBindingGenerators( Path path ) {
    AntClassLoader loader = this.getProject( ).createClassLoader( path );
    loader.setThreadContextLoader( );
    try {
      BindingGenerator.MSG_TYPE = loader.forceLoadClass( "edu.ucsb.eucalyptus.msgs.BaseMessage" );
      BindingGenerator.DATA_TYPE = loader.forceLoadClass( "edu.ucsb.eucalyptus.msgs.EucalyptusData" );
      loader.forceLoadClass( "org.jibx.binding.model.JiBX_bindingFactory" );
      for ( FileSet fs : this.classFileSets ) {
        for ( String classFileName : fs.getDirectoryScanner( getProject( ) ).getIncludedFiles( ) ) {
          try {
            if ( !classFileName.endsWith( "class" ) ) continue;
            Class c = loader.forceLoadClass( classFileName.replaceFirst( "[^/]*/[^/]*/", "" ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" ) );
            if ( BindingGenerator.MSG_TYPE.isAssignableFrom( c ) || BindingGenerator.DATA_TYPE.isAssignableFrom( c ) ) {
              for ( BindingGenerator gen : BindingGenerator.getPreGenerators( ) ) {
                gen.processClass( c );
              }
            }
          } catch ( ClassNotFoundException e ) {
            error( e );
          }
        }
      }
    } catch ( ClassNotFoundException e1 ) {
      error( e1 );
    } finally {
      try {
        for ( BindingGenerator gen : BindingGenerator.getPreGenerators( ) ) {
          gen.close( );
        }
      } catch ( Throwable e ) {
        error( e );
      }
      loader.resetThreadContextLoader( );
      loader.cleanup( );
    }
  }
  
}