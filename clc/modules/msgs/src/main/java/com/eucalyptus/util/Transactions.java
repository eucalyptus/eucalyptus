package com.eucalyptus.util;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.EntityWrapper;

public class Transactions {
  private static Logger LOG = Logger.getLogger( Transactions.class );
  
  public static <T> T one( T search, JoinTx<T>... txs ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    try {
      T entity = db.getUnique( search );
      for ( JoinTx<T> c : txs ) {
        c.fire( db, entity );
      }
      db.commit( );
      return entity;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new TransactionException( e.getMessage( ), e );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    }
  }
  
  public static <T> T one( T search, Tx<T> c ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    try {
      T entity = db.getUnique( search );
      c.fire( entity );
      db.commit( );
      return entity;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new TransactionException( e.getMessage( ), e );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    }
  }
  
  public static <T> T save( T saveMe ) throws TransactionException {
    return save( saveMe, null );
  }
  
  public static <T> T save( T saveMe, Tx<T> c ) throws TransactionException {
    EntityWrapper<T> db = EntityWrapper.get( saveMe );
    try {
      db.add( saveMe );
      T entity = saveMe; //db.getUnique( saveMe );
      if ( c != null ) {
        c.fire( entity );
      }
      db.commit( );
      return entity;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new TransactionException( e.getMessage( ), e );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    }
  }
  
  public static <T> List<T> many( T search, Tx<T> c ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    try {
      List<T> res = db.query( search );
      for ( T entity : res ) {
        c.fire( entity );
      }
      db.commit( );
      return res;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new TransactionException( e.getMessage( ), e );
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    }
  }
  
  public static <T> List<T> list( T search, Tx<List<T>> c ) throws TransactionException {
    if ( search == null ) {
      TransactionException ex = new TransactionException( "A search object must be supplied" );
      LOG.warn( ex.getMessage( ), ex );
      throw ex;
    }
    EntityWrapper<T> db = EntityWrapper.get( search );
    try {
      List<T> res = db.query( search );
      c.fire( res );
      db.commit( );
      return res;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new TransactionException( e.getMessage( ), e );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new TransactionFireException( e.getMessage( ), e );
    }
  }
  
}
