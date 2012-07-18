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
