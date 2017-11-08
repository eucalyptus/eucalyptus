/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.reporting.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.eucalyptus.event.Event;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * {@code ResourceAvailabilityEvent}s are fired periodically on the CLC.
 *
 * <p>Each event is for a single resource type and reflects the availability of
 * that resource. Availability for a resource type is broken down by the tags
 * for that type. A tag can be either a dimension or a type, the essential
 * distinction between these is that is is NOT meaningful to sum values for
 * a type.</p>
 */
public class ResourceAvailabilityEvent implements Event {
  private static final long serialVersionUID = 1L;

  public enum ResourceType { Instance, Core, Memory, Disk, Address, StorageObject, StorageEBS }

  public static final class Availability implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long total;
    private final long available;
    private final Set<Tag> tags;

    public Availability( final long total,
                         final long available ) {
      this( total, available, Collections.<Tag>emptySet() );
    }

    public Availability( final long total,
                         final long available,
                         final Iterable<Tag> tags ) {
      this.total = total;
      this.available = available;
      this.tags = ImmutableSet.copyOf( tags );
    }

    public long getTotal() {
      return total;
    }

    public long getAvailable() {
      return available;
    }

    public Set<Tag> getTags() {
      return tags;
    }

    public String toString() {
      return String.format( "[total:%d,available:%d,tags:%s]", getTotal(), getAvailable(), Joiner.on(",").join( getTags() ) );
    }
  }

  public static abstract class Tag implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String type;
    private final String value;

    protected Tag( final String type,
                   final String value ) {
      Preconditions.checkNotNull( type, "Type is required" );
      Preconditions.checkNotNull( value, "Value is required" );

      this.type = type;
      this.value = value;
    }

    public String getType() {
      return type;
    }

    public String getValue() {
      return value;
    }

    public String toString() {
      return String.format( "[tag:%s=%s]", getType(), getValue() );
    }
  }

  public static final class Dimension extends Tag {
    private static final long serialVersionUID = 1L;
    public Dimension( final String type,
                      final String value ) {
      super( type, value );
    }
  }

  public static final class Type extends Tag {
    private static final long serialVersionUID = 1L;
    public Type( final String type,
                 final String value ) {
      super( type, value );
    }
  }

  private final ResourceType type;
  private final Collection<Availability> availability;

  public ResourceAvailabilityEvent( final ResourceType type,
                                    final Availability availability ) {
    this( type, Collections.singleton( availability ) );
  }


  public ResourceAvailabilityEvent( final ResourceType type,
                                    final Collection<Availability> availability ) {
    Preconditions.checkNotNull( type, "Type is required" );
    this.type = type;
    this.availability = ImmutableList.copyOf( availability );
  }

  public ResourceType getType() {
    return type;
  }

  public Collection<Availability> getAvailability() {
    return availability;
  }

  public String toString() {
    return String.format( "[type:%s,availability:%s]", getType(), Joiner.on(",").join( getAvailability() ) );
  }

  public static Function<Tag,String> tagType() {
    return TagTypeFunction.INSTANCE;
  }

  private enum TagTypeFunction implements Function<Tag,String> {
    INSTANCE;

    @Override
    public String apply( final Tag tag ) {
      return tag.getType();
    }
  }
}
