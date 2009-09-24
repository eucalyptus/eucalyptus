package com.eucalyptus.util;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.ejb.EntityManagerFactoryImpl;

public class TxHandle implements Comparable<TxHandle>, EntityTransaction {
  private static Logger                     LOG         = Logger.getLogger( TxHandle.class );
  private static ConcurrentNavigableMap<String, TxHandle> outstanding = new ConcurrentSkipListMap<String,TxHandle>();

  private EntityManager     em;
  private Session           session;
  private EntityTransaction delegate;
  private StackTraceElement owner;
  private Calendar          startTime;
  private String            txUuid;
  private StopWatch         stopWatch;

  private long splitTime;
  public TxHandle( String ctx ) {
    this.txUuid = String.format("%s:%s:%s",ctx, EntityWrapper.getMyStackTraceElement( ), UUID.randomUUID( ).toString( ) );
    this.startTime = Calendar.getInstance( );
    this.stopWatch = new StopWatch( );
    this.stopWatch.start( );
    EntityManagerFactory anemf = ( EntityManagerFactoryImpl ) DatabaseUtil.getEntityManagerFactory( ctx );
    try {
      this.em = anemf.createEntityManager( );
      this.delegate = em.getTransaction( );
      this.delegate.begin( );
      this.session = ( Session ) em.getDelegate( );
      outstanding.put( txUuid, this );
    } catch ( Throwable e ) {
      this.rollback( );
      LOG.error( e, e );
      throw new RuntimeException( e );
    }
  }

  public boolean isExpired() {
    long splitTime = split( );
    return (splitTime-DatabaseUtil.MAX_OPEN_TIME)>this.startTime.getTimeInMillis( );
  }

  public long splitOperation( ) {
    long oldSplit = splitTime;
    this.stopWatch.split( );
    splitTime = this.stopWatch.getSplitTime( );
    this.stopWatch.unsplit( );
    return splitTime - oldSplit;
  }

  public long split( ) {
    this.stopWatch.split( );
    splitTime = this.stopWatch.getSplitTime( );
    this.stopWatch.unsplit( );
    return splitTime;
  }

  public void rollback( ) {
    try {
      if ( this.delegate != null && this.delegate.isActive( ) ) {
        this.delegate.rollback( );
      }
    } catch( Throwable e ) {
      LOG.trace( LogUtil.dumpObject( Thread.currentThread( ).getStackTrace( ) ), e );
    } finally {
      this.session = null;
      outstanding.remove( this.txUuid );
      if( this.em != null ) {
        this.em.close( );
      }
    }
  }

  public void commit( ) {
    try {
      this.delegate.commit( );
    } catch( RuntimeException e ) {
      if( this.delegate != null && this.delegate.isActive( ) ) {
        this.delegate.rollback( );
        this.delegate = null;
        LOG.debug( e, e );
        throw e;
      }
    } finally {
      outstanding.remove( this.txUuid );
      this.em.close( );
    }
  }
  
  public static void printTxStatus( ) {
    for ( String uuid : outstanding.keySet( ) ) {
      TxHandle tx = outstanding.get( uuid );
      if( tx.isExpired() ) {
        LOG.error( LogUtil.subheader( "Long outstanding transaction handle for: " + uuid + " " + tx.txUuid ) );
      }
    }
  }

  public String getTxUuid( ) {
    this.split();
    return this.txUuid;
  }

  public boolean getRollbackOnly( ) {
    return delegate.getRollbackOnly( );
  }

  public boolean isActive( ) {
    return delegate.isActive( );
  }

  public void setRollbackOnly( ) {
    delegate.setRollbackOnly( );
  }

  public Session getSession( ) {
    return session;
  }

  public Calendar getStartTime( ) {
    return startTime;
  }

  public EntityManager getEntityManager( ) {
    return this.em;
  }

  public void begin( ) {
    delegate.begin( );
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( owner == null ) ? 0 : owner.hashCode( ) );
    result = prime * result + ( ( startTime == null ) ? 0 : startTime.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    TxHandle other = ( TxHandle ) obj;
    if ( owner == null ) {
      if ( other.owner != null ) return false;
    } else if ( !owner.equals( other.owner ) ) return false;
    if ( startTime == null ) {
      if ( other.startTime != null ) return false;
    } else if ( !startTime.equals( other.startTime ) ) return false;
    return true;
  }

  @Override
  public int compareTo( TxHandle o ) {
    return this.startTime.compareTo( o.getStartTime( ) );
  }

}
