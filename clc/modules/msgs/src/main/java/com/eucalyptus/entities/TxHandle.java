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
import org.hibernate.event.EventListeners;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.LogUtil;

public class TxHandle implements Comparable<TxHandle>, EntityTransaction {
  private static Logger                                   LOG         = Logger.getLogger( TxHandle.class );
  private static ConcurrentNavigableMap<String, TxHandle> outstanding = new ConcurrentSkipListMap<String, TxHandle>( );
  
  private EntityManager                             em;
  private final WeakReference<Session>                    session;
  private EntityTransaction                         delegate;
  private final Exception                                 owner;
  private final Calendar                                  startTime;
  private final String                                    txUuid;
  private final StopWatch                                 stopWatch;
  
  private volatile long                                   splitTime   = 0l;
  
  public TxHandle( String ctx ) {
    this.txUuid = String.format( "%s:%s", ctx, UUID.randomUUID( ).toString( ) );
    this.owner = new RuntimeException( );
    this.startTime = Calendar.getInstance( );
    this.stopWatch = new StopWatch( );
    this.stopWatch.start( );
    EntityManagerFactory anemf = ( EntityManagerFactoryImpl ) PersistenceContexts.getEntityManagerFactory( ctx );
    Assertions.assertNotNull( anemf, "Failed to find persistence context for ctx=" + ctx );
    try {
      this.em = anemf.createEntityManager( );
      Assertions.assertNotNull( this.em, "Failed to build entity manager for persistence context ctx=" + ctx );
      this.delegate = this.em.getTransaction( );
      this.delegate.begin( );
      this.session = new WeakReference<Session>( ( Session ) this.em.getDelegate( ) );
      outstanding.put( this.txUuid, this );
    } catch ( Throwable e ) {
      this.rollback( );
      LOG.error( e, e );
      throw new RuntimeException( e );
    }
  }
  
  public boolean isExpired( ) {
    long splitTime = split( );
    return ( splitTime - 30000 ) > this.startTime.getTimeInMillis( );
  }
  
  public long splitOperation( ) {
    long oldSplit = this.splitTime;
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
  
  public void rollback( ) {
    try {
      if ( this.delegate != null && this.delegate.isActive( ) ) {
        this.delegate.rollback( );
      }
    } catch ( Throwable e ) {
      LOG.error( e, e );
    } finally {
      this.cleanup( );
    }
  }

  private void cleanup( ) {
    if ( this.session != null ) {
      this.session.clear( );
    }
    this.delegate = null;
    if ( this.em != null ) {
      this.em.close( );
    }
    this.em = null;
    outstanding.remove( this.txUuid );
  }
  
  private void verifyOpen( ) {
    if ( this.delegate == null || this.em == null ) {
      throw new RuntimeException( "Calling a closed tx handle: " + this.txUuid );
    }
  }
  
  public void commit( ) {
    this.verifyOpen( );
    try {
      this.delegate.commit( );
    } catch ( RuntimeException e ) {
      if ( this.delegate != null && this.delegate.isActive( ) ) {
        this.delegate.rollback( );
        LOG.debug( e, e );
        throw e;
      }
    } finally {
      cleanup( );
    }
  }
  
  public String getTxUuid( ) {
    this.split( );
    return this.txUuid;
  }
  
  public boolean getRollbackOnly( ) {
    return this.delegate.getRollbackOnly( );
  }
  
  public boolean isActive( ) {
    return this.delegate.isActive( );
  }
  
  public void setRollbackOnly( ) {
    this.delegate.setRollbackOnly( );
  }
  
  public Session getSession( ) {
    if ( this.session.get( ) == null ) {
      RuntimeException e = new RuntimeException( "Someone is calling a closed tx handle: " + this.txUuid );
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
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    TxHandle other = ( TxHandle ) obj;
    if ( this.owner == null ) {
      if ( other.owner != null ) return false;
    } else if ( !this.owner.equals( other.owner ) ) return false;
    if ( this.startTime == null ) {
      if ( other.startTime != null ) return false;
    } else if ( !this.startTime.equals( other.startTime ) ) return false;
    return true;
  }
  
  @Override
  public int compareTo( TxHandle o ) {
    return this.startTime.compareTo( o.getStartTime( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format( "TxHandle:txUuid=%s:startTime=%s:splitTime=%s", this.txUuid, this.startTime, this.splitTime );
  }

  public static class TxWatchdog implements EventListener {
    
    @Override
    public void fireEvent( Event event ) {
      if( event instanceof ClockTick ) {
        for( TxHandle tx : TxHandle.outstanding.values( ) ) {
          if( tx.isExpired( ) ) {
            LOG.error( "Found expired TxHandle: " + tx );
            LOG.error( tx.owner, tx.owner );
          }
        }
      }
    }
  }

}
