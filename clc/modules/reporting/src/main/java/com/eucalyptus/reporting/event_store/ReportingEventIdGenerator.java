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

import java.io.Serializable;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.UUIDHexGenerator;

/**
 * Custom identifier generator that preserves an existing value.
 */
public class ReportingEventIdGenerator extends UUIDHexGenerator {
  @Override
  public Serializable generate( final SessionImplementor session,
                                final Object object ) throws HibernateException {
    final Serializable id = session.getEntityPersister(null, object)
        .getClassMetadata().getIdentifier(object, session);
    return id != null ? id : super.generate(session, object);
  }
}
