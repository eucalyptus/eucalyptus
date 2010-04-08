
package com.eucalyptus.entities;

import java.util.List;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import com.eucalyptus.util.EucalyptusCloudException;

public abstract class _anon<T> {
  private static Logger LOG = Logger.getLogger( _anon.class );
  protected final T search;
  protected final String ctx;
  protected _anon( T search ) {
    this.search = search;
    if( !search.getClass( ).isAnnotationPresent( PersistenceContext.class ) ) {
      throw new RuntimeException( "Attempting to create an entity wrapper instance for non persistent type: " + search.getClass( ).getCanonicalName( ) );
    }
    this.ctx = search.getClass( ).getAnnotation( PersistenceContext.class ).name( );
  }
  public _anon set( _mutator m ) throws EucalyptusCloudException {
    m.set( );
    return this;
  }
  protected  abstract class _mutator {    
    public abstract void set( T e );
    public T set() throws EucalyptusCloudException {
      if( _anon.this.search == null ) {
        EucalyptusCloudException ex = new EucalyptusCloudException( "A search object must be supplied" );
        LOG.warn( ex.getMessage( ), ex );
        throw ex;
      }
      EntityWrapper<T> db = new EntityWrapper<T>( ctx );
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
      EntityWrapper<T> db = new EntityWrapper<T>( ctx );
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
