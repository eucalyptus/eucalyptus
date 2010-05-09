package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public abstract class ServiceJarDiscovery implements Comparable<ServiceJarDiscovery> {
  private static Logger                         LOG       = Logger.getLogger( ServiceJarDiscovery.class );
  private static SortedSet<ServiceJarDiscovery> discovery = Sets.newTreeSet( );
  private static Multimap<Class, String>        classList = Multimaps.newArrayListMultimap( );
  
  @SuppressWarnings( { "deprecation", "unchecked" } )
  public static void processFile( File f ) throws IOException {
    JarFile jar = new JarFile( f );
    Properties props = new Properties( );
    Enumeration<JarEntry> jarList = jar.entries( );
    LOG.info( "-> Trying to load component info from " + f.getAbsolutePath( ) );
    while ( jarList.hasMoreElements( ) ) {
      JarEntry j = jarList.nextElement( );
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
              jar.close();
              throw new RuntimeException( e );
            }
          }
        } catch ( ClassNotFoundException e ) {
          LOG.debug( e, e );
        }
      }
    }
    jar.close();
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
      EventRecord.here( ServiceJarDiscovery.class, EventType.DISCOVERY_STARTED, s.getClass( ).getSimpleName( ) ).info( );
      for ( Class c : classList.keySet( ) ) {
        try {
          s.checkClass( c );
        } catch ( Throwable t ) {
          LOG.debug( t, t );
        }
      }
    }
    for ( ServiceJarDiscovery s : discovery ) {
      EventRecord.here( ServiceJarDiscovery.class, EventType.DISCOVERY_FINISHED, s.getClass( ).getSimpleName( ) ).info( );
    }
  }
  
  public void checkClass( Class candidate ) {
    try {
      if ( this.processsClass( candidate ) ) {
        ServiceJarDiscovery.checkUniqueness( candidate );
        EventRecord.here( ServiceJarDiscovery.class, EventType.DISCOVERY_LOADED_ENTRY, this.getClass( ).getSimpleName( ), candidate.getName( ) ).info( );
      }
    } catch ( Throwable e ) {
      LOG.trace( e, e );
    }
  }
  
  /**
   * Process the potential bootstrap-related class. Return false or throw an exception if the class is rejected.
   * 
   * @param candidate
   * @return TODO
   * @throws Throwable
   */
  public abstract boolean processsClass( Class candidate ) throws Throwable;
  
  public Double getDistinctPriority( ) {
    return this.getPriority( ) + ( .1d / this.getClass( ).hashCode( ) );
  }
  
  public abstract Double getPriority( );
  
  @Override
  public int compareTo( ServiceJarDiscovery that ) {
    return this.getDistinctPriority( ).compareTo( that.getDistinctPriority( ) );
  }
  
}
