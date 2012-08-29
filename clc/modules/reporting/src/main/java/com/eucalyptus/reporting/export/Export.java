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
package com.eucalyptus.reporting.export;

import static com.eucalyptus.reporting.export.ExportUtils.eventClasses;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.google.common.collect.Sets;

/**
 *
 */
public class Export {
  private static final Logger log = Logger.getLogger( Export.class );

  private static final String CREATION_TIMESTAMP = "creationTimestamp";

  public static ReportingExport export( final Date startDate, final Date endDate ) {
    final ReportingExport export = new ReportingExport();
    final Conjunction criterion = Restrictions.conjunction();
    if ( startDate != null ) {
      criterion.add( Restrictions.ge( CREATION_TIMESTAMP, startDate ) );
    }
    if ( endDate != null ) {
      criterion.add( Restrictions.lt( CREATION_TIMESTAMP, endDate ) );
    }

    final Set<ReportingEventSupport.EventDependency> dependencies = Sets.newHashSet();
    for ( final Class<? extends AbstractPersistent> eventClass : eventClasses ) {
      final EntityTransaction transaction = Entities.get( eventClass );
      try {
        addToExportList(
            export,
            dependencies,
            criteriaFor(eventClass, criterion).list() );
      } catch ( Exception e) {
        log.error(e, e);
      } finally {
        transaction.rollback();
      }
    }

    return export;
  }

  private static Criteria criteriaFor( final Class<?> persistentClass, final Criterion criterion ) {
    return Entities.createCriteria(persistentClass)
        .setReadOnly(true)
        .add(criterion)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .setCacheable(false);
  }

  private static void addToExportList( final ReportingExport export,
                                       final Set<ReportingEventSupport.EventDependency> dependencies,
                                       final List entities ) {
    for ( final Object entity : entities ) {
      if ( entity instanceof ReportingEventSupport ) {
        final ReportingEventSupport eventSupport = (ReportingEventSupport) entity;

        addToExportList(export, dependencies, eventSupport);
      }
    }
  }

  private static void addToExportList( final ReportingExport export,
                                       final Set<ReportingEventSupport.EventDependency> dependencies,
                                       final ReportingEventSupport eventSupport ) {
    final ReportingEventSupport.EventDependency dependency = eventSupport.asDependency();
    if ( dependency != null ) {
      dependencies.add( dependency );
    }
    ensureDependencies( export, dependencies, eventSupport );
    export.add( eventSupport );
  }

  private static void ensureDependencies( final ReportingExport export,
                                          final Set<ReportingEventSupport.EventDependency> dependencies,
                                          final ReportingEventSupport persistent ) {
    for ( final ReportingEventSupport.EventDependency dependency : persistent.getDependencies() ) {
      ensureDependency( export, dependencies, dependency );
    }
  }

  private static void ensureDependency( final ReportingExport export,
                                        final Set<ReportingEventSupport.EventDependency> dependencies,
                                        final ReportingEventSupport.EventDependency dependency ) {
    if ( !dependencies.contains( dependency ) ) {
      final Object value = criteriaFor(
          dependency.getDependencyType(),
          Restrictions.eq(dependency.getProperty(), dependency.getValue()) )
          .uniqueResult();

      if ( value instanceof ReportingEventSupport ) {
        addToExportList(export, dependencies, (ReportingEventSupport) value);
      } else if ( value instanceof ReportingUser) {
        final ReportingUser user = (ReportingUser) value;
        dependencies.add( dependency );

        ensureDependency(
            export,
            dependencies,
            new ReportingEventSupport.EventDependency( ReportingAccount.class, "id", user.getAccountId() ) );

        export.add( user );
      }  else if ( value instanceof ReportingAccount ) {
        final ReportingAccount account = (ReportingAccount) value;
        dependencies.add( dependency );
        export.add( account );
      }
    }
  }
}
