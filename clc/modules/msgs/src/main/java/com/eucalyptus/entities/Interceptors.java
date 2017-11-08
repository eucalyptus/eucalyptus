/*************************************************************************
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

package com.eucalyptus.entities;

import java.io.Serializable;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

public class Interceptors {//TODO:GRZE:@Configurable
  private static final Table stringify( Object[] state, final String[] propertyNames, final Type[] types ) {
    return HashBasedTable.create( );
  }
  
  private static final class LogMonitorInterceptor extends EmptyInterceptor {
    private static final long serialVersionUID = 1L;
    private int               operations       = 0;
    
    private String toStringNullably( Object o ) {
      try {
        return o != null
          ? "" + o.toString( )
          : "null";
      } catch ( Exception ex ) {
        return o.getClass( ).getCanonicalName( ) + ".toString(): " + ex.getMessage( );
      }
    }
    
    @Override
    public void onDelete( final Object entity, final Serializable id, final Object[] state, final String[] propertyNames, final Type[] types ) {
      try {
        LOG.debug( String.format( "%s():%d %s %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations, entity.getClass( ).getSimpleName( ),
                                  id, toStringNullably( entity ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.onDelete( entity, id, state, propertyNames, types );
    }
    
    @Override
    public boolean onFlushDirty( final Object entity, final Serializable id, final Object[] currentState, final Object[] previousState, final String[] propertyNames, final Type[] types ) {
      try {
        LOG.debug( String.format( "%s():%d %s %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations, entity.getClass( ).getSimpleName( ),
                                  id, toStringNullably( entity ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      return super.onFlushDirty( entity, id, currentState, previousState, propertyNames, types );
    }
    
    /**
     * NOTE: <b>MUST</b> remember that the {@code entity} is {@code null} at this time!
     */
    @Override
    public boolean onLoad( final Object entity, final Serializable id, final Object[] state, final String[] propertyNames, final Type[] types ) {
      try {
        LOG.debug( String.format( "%s():%d %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations, entity.getClass( ).getSimpleName( ),
                                  id ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      return super.onLoad( entity, id, state, propertyNames, types );
    }
    
    @Override
    public boolean onSave( final Object entity, final Serializable id, final Object[] state, final String[] propertyNames, final Type[] types ) {
      try {
        LOG.debug( String.format( "%s():%d %s %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations, entity.getClass( ).getSimpleName( ),
                                  id, toStringNullably( entity ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      return super.onSave( entity, id, state, propertyNames, types );
    }
    
    @Override
    public void postFlush( final Iterator entities ) {
      try {
        if ( Logs.isExtrrreeeme( ) ) Logs.exhaust( ).debug( String.format( "%s():%d %s", Threads.currentStackFrame( ).getMethodName( ), this.operations,
                                                                           Iterators.transform( entities, Classes.simpleNameFunction( ) ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.postFlush( entities );
    }
    
    @Override
    public void preFlush( final Iterator entities ) {
      try {
        if ( Logs.isExtrrreeeme( ) ) Logs.exhaust( ).debug( String.format( "%s():%d %s", Threads.currentStackFrame( ).getMethodName( ), this.operations,
                                                                           Iterators.transform( entities, Classes.simpleNameFunction( ) ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.preFlush( entities );
    }
    
    @Override
    public Boolean isTransient( final Object entity ) {
      return super.isTransient( entity );
    }
    
    @Override
    public void afterTransactionBegin( final Transaction tx ) {
      try {
        LOG.debug( String.format( "%s():%d %s", Threads.currentStackFrame( ).getMethodName( ), this.operations = 0, tx.toString( ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.afterTransactionBegin( tx );
    }
    
    @Override
    public void afterTransactionCompletion( final Transaction tx ) {
      try {
        LOG.debug( String.format( "%s():%d %s", Threads.currentStackFrame( ).getMethodName( ), this.operations, tx.toString( ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.afterTransactionCompletion( tx );
    }
    
    @Override
    public void beforeTransactionCompletion( final Transaction tx ) {
      if ( this.operations == 0 ) {
        LOG.error( Threads.currentStackString( ) );
      }
      try {
        LOG.debug( String.format( "%s():%d %s", Threads.currentStackFrame( ).getMethodName( ), this.operations, tx.toString( ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.beforeTransactionCompletion( tx );
    }
    
    @Override
    public void onCollectionRemove( final Object collection, final Serializable key ) throws CallbackException {
      try {
        Iterable<Object> iter = ( collection instanceof Iterable
          ? ( Iterable ) collection
          : Lists.newArrayList( collection ) );
        String summary = Iterables.toString( Iterables.transform( iter, Classes.canonicalNameFunction( ) ) );
        if ( Logs.isExtrrreeeme( ) ) Logs.exhaust( ).debug( String.format( "%s():%d %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations,
                                                                           key, summary ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.onCollectionRemove( collection, key );
    }
    
    @Override
    public void onCollectionRecreate( final Object collection, final Serializable key ) throws CallbackException {
      try {
        Iterable<Object> iter = ( collection instanceof Iterable
          ? ( Iterable ) collection
          : Lists.newArrayList( collection ) );
        String summary = Iterables.toString( Iterables.transform( iter, Classes.canonicalNameFunction( ) ) );
        if ( Logs.isExtrrreeeme( ) ) Logs.exhaust( ).debug( String.format( "%s():%d %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations,
                                                                           key, summary ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.onCollectionRecreate( collection, key );
    }
    
    @Override
    public void onCollectionUpdate( final Object collection, final Serializable key ) throws CallbackException {
      try {
        Iterable<Object> iter = ( collection instanceof Iterable
          ? ( Iterable ) collection
          : Lists.newArrayList( collection ) );
        String summary = Iterables.toString( Iterables.transform( iter, Classes.canonicalNameFunction( ) ) );
        LOG.debug( String.format( "%s():%d %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations, key, summary ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      super.onCollectionUpdate( collection, key );
    }
    
    @Override
    public Object instantiate( String entityName, EntityMode entityMode, Serializable id ) {
      try {
        LOG.debug( String.format( "%s():%d", Threads.currentStackFrame( ).getMethodName( ), ++this.operations ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      return super.instantiate( entityName, entityMode, id );
    }
    
    @Override
    public String getEntityName( Object object ) {
      try {
        LOG.debug( String.format( "%s():%d %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations, object.getClass( ).getSimpleName( ),
                                  toStringNullably( object ) ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      return super.getEntityName( object );
    }
    
    @Override
    public Object getEntity( String entityName, Serializable id ) {
      try {
        LOG.debug( String.format( "%s():%d %s %s", Threads.currentStackFrame( ).getMethodName( ), ++this.operations, entityName, id ) );
      } catch ( Exception ex ) {
        LOG.error( ex );
      }
      return super.getEntity( entityName, id );
    }
  }
  
  private static Logger LOG = Logger.getLogger( Interceptors.class );
  
  private static Interceptor empty( ) {
    if ( interceptor != null && interceptor instanceof EmptyInterceptor ) {
      return interceptor;
    } else {
      final Interceptor i = new EmptyInterceptor( ) {
        private static final long serialVersionUID = 1L;
      };
      if ( interceptor != null && !( interceptor instanceof EmptyInterceptor ) ) {
        interceptor = i;
      }
      return i;
    }
  }
  
  @SuppressWarnings( "synthetic-access" )
  private static Interceptor logger( ) {
    if ( interceptor != null && interceptor instanceof LogMonitorInterceptor ) {
      return interceptor;
    } else {
      final Interceptor i = new LogMonitorInterceptor( );
      if ( interceptor != null && !( interceptor instanceof LogMonitorInterceptor ) ) {
        interceptor = i;
      }
      return i;
    }
  }
  
  private static Boolean     TRANSACTION_INTERCEPT = Boolean.FALSE;
  private static Interceptor interceptor           = get( );
  
  static Interceptor get( ) {
    return TRANSACTION_INTERCEPT
      ? logger( )
      : empty( );
  }
  
}
