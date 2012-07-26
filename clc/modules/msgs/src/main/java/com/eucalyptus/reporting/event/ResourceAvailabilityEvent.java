/*
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 *
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 *
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 *
 *    Software License Agreement (BSD License)
 *
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 *
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 *
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
package com.eucalyptus.reporting.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import com.google.common.base.Joiner;
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

  public enum ResourceType { Instance, Core, Memory, Disk, Address, StorageWalrus, StorageEBS }

  public static final class Availability {
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

  public static abstract class Tag {
    private final String type;
    private final String value;

    protected Tag( final String type,
                   final String value ) {
      this.type = type;
      this.value = value;
    }

    public String toString() {
      return String.format( "[tag:%s=%s]", type, value );
    }
  }

  public static final class Dimension extends Tag {
    public Dimension( final String type,
                      final String value ) {
      super( type, value );
    }
  }

  public static final class Type extends Tag {
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
    this.type = type;
    this.availability = ImmutableList.copyOf( availability );
  }

  public ResourceType getType() {
    return type;
  }

  public Collection<Availability> getAvailability() {
    return availability;
  }

  @Override
  public boolean requiresReliableTransmission() {
    return false;
  }

  public String toString() {
    return String.format( "[type:%s,availability:%s]", getType(), Joiner.on(",").join( getAvailability() ) );
  }
}
