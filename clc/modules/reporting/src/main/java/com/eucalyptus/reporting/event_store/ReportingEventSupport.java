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
package com.eucalyptus.reporting.event_store;

import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Index;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.google.common.collect.Sets;

/**
 * Support class for persistent reporting events
 */
@MappedSuperclass
public abstract class ReportingEventSupport extends AbstractPersistent {

  private static final long serialVersionUID = 1L;

  @Index(name="timestampmsIdx")
  @Column(name="timestamp_ms", nullable=false)
  protected Long timestampMs;

  public final Long getTimestampMs()
  {
    return timestampMs;
  }

  @Nullable
  public EventDependency asDependency() {
    return null;
  }

  public abstract Set<EventDependency> getDependencies();

  protected final EventDependency asDependency( final String property, final String value ) {
    return new EventDependency( this.getClass(), property, value );
  }

  protected static EventDependencyBuilder withDependencies() {
    return new EventDependencyBuilder();
  }

  public static class EventDependencyBuilder {
    private final Set<EventDependency> dependencies = Sets.newHashSet();

    private EventDependencyBuilder(){}

    public EventDependencyBuilder user( final String userId ) {
      if ( userId != null ) {
        dependencies.add( new EventDependency( ReportingUser.class, "id", userId ) );
      }
      return this;
    }

    public EventDependencyBuilder account( final String accountId ) {
      if ( accountId != null ) {
        dependencies.add( new EventDependency( ReportingAccount.class, "id", accountId ) );
      }
      return this;
    }

    public EventDependencyBuilder relation( final Class<? extends ReportingEventSupport> dependencyType,
                                            final String property,
                                            final String value ) {
      if ( value != null ) {
        dependencies.add( new EventDependency( dependencyType, property, value ) );
      }
      return this;
    }

    public Set<EventDependency> set() {
      return dependencies;
    }
  }

  public static final class EventDependency {
    private final Class<?> dependencyType;
    private final String property;
    private final String value;

    public EventDependency( final Class<?> dependencyType,
                            final String property,
                            final String value ) {
      this.dependencyType = dependencyType;
      this.property = property;
      this.value = value;
    }

    public Class<?> getDependencyType() {
      return dependencyType;
    }

    public String getProperty() {
      return property;
    }

    public String getValue() {
      return value;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final EventDependency that = (EventDependency) o;

      if (!dependencyType.equals(that.dependencyType)) return false;
      if (!property.equals(that.property)) return false;
      if (!value.equals(that.value)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = dependencyType.hashCode();
      result = 31 * result + property.hashCode();
      result = 31 * result + value.hashCode();
      return result;
    }
  }
}
