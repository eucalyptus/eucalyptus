package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.persistence.PersistenceContext;
import org.apache.bcel.util.ClassPath;
import org.apache.log4j.Logger;
import org.jibx.binding.Utility;
import org.jibx.binding.classes.BoundClass;
import org.jibx.binding.classes.BranchWrapper;
import org.jibx.binding.classes.ClassCache;
import org.jibx.binding.classes.ClassFile;
import org.jibx.binding.classes.MungedClass;
import org.jibx.binding.def.BindingDefinition;
import org.jibx.binding.model.BindingElement;
import org.jibx.binding.model.ElementBase;
import org.jibx.binding.model.IncludeElement;
import org.jibx.binding.model.MappingElement;
import org.jibx.binding.model.MappingElementBase;
import org.jibx.binding.model.ValidationContext;
import org.jibx.runtime.JiBXException;
import org.jibx.util.ClasspathUrlExtender;
import com.eucalyptus.binding.InternalSoapBindingGenerator;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

/**
 * TODO: DOCUMENT
 */
public abstract class ServiceJarDiscovery implements Comparable<ServiceJarDiscovery> {
  private static Logger                         LOG       = Logger.getLogger( ServiceJarDiscovery.class );
  private static SortedSet<ServiceJarDiscovery> discovery = Sets.newTreeSet( );
  private static Multimap<Class, String>        classList = ArrayListMultimap.create( );
  
  enum BindingFileSearch implements Predicate<URI> {
    INSTANCE;
    private static final Boolean                BINDING_DEBUG                = System.getProperty( "euca.binding.debug" ) != null;
    private static List<URI>                    BINDING_LIST                 = Lists.newArrayList( );
    private static ConcurrentMap<String, Class> BINDING_CLASS_MAP            = Maps.newConcurrentMap( );
    private static final String                 BINDING_CACHE_JAR_PREFIX     = "jar.";
    private static final String                 BINDING_CACHE_BINDING_PREFIX = "binding.";
    private static final String                 BINDING_CACHE_DIGEST_LIST    = "classcache.properties";
    private static final File                   CACHE_LIST                   = SubDirectory.CLASSCACHE.getChildFile( BINDING_CACHE_DIGEST_LIST );
    private final ClassLoader                   CACHE_CLASS_LOADER;
    private final Class<?>                      MSG_BASE_CLASS;
    private final Class<?>                      MSG_DATA_CLASS;
    private static final String                 FILE_PATTERN                 = System.getProperty( "euca.binding.pattern", ".*\\-binding.xml" );
    private static final Properties             CURRENT_PROPS                = new Properties( );
    
    private BindingFileSearch( ) {
      try {
        CACHE_CLASS_LOADER = new URLClassLoader( new URL[] { SubDirectory.CLASSCACHE.getFile( ).toURL( ) } );
        MSG_BASE_CLASS = Class.forName( "edu.ucsb.eucalyptus.msgs.BaseMessage" );
        MSG_DATA_CLASS = Class.forName( "edu.ucsb.eucalyptus.msgs.EucalyptusData" );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw Exceptions.toUndeclared( ex );
      }
    }
    
    public boolean check( ) {
      final Properties oldProps = new Properties( );
      if ( BindingFileSearch.CACHE_LIST.exists( ) ) {
        try {
          Reader propIn = Files.newReader( BindingFileSearch.CACHE_LIST, Charset.defaultCharset( ) );
          oldProps.load( propIn );
        } catch ( Exception ex ) {
          LOG.debug( ex, ex );
        }
      }
      Map<String, String> oldBindings = Maps.fromProperties( oldProps );
      Map<String, String> newBindings = Maps.fromProperties( BindingFileSearch.CURRENT_PROPS );
      if ( oldBindings.equals( newBindings ) ) {
        LOG.info( "Found up-to-date binding class cache: skipping message binding." );
        return true;
      } else {
        MapDifference<String, String> diffBindings = Maps.difference( oldBindings, newBindings );
        LOG.info( "Binding class cache expired (old,new): \n" + diffBindings.entriesDiffering( ) );
        try {
          Files.deleteRecursively( SubDirectory.CLASSCACHE.getFile( ) );
        } catch ( IOException ex ) {
          LOG.error( ex, ex );
        }
        SubDirectory.CLASSCACHE.getFile( ).mkdir( );
        return false;
      }
    }
    
    public void store( ) throws IOException {
      Writer propOut = new FileWriter( CACHE_LIST );
      try {
        try {
          CURRENT_PROPS.store( propOut, "Binding class cache generated on: " );
          propOut.close( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          propOut.close( );
        }
      } catch ( IOException ex ) {
        for ( File f : SubDirectory.CLASSCACHE.getFile( ).listFiles( ) ) {
          try {
            LOG.info( "Cleaning up class cache: " + f.getCanonicalPath( ) );
            Files.deleteRecursively( f );
          } catch ( IOException ex1 ) {
            LOG.error( ex1, ex1 );
          }
        }
        throw ex;
      }
    }
    
    public void process( File f ) throws Exception {
      if ( f.isDirectory( ) ) {
        File[] files = f.listFiles( new FilenameFilter( ) {
          
          @Override
          public boolean accept( File dir, String name ) {
            return name.matches( FILE_PATTERN );
          }
        } );
        for ( File ff : files ) {
          byte[] bindingBytes = Files.toByteArray( ff );
          this.addCurrentBinding( bindingBytes, ff.getName( ), "file:" + ff.getAbsolutePath( ) );
        }
      } else {
        String digest = new BigInteger( Files.getDigest( f, Digest.MD5.get( ) ) ).abs( ).toString( 16 );
        CURRENT_PROPS.put( BINDING_CACHE_JAR_PREFIX + f.getName( ), digest );
        final JarFile jar = new JarFile( f );
        final List<JarEntry> jarList = Collections.list( jar.entries( ) );
        for ( final JarEntry j : jarList ) {
          try {
            if ( j.getName( ).matches( FILE_PATTERN ) ) {
              byte[] bindingBytes = ByteStreams.toByteArray( jar.getInputStream( j ) );
              String bindingName = j.getName( );
              String bindingFullPath = "jar:file:" + f.getAbsolutePath( ) + "!/" + bindingName;
              this.addCurrentBinding( bindingBytes, bindingName, bindingFullPath );
            } else if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
              final String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
              final Class candidate = ClassLoader.getSystemClassLoader( ).loadClass( classGuess );
              if ( MSG_BASE_CLASS.isAssignableFrom( candidate ) || MSG_DATA_CLASS.isAssignableFrom( candidate ) ) {
                if ( BINDING_CLASS_MAP.putIfAbsent( classGuess, candidate ) == null ) {
                  InputSupplier<InputStream> classSupplier = Resources.newInputStreamSupplier( ClassLoader.getSystemResource( j.getName( ) ) );
                  File destClassFile = SubDirectory.CLASSCACHE.getChildFile( j.getName( ) );
                  if ( !destClassFile.exists( ) ) {
                    Files.createParentDirs( destClassFile );
                    Files.copy( classSupplier, destClassFile );
                    if ( BINDING_DEBUG ) {
                      LOG.info( "Caching: " + j.getName( ) + " => " + destClassFile.getAbsolutePath( ) );
                    }
                  }
                }
              }
            }
          } catch ( RuntimeException ex ) {
            LOG.error( ex, ex );
            jar.close( );
            throw ex;
          }
        }
        jar.close( );
      }
    }
    
    private void addCurrentBinding( byte[] bindingBytes, String bindingName, String bindingFullPath ) {
      LOG.info( "Loading binding from: " + bindingFullPath );
      BINDING_LIST.add( URI.create( bindingFullPath ) );
      String digest = new BigInteger( Digest.MD5.get( ).digest( bindingBytes ) ).abs( ).toString( 16 );
      String entryName = BINDING_CACHE_BINDING_PREFIX + bindingName;
      if ( !CURRENT_PROPS.containsKey( entryName ) ) {
        CURRENT_PROPS.put( entryName, digest );
      } else {
        //TODO:GRZE finish up this case.
        LOG.info( "Duplicate binding entry: " + CURRENT_PROPS.getProperty( entryName ) );
      }
    }
    
    @Override
    public boolean apply( URI input ) {
      try {
        String shortPath = input.toURL( ).getPath( ).replaceAll( ".*!/", "" );
        String sname = Utility.bindingFromFileName( shortPath );
//        BindingDefinition def = Utility.loadBinding( input.toASCIIString( ), sname, input.toURL( ).openStream( ), input.toURL( ), true );
        ValidationContext vctx = BindingElement.newValidationContext( );
        BindingElement root = BindingElement.validateBinding( input.toASCIIString( ), input.toURL( ), input.toURL( ).openStream( ), vctx );
        Predicate<BindingElement> writeFile = new Predicate<BindingElement>( ) {
          
          @SuppressWarnings( "unchecked" )
          @Override
          public boolean apply( BindingElement input ) {
            for ( ElementBase child : ( List<ElementBase> ) input.topChildren( ) ) {
              try {
                if ( child instanceof MappingElement ) {
                  MappingElementBase mapping = ( MappingElementBase ) child;
                  ClassFile classFile = mapping.getHandledClass( ).getClassFile( );
                  String classFileName = classFile.getName( ).replace( ".", "/" ) + ".class";
                  InputSupplier<InputStream> classSupplier = Resources.newInputStreamSupplier( ClassLoader.getSystemResource( classFileName ) );
                  File destClassFile = SubDirectory.CLASSCACHE.getChildFile( classFileName );
                  if ( !destClassFile.exists( ) ) {
                    Files.createParentDirs( destClassFile );
                    Files.copy( classSupplier, destClassFile );
                  }
                  ClassFile cf = ClassFile.getClassFile( classFile.getName( ) );
                  if ( BINDING_DEBUG ) {
                    LOG.info( "Caching: " + classFile.getName( ) + " => " + destClassFile.getAbsolutePath( ) );
                  }
                } else if ( child instanceof IncludeElement ) {
                  BindingElement bind = ( ( IncludeElement ) child ).getBinding( );
                  if ( bind != null ) {
                    this.apply( bind );
                  }
                }
              } catch ( Exception ex ) {
                LOG.error( ex, ex );
              }
            }
            return true;
          }
        };
        writeFile.apply( root );
        return true;
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
    public static void compile( ) {
      processFiles( );
      if ( !BindingFileSearch.INSTANCE.check( ) ) {
        try {
          InternalSoapBindingGenerator gen = new InternalSoapBindingGenerator( );
          gen.getOutFile( ).delete( );
          // load *-binding.xml, populate cache w/ all referenced files
          BindingFileSearch.reset( Utility.getClassPaths( ) );
          Iterables.all( BindingFileSearch.BINDING_LIST, BindingFileSearch.INSTANCE );
          // generate msgs-binding
          for ( Class genBindClass : BindingFileSearch.BINDING_CLASS_MAP.values( ) ) {
            if ( BINDING_DEBUG ) {
              LOG.info( "Generating binding: " + genBindClass );
            }
            gen.processClass( genBindClass );
          }
          gen.close( );
          BINDING_LIST.add( gen.getOutFile( ).toURI( ) );
          BindingFileSearch.reset( Utility.getClassPaths( ) );
          Map<URI, BindingDefinition> bindingDefs = Maps.newHashMap( );
          for ( URI binding : BINDING_LIST ) {
            String shortPath = binding.toURL( ).getPath( ).replaceAll( ".*!/", "" );
            String sname = Utility.bindingFromFileName( shortPath );
            BindingDefinition def = Utility.loadBinding( binding.toASCIIString( ), sname, binding.toURL( ).openStream( ), binding.toURL( ), true );
//            def.setFactoryLocation( "", SubDirectory.CLASSCACHE.getFile( ) );
            bindingDefs.put( binding, def );
          }
          for ( Entry<URI, BindingDefinition> def : bindingDefs.entrySet( ) ) {
//            def.setFactoryLocation( "", SubDirectory.CLASSCACHE.getFile( ) );
            try {
              LOG.info( "Compiling binding: " + def.getKey( ) );
              def.getValue( ).generateCode( BindingFileSearch.BINDING_DEBUG, BindingFileSearch.BINDING_DEBUG );
            } catch ( RuntimeException e ) {
              throw new JiBXException( "\n*** Error during code generation for file '" +
                                           def.getKey( ) + "' -\n this may be due to an error in " +
                                           "your binding or classpath, or to an error in the " +
                                           "JiBX code ***\n", e );
            }
          }
          // get the lists of class names modified, kept unchanged, and unused
          ClassFile[][] lists = MungedClass.fixDispositions( );
          // add class used list to each binding factory and output files
          for ( BindingDefinition def : bindingDefs.values( ) ) {
            def.addClassList( lists[0], lists[1] );
          }
          MungedClass.writeChanges( );
          // report modified file results to user
          ClassFile[] adds = lists[0];
          int addcount = adds.length;
          LOG.info( "\nWrote " + addcount + " files" );
          
          // report summary information for files unchanged or deleted
          if ( BindingFileSearch.BINDING_DEBUG ) {
            ClassFile[] keeps = lists[1];
            LOG.info( "\nKept " + keeps.length + " files unchanged:" );
            for ( int i = 0; i < keeps.length; i++ ) {
              LOG.info( " " + keeps[i].getName( ) );
            }
            ClassFile[] dels = lists[2];
            LOG.info( "\nDeleted " + dels.length + " files:" );
            for ( int i = 0; i < dels.length; i++ ) {
              LOG.info( " " + dels[i].getName( ) );
            }
          }
          BindingFileSearch.INSTANCE.store( );
          
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          System.exit( 1 );
          throw new Error( "Failed to prepare the system while trying to compile bindings: " + ex.getMessage( ), ex );
        }
        System.exit( 1 );
      }
    }
    
    public static void processFiles( ) {
      final File libDir = new File( BaseDirectory.LIB.toString( ) );
      for ( final File f : libDir.listFiles( ) ) {
        if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" )
               && !f.getName( ).matches( ".*-ext-.*" ) ) {
          EventRecord.here( ServiceJarDiscovery.class, EventType.BOOTSTRAP_INIT_SERVICE_JAR, f.getName( ) ).info( );
          try {
            BindingFileSearch.INSTANCE.process( f );
          } catch ( final Throwable e ) {
            LOG.error( e.getMessage( ) );
            continue;
          }
        }
      }
      for ( String pathName : ClassPath.SYSTEM_CLASS_PATH.getClassPath( ).split( File.pathSeparator ) ) {
        File pathFile = new File( pathName );
        if ( pathFile.isDirectory( ) ) {
          try {
            BindingFileSearch.INSTANCE.process( pathFile );
          } catch ( final Throwable e ) {
            LOG.error( e.getMessage( ) );
            continue;
          };
        }
      }
    }
    
    public static String[] reset( String[] paths ) {
      ClassCache.setPaths( paths );
      ClassFile.setPaths( paths );
      ClasspathUrlExtender.setClassLoader( ClassFile.getClassLoader( ) );
      BoundClass.reset( );
      MungedClass.reset( );
      BindingDefinition.reset( );
      BranchWrapper.setTracking( false );
      BranchWrapper.setErrorOverride( false );
      return paths;
    }
  }
  
  enum JarFilePass {
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
                final Bootstrap.Discovery annote   = Ats.from( candidate ).get( Bootstrap.Discovery.class );
                final Predicate<Class>    instance = ( Predicate<Class> ) Classes.builder( candidate ).newInstance( );
                
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
    
    JarFilePass( ) {}
    
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
    BindingFileSearch.compile( );
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
