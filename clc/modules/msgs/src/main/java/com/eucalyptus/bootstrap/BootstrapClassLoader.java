/*************************************************************************
 * Copyright 2003-2010 Dennis M. Sosnoski
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 *   Neither the name of JiBX nor the names of its contributors may
 *   be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

/**
 * Binding classloader. This is intended to substitute for the System
 * classloader (i.e., the one used for loading user classes). It first processes
 * one or more binding definitions, caching the binary classes modified by the
 * bindings. It then uses these modified forms of the classes when they're
 * requested for loading.
 *
 * @author Dennis M. Sosnoski
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jibx.binding.Loader;
import org.jibx.binding.Utility;
import org.jibx.binding.classes.BoundClass;
import org.jibx.binding.classes.ClassCache;
import org.jibx.binding.classes.ClassFile;
import org.jibx.binding.classes.MungedClass;
import org.jibx.binding.def.BindingBuilder;
import org.jibx.binding.def.BindingDefinition;
import org.jibx.binding.model.BindingElement;
import org.jibx.binding.model.IncludeElement;
import org.jibx.binding.model.MappingElement;
import org.jibx.binding.model.MappingElementBase;
import org.jibx.binding.model.ValidationContext;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.UnmarshallingContext;
import org.jibx.util.ClasspathUrlExtender;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class BootstrapClassLoader extends URLClassLoader {
  private static Logger               LOG       = Logger.getLogger( BootstrapClassLoader.class );
  private static volatile BootstrapClassLoader singleton;

  public static BootstrapClassLoader init( ) {
    try {
      if ( singleton == null ) {
        String[] paths = Utility.getClassPaths( );
        URL[] urls = new URL[paths.length];
        for ( int i = 0; i < urls.length; i++ ) {
          urls[i] = new File( paths[i] ).toURI( ).toURL( );
        }
        singleton = new BootstrapClassLoader( urls );
      }
      Thread.currentThread( ).setContextClassLoader( singleton );
      return singleton;
    } catch ( final MalformedURLException ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }

  private final List<BindingDefinition> bindings = Lists.newArrayList( );
  private boolean                       isBound;
  private final Map<String, ClassFile>  classMap = Maps.newHashMap( );

  public static BootstrapClassLoader getInstance( ) {
    return init( );
  }

  private BootstrapClassLoader( URL[] urls ) throws MalformedURLException {
    super( urls, ClassLoader.getSystemClassLoader( ) );
    List<String> fpaths = Lists.newArrayList( );
    for ( URL path : Loader.getClassPaths( ) ) {
      LOG.debug( path );
      if ( "file".equals( path.getProtocol( ) ) ) {
        fpaths.add( path.getPath( ) );
      }
    }
    // set paths to be used for loading referenced classes
    String[] dirs = ( String[] ) fpaths.toArray( new String[0] );
    ClassCache.setPaths( dirs );
    ClassFile.setPaths( dirs );
    ClasspathUrlExtender.setClassLoader( ClassFile.getClassLoader( ) );
    // reset static information accumulation for binding
    BoundClass.reset( );
    MungedClass.reset( );
    BindingDefinition.reset( );
  }

  public void reset( ) {
    this.bindings.clear( );
    this.classMap.clear( );
    this.isBound = false;
    BoundClass.reset( );
    MungedClass.reset( );
    BindingDefinition.reset( );
  }

  public static URL[] getClassPaths( ) throws MalformedURLException {
    String[] paths = Utility.getClassPaths( );
    URL[] urls = new URL[paths.length];
    for ( int i = 0; i < urls.length; i++ ) {
      urls[i] = new File( paths[i] ).toURI( ).toURL( );
    }
    return urls;
  }

  public void loadBinding( String fname, String sname, InputStream is, URL url ) throws JiBXException, IOException {
    throw new IllegalStateException( "Call not allowed: only resources can currently be loaded." );
  }

  public void loadFileBinding( String path ) throws JiBXException, IOException {
    throw new IllegalStateException( "Call not allowed: only resources can currently be loaded." );
  }

  public void loadResourceBinding( String path ) throws JiBXException, IOException {
    if ( this.isBound ) {
      throw new IllegalStateException( "Call not allowed after bindings compiled" );
    } else {
      URL url = Resources.getResource( path );
      ByteSource inSupplier = Resources.asByteSource( url );
      String fname = path;
      int split = fname.lastIndexOf( '/' );
      if ( split >= 0 ) {
        fname = fname.substring( split + 1 );
      }
      String defaultBindingName = Utility.bindingFromFileName( fname );
      try {
        ValidationContext vctx = BindingElement.newValidationContext( );
        BindingElement root = BindingElement.validateBinding( fname, url, inSupplier.openBufferedStream( ), vctx );
        if ( vctx.getErrorCount( ) == 0 && vctx.getFatalCount( ) == 0 ) {
          ClassFile classFile = findMappedClass( root );
          String tpack = root.getTargetPackage( );
          if ( tpack == null && classFile != null ) {
            tpack = classFile.getPackage( );
          }
          String bindingName = root.getName( );
          UnmarshallingContext uctx = new UnmarshallingContext( );
          uctx.setDocument( inSupplier.openBufferedStream( ), fname, null );
          if ( classFile != null ) {
            bindingName = ( bindingName == null
              ? defaultBindingName
              : bindingName );
            BoundClass.setModify( classFile.getRoot( ), tpack, bindingName );
          }
          BindingDefinition bindingDefinition = BindingBuilder.unmarshalBindingDefinition( uctx, defaultBindingName, url );
          File rootFile = null;
          if ( tpack == null ) {
            tpack = bindingDefinition.getDefaultPackage( );
          }
          if ( classFile == null ) {
            rootFile = ClassCache.getModifiablePath( );
            if ( root == null ) {
              throw new IllegalStateException( "Need modifiable directory on classpath for storing generated factory class file" );
            }
            if ( tpack == null ) {
              tpack = "";
            }
          } else {
            rootFile = classFile.getRoot( );
            if ( tpack == null ) {
              tpack = classFile.getPackage( );
            }
          }
          bindingDefinition.setFactoryLocation( tpack, rootFile );
        }
      } catch ( JiBXException ex ) {
        LOG.error( "Unable to process binding " + url, ex );
      }
    }
  }

  private static ClassFile findMappedClass( BindingElement root ) {
    ArrayList childs = root.topChildren( );
    if ( childs != null ) {

      // recursively search for modifiable mapped class
      for ( int i = childs.size( ) - 1; i >= 0; i-- ) {
        Object child = childs.get( i );
        if ( child instanceof MappingElement ) {

          // end scan if a real mapping is found
          MappingElementBase map = ( MappingElementBase ) child;
          ClassFile cf = map.getHandledClass( ).getClassFile( );
          if ( !cf.isInterface( ) && cf.isModifiable( ) ) {
            return cf;
          }

        } else if ( child instanceof IncludeElement ) {

          // recurse on included binding
          BindingElement bind = ( ( IncludeElement ) child ).getBinding( );
          if ( bind != null ) {
            ClassFile cf = findMappedClass( bind );
            if ( cf != null ) {
              return cf;
            }
          }
        }
      }
    }
    return null;
  }

  public void processBindings( ) throws JiBXException {
    if ( !this.isBound ) {
      for ( BindingDefinition binding : this.bindings ) {
        binding.generateCode( System.getProperty( "euca.debug.binding.compile" ) != null, System.getProperty( "euca.debug.binding.compile" ) != null );
      }
      ClassFile[][] lists = MungedClass.fixDispositions( );
      for ( BindingDefinition binding : this.bindings ) {
        binding.addClassList( lists[0], lists[1] );
      }
      for ( int i = 0; i < lists[0].length; i++ ) {
        ClassFile clas = lists[0][i];
        this.classMap.put( clas.getName( ), clas );
      }
      this.isBound = true;
    }
  }

  protected boolean isBoundClass( String name ) {
//    if ( !this.isBound ) {
//      try {
//        this.processBindings( );
//      } catch ( JiBXException e ) {
//        e.printStackTrace( );
//      }
//    }
    return this.classMap.containsKey( name );
  }

  protected Class findClass( String name ) throws ClassNotFoundException {
    if ( isBoundClass( name ) ) {
      try {
        ClassFile clas = ( ClassFile ) this.classMap.get( name );
        ByteArrayOutputStream bos = new ByteArrayOutputStream( );
        clas.writeFile( bos );
        byte[] bytes = bos.toByteArray( );
        return defineClass( name, bytes, 0, bytes.length );
      } catch ( IOException e ) {
        throw new ClassNotFoundException( "Unable to load modified class " + name );
      }
    } else {
      return super.findClass( name );

    }
  }

}
