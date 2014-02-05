/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.binding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.annotation.Nullable;
import javax.persistence.Transient;
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
import com.eucalyptus.bootstrap.BillOfMaterials;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

public class BindingCache {
  private static Logger LOG = Logger.getLogger( BindingCache.class );
  
  public static void compileBindings( ) {
    BindingFileSearch.compile( );
  }
  
  enum BindingFileSearch implements Predicate<URI> {
    INSTANCE;
    private static final String                 BINDING_EMPTY                = "<binding>\n</binding>";
    private static final Boolean                BINDING_DEBUG                = System.getProperty( "euca.binding.debug" ) != null;
    private static final Boolean                BINDING_DEBUG_EXTREME        = System.getProperty( "euca.binding.debug.extreme" ) != null;
    private static List<URI>                    BINDING_LIST                 = Lists.newArrayList( );
    private static ConcurrentMap<String, Class> BINDING_CLASS_MAP            = Maps.newConcurrentMap( );
    /**
     * We need to track the default computed element name assignments per class in order to
     * determine multiple assignments during binnding time so we can do conflict resolution.
     */
    private static Multimap<String, Class>      BINDING_CLASS_ELEMENT_MAP    = HashMultimap.create( );
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
        if ( !diffBindings.entriesDiffering( ).isEmpty( ) ) {
          LOG.info( "Binding class cache expired (old,new): \n" + diffBindings.entriesDiffering( ) );
          DeleteRecursively.PREDICATE.apply( SubDirectory.CLASSCACHE.getFile( ) );
          SubDirectory.CLASSCACHE.getFile( ).mkdir( );
        }
        return false;
      }
    }
    
    enum DeleteRecursively implements Predicate<File> {
      PREDICATE;
      
      @Override
      public boolean apply( @Nullable File input ) {
        try {
          if ( input.isDirectory( ) ) {
            LOG.info( "Cleaning up class cache: " + input.getCanonicalPath( ) );
            Iterables.all( Arrays.asList( input.listFiles( ) ), DeleteRecursively.PREDICATE );
            input.delete( );
          } else {
            input.delete( );
          }
        } catch ( SecurityException ex ) {
          LOG.error( ex );
          throw ex;
        } catch ( Exception ex ) {
          LOG.error( ex );
        }
        return true;
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
        DeleteRecursively.PREDICATE.apply( SubDirectory.CLASSCACHE.getFile( ) );
        SubDirectory.CLASSCACHE.getFile( ).mkdir( );
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
        byte[] digestBytes = Files.hash( f, Hashing.md5() ).asBytes( );
        String digest = new BigInteger( digestBytes ).abs( ).toString( 16 );
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
                InputSupplier<InputStream> classSupplier = Resources.newInputStreamSupplier( ClassLoader.getSystemResource( j.getName( ) ) );
                File destClassFile = SubDirectory.CLASSCACHE.getChildFile( j.getName( ) );
                if ( !destClassFile.exists( ) ) {
                  Files.createParentDirs( destClassFile );
                  Files.copy( classSupplier, destClassFile );
                  Logs.extreme( ).debug( "Caching: " + j.getName( ) + " => " + destClassFile.getAbsolutePath( ) );
                }
                BINDING_CLASS_MAP.putIfAbsent( classGuess, candidate );
                BINDING_CLASS_ELEMENT_MAP.put( candidate.getSimpleName( ), candidate );
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
      LOG.debug( "Binding cache: loading binding from: " + bindingFullPath );
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
                  Logs.extreme( ).debug( "Caching: " + classFile.getName( ) + " => " + destClassFile.getAbsolutePath( ) );
                } else if ( child instanceof IncludeElement ) {
                  IncludeElement includeElement = ( IncludeElement ) child;
                  BindingElement bind = includeElement.getBinding( );
                  if ( bind != null ) {
                    this.apply( bind );
                  } else {
                    Files.write( BINDING_EMPTY.getBytes( ), SubDirectory.CLASSCACHE.getChildFile( includeElement.getIncludePath( ).replace( "classpath:", "" ) ) );
                  }
                }
              } catch ( Exception ex ) {
                LOG.error( "Failed in caching message class for mapping element: " + ((MappingElementBase)child).getClassName( ) + " because of: " + ex.getMessage( ), ex );
              }
            }
            return true;
          }
        };
        if ( !writeFile.apply( root ) ) {
          writeFile.apply( root );
        }
        return true;
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
    public static void compile( ) {
      LOG.info( "Binding cache: processing message and binding files." );
      processFiles( );
      if ( BindingFileSearch.INSTANCE.check( ) ) {
        LOG.info( "Binding cache: nothing to do." );
      } else {
        LOG.info( "Binding cache: regenerating cache." );
        try {
          LOG.info( "Binding cache: generating internal bindings." );
          // generate msgs-binding
          InternalSoapBindingGenerator gen = new InternalSoapBindingGenerator( );
          for ( Class genBindClass : BindingFileSearch.BINDING_CLASS_MAP.values( ) ) {
            Logs.extreme( ).debug( "Generating binding: " + genBindClass );
            gen.processClass( genBindClass );
          }
          gen.close( );
          BINDING_LIST.add( gen.getOutFile( ).toURI( ) );
          LOG.info( "Binding cache: populating cache from transitive closure of bindings." );
          // load *-binding.xml, populate cache w/ all referenced files
          BindingFileSearch.reset( Utility.getClassPaths( ) );
          Iterables.all( BindingFileSearch.BINDING_LIST, BindingFileSearch.INSTANCE );
          BindingFileSearch.reset( Utility.getClassPaths( ) );
          LOG.info( "Binding cache: loading and validating bindings." );
          Map<URI, BindingDefinition> bindingDefs = Maps.newTreeMap( );
          PrintStream oldOut = System.out, oldErr = System.err;
          
          for ( URI binding : BINDING_LIST ) {
            String shortPath = binding.toURL( ).getPath( ).replaceAll( ".*!/", "" );
            String sname = Utility.bindingFromFileName( shortPath );
            BindingDefinition def = Utility.loadBinding( binding.toASCIIString( ), sname, binding.toURL( ).openStream( ), binding.toURL( ), true );
            bindingDefs.put( binding, def );
            def.print( );
          }
          LOG.info( "Binding cache: compiling bindings." );
          for ( Entry<URI, BindingDefinition> def : bindingDefs.entrySet( ) ) {
            try {
              LOG.debug( "Binding cache: " + def.getKey( ) );
              def.getValue( ).generateCode( BindingFileSearch.BINDING_DEBUG, BindingFileSearch.BINDING_DEBUG_EXTREME );
            } catch ( RuntimeException e ) {
              throw new JiBXException( "\n*** Error during code generation for file '" +
                                       def.getKey( ) + "' -\n this may be due to an error in " +
                                       "your binding or classpath, or to an error in the " +
                                       "JiBX code ***\n", e );
            }
          }
          ClassFile[][] lists = MungedClass.fixDispositions( );
          for ( BindingDefinition def : bindingDefs.values( ) ) {
            def.addClassList( lists[0], lists[1] );
          }
          MungedClass.writeChanges( );
          LOG.info( "Binding cache: wrote " + lists[0].length + " files" );
          LOG.info( "Binding cache: kept " + lists[1].length + " files unchanged:" );
          LOG.info( "Binding cache: deleted " + lists[2].length + " files:" );
          BindingFileSearch.INSTANCE.store( );
          System.exit( 123 );//success! now we restart.
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          System.exit( 1 );
          throw new Error( "Failed to prepare the system while trying to compile bindings: " + ex.getMessage( ), ex );
        }
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
  
  private static class InternalSoapBindingGenerator {
    private final String             ns           = "http://msgs.eucalyptus.com/" + BillOfMaterials.getVersion( );
    private static String            INDENT       = "";
    private final File               outFile;
    private PrintWriter              out;
    private String                   bindingName;
    private int                      indent       = 0;
    private Map<String, TypeBinding> typeBindings = new HashMap<String, TypeBinding>( ) {
                                                    {
                                                      put( Integer.class.getCanonicalName( ), new IntegerTypeBinding( ) );
                                                      put( Boolean.class.getCanonicalName( ), new BooleanTypeBinding( ) );
                                                      put( String.class.getCanonicalName( ), new StringTypeBinding( ) );
                                                      put( Long.class.getCanonicalName( ), new LongTypeBinding( ) );
                                                      put( Double.class.getCanonicalName( ), new DoubleTypeBinding( ) );
                                                      put( "boolean", new BooleanTypeBinding( ) );
                                                      put( "int", new IntegerTypeBinding( ) );
                                                      put( "long", new LongTypeBinding( ) );
                                                      put( "double", new DoubleTypeBinding( ) );
                                                      put( java.util.Date.class.getCanonicalName( ), new StringTypeBinding( ) );
                                                    }
                                                  };
    
    private static List<String>      badClasses   = new ArrayList<String>( ) {
                                                    {
                                                      add( ".*HttpResponseStatus" );
                                                      add( ".*Closure" );
                                                      add( ".*Channel" );
                                                      add( ".*\\.JiBX_*" );
                                                    }
                                                  };
    private static List<String>      badFields    = new ArrayList<String>( ) {
                                                    {
                                                      add( "__.*" );
                                                      add( "\\w*\\$\\w*\\$*.*" );
                                                      add( "class\\$.*" );
                                                      add( "metaClass" );
                                                      add( "JiBX_.*" );
                                                      add( "serialVersionUID" );
                                                    }
                                                  };
    
    public InternalSoapBindingGenerator( ) {
      this.outFile = new File( SubDirectory.CLASSCACHE.getFile( ).getAbsolutePath( ) + "/msgs-binding.xml" );
    }
    
    public ElemItem peek( ) {
      return this.elemStack.peek( );
    }
    
    private static Set<String> classNames = new TreeSet<String>( );
    
    public void processClass( Class klass ) {
      if ( this.out == null ) {
        if ( this.outFile.exists( ) ) {
          this.outFile.delete( );
        }
        try {
          this.out = new PrintWriter( this.outFile );
        } catch ( FileNotFoundException e ) {
          e.printStackTrace( System.err );
          System.exit( -1 );//GRZE: special case to fail build
        }
        this.bindingName = this.ns.replaceAll( "(http://)|(/$)", "" ).replaceAll( "[./-]", "_" );
        this.out.write( "<binding xmlns:euca=\"" + this.ns + "\" name=\"" + this.bindingName + "\">\n" );
        this.out.write( "  <namespace uri=\"" + this.ns + "\" default=\"elements\" prefix=\"euca\"/>\n" );
        this.out.flush( );
      }
      if ( !classNames.contains( klass.getName( ) ) ) {
        classNames.add( klass.getName( ) );
        final String mapping = new RootObjectTypeBinding( klass ).process( );
        this.out.write( mapping );
        this.out.flush( );
      } else {
        Logs.extreme( ).debug( "Skipping duplicate class: " + klass );
      }
    }
    
    //TODO:GRZE: just use the suffix for the moment.
    private String getNamespacePrefix( final Class klass ) {
      return getNamespaceSuffix( klass );
    }

    private String getNamespaceSuffix( final Class klass ) {
      String namespace = "";
      final ComponentMessage componentMessage =
        Ats.inClassHierarchy( klass ).get( ComponentMessage.class );
      if ( componentMessage != null ) {
        namespace += Classes.newInstance( componentMessage.value( ) ).name( );
      } else {
        namespace += "euca";//bad bad person, abondoning your message types.
      }
      return namespace;
    }
    
    public void close( ) {
      try {
        this.out.flush( );
        this.out.write( "</binding>" );
        this.out.flush( );
        this.out.close( );
      } catch ( Exception ex ) {
        ex.printStackTrace( );
      }
    }
    
    public TypeBinding getTypeBinding( Field field ) {
      Class itsType = field.getType( );
      if ( this.isIgnored( field ) ) {
        return new NoopTypeBinding( field );
      } else if ( List.class.isAssignableFrom( itsType ) ) {
        Class listType = getTypeArgument( field );
        if ( listType == null ) {
          Logs.extreme( ).debug(
            String.format( "IGNORE: %-70s [type=%s] NO GENERIC TYPE FOR LIST\n", field.getDeclaringClass( ).getCanonicalName( ) + "." + field.getName( ),
              listType ) );
          return new NoopTypeBinding( field );
        } else if ( this.typeBindings.containsKey( listType.getCanonicalName( ) ) ) {
          return new CollectionTypeBinding( field.getName( ), this.typeBindings.get( listType.getCanonicalName( ) ) );
        } else if ( BindingFileSearch.INSTANCE.MSG_DATA_CLASS.isAssignableFrom( listType ) ) {
          return new CollectionTypeBinding( field.getName( ), new ObjectTypeBinding( field.getName( ), listType ) );
        } else {
          Logs.extreme( ).debug(
            String.format( "IGNORE: %-70s [type=%s] LIST'S GENERIC TYPE DOES NOT CONFORM TO EucalyptusData\n", field.getDeclaringClass( ).getCanonicalName( )
                                                                                                               + "." + field.getName( ),
              listType.getCanonicalName( ) ) );
          return new NoopTypeBinding( field );
        }
      } else if ( this.typeBindings.containsKey( itsType.getCanonicalName( ) ) ) {
        TypeBinding t = this.typeBindings.get( itsType.getCanonicalName( ) );
        try {
          t = this.typeBindings.get( itsType.getCanonicalName( ) ).getClass( ).newInstance( );
        } catch ( Exception e ) {}
        return t.value( field.getName( ) );
      } else if ( BindingFileSearch.INSTANCE.MSG_DATA_CLASS.isAssignableFrom( field.getType( ) ) ) {
        return new ObjectTypeBinding( field );
      } else {
        Logs.extreme( ).debug( String.format( "IGNORE: %-70s [type=%s] TYPE DOES NOT CONFORM TO EucalyptusData\n",
          field.getDeclaringClass( ).getCanonicalName( ) + "." + field.getName( ), field.getType( ).getCanonicalName( ) ) );
        return new NoopTypeBinding( field );
      }
    }
    
    class RootObjectTypeBinding extends TypeBinding {
      private Class   type;
      private String  namespace;
      private String  nsPrefix;
      private boolean abs;
      
      public RootObjectTypeBinding( Class type ) {
        InternalSoapBindingGenerator.this.indent = 2;
        this.type = type;
        this.namespace = ns + "/" + getNamespaceSuffix( type );
        this.nsPrefix = getNamespacePrefix( type );
        if ( Object.class.equals( type.getSuperclass( ) ) ) {
          this.abs = true;
        } else if ( type.getSuperclass( ).getSimpleName( ).equals( "EucalyptusData" ) ) {
          this.abs = true;
        } else {
          this.abs = false;
        }
      }
      
      @Override
      public String getTypeName( ) {
        return this.type.getCanonicalName( );
      }
      
      public String process( ) {
        if ( this.type.getCanonicalName( ) == null ) {
//          new RuntimeException( "Ignoring anonymous class: " + this.type ).printStackTrace( );
        } else {
          this.elem( Elem.mapping );
          if ( this.abs ) {
            this.attr( "abstract", "true" );
          } else {
            String elementName = this.type.getSimpleName( );
            /**
             * GRZE: This tells us there is an element naming conflict.
             * Since we cannot agree, nobody gets the element name
             * ==> We prepend the component simple name (if we can find it)
             */
            if ( BindingFileSearch.BINDING_CLASS_ELEMENT_MAP.get( elementName ).size( ) > 1 ) {
              if ( Ats.inClassHierarchy( this.type ).has( ComponentMessage.class ) ) {
                ComponentMessage compMsg = Ats.inClassHierarchy( this.type ).get( ComponentMessage.class );
                elementName = compMsg.value( ).getSimpleName( ) + "." + elementName;
                LOG.info( "Binding generation encountered an element naming conflict.  Using " + elementName + " for " + this.type.getCanonicalName( ) );
              } else {
                /**
                 * GRZE:WTF: this is a degenerate case which is ugly:
                 * 1. we have found a naming conflict for the element
                 * 2. we have /not/ found a component id which would allows us to give the element a
                 * qualified unique name
                 * So...
                 * 3. we use the fully qualified class name...
                 * ==> the auteur has screwed up way earlier and it isn't our problem here.
                 * Log something so we don't feel to guilty.
                 */
                elementName = this.type.getCanonicalName( );
                LOG.error( "BUG: Fix your message type definitions for " + this.type );
                LOG.error( "BUG: Binding generation encountered an element naming conflict.  Using " + elementName + " for " + this.type.getCanonicalName( ) );
              }
            }
//GRZE:TODO: looks like namespace mapping doesn't actual account for naming conflicts, come back to this later.
//            this.attr( "ns", namespace );
            this.attr( "name", elementName );
            this.attr( "extends", this.type.getSuperclass( ).getCanonicalName( ) );
          }
          this.attr( "class", this.type.getCanonicalName( ) );
//GRZE:TODO: looks like namespace mapping doesn't actual account for naming conflicts, come back to this later.
//          this.elem( Elem.namespace ).attr( "uri", namespace ).attr( "default", "elements" ).attr( "prefix", this.nsPrefix ).end( );
          if ( BindingFileSearch.INSTANCE.MSG_BASE_CLASS.isAssignableFrom( this.type.getSuperclass( ) )
               || BindingFileSearch.INSTANCE.MSG_DATA_CLASS.isAssignableFrom( this.type.getSuperclass( ) ) ) {
            this.elem( Elem.structure ).attr( "map-as", this.type.getSuperclass( ).getCanonicalName( ) ).end( );
          }
          for ( Field f : this.type.getDeclaredFields( ) ) {
            if ( !Ats.from( f ).has( Transient.class ) || Modifier.isTransient( f.getModifiers( ) ) ) {
              TypeBinding tb = getTypeBinding( f );
              if ( !( tb instanceof NoopTypeBinding ) ) {
//                System.out.printf( "BOUND:  %-70s [type=%s:%s]\n", f.getDeclaringClass( ).getCanonicalName( ) +"."+ f.getName( ), tb.getTypeName( ), f.getType( ).getCanonicalName( ) );          
                this.append( tb.toString( ) );
              }
            }
          }
          this.end( );
        }
        return this.toString( );
      }
    }
    
    @SuppressWarnings( "unchecked" )
    public static Class getTypeArgument( Field f ) {
      Type t = f.getGenericType( );
      if ( t != null && t instanceof ParameterizedType ) {
        Type tv = ( ( ParameterizedType ) t ).getActualTypeArguments( )[0];
        if ( tv instanceof Class ) {
          return ( ( Class ) tv );
        }
      }
      return null;
    }
    
    abstract class TypeBinding {
      private StringBuilder buf = new StringBuilder( );
      
      public abstract String getTypeName( );
      
      private TypeBinding reindent( int delta ) {
        InternalSoapBindingGenerator.this.indent += delta;
        INDENT = "";
        for ( int i = 0; i < InternalSoapBindingGenerator.this.indent; i++ ) {
          INDENT += "  ";
        }
        return this;
      }
      
      private TypeBinding indent( String addMe ) {
        this.reindent( +1 ).append( INDENT ).append( addMe );
        return this;
      }
      
      private TypeBinding outdent( String addMe ) {
        this.reindent( -1 ).append( INDENT ).append( addMe );
        return this;
      }
      
      protected TypeBinding append( Object o ) {
        this.buf.append( "" + o );
        return this;
      }
      
      protected TypeBinding eolIn( ) {
        this.append( "\n" ).indent( INDENT );
        return this;
      }
      
      protected TypeBinding eolOut( ) {
        this.append( "\n" ).outdent( INDENT );
        return this;
      }
      
      protected TypeBinding eol( ) {
        this.append( "\n" ).append( INDENT );
        return this;
      }
      
      protected TypeBinding value( String name ) {
        this.elem( Elem.value ).attr( "name", name ).attr( "field", name ).attr( "usage", "optional" ).attr( "style", "element" ).end( );
        return this;
      }
      
      private TypeBinding begin( ) {
        ElemItem top = InternalSoapBindingGenerator.this.elemStack.peek( );
        if ( top != null && top.children ) {
          this.eol( );
        } else if ( top != null && !top.children ) {
          this.append( ">" ).eolIn( );
          top.children = true;
        } else {
          this.eolIn( );
        }
        return this;
      }
      
      protected TypeBinding elem( Elem name ) {
        this.begin( ).append( "<" ).append( name.toString( ) ).append( " " );
        InternalSoapBindingGenerator.this.elemStack.push( new ElemItem( name, InternalSoapBindingGenerator.this.indent, false ) );
        return this;
      }
      
      protected TypeBinding end( ) {
        ElemItem top = InternalSoapBindingGenerator.this.elemStack.pop( );
        if ( top != null && top.children ) {
          this.eolOut( ).append( "</" ).append( top.name.toString( ) ).append( ">" );
        } else if ( top != null && !top.children ) {
          this.append( "/>" );
        } else {
          this.append( "/>" );
        }
        return this;
      }
      
      protected TypeBinding attr( String name, String value ) {
        this.append( name ).append( "=\"" ).append( value ).append( "\" " );
        return this;
      }
      
      public String toString( ) {
        String s = this.buf.toString( );
        this.buf = new StringBuilder( this.buf.capacity( ) );
        return s;
      }
      
      protected TypeBinding collection( String name ) {
        this.elem( Elem.structure ).attr( "name", name ).attr( "usage", "optional" );
        this.elem( Elem.collection ).attr( "factory", "com.eucalyptus.binding.Binding.listFactory" ).attr( "field", name )
            .attr( "item-type", this.getTypeName( ) ).attr( "usage", "required" );
        this.elem( Elem.structure ).attr( "name", "item" );
        this.elem( Elem.value ).attr( "name", "entry" ).end( ).end( ).end( ).end( );
        return this;
      }
      
    }
    
    public boolean isIgnored( final Field field ) {
      final int mods = field.getModifiers( );
      final String name = field.getName( );
      final String type = field.getType( ).getSimpleName( );
      if ( Modifier.isFinal( mods ) ) {
        Logs.extreme( ).debug( "Ignoring field with bad type: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type
                               + " due to: final modifier" );
      } else if ( Modifier.isStatic( mods ) ) {
        Logs.extreme( ).debug( "Ignoring field with bad type: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type
                               + " due to: static modifier" );
      }
      boolean ret = Iterables.any( badClasses, new Predicate<String>( ) {
        @Override
        public boolean apply( String arg0 ) {
          if ( type.matches( arg0 ) ) {
            Logs.extreme( ).debug(
              "Ignoring field with bad type: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type + " due to: " + arg0 );
            return true;
          } else {
            return false;
          }
        }
      } );
      ret |= Iterables.any( badFields, new Predicate<String>( ) {
        @Override
        public boolean apply( String arg0 ) {
          if ( name.matches( arg0 ) ) {
            Logs.extreme( ).debug(
              "Ignoring field with bad name: " + field.getDeclaringClass( ).getCanonicalName( ) + "." + name + " of type " + type + " due to: " + arg0 );
            return true;
          } else {
            return false;
          }
        }
      } );
      
      return ret;
    }
    
    private class ElemItem {
      Elem    name;
      int     indentCount;
      boolean children;
      
      public ElemItem( Elem name, int indent, boolean children ) {
        this.name = name;
        this.indentCount = indent;
        this.children = children;
      }
      
      @Override
      public String toString( ) {
        return String.format( "ElemItem [name=%s, indent=%s, children=%s]", this.name, this.indentCount, Boolean.valueOf( this.children ) );
      }
      
    }
    
    private Deque<ElemItem> elemStack = new LinkedList<ElemItem>( );
    
    enum Elem {
      structure,
      collection,
      value,
      mapping,
      binding,
      namespace
    }
    
    class IgnoredTypeBinding extends NoopTypeBinding {
      
      public IgnoredTypeBinding( Field field ) {
        super( field );
      }
    }
    
    class NoopTypeBinding extends TypeBinding {
      private String name;
      private Class  type;
      
      public NoopTypeBinding( Field field ) {
        this.name = field.getName( );
        this.type = field.getType( );
      }
      
      @Override
      public String toString( ) {
        return "";
      }
      
      @Override
      public String getTypeName( ) {
        return "NOOP";
      }
      
    }
    
    class ObjectTypeBinding extends TypeBinding {
      private String name;
      private Class  type;
      
      public ObjectTypeBinding( String name, Class type ) {
        this.name = name;
        this.type = type;
      }
      
      public ObjectTypeBinding( Field field ) {
        this.name = field.getName( );
        this.type = field.getType( );
      }
      
      @Override
      protected TypeBinding collection( String name ) {
        this.elem( Elem.structure ).attr( "name", name ).attr( "usage", "optional" );
        this.elem( Elem.collection ).attr( "factory", "com.eucalyptus.binding.Binding.listFactory" ).attr( "field", name ).attr( "usage", "required" );
        this.elem( Elem.structure ).attr( "name", "item" ).attr( "map-as", this.type.getCanonicalName( ) );
        this.end( ).end( ).end( );
        return this;
      }
      
      @Override
      public String getTypeName( ) {
        return this.type.getCanonicalName( );
      }
      
      public String toString( ) {
        this.elem( Elem.structure ).attr( "name", this.name ).attr( "field", this.name ).attr( "map-as", this.type.getCanonicalName( ) ).attr( "usage",
          "optional" ).end( );
        return super.toString( );
      }
      
    }
    
    class CollectionTypeBinding extends TypeBinding {
      private TypeBinding type;
      private String      name;
      
      public CollectionTypeBinding( String name, TypeBinding type ) {
        this.name = name;
        this.type = type;
        Logs.extreme( ).debug( "Found list type: " + type.getClass( ).getCanonicalName( ) );
      }
      
      @Override
      public String getTypeName( ) {
        return this.type.getTypeName( );
      }
      
      @Override
      public String toString( ) {
        Logs.extreme( ).debug( "Found list type: " + this.type.getTypeName( ) + " for name: " + this.name );
        String ret = this.type.collection( this.name ).buf.toString( );
        this.type.collection( this.name ).buf = new StringBuilder( );
        return ret;
      }
      
    }
    
    class IntegerTypeBinding extends TypeBinding {
      @Override
      public String getTypeName( ) {
        return Integer.class.getCanonicalName( );
      }
    }
    
    class LongTypeBinding extends TypeBinding {
      @Override
      public String getTypeName( ) {
        return Long.class.getCanonicalName( );
      }
    }
    
    class DoubleTypeBinding extends TypeBinding {
      @Override
      public String getTypeName( ) {
        return Double.class.getCanonicalName( );
      }
    }
    
    class StringTypeBinding extends TypeBinding {
      @Override
      public String getTypeName( ) {
        return String.class.getCanonicalName( );
      }
    }
    
    class BooleanTypeBinding extends TypeBinding {
      @Override
      public String getTypeName( ) {
        return Boolean.class.getCanonicalName( );
      }
    }
    
    public File getOutFile( ) {
      return this.outFile;
    }
    
  }
  
}
