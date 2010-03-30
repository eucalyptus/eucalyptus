package com.eucalyptus.entities;

import org.apache.log4j.Logger;
import com.eucalyptus.util.EucalyptusCloudException;

public abstract class _anon<T> {
  private static Logger LOG = Logger.getLogger( _anon.class );
  protected final T search;
  protected _anon( T search ) {
    this.search = search;
  }
  protected  abstract class _mutator {    
    public abstract void set( T e );
    public T set() throws EucalyptusCloudException {
      if( _anon.this.search == null ) {
        EucalyptusCloudException ex = new EucalyptusCloudException( "A search object must be supplied" );
        LOG.warn( ex.getMessage( ), ex );
        throw ex;
      }
      EntityWrapper<T> db = new EntityWrapper<T>( );
      try {
        T entity = db.getUnique( _anon.this.search );
        this.set( entity );
        db.commit( );
        return entity;
      } catch ( EucalyptusCloudException e ) {
        db.rollback( );
        throw e;
      }
    }
  }

  protected abstract class _accessor<V> {
    public abstract V get( T e );
    public V get() throws EucalyptusCloudException {
      if( _anon.this.search == null ) {
        EucalyptusCloudException ex = new EucalyptusCloudException( "A search object must be supplied" );
        LOG.warn( ex.getMessage( ), ex );
        throw ex;
      }
      EntityWrapper<T> db = new EntityWrapper<T>( );
      try {
        T entity = db.getUnique( _anon.this.search );
        V result = this.get( entity );
        db.commit( );
        return result;
      } catch ( EucalyptusCloudException e ) {
        db.rollback( );
        throw e;
      }
    }
  }

}
