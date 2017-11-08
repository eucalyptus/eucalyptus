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
import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public class DelegatingInterceptor implements Interceptor {
  private final Interceptor interceptor;
  
  public DelegatingInterceptor( ) {
    this.interceptor = Interceptors.get( );
  }
  
  public DelegatingInterceptor( Interceptor interceptor ) {
    super( );
    this.interceptor = interceptor;
  }
  
  public boolean onLoad( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
    return this.interceptor.onLoad( entity, id, state, propertyNames, types );
  }
  
  public boolean onFlushDirty( Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types ) throws CallbackException {
    return this.interceptor.onFlushDirty( entity, id, currentState, previousState, propertyNames, types );
  }
  
  public boolean onSave( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
    return this.interceptor.onSave( entity, id, state, propertyNames, types );
  }
  
  public void onDelete( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
    this.interceptor.onDelete( entity, id, state, propertyNames, types );
  }
  
  public void onCollectionRecreate( Object collection, Serializable key ) throws CallbackException {
    this.interceptor.onCollectionRecreate( collection, key );
  }
  
  public void onCollectionRemove( Object collection, Serializable key ) throws CallbackException {
    this.interceptor.onCollectionRemove( collection, key );
  }
  
  public void onCollectionUpdate( Object collection, Serializable key ) throws CallbackException {
    this.interceptor.onCollectionUpdate( collection, key );
  }
  
  public void preFlush( Iterator entities ) throws CallbackException {
    this.interceptor.preFlush( entities );
  }
  
  public void postFlush( Iterator entities ) throws CallbackException {
    this.interceptor.postFlush( entities );
  }
  
  public Boolean isTransient( Object entity ) {
    return this.interceptor.isTransient( entity );
  }
  
  public int[] findDirty( Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types ) {
    return this.interceptor.findDirty( entity, id, currentState, previousState, propertyNames, types );
  }
  
  public Object instantiate( String entityName, EntityMode entityMode, Serializable id ) throws CallbackException {
    return this.interceptor.instantiate( entityName, entityMode, id );
  }
  
  public String getEntityName( Object object ) throws CallbackException {
    return this.interceptor.getEntityName( object );
  }
  
  public Object getEntity( String entityName, Serializable id ) throws CallbackException {
    return this.interceptor.getEntity( entityName, id );
  }
  
  public void afterTransactionBegin( Transaction tx ) {
    this.interceptor.afterTransactionBegin( tx );
  }
  
  public void beforeTransactionCompletion( Transaction tx ) {
    this.interceptor.beforeTransactionCompletion( tx );
  }
  
  public void afterTransactionCompletion( Transaction tx ) {
    this.interceptor.afterTransactionCompletion( tx );
  }
  
  public String onPrepareStatement( String sql ) {
    return this.interceptor.onPrepareStatement( sql );
  }
  
}
