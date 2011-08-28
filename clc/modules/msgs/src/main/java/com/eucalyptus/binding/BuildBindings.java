package com.eucalyptus.binding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.jibx.binding.Compile;
import com.google.common.collect.Sets;

public class BuildBindings extends Task {
  private List<FileSet> classFileSets = null;
  
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
          System.out.println( "Found class directory: " + buildDir );
          dirs.add( buildDir );
        }
      }
    }
    return dirs.toArray( new String[] {} );
  }
  
  private URL[] pathUrls( ) {
    Set<URL> dirUrls = new HashSet<URL>( );
    for ( FileSet fs : this.classFileSets ) {
      final String dirName = fs.getDir( getProject( ) ).getAbsolutePath( );
      for ( String d : fs.getDirectoryScanner( getProject( ) ).getIncludedFiles( ) ) {
        final String buildDir = dirName + File.separator + d.replaceAll( "build/.*", "build" );
        try {
          URL buildDirUrl = new File( buildDir ).toURL( );
          if ( !dirUrls.contains( buildDirUrl ) ) {
            System.out.println( "Found class directory: " + buildDirUrl );
            dirUrls.add( buildDirUrl );
          }
        } catch ( MalformedURLException ex ) {
          ex.printStackTrace( );
          throw new RuntimeException( ex );
        }
      }
    }
    for ( File f : new File( this.project.getBaseDir( ).getAbsolutePath( ) + File.separator + "lib" ).listFiles( new FilenameFilter( ) {
      @Override
      public boolean accept( File dir, String name ) {
        return name.endsWith( ".jar" );
      }
    } ) ) {
      try {
        dirUrls.add( f.toURL( ) );
      } catch ( MalformedURLException ex ) {
        ex.printStackTrace( );
        throw new RuntimeException( ex );
      }
    }
    return dirUrls.toArray( new URL[] {} );
  }
  
  PrintStream oldOut = System.out, oldErr = System.err;
  
  public void execute( ) {
    PrintStream buildLog;
    if ( this.classFileSets.isEmpty( ) ) {
      throw new BuildException( "No classes were provided to bind." );
    } else {
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
      runPreBindingGenerators( pathUrls( ) );
      
    }
  }
  
  private void runPreBindingGenerators( URL[] urls ) {
    ClassLoader old = Thread.currentThread( ).getContextClassLoader( );
    ClassLoader cl = getUrlClassLoader( );
    Thread.currentThread( ).setContextClassLoader( cl );
    try {
      BindingGenerator.MSG_TYPE = cl.loadClass( "edu.ucsb.eucalyptus.msgs.BaseMessage" );
      BindingGenerator.DATA_TYPE = cl.loadClass( "edu.ucsb.eucalyptus.msgs.EucalyptusData" );
      Map<String, Class> classes = new ConcurrentHashMap<String, Class>( ) {
        {
          put( BindingGenerator.MSG_TYPE.getName( ), BindingGenerator.MSG_TYPE );
          put( BindingGenerator.DATA_TYPE.getName( ), BindingGenerator.DATA_TYPE );
        }
      };
      for ( FileSet fs : this.classFileSets ) {
        for ( String classFileName : fs.getDirectoryScanner( getProject( ) ).getIncludedFiles( ) ) {
          try {
            if ( !classFileName.endsWith( "class" ) ) continue;
            Class c = cl.loadClass( classFileName.replaceFirst( "[^/]*/[^/]*/", "" ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" ) );
            classes.put( c.getName( ), c );
          } catch ( ClassNotFoundException e ) {
            e.printStackTrace( );
          }
        }
      }
      try {
        for ( Class c : classes.values( ) ) {
          if ( BindingGenerator.MSG_TYPE.isAssignableFrom( c ) || BindingGenerator.DATA_TYPE.isAssignableFrom( c ) ) {
            for ( BindingGenerator gen : BindingGenerator.getPreGenerators( ) ) {
              gen.processClass( c );
            }
          }
          classes.remove( c.getName( ) );
        }
      } finally {
        for ( BindingGenerator gen : BindingGenerator.getPreGenerators( ) ) {
          gen.close( );
        }
      }
    } catch ( Exception e1 ) {
      e1.printStackTrace( );
    } finally {
      Thread.currentThread( ).setContextClassLoader( old );
    }
  }
  
  private ClassLoader getUrlClassLoader( ) {
    ClassLoader cl = URLClassLoader.newInstance( this.pathUrls( ), Thread.currentThread( ).getContextClassLoader( ) );
    return cl;
  }
  
}
