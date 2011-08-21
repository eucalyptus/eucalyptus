/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.entities;

import java.io.Serializable;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.jboss.util.collection.Iterators;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.google.common.collect.Collections2;

public class Interceptors {
  private static Logger LOG = Logger.getLogger( Interceptors.class );
  
  static Interceptor empty( ) {
    Interceptor i = new EmptyInterceptor( ) {
      private static final long serialVersionUID = 1L;
    };
    return interceptor = i;
  }
  
  static Interceptor logger( ) {
    Interceptor i = new EmptyInterceptor( ) {
      private static final long serialVersionUID = 1L;
      
      @Override
      public void onDelete( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) {
        LOG.debug( String.format( "%s(): %s %s", Threads.currentStackFrame( ).getMethodName( ), entity.getClass( ).getSimpleName( ), id ) );
        super.onDelete( entity, id, state, propertyNames, types );
      }
      
      @Override
      public boolean onFlushDirty( Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types ) {
        LOG.debug( String.format( "%s(): %s %s", Threads.currentStackFrame( ).getMethodName( ), entity.getClass( ).getSimpleName( ), id ) );
        return super.onFlushDirty( entity, id, currentState, previousState, propertyNames, types );
      }
      
      @Override
      public boolean onLoad( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) {
        LOG.debug( String.format( "%s(): %s %s", Threads.currentStackFrame( ).getMethodName( ), entity.getClass( ).getSimpleName( ), id ) );
        return super.onLoad( entity, id, state, propertyNames, types );
      }
      
      @Override
      public boolean onSave( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) {
        LOG.debug( String.format( "%s(): %s %s", Threads.currentStackFrame( ).getMethodName( ), entity.getClass( ).getSimpleName( ), id ) );
        return super.onSave( entity, id, state, propertyNames, types );
      }
      
      @Override
      public void postFlush( Iterator entities ) {
        LOG.debug( String.format( "%s()", Threads.currentStackFrame( ).getMethodName( ) ) ); 
        super.postFlush( entities );
      }
      
      @Override
      public void preFlush( Iterator entities ) {
        LOG.debug( String.format( "%s(): %s", Threads.currentStackFrame( ).getMethodName( ), Iterators.toString( entities, "\n" ) ) ); 
        super.preFlush( entities );
      }
      
      @Override
      public Boolean isTransient( Object entity ) {
        return super.isTransient( entity );
      }
      
      @Override
      public void afterTransactionBegin( Transaction tx ) {
        LOG.debug( String.format( "%s(): %s", Threads.currentStackFrame( ).getMethodName( ), tx.toString( ) ) );
        super.afterTransactionBegin( tx );
      }
      
      @Override
      public void afterTransactionCompletion( Transaction tx ) {
        LOG.debug( String.format( "%s(): %s", Threads.currentStackFrame( ).getMethodName( ), tx.toString( ) ) );
        super.afterTransactionCompletion( tx );
      }
      
      @Override
      public void beforeTransactionCompletion( Transaction tx ) {
        LOG.debug( String.format( "%s(): %s", Threads.currentStackFrame( ).getMethodName( ), tx.toString( ) ) );
        super.beforeTransactionCompletion( tx );
      }
      
      @Override
      public void onCollectionRemove( Object collection, Serializable key ) throws CallbackException {
        super.onCollectionRemove( collection, key );
      }
      
      @Override
      public void onCollectionRecreate( Object collection, Serializable key ) throws CallbackException {
        LOG.debug( String.format( "%s(): %s %s", Threads.currentStackFrame( ).getMethodName( ), key, collection ) );
        super.onCollectionRecreate( collection, key );
      }
      
      @Override
      public void onCollectionUpdate( Object collection, Serializable key ) throws CallbackException {
        LOG.debug( String.format( "%s(): %s %s", Threads.currentStackFrame( ).getMethodName( ), key, collection ) );
        super.onCollectionUpdate( collection, key );
      }
    };
    return interceptor = i;
  }
  
  private static Interceptor interceptor = Logs.isExtrrreeeme( ) ? logger( ) : empty( );
  
  static Interceptor get( ) {
    return interceptor;
  }
  
  private static void set( Interceptor interceptor ) {
    Interceptors.interceptor = interceptor;
  }
  
}
