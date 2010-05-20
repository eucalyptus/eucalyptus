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
  private List<FileSet> bindingFileSets = null;
  private List<String>  bindings        = null;
  
  @Override
  public void init( ) throws BuildException {
    super.init( );
    this.classFileSets = new ArrayList<FileSet>( );
    this.bindingFileSets = new ArrayList<FileSet>( );
    this.bindings = new ArrayList<String>( );
  }
  
  public void addClassFileSet( FileSet classFiles ) {
    this.classFileSets.add( classFiles );
  }
  
  public void addBindingFileSet( FileSet bindings ) {
    this.bindingFileSets.add( bindings );
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
  
  private String[] bindings( ) {
    List<String> bindings = new ArrayList<String>( );
    boolean addMsgs = true;
    for ( FileSet fs : this.bindingFileSets ) {
      final String dirName = fs.getDir( getProject( ) ).getAbsolutePath( );
      for ( String b : fs.getDirectoryScanner( getProject( ) ).getIncludedFiles( ) ) {
        final String bindingFilePath = dirName + File.separator + b;
        log( "Found binding: " + bindingFilePath );
        if( bindingFilePath.endsWith( "msgs-binding.xml" ) ) {
          addMsgs = false;
        }
        bindings.add( bindingFilePath );
      }
    }
    if( addMsgs ) {
      bindings.add( "modules/msgs/src/main/resources/msgs-binding.xml" );
    }
    return bindings.toArray( new String[] {} );
  }
  
  PrintStream oldOut = System.out, oldErr = System.err;
  public void error( Throwable e ) {
    e.printStackTrace( System.err );
    System.setOut( oldOut );
    System.setErr( oldErr );
    e.printStackTrace( System.err );
    log( e.getMessage( ) );
    System.exit( -1 );
  }
  
  public void execute( ) {
    PrintStream buildLog;
    try {
      buildLog = new PrintStream( new FileOutputStream( "bind.log", false ) );
      System.setOut( buildLog );
      System.setErr( buildLog );
    } catch ( FileNotFoundException e2 ) {
      System.setOut( oldOut );
      System.setErr( oldErr );      
    }
    if ( this.classFileSets.isEmpty( ) ) {
      throw new BuildException( "No classes were provided to bind." );
    } else if ( this.bindingFileSets.isEmpty( ) ) {
      throw new BuildException( "No bindings were provided to bind." );
    } else {
      try {
        System.setProperty( "java.class.path", ( ( AntClassLoader ) BuildBindings.class.getClassLoader( ) ).getClasspath( ) );
      } catch ( Exception e ) {
        System.err.println( "Failed setting classpath from Ant task" );
      }
      Path path = new Path( getProject( ) );
      for( String p : paths( ) ) {
        path.add( new Path( getProject( ), p ) );
      }
      for( File f : new File( "lib" ).listFiles( new FilenameFilter() {
        @Override
        public boolean accept( File dir, String name ) {
          return name.endsWith( ".jar" );
        }} ) ) {
        path.add( new Path( getProject( ), f.getAbsolutePath( ) ) );
      }
      ClassLoader old = Thread.currentThread( ).getContextClassLoader( );
      List<BindingGenerator> generators = BindingGenerator.getGenerators(); 
      try {
        AntClassLoader loader = this.getProject( ).createClassLoader( path );
        Thread.currentThread( ).setContextClassLoader( loader );
//        System.err.print( "class path: " + loader.getClasspath( ) );
        BindingGenerator.MSG_TYPE = loader.forceLoadClass( "edu.ucsb.eucalyptus.msgs.BaseMessage" );
        BindingGenerator.DATA_TYPE = loader.forceLoadClass( "edu.ucsb.eucalyptus.msgs.EucalyptusData" );
        loader.forceLoadClass( "org.jibx.binding.model.JiBX_bindingFactory" );
        for ( FileSet fs : this.classFileSets ) {
          for ( String classFileName : fs.getDirectoryScanner( getProject( ) ).getIncludedFiles( ) ) {
            try {
              if( !classFileName.endsWith( "class" ) ) continue;
              Class c = loader.forceLoadClass( classFileName.replaceFirst( "[^/]*/[^/]*/", "" ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" ) );
              if ( BindingGenerator.MSG_TYPE.isAssignableFrom( c ) || BindingGenerator.DATA_TYPE.isAssignableFrom( c ) ) {
                for( BindingGenerator gen : generators ) {
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
          for( BindingGenerator gen : generators ) {
            gen.close( );
          }
        } catch ( Throwable e ) {
          error( e );
        }
        Thread.currentThread( ).setContextClassLoader( old );
      }
      try {
        Compile compiler = new Compile( true, true, false, false, false );
        compiler.compile( paths( ), bindings() );
      } catch ( Throwable e ) {
        error( e );
      } finally {
        System.setOut( oldOut );
        System.setErr( oldErr );
      }
    }
  }
  
}