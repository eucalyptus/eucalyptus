package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * TODO: DOCUMENT
 */
public abstract class ServiceJarDiscovery implements Comparable<ServiceJarDiscovery> {
  private static Logger                         LOG       = Logger.getLogger( ServiceJarDiscovery.class );
  private static SortedSet<ServiceJarDiscovery> discovery = Sets.newTreeSet( );
  private static Multimap<Class, String>        classList = ArrayListMultimap.create( );
  
  @SuppressWarnings( { "deprecation", "unchecked" } )
  private static void processFile( File f ) throws IOException {
    JarFile jar = new JarFile( f );
    Properties props = new Properties( );
    List<JarEntry> jarList = Collections.list( jar.entries( ) );
    LOG.trace( "-> Trying to load component info from " + f.getAbsolutePath( ) );
    for ( JarEntry j : jarList ) {
      if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
        String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
        try {
          Class candidate = ClassLoader.getSystemClassLoader( ).loadClass( classGuess );
          classList.put( candidate, f.getAbsolutePath( ) );
          if ( ServiceJarDiscovery.class.isAssignableFrom( candidate ) && !ServiceJarDiscovery.class.equals( candidate ) ) {
            try {
              ServiceJarDiscovery discover = ( ServiceJarDiscovery ) candidate.newInstance( );
              discovery.add( discover );
            } catch ( Exception e ) {
              LOG.fatal( e, e );
              jar.close( );
              throw new RuntimeException( e );
            }
          }
        } catch ( ClassNotFoundException e ) {
          LOG.debug( e, e );
        }
      }
    }
    jar.close( );
  }
  
  private static void doDiscovery( ) {
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
           && !f.getName( ).matches( ".*-ext-.*" ) ) {
        LOG.debug( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          ServiceJarDiscovery.processFile( f );
        } catch ( Throwable e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    ServiceJarDiscovery.runDiscovery( );
  }
  
  public static void doSingleDiscovery( ServiceJarDiscovery s ) {
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
             && !f.getName( ).matches( ".*-ext-.*" ) ) {
        LOG.debug( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          ServiceJarDiscovery.processFile( f );
        } catch ( Throwable e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    ServiceJarDiscovery.runDiscovery( s );
  }
  
  public static void checkUniqueness( Class c ) {
    if ( classList.get( c ).size( ) > 1 ) {
      
      LOG.fatal( "Duplicate bootstrap class registration: " + c.getName( ) );
      for ( String fileName : classList.get( c ) ) {
        LOG.fatal( "\n==> Defined in: " + fileName );
      }
      System.exit( 1 );
    }
  }
  
  public static void runDiscovery( ) {
    for ( ServiceJarDiscovery s : discovery ) {
      EventRecord.here( ServiceJarDiscovery.class, EventType.BOOTSTRAP_INIT_DISCOVERY, s.getClass( ).getCanonicalName( ) ).info( );
    }
    for ( ServiceJarDiscovery s : discovery ) {
      runDiscovery( s );
    }
  }
  
  public static void runDiscovery( ServiceJarDiscovery s ) {
    LOG.info( LogUtil.subheader( s.getClass( ).getSimpleName( ) ) );
    for ( Class c : classList.keySet( ) ) {
      try {
        s.checkClass( c );
      } catch ( Throwable t ) {
        LOG.debug( t, t );
      }
    }
  }
  
  private void checkClass( Class candidate ) {
    try {
      if ( this.processClass( candidate ) ) {
        ServiceJarDiscovery.checkUniqueness( candidate );
        EventRecord.here( ServiceJarDiscovery.class, EventType.DISCOVERY_LOADED_ENTRY, this.getClass( ).getSimpleName( ), candidate.getName( ) ).info( );
      }
    } catch ( Throwable e ) {
      if ( e instanceof InstantiationException ) {} else {
        LOG.trace( e, e );
      }
    }
  }
  
  /**
   * Process the potential bootstrap-related class. Return false or throw an exception if the class
   * is rejected.
   * 
   * @param candidate
   * @return true if the candidate is accepted.
   * 
   * @throws Throwable
   */
  public abstract boolean processClass( Class candidate ) throws Throwable;
  
  public Double getDistinctPriority( ) {
    return this.getPriority( ) + ( .1d / this.getClass( ).hashCode( ) );
  }
  
  public abstract Double getPriority( );
  
  @Override
  public int compareTo( ServiceJarDiscovery that ) {
    return this.getDistinctPriority( ).compareTo( that.getDistinctPriority( ) );
  }
  
  public static void processLibraries( ) {
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
           && !f.getName( ).matches( ".*-ext-.*" ) ) {
        Bootstrap.LOG.info( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          processFile( f );
        } catch ( Throwable e ) {
          Bootstrap.LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
  }
  
  public static URLClassLoader makeClassLoader( File libDir ) {
    URLClassLoader loader = new URLClassLoader( Lists.transform( Arrays.asList( libDir.listFiles( ) ), new Function<File, URL>( ) {
      @Override
      public URL apply( File arg0 ) {
        try {
          return URI.create( "file://" + arg0.getAbsolutePath( ) ).toURL( );
        } catch ( MalformedURLException e ) {
          LOG.debug( e, e );
          return null;
        }
      }
    } ).toArray( new URL[] {} ) );
    return loader;
  }
  
  public static List<String> contextsInDir( File libDir ) {
    ClassLoader oldLoader = Thread.currentThread( ).getContextClassLoader( );
    try {
      Thread.currentThread( ).setContextClassLoader( makeClassLoader( libDir ) );
      Set<String> ctxs = Sets.newHashSet( );
      for ( Class candidate : getClassList( libDir ) ) {
        if ( PersistenceContexts.isEntityClass( candidate ) ) {
          if ( Ats.from( candidate ).has( PersistenceContext.class ) ) {
            ctxs.add( Ats.from( candidate ).get( PersistenceContext.class ).name( ) );
          }
        }
      }
      return Lists.newArrayList( ctxs );
    } finally {
      Thread.currentThread( ).setContextClassLoader( oldLoader );
    }
  }
  
  public static List<Class> classesInDir( File libDir ) {
    ClassLoader oldLoader = Thread.currentThread( ).getContextClassLoader( );
    try {
      Thread.currentThread( ).setContextClassLoader( makeClassLoader( libDir ) );
      return getClassList( libDir );
    } finally {
      Thread.currentThread( ).setContextClassLoader( oldLoader );
    }
  }
  
  private static List<Class> getClassList( File libDir ) {
    List<Class> classList = Lists.newArrayList( );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" ) && !f.getName( ).matches( ".*-ext-.*" ) ) {
//        LOG.trace( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          JarFile jar = new JarFile( f );
          for( JarEntry j : Collections.list( jar.entries( ) ) ) {
            if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
              String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
              try {
                Class candidate = ClassLoader.getSystemClassLoader( ).loadClass( classGuess );
                classList.add( candidate );
              } catch ( ClassNotFoundException e ) {
//                LOG.trace( e, e );
              }
            }
          }
          jar.close( );
        } catch ( Throwable e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    return classList;
  }
  
}
