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

import static com.eucalyptus.reporting.event_store.ReportingEventSupport.EventDependency;
import static com.eucalyptus.reporting.export.ExportUtils.getEventClasses;
import static com.eucalyptus.reporting.export.ExportUtils.getUsageClasses;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 */
public class Export {
  private static final Logger log = Logger.getLogger( Export.class );

  private static final String CREATION_TIMESTAMP = "creationTimestamp";

  public static ReportingExport export( final Date startDate,
                                        final Date endDate,
                                        final boolean includeDependencies ) {
    final ReportingExport export = new ReportingExport();
    final Conjunction criterion = Restrictions.conjunction();
    if ( startDate != null ) {
      criterion.add( Restrictions.ge( CREATION_TIMESTAMP, startDate ) );
    }
    if ( endDate != null ) {
      criterion.add( Restrictions.lt( CREATION_TIMESTAMP, endDate ) );
    }

    final List<ReportingEventSupport> actions = Lists.newArrayList();
    export.addUsage( Iterables.filter( Iterables.transform(
        iterableExporter( criterion, getUsageClasses(), Collections.<ReportingEventSupport>emptyList(), includeDependencies ),
        ExportUtils.toExportUsage( includeDependencies ? actions : null ) ), Predicates.notNull() ) );
    export.addActions( Iterables.transform(
        Iterables.concat( actions, iterableExporter( criterion, getEventClasses(), actions, includeDependencies ) ),
        Functions.compose( userAccountDecorator(), ExportUtils.toExportAction() ) ) );

    return export;
  }

  private static Function<ReportedAction,ReportedAction> userAccountDecorator() {
    final Map<String,ReportingUser> userIdToUserNameMap = Maps.newHashMap();
    final Map<String,String> accountIdToNameMap = Maps.newHashMap();
    return new Function<ReportedAction,ReportedAction>() {
      @Override
      public ReportedAction apply( final ReportedAction reportedAction ) {
        if ( reportedAction.getUserId() != null ) {
          ReportingUser user = userIdToUserNameMap.get( reportedAction.getUserId() );
          if ( user == null ) {
            user = getById( ReportingUser.class, reportedAction.getUserId() );
            userIdToUserNameMap.put( reportedAction.getUserId(), user );
          }
          String accountName = accountIdToNameMap.get( user.getAccountId() );
          if ( accountName == null ) {
            accountName = getById( ReportingAccount.class, user.getAccountId() ).getName();
            accountIdToNameMap.put( user.getAccountId(), accountName );
          }
          reportedAction.setUserName( user.getName() );
          reportedAction.setAccountId( user.getAccountId() );
          reportedAction.setAccountName( accountName );
        }
        return reportedAction;
      }
    };
  }

  @SuppressWarnings( "unchecked" )
  private static <T> T getById( final Class<T> itemClass, final Object id ) {
    final EntityTransaction transaction = Entities.get( itemClass );
    try {
      return (T) criteriaFor(
          itemClass,
          Restrictions.idEq( id ))
          .uniqueResult();
    } finally {
      transaction.rollback();
    }
  }

  private static Iterable<ReportingEventSupport> iterableExporter(
      final Conjunction criterion,
      final Iterable<Class<? extends ReportingEventSupport>> classes,
      final Iterable<ReportingEventSupport> existingDependencies,
      final boolean includeDependencies ) {
    return new Iterable<ReportingEventSupport>() {
      @Override
      public Iterator<ReportingEventSupport> iterator() {
        final List<Iterator<ReportingEventSupport>> iterators = Lists.newArrayList();
        final Set<EventDependency> dependencies =
            Sets.newHashSet( Iterables.transform( existingDependencies, toDependency() ) );
        for ( final Class<? extends ReportingEventSupport> eventClass : classes ) {
          iterators.add( iteratorFor( eventClass, dependencies, criterion, includeDependencies ) );
        }
        return Iterators.concat( iterators.iterator() );
      }
    };
  }

  private static Function<ReportingEventSupport, EventDependency> toDependency() {
    return new Function<ReportingEventSupport, EventDependency>() {
      @Override
      public EventDependency apply( final ReportingEventSupport reportingEventSupport ) {
        return reportingEventSupport.asDependency();
      }
    };
  }

  private static Iterator<ReportingEventSupport> iteratorFor(
      final Class<? extends ReportingEventSupport> eventClass,
      final Set<EventDependency> dependencies,
      final Criterion criterion,
      final boolean includeDependencies ) {
    return new Iterator<ReportingEventSupport>() {
      private static final int batchSize = 5000;
      private int offset = 0;
      private final LinkedList<ReportingEventSupport> data = Lists.newLinkedList();

      @Override
      public boolean hasNext() {
        ensureData();
        return !data.isEmpty();
      }

      @Override
      public ReportingEventSupport next() {
        ensureData();
        return data.removeFirst();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private void ensureData() {
        if ( data.isEmpty() ) {
          readNextBatch();
        }
      }

      private void readNextBatch() {
        final EntityTransaction transaction = Entities.get( eventClass );
        try {
          addToExportList(
              data,
              dependencies,
              includeDependencies,
              criteriaFor(eventClass, criterion).setFirstResult( offset ).setMaxResults( batchSize ).list() );
        } catch ( Exception e) {
          log.error(e, e);
        } finally {
          offset += batchSize;
          transaction.rollback();
        }
      }
    };
  }

  private static Criteria criteriaFor( final Class<?> persistentClass, final Criterion criterion ) {
    return Entities.createCriteria(persistentClass)
        .setReadOnly(true)
        .setCacheable(false)
        .setCacheMode(CacheMode.IGNORE)
        .setFetchSize(500)
        .add( criterion );
  }

  private static void addToExportList( final List<ReportingEventSupport> export,
                                       final Set<EventDependency> dependencies,
                                       final boolean includeDependencies,
                                       final List entities ) {
    for ( final Object entity : entities ) {
      if ( entity instanceof ReportingEventSupport ) {
        final ReportingEventSupport eventSupport = (ReportingEventSupport) entity;

        addToExportList(export, dependencies, includeDependencies, eventSupport);
      }
    }
  }

  private static void addToExportList( final List<ReportingEventSupport> export,
                                       final Set<EventDependency> dependencies,
                                       final boolean includeDependencies,
                                       final ReportingEventSupport eventSupport ) {
    final EventDependency dependency = eventSupport.asDependency();
    if ( dependency != null ) {
      if ( dependencies.contains( dependency ) ) {
        return; // already present
      }
      dependencies.add( dependency );
    }
    if ( includeDependencies ) {
      ensureDependencies( export, dependencies, eventSupport );
    }
    export.add( eventSupport );
  }

  private static void ensureDependencies( final List<ReportingEventSupport> export,
                                          final Set<EventDependency> dependencies,
                                          final ReportingEventSupport persistent ) {
    for ( final EventDependency dependency : persistent.getDependencies() ) {
      ensureDependency( export, dependencies, dependency );
    }
  }

  private static void ensureDependency( final List<ReportingEventSupport> export,
                                        final Set<EventDependency> dependencies,
                                        final EventDependency dependency ) {
    if ( !dependencies.contains( dependency ) &&
        ReportingEventSupport.class.isAssignableFrom( dependency.getDependencyType() ) ) {
      final Object value = criteriaFor(
          dependency.getDependencyType(),
          Restrictions.eq(dependency.getProperty(), dependency.getValue()) )
          .uniqueResult();

      if ( value instanceof ReportingEventSupport ) {
        addToExportList(export, dependencies, true, (ReportingEventSupport) value);
      }
    }
  }
}
