package com.eucalyptus.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;

public class TimedEvictionSet<E extends Comparable> implements Set<E> {
  private static Logger LOG = Logger.getLogger( TimedEvictionSet.class );
  private NavigableSet<E> entries = new ConcurrentSkipListSet<E>();
  private NavigableSet<TimestampedElement> timestamps = new ConcurrentSkipListSet<TimestampedElement>();
  private Long evictionMillis = 15*1000*60l;
  private AtomicBoolean busy = new AtomicBoolean( false );
  
  class TimestampedElement implements Comparable<TimestampedElement> {
    private E element;
    private Long timeNanos;
    public TimestampedElement( E element ) {
      super( );
      this.element = element;
      this.timeNanos = System.nanoTime( );
    }
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.element == null ) ? 0 : this.element.hashCode( ) );
      result = prime * result + ( ( this.timeNanos == null ) ? 0 : this.timeNanos.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass( ) != obj.getClass( ) ) return false;
      TimestampedElement other = ( TimestampedElement ) obj;
      if ( this.element == null ) {
        if ( other.element != null ) return false;
      } else if ( !this.element.equals( other.element ) ) return false;
      if ( this.timeNanos == null ) {
        if ( other.timeNanos != null ) return false;
      } else if ( !this.timeNanos.equals( other.timeNanos ) ) return false;
      return true;
    }
    
    @Override
    public int compareTo( TimestampedElement that ) {
      if( !this.equals( that ) && this.timeNanos.compareTo( that.timeNanos ) == 0 ) {
        return this.element.compareTo( that.element );
      } else {
        return this.timeNanos.compareTo( that.timeNanos );
      }
    }
    public boolean isExpired( ) {
      return System.nanoTime( ) > ( this.timeNanos + TimedEvictionSet.this.evictionMillis );
    }
    public E get( ) {
      return this.element;
    }
    public Long getTimestamp( ) {
      return this.timeNanos;
    }
  }
  private boolean timestamp( E e ) {
    this.scavenge( );
    if( this.entries.add( e ) ) {
      TimestampedElement elem = new TimestampedElement( e );
      this.timestamps.add( elem );
      return true;
    } else {
      TimestampedElement elem = new TimestampedElement( e );
      if( this.timestamps.contains( elem ) && TimeUnit.SECONDS.convert( System.nanoTime( ) - elem.getTimestamp( ), TimeUnit.NANOSECONDS ) < 2 ) {
        return true;
      } else {
        return false;
      }
    }
  }
  public TimedEvictionSet( Long evictionMillis ) {
    super( );
    this.evictionMillis = evictionMillis;
  }
  
  private void scavenge() {
    if( this.busy.compareAndSet( false, true ) ) {
      try {
        TimestampedElement elem = null;        
        while( !this.timestamps.isEmpty( ) && this.timestamps.first( ).isExpired( ) && ( elem = this.timestamps.pollFirst( ) )!= null ) {
          this.entries.remove( elem.get( ) );
          LOG.trace( "Evicting previous entry: " + elem.get( ) + " " + elem.getTimestamp( ) );
        }
      } finally {
        this.busy.lazySet( false );
      }
    }
  }
  
  public boolean add( E e ) {
    return timestamp( e );
  }

  @Deprecated
  public boolean addAll( Collection<? extends E> c ) {
    return false;
  }

  public void clear( ) {
    this.timestamps.clear( );
    this.entries.clear( );
    this.busy.set( false );
  }

  public Comparator<? super E> comparator( ) {
    return this.entries.comparator( );
  }

  public boolean contains( Object o ) {
    this.scavenge( );
    return this.entries.contains( o );
  }

  public boolean containsAll( Collection<?> c ) {
    return this.entries.containsAll( c );
  }

  public E first( ) {
    return this.entries.first( );
  }

  public SortedSet<E> headSet( E toElement ) {
    return this.entries.headSet( toElement );
  }

  public boolean isEmpty( ) {
    return this.entries.isEmpty( );
  }

  public Iterator<E> iterator( ) {
    return this.entries.iterator( );
  }

  public E last( ) {
    return this.entries.last( );
  }

  @Deprecated
  public boolean remove( Object o ) {
    return false;
  }

  @Deprecated
  public boolean removeAll( Collection<?> c ) {
    return false;
  }

  @Deprecated
  public boolean retainAll( Collection<?> c ) {
    return false;
  }

  public int size( ) {
    return this.entries.size( );
  }

  public SortedSet<E> subSet( E fromElement, E toElement ) {
    return this.entries.subSet( fromElement, toElement );
  }

  public SortedSet<E> tailSet( E fromElement ) {
    return this.entries.tailSet( fromElement );
  }

  public Object[] toArray( ) {
    return this.entries.toArray( );
  }

  public <T> T[] toArray( T[] a ) {
    return this.entries.toArray( a );
  }

}
