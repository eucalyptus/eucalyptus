/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.entities;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import edu.emory.mathcs.backport.java.util.Collections;

@SuppressWarnings( "unchecked" )
public class PersistenceContexts {
  private static Logger                                 LOG              = Logger.getLogger( PersistenceContexts.class );
  private static Long                                   MAX_FAIL_SECONDS = 60L;                                                           //TODO:GRZE:@Configurable
  private static final int                              MAX_EMF_RETRIES  = 20;
  private static AtomicStampedReference<Long>           failCount        = new AtomicStampedReference<Long>( 0L, 0 );
  private static final ArrayListMultimap<String, Class> entities         = ArrayListMultimap.create( );
  private static final List<Class>                      sharedEntities   = Lists.newArrayList( );
  private static Map<String, EntityManagerFactoryImpl>  emf              = new ConcurrentSkipListMap<String, EntityManagerFactoryImpl>( );
  private static Multimap<String, Exception>            illegalAccesses  = ArrayListMultimap.create( );
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.PersistenceInit )
  public static class PersistenceContextBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      Groovyness.run( "setup_persistence.groovy" );
      return true;
    }
  }
  
  public static boolean isPersistentClass( Class candidate ) {
    return isSharedEntityClass( candidate ) || isEntityClass( candidate );
  }
  
  public static boolean isSharedEntityClass( Class candidate ) {
    return Ats.from( candidate ).has( MappedSuperclass.class ) || Ats.from( candidate ).has( Embeddable.class );
  }
  
  public static boolean isEntityClass( Class candidate ) {
    if ( Ats.from( candidate ).has( javax.persistence.Entity.class ) && Ats.from( candidate ).has( org.hibernate.annotations.Entity.class ) ) {
      if ( !Ats.from( candidate ).has( PersistenceContext.class ) ) {
        throw Exceptions.toUndeclared( "Database entity does not have required @PersistenceContext annotation: " + candidate.getCanonicalName( ) );
      } else {
        return true;
      }
    } else if ( Ats.from( candidate ).has( javax.persistence.Entity.class ) && !Ats.from( candidate ).has( org.hibernate.annotations.Entity.class ) ) {
      throw Exceptions.toUndeclared( "Database entity missing required annotation @org.hibernate.annotations.Entity. Database entities must have BOTH @javax.persistence.Entity and @org.hibernate.annotations.Entity annotations: "
        + candidate.getCanonicalName( ) );
    } else if ( Ats.from( candidate ).has( org.hibernate.annotations.Entity.class ) && !Ats.from( candidate ).has( javax.persistence.Entity.class ) ) {
      throw Exceptions.toUndeclared( "Database entity missing required annotation @javax.persistence.Entity. Database entities must have BOTH @javax.persistence.Entity and @org.hibernate.annotations.Entity annotations: "
        + candidate.getCanonicalName( ) );
    } else {
      return false;
    }
  }
  
  static void addEntity( Class entity ) {
    if ( !isDuplicate( entity ) ) {
      String ctxName = Ats.from( entity ).get( PersistenceContext.class ).name( );
      EventRecord.here( PersistenceContextDiscovery.class, EventType.PERSISTENCE_ENTITY_REGISTERED, ctxName, entity.getCanonicalName( ) ).trace( );
      entities.put( ctxName, entity );
    }
  }
  
  static void addSharedEntity( Class entity ) {
    if ( !isDuplicate( entity ) ) {
      EventRecord.here( PersistenceContextDiscovery.class, EventType.PERSISTENCE_ENTITY_REGISTERED, "shared", entity.getCanonicalName( ) ).trace( );
      sharedEntities.add( entity );
    }
  }
  
  private static boolean isDuplicate( Class entity ) {
    PersistenceContext ctx = Ats.from( entity ).get( PersistenceContext.class );
    if ( Ats.from( entity ).has( MappedSuperclass.class ) || Ats.from( entity ).has( Embeddable.class ) ) {
      return false;
    } else if ( ctx == null || ctx.name( ) == null ) {
      RuntimeException ex = new RuntimeException( "Failed to register broken entity class: " + entity.getCanonicalName( )
                                                  + ".  Ensure that the class has a well-formed @PersistenceContext annotation." );
      LOG.error( ex, ex );
      return false;
    } else if ( sharedEntities.contains( entity ) ) {
      Class old = sharedEntities.get( sharedEntities.indexOf( entity ) );
      LOG.error( "Duplicate entity definition detected: " + entity.getCanonicalName( ) );
      LOG.error( "=> OLD: " + old.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      LOG.error( "=> NEW: " + entity.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      throw BootstrapException.throwFatal( "Duplicate entity definition in shared entities: " + entity.getCanonicalName( )
                                           + ". See error logs for details." );
    } else if ( entities.get( ctx.name( ) ) != null && entities.get( ctx.name( ) ).contains( entity ) ) {
      List<Class> context = entities.get( ctx.name( ) );
      Class old = context.get( context.indexOf( entity ) );
      LOG.error( "Duplicate entity definition detected: " + entity.getCanonicalName( ) );
      LOG.error( "=> OLD: " + old.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      LOG.error( "=> NEW: " + entity.getProtectionDomain( ).getCodeSource( ).getLocation( ) );
      throw BootstrapException.throwFatal( "Duplicate entity definition in '" + ctx.name( )
                                           + "': "
                                           + entity.getCanonicalName( )
                                           + ". See error logs for details." );
    } else {
      return false;
    }
  }
  
  public static EntityManagerFactoryImpl registerPersistenceContext( final String persistenceContext, final Ejb3Configuration config ) {
    if ( !emf.containsKey( persistenceContext ) ) {
      try {
        LOG.trace( "-> Setting up persistence context for: " + persistenceContext );
        EntityManagerFactoryImpl entityManagerFactory = ( EntityManagerFactoryImpl ) config.buildEntityManagerFactory( );
        LOG.trace( LogUtil.subheader( LogUtil.dumpObject( config ) ) );
        emf.put( persistenceContext, entityManagerFactory );
        LOG.info( "-> Setup done for persistence context: " + persistenceContext );
      } catch ( Exception ex ) {
        LOG.error( "-> Error in persistence context setup: " + persistenceContext, ex );
      }
    }
    return emf.get( persistenceContext );
  }
  
  public static void flush( String ctx ) {
    emf.get( ctx ).getCache( ).evictAll( );
  }
  
  public static List<String> list( ) {
    return Lists.newArrayList( entities.keySet( ) );
  }
  
  public static List<Class> listEntities( String persistenceContext ) {
    List<Class> ctxEntities = Lists.newArrayList( entities.get( persistenceContext ) );
    Collections.sort( ctxEntities, Ordering.usingToString( ) );
    return ctxEntities;
  }
  
  private static void touchDatabase( ) {
    long failInterval = System.currentTimeMillis( ) - failCount.getReference( );
    if ( MAX_FAIL_SECONDS * 1000L > failInterval ) {
      LOG.fatal( LogUtil.header( "Database connection failure time limit reached (" + MAX_FAIL_SECONDS
                                 + " seconds):  HUPping the system." ) );
    } else if ( failCount.getStamp( ) > 0 ) {
      LOG.warn( "Found database connection errors: # " + failCount.getStamp( )
                + " over the last "
                + failInterval
                + " seconds." );
    }
  }
  
  @SuppressWarnings( "deprecation" )
  public static EntityManagerFactoryImpl getEntityManagerFactory( final String persistenceContext ) {
    Databases.awaitSynchronized( );
    if ( emf.containsKey( persistenceContext ) ) {
      return emf.get( persistenceContext );
    } else {
      for ( int i = 0; i < MAX_EMF_RETRIES; ++i ) {
        if ( emf.containsKey( persistenceContext ) ) {
          return emf.get( persistenceContext );
        }
        Exceptions.trace( persistenceContext
                          + ": Persistence context has not been configured yet."
                          + " (see debug logs for details)"
                          + "\nThe available contexts are: \n"
                          + Joiner.on( "\n" ).join( emf.keySet( ) ) );
        try {
          TimeUnit.MILLISECONDS.sleep( 100 );
        } catch ( InterruptedException ex ) {
          throw Exceptions.toUndeclared( Exceptions.maybeInterrupted( ex ) );
        }
      }
    }
    throw Exceptions.error( "Failed to lookup persistence context after " + MAX_EMF_RETRIES + " tries.\n" );
  }
  
  public static void shutdown( ) {
    for ( String ctx : emf.keySet( ) ) {
      EntityManagerFactoryImpl em = emf.remove( ctx );
      if ( em.isOpen( ) ) {
        LOG.info( "Closing persistence context: " + ctx );
        em.close( );
      } else {
        LOG.info( "Closing persistence context: " + ctx
                  + " (found it closed already)" );
      }
    }
  }
  
}
