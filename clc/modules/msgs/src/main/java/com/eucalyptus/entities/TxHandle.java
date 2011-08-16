package com.eucalyptus.entities;

import java.lang.ref.WeakReference;
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
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.Logs;

public class TxHandle implements Comparable<TxHandle>, EntityTransaction {
  private static Logger                                   LOG         = Logger.getLogger( TxHandle.class );
  private static ConcurrentNavigableMap<String, TxHandle> outstanding = new ConcurrentSkipListMap<String, TxHandle>( );
  
  private EntityManager                                   em;
  private final WeakReference<Session>                    session;
  private EntityTransaction                               delegate;
  private final String                                    owner;
  private final Calendar                                  startTime;
  private final String                                    txUuid;
  private final StopWatch                                 stopWatch;
  
  private volatile long                                   splitTime   = 0l;
  private final Runnable                                  runnable;
  
  TxHandle( final String ctx, final Runnable runnable ) {
    this.runnable = runnable;
    this.txUuid = String.format( "%s:%s", ctx, UUID.randomUUID( ).toString( ) );
    this.owner = Threads.currentStackString( );
    this.startTime = Calendar.getInstance( );
    this.stopWatch = new StopWatch( );
    this.stopWatch.start( );
    final EntityManagerFactory anemf = ( EntityManagerFactoryImpl ) PersistenceContexts.getEntityManagerFactory( ctx );
    Assertions.assertNotNull( anemf, "Failed to find persistence context for ctx=" + ctx );
    try {
      this.em = anemf.createEntityManager( );
      Assertions.assertNotNull( this.em, "Failed to build entity manager for persistence context ctx=" + ctx );
      this.delegate = this.em.getTransaction( );
      this.delegate.begin( );
      this.session = new WeakReference<Session>( ( Session ) this.em.getDelegate( ) );
    } catch ( final Throwable e ) {
      this.rollback( );
      LOG.error( e, e );
      throw new RuntimeException( e );
    } finally {
      outstanding.put( this.txUuid, this );
    }
  }
  
  public boolean isExpired( ) {
    final long splitTime = this.split( );
    return ( splitTime - 30000 ) > this.startTime.getTimeInMillis( );
  }
  
  public long splitOperation( ) {
    final long oldSplit = this.splitTime;
    this.stopWatch.split( );
    this.splitTime = this.stopWatch.getSplitTime( );
    this.stopWatch.unsplit( );
    return this.splitTime - oldSplit;
  }
  
  public long split( ) {
    this.stopWatch.split( );
    this.splitTime = this.stopWatch.getSplitTime( );
    this.stopWatch.unsplit( );
    return this.splitTime;
  }
  
  @Override
  public void rollback( ) {
    try {
      if ( ( this.delegate != null ) && this.delegate.isActive( ) ) {
        this.delegate.rollback( );
      }
    } catch ( final Throwable e ) {
      LOG.error( e, e );
    } finally {
      this.cleanup( );
    }
  }
  
  private void cleanup( ) {
    try {
      this.runnable.run( );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
    try {
      if ( ( this.session != null ) && ( this.session.get( ) != null ) ) {
        this.session.clear( );
      }
      this.delegate = null;
      if ( this.em != null ) {
        this.em.close( );
      }
      this.em = null;
    } finally {
      outstanding.remove( this.txUuid );
    }
  }
  
  private void verifyOpen( ) {
    if ( ( this.delegate == null ) || ( this.em == null ) ) {
      throw new RuntimeException( "Calling a closed tx handle: " + this.txUuid );
    }
  }
  
  @Override
  public void commit( ) {
    this.verifyOpen( );
    try {
      this.delegate.commit( );
    } catch ( final RuntimeException e ) {
      if ( ( this.delegate != null ) && this.delegate.isActive( ) ) {
        this.delegate.rollback( );
        LOG.debug( e, e );
      }
      throw e;
    } finally {
      this.cleanup( );
    }
  }
  
  public String getTxUuid( ) {
    this.split( );
    return this.txUuid;
  }
  
  @Override
  public boolean getRollbackOnly( ) {
    return this.delegate.getRollbackOnly( );
  }
  
  @Override
  public boolean isActive( ) {
    return this.delegate.isActive( );
  }
  
  @Override
  public void setRollbackOnly( ) {
    this.delegate.setRollbackOnly( );
  }
  
  public Session getSession( ) {
    if ( this.session.get( ) == null ) {
      final RuntimeException e = new RuntimeException( "Someone is calling a closed tx handle: " + this.txUuid );
      LOG.error( e, e );
      throw e;
    }
    return this.session.get( );
  }
  
  public Calendar getStartTime( ) {
    return this.startTime;
  }
  
  public EntityManager getEntityManager( ) {
    return this.em;
  }
  
  @Override
  public void begin( ) {
    this.delegate.begin( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.owner == null )
      ? 0
      : this.owner.hashCode( ) );
    result = prime * result + ( ( this.startTime == null )
      ? 0
      : this.startTime.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( this.getClass( ) != obj.getClass( ) ) return false;
    final TxHandle other = ( TxHandle ) obj;
    if ( this.owner == null ) {
      if ( other.owner != null ) return false;
    } else if ( !this.owner.equals( other.owner ) ) return false;
    if ( this.startTime == null ) {
      if ( other.startTime != null ) return false;
    } else if ( !this.startTime.equals( other.startTime ) ) return false;
    return true;
  }
  
  @Override
  public int compareTo( final TxHandle o ) {
    return this.startTime.compareTo( o.getStartTime( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format( "TxHandle:txUuid=%s:startTime=%s:splitTime=%s:owner=%s", this.txUuid, this.startTime, this.splitTime, Logs.EXTREME
      ? this.owner
      : "n/a" );
  }
  
  public static class TxWatchdog implements EventListener {
    
    @Override
    public void fireEvent( final Event event ) {
      if ( event instanceof ClockTick ) {
        for ( final TxHandle tx : TxHandle.outstanding.values( ) ) {
          if ( tx.isExpired( ) ) {
            LOG.error( "Found expired TxHandle: " + tx );
            LOG.error( tx.owner );
          }
        }
      }
    }
  }
  
}
