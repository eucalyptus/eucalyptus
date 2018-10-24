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
import com.eucalyptus.ws.StackConfiguration;

public class TimedEvictionSet<E extends Comparable> implements Set<E> {
  private static Logger LOG = Logger.getLogger( TimedEvictionSet.class );
  private NavigableSet<E> entries = new ConcurrentSkipListSet<E>();
  private NavigableSet<TimestampedElement> timestamps = new ConcurrentSkipListSet<TimestampedElement>();
  private Long evictionNanos = 15*1000*60*1000*1000l;
  private AtomicBoolean busy = new AtomicBoolean( false );
  
  class TimestampedElement implements Comparable<TimestampedElement> {
    private E element;
    private Long timeNanos;
    
    public TimestampedElement( E element ) {
      super( );
      this.element = element;
      this.timeNanos = System.nanoTime( );
    }
    
    protected TimestampedElement( E element, Long nanos) {
    	super();
    	this.element = element;
    	this.timeNanos = nanos;
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
      return System.nanoTime( ) > ( this.timeNanos + TimedEvictionSet.this.evictionNanos);
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

	LOG.debug("Use of the same signature is detected: " + e );
	
	// Allow message with the same signature within the REPLAY_SKEW_WINDOW_SEC
	// time interval
    	Long now = System.nanoTime( );
	Long adjust = TimeUnit.NANOSECONDS.convert(StackConfiguration.REPLAY_SKEW_WINDOW_SEC, TimeUnit.SECONDS);

	// special case
	if( adjust <= 0 ) {
	    // no replay is allowed at all
	    return false;
	} 

    	Long timeNanosAdj = now - adjust;
    	TimestampedElement fakeElem = new TimestampedElement( e, timeNanosAdj );
    	NavigableSet<TimestampedElement> elems = this.timestamps.tailSet(fakeElem, true);
    	for(Iterator<TimestampedElement> iter = elems.iterator(); iter.hasNext();) {
    		TimestampedElement elem = iter.next();
    		E sig = elem.get();
		// the signature was used within the allowed window, don't trigger replay
    		if(e.equals(sig)) {
    			LOG.debug("Found elem with signature " + sig + " within allowed " + StackConfiguration.REPLAY_SKEW_WINDOW_SEC
    					+ " sec window ");
    			return true;
    		}
    	}
    	// a replay attack
    	return false;
    }
  }
  
  public TimedEvictionSet( Long evictionMillis ) {
    super( );
    this.evictionNanos = evictionMillis * 1000 * 1000;
  }
  
  private void scavenge() {
    if( this.busy.compareAndSet( false, true ) ) {
      try {
        TimestampedElement elem = null;        
        while( !this.timestamps.isEmpty( ) && this.timestamps.first( ).isExpired( ) && ( elem = this.timestamps.pollFirst( ) )!= null ) {
          this.entries.remove( elem.get( ) );
        }
      } finally {
        this.busy.lazySet( false );
      }
    }
  }
  
  
  public Long getEvictionNanos() {
	  return evictionNanos;
  }
  
  public boolean add( E e ) {
    Long skew = TimeUnit.NANOSECONDS.convert(StackConfiguration.REPLAY_SKEW_WINDOW_SEC, TimeUnit.SECONDS);
    // replay detection is disabled
    if(skew >= this.evictionNanos)
	return true;
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
