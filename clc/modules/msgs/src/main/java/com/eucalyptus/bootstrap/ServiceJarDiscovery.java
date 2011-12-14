package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jibx.binding.Loader;
import org.jibx.binding.Utility;
import org.jibx.runtime.JiBXException;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * TODO: DOCUMENT
 */
public abstract class ServiceJarDiscovery implements Comparable<ServiceJarDiscovery> {
  private static Logger                         LOG         = Logger.getLogger( ServiceJarDiscovery.class );
  private static SortedSet<ServiceJarDiscovery> discovery   = Sets.newTreeSet( );
  private static Multimap<Class, String>        classList   = ArrayListMultimap.create( );
  private static List<String>                   bindingList = Lists.newArrayList( );
  
  enum JarFilePass {
    BINDINGS {
      @Override
      public void process( File f ) throws Exception {
        final JarFile jar = new JarFile( f );
        final Properties props = new Properties( );
        final List<JarEntry> jarList = Collections.list( jar.entries( ) );
        LOG.trace( "-> Trying to load message binding info from " + f.getAbsolutePath( ) );
        for ( final JarEntry j : jarList ) {
          try {
            if ( j.getName( ).matches( ".*\\-binding.xml" ) ) {
              LOG.info( "Loading binding from: " + f.getAbsolutePath( ) + "!/" + j.getName( ) );
              bindingList.add( j.getName( ) );
            }
          } catch ( RuntimeException ex ) {
            LOG.error( ex, ex );
            jar.close( );
            throw ex;
          }
        }
        jar.close( );
      }
    },
    CLASSES {
      @Override
      public void process( File f ) throws Exception {
        final JarFile jar = new JarFile( f );
        final Properties props = new Properties( );
        final List<JarEntry> jarList = Collections.list( jar.entries( ) );
        LOG.trace( "-> Trying to load component info from " + f.getAbsolutePath( ) );
        for ( final JarEntry j : jarList ) {
          try {
            if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
              handleClassFile( f, j );
            }
          } catch ( RuntimeException ex ) {
            LOG.error( ex, ex );
            jar.close( );
            throw ex;
          }
        }
        jar.close( );
      }
      
      private void handleClassFile( final File f, final JarEntry j ) throws IOException, RuntimeException {
        final String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
        try {
          final Class candidate = ClassLoader.getSystemClassLoader( ).loadClass( classGuess );
          classList.put( candidate, f.getAbsolutePath( ) );
          if ( ServiceJarDiscovery.class.isAssignableFrom( candidate ) && !ServiceJarDiscovery.class.equals( candidate ) && !candidate.isAnonymousClass( ) ) {
            try {
              final ServiceJarDiscovery discover = ( ServiceJarDiscovery ) candidate.newInstance( );
              discovery.add( discover );
            } catch ( final Exception e ) {
              LOG.fatal( e, e );
              throw new RuntimeException( e );
            }
          } else if ( Ats.from( candidate ).has( Bootstrap.Discovery.class ) && Predicate.class.isAssignableFrom( candidate ) ) {
            try {
              @SuppressWarnings( { "rawtypes",
                  "unchecked" } )
              final ServiceJarDiscovery discover = new ServiceJarDiscovery( ) {
                final Bootstrap.Discovery annote = Ats.from( candidate ).get( Bootstrap.Discovery.class );
                final Predicate<Class> instance = ( Predicate<Class> ) Classes.builder( candidate ).newInstance( );
                @Override
                public boolean processClass( Class discoveryCandidate ) throws Exception {
                  boolean classFiltered =
                    this.annote.value( ).length != 0 ? Iterables.any( Arrays.asList( this.annote.value( ) ), Classes.assignableTo( discoveryCandidate ) )
                                               : true;
                  if ( classFiltered ) {
                    boolean annotationFiltered =
                      this.annote.annotations( ).length != 0 ? Iterables.any( Arrays.asList( this.annote.annotations( ) ), Ats.from( discoveryCandidate ) )
                                                       : true;
                    if ( annotationFiltered ) {
                      return this.instance.apply( discoveryCandidate );
                    } else {
                      return false;
                    }
                  } else {
                    return false;
                  }
                }
                
                @Override
                public Double getPriority( ) {
                  return this.annote.priority( );
                }
              };
              discovery.add( discover );
            } catch ( final Exception e ) {
              LOG.fatal( e, e );
              throw new RuntimeException( e );
            }
          }
        } catch ( final ClassNotFoundException e ) {
          LOG.debug( e, e );
        }
      }
      
    };
    public abstract void process( final File f ) throws Exception;
  }
  
  private static void doDiscovery( ) {
    final File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( final File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
           && !f.getName( ).matches( ".*-ext-.*" ) ) {
        LOG.debug( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          ServiceJarDiscovery.JarFilePass.CLASSES.process( f );
        } catch ( final Throwable e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    ServiceJarDiscovery.runDiscovery( );
  }
  
  public static void doSingleDiscovery( final ServiceJarDiscovery s ) {
    final File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( final File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
             && !f.getName( ).matches( ".*-ext-.*" ) ) {
        LOG.debug( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          ServiceJarDiscovery.JarFilePass.CLASSES.process( f );
        } catch ( final Throwable e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    ServiceJarDiscovery.runDiscovery( s );
  }
  
  public static void checkUniqueness( final Class c ) {
    if ( classList.get( c ).size( ) > 1 ) {
      
      LOG.fatal( "Duplicate bootstrap class registration: " + c.getName( ) );
      for ( final String fileName : classList.get( c ) ) {
        LOG.fatal( "\n==> Defined in: " + fileName );
      }
      System.exit( 1 );//GRZE: special case, broken installation
    }
  }
  
  public static void runDiscovery( ) {
    for ( final ServiceJarDiscovery s : discovery ) {
      EventRecord.here( ServiceJarDiscovery.class, EventType.BOOTSTRAP_INIT_DISCOVERY, s.getClass( ).getCanonicalName( ) ).trace( );
    }
    for ( final ServiceJarDiscovery s : discovery ) {
      runDiscovery( s );
    }
  }
  
  public static void runDiscovery( final ServiceJarDiscovery s ) {
    LOG.info( LogUtil.subheader( s.getClass( ).getSimpleName( ) ) );
    for ( final Class c : classList.keySet( ) ) {
      try {
        s.checkClass( c );
      } catch ( final Throwable t ) {
        LOG.debug( t, t );
      }
    }
  }
  
  private void checkClass( final Class candidate ) {
    try {
      if ( this.processClass( candidate ) ) {
        ServiceJarDiscovery.checkUniqueness( candidate );
        EventRecord.here( ServiceJarDiscovery.class, EventType.DISCOVERY_LOADED_ENTRY, this.getClass( ).getSimpleName( ), candidate.getName( ) ).trace( );
      }
    } catch ( final Throwable e ) {
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
   * @throws Exception
   */
  public abstract boolean processClass( Class candidate ) throws Exception;
  
  public Double getDistinctPriority( ) {
    return this.getPriority( ) + ( .1d / this.getClass( ).hashCode( ) );
  }
  
  public abstract Double getPriority( );
  
  @Override
  public int compareTo( final ServiceJarDiscovery that ) {
    return this.getDistinctPriority( ).compareTo( that.getDistinctPriority( ) );
  }
  
  public static void compileBindings( ) {
    BootstrapClassLoader jibxLoader = BootstrapClassLoader.getInstance( );
    final File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( final File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
           && !f.getName( ).matches( ".*-ext-.*" ) ) {
        EventRecord.here( ServiceJarDiscovery.class, EventType.BOOTSTRAP_INIT_SERVICE_JAR, f.getName( ) ).info( );
        try {
//          ServiceJarDiscovery.JarFilePass.BINDINGS.process( f );
        } catch ( final Throwable e ) {
          Bootstrap.LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    try {
      for ( String binding : bindingList ) {
        jibxLoader.loadResourceBinding( binding );
      }
      jibxLoader.processBindings( );
    } catch ( JiBXException ex ) {
      LOG.error( ex, ex );
      throw new Error( "Failed to prepare the system while trying to compile bindings: " + ex.getMessage( ), ex );
    } catch ( IOException ex ) {
      LOG.error( ex, ex );
      throw new Error( "Failed to prepare the system while trying to compile bindings: " + ex.getMessage( ), ex );
    }
  }
  
  public static void processLibraries( ) {
    final File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( final File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
           && !f.getName( ).matches( ".*-ext-.*" ) ) {
        EventRecord.here( ServiceJarDiscovery.class, EventType.BOOTSTRAP_INIT_SERVICE_JAR, f.getName( ) ).info( );
        try {
          ServiceJarDiscovery.JarFilePass.CLASSES.process( f );
        } catch ( final Throwable e ) {
          Bootstrap.LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
  }
  
  public static URLClassLoader makeClassLoader( final File libDir ) {
    final URLClassLoader loader = new URLClassLoader( Lists.transform( Arrays.asList( libDir.listFiles( ) ), new Function<File, URL>( ) {
      @Override
      public URL apply( final File arg0 ) {
        try {
          return URI.create( "file://" + arg0.getAbsolutePath( ) ).toURL( );
        } catch ( final MalformedURLException e ) {
          LOG.debug( e, e );
          return null;
        }
      }
    } ).toArray( new URL[] {} ) );
    return loader;
  }
  
  public static List<String> contextsInDir( final File libDir ) {
    final ClassLoader oldLoader = Thread.currentThread( ).getContextClassLoader( );
    try {
      Thread.currentThread( ).setContextClassLoader( makeClassLoader( libDir ) );
      final Set<String> ctxs = Sets.newHashSet( );
      for ( final Class candidate : getClassList( libDir ) ) {
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
  
  public static List<Class> classesInDir( final File libDir ) {
    final ClassLoader oldLoader = Thread.currentThread( ).getContextClassLoader( );
    try {
      Thread.currentThread( ).setContextClassLoader( makeClassLoader( libDir ) );
      return getClassList( libDir );
    } finally {
      Thread.currentThread( ).setContextClassLoader( oldLoader );
    }
  }
  
  private static List<Class> getClassList( final File libDir ) {
    final List<Class> classList = Lists.newArrayList( );
    for ( final File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" ) && !f.getName( ).matches( ".*-ext-.*" ) ) {
//        LOG.trace( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          final JarFile jar = new JarFile( f );
          for ( final JarEntry j : Collections.list( jar.entries( ) ) ) {
            if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
              final String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
              try {
                final Class candidate = ClassLoader.getSystemClassLoader( ).loadClass( classGuess );
                classList.add( candidate );
              } catch ( final ClassNotFoundException e ) {
//                LOG.trace( e, e );
              }
            }
          }
          jar.close( );
        } catch ( final Throwable e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    return classList;
  }
  
}
