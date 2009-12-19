/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DeferredInitializer;
import com.eucalyptus.bootstrap.NeedsDeferredInitialization;
import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.event.EventListener;
import com.google.common.collect.Lists;

public class ServiceJarFile extends JarFile {
  private static Logger       LOG = Logger.getLogger( ServiceJarFile.class );
  private URLClassLoader      classLoader;
  private List<Class>         bootstrappers;
  private Map<String, String> components;

  @SuppressWarnings( { "deprecation", "unchecked" } )
  public ServiceJarFile( File f ) throws IOException {
    super( f );
    Properties props = new Properties( );
    this.bootstrappers = Lists.newArrayList( );
    Enumeration<JarEntry> jarList = this.entries( );
    this.classLoader = URLClassLoader.newInstance( new URL[] { f.getAbsoluteFile( ).toURL( ) } );
    LOG.info( "-> Trying to load component info from " + f.getAbsolutePath( ) );
    while ( jarList.hasMoreElements( ) ) {
      JarEntry j = jarList.nextElement( );
      LOG.trace( "--> Handling entry: " + j.getName( ) );
      if ( j.getName( ).endsWith( ".class" ) ) {
        try {
          Class c = this.getEventListener( j );
          LOG.info( "---> Loading event listener from entry: " + j.getName( ) );
        } catch ( Exception e ) {
          LOG.trace( e, e );
        }
        try {
          Class c = this.getBootstrapper( j );
          LOG.info( "---> Loading bootstrapper from entry: " + j.getName( ) );
          this.bootstrappers.add( c );
        } catch ( Exception e ) {
          LOG.trace( e, e );
        }
        try {
	      this.addDeferredInitializers(j);
		} catch (Exception e) {
	      LOG.trace( e, e );
		}
      }
    }
  }

  @SuppressWarnings("unchecked")
private void addDeferredInitializers(JarEntry j) throws Exception {
	String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( ".class", "" );
    Class candidate = this.classLoader.loadClass( classGuess );
    if(candidate.getAnnotation(NeedsDeferredInitialization.class) != null) {
    	NeedsDeferredInitialization needsDeferredInit = (NeedsDeferredInitialization) candidate.getAnnotation(NeedsDeferredInitialization.class);
    	if(needsDeferredInit.component().isEnabled())
    	  DeferredInitializer.getInstance().add(candidate);
    }
  }

@SuppressWarnings( "unchecked" )
  public List<Bootstrapper> getBootstrappers( ) {
    List<Bootstrapper> ret = Lists.newArrayList( );
    for ( Class c : this.bootstrappers ) {
      if ( c.equals( SystemBootstrapper.class ) ) continue;
      try {
        LOG.debug( "-> Calling <init>()V on bootstrapper: " + c.getCanonicalName( ) );
        try {
          ret.add( ( Bootstrapper ) c.newInstance( ) );
        } catch ( Exception e ) {
          LOG.debug( "-> Calling getInstance()L; on bootstrapper: " + c.getCanonicalName( ) );
          Method m = c.getDeclaredMethod( "getInstance", new Class[] {} );
          ret.add( ( Bootstrapper ) m.invoke( null, new Object[] {} ) );
        }
      } catch ( Exception e ) {
        LOG.warn( "Error in <init>()V and getInstance()L; in bootstrapper: " + c.getCanonicalName( ) );
        LOG.warn( e.getMessage( ) );
        // LOG.debug( e, e );
      }
    }
    return ret;
  }

  @SuppressWarnings( "unchecked" )
  private Class getBootstrapper( JarEntry j ) throws Exception {
    String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( ".class", "" );
    Class candidate = this.classLoader.loadClass( classGuess );
    if ( Bootstrapper.class.equals( candidate ) ) throw new InstantiationException( Bootstrapper.class + " is abstract." );
    if ( !Bootstrapper.class.isAssignableFrom( candidate ) ) throw new InstantiationException( candidate + " does not conform to " + Bootstrapper.class );
    LOG.warn( "Candidate bootstrapper: " + candidate.getName( ) );
    if ( !Modifier.isPublic( candidate.getDeclaredConstructor( new Class[] {} ).getModifiers( ) ) ) {
      Method factory = candidate.getDeclaredMethod( "getInstance", new Class[] {} );
      if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) { throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public <init>()V or public static getInstance()L;" ); }
    }
    LOG.info( "Found bootstrapper: " + candidate.getName( ) );
    return candidate;
  }

  @SuppressWarnings( "unchecked" )
  private Class getEventListener( JarEntry j ) throws Exception {
    String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( ".class", "" );
    Class candidate = this.classLoader.loadClass( classGuess );
    if ( !EventListener.class.isAssignableFrom( candidate ) ) throw new InstantiationException( candidate + " does not conform to " + EventListener.class );
    LOG.warn( "Candidate event listener: " + candidate.getName( ) );
    Method factory;
    factory = candidate.getDeclaredMethod( "register", new Class[] {} );
    if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public static register()V" );
    LOG.info( "-> Registered event listener: " + candidate.getName( ) );
    factory.invoke( null, new Object[]{} );
    return candidate;
  }
}
