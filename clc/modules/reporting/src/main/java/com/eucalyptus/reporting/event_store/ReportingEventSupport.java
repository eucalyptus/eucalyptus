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
package com.eucalyptus.reporting.event_store;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.GenericGenerator;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.google.common.collect.Sets;

/**
 * Support class for persistent reporting events
 */
@MappedSuperclass
public abstract class ReportingEventSupport implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(generator = "reporting-uuid")
  @GenericGenerator(
      name="reporting-uuid",
      strategy = "com.eucalyptus.reporting.event_store.ReportingEventIdGenerator")
  @Column( name = "id", nullable = false, updatable = false )
  private String id;

  @Temporal( TemporalType.TIMESTAMP)
  @Column(name = "creation_timestamp", updatable = false, nullable = false)
  private Date creationTimestamp;

  @Column(name="timestamp_ms", nullable = false)

  protected Long timestampMs;

  public String getId() {
    return id;
  }

  public Date getCreationTimestamp() {
    return creationTimestamp;
  }

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

  void initialize( final String eventId,
                   final Date created ) {
    this.id = eventId;
    this.creationTimestamp = created;
  }

  @PreUpdate
  @PrePersist
  public void updateTimeStamps() {
    if ( creationTimestamp == null ) {
      this.creationTimestamp = new Date();
    }
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
