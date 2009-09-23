package com.eucalyptus.util;

import java.util.Calendar;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.ejb.EntityManagerFactoryImpl;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;

public class TxHandle implements Comparable<TxHandle>, EntityTransaction {
  private static Logger                     LOG         = Logger.getLogger( TxHandle.class );
  private static Multimap<String, TxHandle> outstanding = getMap();

  private EntityManager     em;
  private Session           session;
  private EntityTransaction delegate;
  private StackTraceElement owner;
  private Calendar          startTime;
  private String            ctx;
  private String            dbUrl;
  private StopWatch         stopWatch;

  public TxHandle( String ctx ) {
    this.ctx = ctx;
    this.owner = DebugUtil.getMyStackTraceElement( );
    this.startTime = Calendar.getInstance( );
    this.stopWatch = new StopWatch( );
    this.stopWatch.start( );
    EntityManagerFactory anemf = ( EntityManagerFactoryImpl ) DatabaseUtil.getEntityManagerFactory( ctx );
    try {
      this.em = anemf.createEntityManager( );
      this.session = ( Session ) em.getDelegate( );
      this.dbUrl = this.session.connection( ).getMetaData( ).getURL( );//FIXME: check to see if failing
      this.delegate = em.getTransaction( );
      this.delegate.begin( );
      outstanding.put( ctx, this );
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new RuntimeException( e );
    }
  }
  @SuppressWarnings( "unchecked" )
  private static Multimap<String, TxHandle> getMap( ) {
    return (Multimap)Multimaps.synchronizedMultimap( Multimaps.newTreeMultimap( ) );
  }
  public boolean isExpired() {
    this.stopWatch.split( );
    long splitTime = this.stopWatch.getSplitTime( );
    this.stopWatch.unsplit( );
    return (splitTime-DatabaseUtil.MAX_OPEN_TIME)>this.startTime.getTimeInMillis( );
  }

  public void rollback( ) {
    if ( this.delegate != null && this.delegate.isActive( ) ) {
      try {
        this.delegate.rollback( );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
      }
    }
    if( this.em != null && this.em.isOpen( ) ) {
      try {
        this.em.close( );
      } catch ( Throwable e ) {
        LOG.error( e, e );
        throw new RuntimeException( e );
      }
    } else {
      Exception e = new Exception();
      e.fillInStackTrace( );
      LOG.trace( e, e );
      //TODO: trace the stack here.  rollback might be OK for most use cases.
    }
  }

  public Session getSession( ) {
    return session;
  }

  public Calendar getStartTime( ) {
    return startTime;
  }

  public String getDbUrl( ) {
    return dbUrl;
  }

  public String getCtx( ) {
    return ctx;
  }

  public EntityManager getEntityManager( ) {
    return this.em;
  }

  public void begin( ) {
    delegate.begin( );
  }

  public void commit( ) {
    if( this.em.isOpen( ) ) {
      try {
        this.em.flush( );
        this.delegate.commit( );
        this.em.close( );
      } catch ( Throwable e1 ) {
        this.em.close( );
        throw new RuntimeException( e1 );
      } 
    } else {
      DebugUtil.debug( );
      throw new RuntimeException( "Database is closed already." );
    }
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

  public static void printTxStatus( ) {
    for ( String ctx : outstanding.keySet( ) ) {
      for ( TxHandle tx : outstanding.get( ctx ) ) {
        if( tx.isExpired() ) {
          LOG.error( LogUtil.subheader( "Long outstanding transaction handle for: " + ctx + "\n" + LogUtil.dumpObject( tx ) ) );
        }
      }
    }
  }

}
