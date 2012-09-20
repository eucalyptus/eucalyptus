/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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

  public enum ResourceType { Instance, Core, Memory, Disk, Address, StorageWalrus, StorageEBS }

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
