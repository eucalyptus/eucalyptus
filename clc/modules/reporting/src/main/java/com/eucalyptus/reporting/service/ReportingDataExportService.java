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
package com.eucalyptus.reporting.service;

import static com.eucalyptus.reporting.event_store.ReportingEventSupport.EventDependency;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.*;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 */
public class ReportingDataExportService {

  private static final Logger log = Logger.getLogger( ReportingDataExportService.class );

  private static final List<Class<? extends ReportingEventSupport>> eventClasses = ImmutableList.of(
      ReportingElasticIpCreateEvent.class,
      ReportingElasticIpAttachEvent.class,
      ReportingElasticIpDetachEvent.class,
      ReportingElasticIpDeleteEvent.class,
      ReportingInstanceCreateEvent.class,
      ReportingInstanceUsageEvent.class,
      ReportingS3BucketCreateEvent.class,
      ReportingS3BucketDeleteEvent.class,
      ReportingS3ObjectCreateEvent.class,
      ReportingS3ObjectDeleteEvent.class,
      ReportingS3ObjectUsageEvent.class,
      ReportingVolumeCreateEvent.class,
      ReportingVolumeAttachEvent.class,
      ReportingVolumeDetachEvent.class,
      ReportingVolumeDeleteEvent.class,
      ReportingVolumeSnapshotCreateEvent.class,
      ReportingVolumeSnapshotDeleteEvent.class,
      ReportingVolumeUsageEvent.class
  );

  private static final String CREATION_TIMESTAMP = "creationTimestamp";

  public ExportDataResponseType exportData( final ExportDataType request ) throws EucalyptusCloudException {
    final ExportDataResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final User requestUser = ctx.getUser( );

    if ( !requestUser.isSystemAdmin() ) {
      throw new ReportingException( HttpResponseStatus.UNUATHORIZED, ReportingException.NOT_AUTHORIZED, "Not authorized");
    }

    final Conjunction criterion = Restrictions.conjunction();
    if ( request.getStartDate() != null ) {
      criterion.add( Restrictions.ge( CREATION_TIMESTAMP, request.getStartDate() ) );
    }
    if ( request.getEndDate() != null ) {
      criterion.add( Restrictions.lt( CREATION_TIMESTAMP, request.getEndDate() ) );
    }

    final List<Serializable> exportData = Lists.newArrayList();
    final Set<EventDependency> dependencies = Sets.newHashSet();
    for ( final Class<? extends AbstractPersistent> eventClass : eventClasses ) {
      final EntityTransaction transaction = Entities.get( eventClass );
      try {
        addToExportList(
            exportData,
            dependencies,
            criteriaFor(eventClass, criterion).list() );
      } catch ( Exception e) {
        log.error(e, e);
      } finally {
        transaction.rollback();
      }
    }

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      final ObjectOutputStream oout = new ObjectOutputStream( out );
      oout.writeObject( exportData );
      oout.flush();
      oout.close();
      reply.setResult( new ExportDataResultType( B64.standard.encString( out.toByteArray() ) ) );
    } catch (IOException e) {
      throw new EucalyptusCloudException( e );
    }

    return reply;
  }

  private static Criteria criteriaFor( final Class<?> persistentClass, final Criterion criterion ) {
    return Entities.createCriteria(persistentClass)
        .setReadOnly(true)
        .add(criterion)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .setCacheable(false);
  }

  private static void addToExportList( final List<Serializable> exportData,
                                       final Set<EventDependency> dependencies,
                                       final List entities ) {
    for ( final Object entity : entities ) {
      if ( entity instanceof ReportingEventSupport ) {
        final ReportingEventSupport eventSupport = (ReportingEventSupport) entity;

        addToExportList(exportData, dependencies, eventSupport);
      }
    }
  }

  private static void addToExportList( final List<Serializable> exportData,
                                       final Set<EventDependency> dependencies,
                                       final ReportingEventSupport eventSupport ) {
    final EventDependency dependency = eventSupport.asDependency();
    if ( dependency != null ) {
      dependencies.add( dependency );
    }
    ensureDependencies( exportData, dependencies, eventSupport );
    exportData.add( eventSupport );
  }

  private static void ensureDependencies( final List<Serializable> exportData,
                                          final Set<EventDependency> dependencies,
                                          final ReportingEventSupport persistent ) {
    for ( final EventDependency dependency : persistent.getDependencies() ) {
      ensureDependency( exportData, dependencies, dependency );
    }
  }

  private static void ensureDependency( final List<Serializable> exportData,
                                        final Set<EventDependency> dependencies,
                                        final EventDependency dependency ) {
    if ( !dependencies.contains( dependency ) ) {
      final Object value = criteriaFor(
          dependency.getDependencyType(),
          Restrictions.eq(dependency.getProperty(), dependency.getValue()) )
          .uniqueResult();

      if ( value instanceof ReportingEventSupport ) {
        addToExportList(exportData, dependencies, (ReportingEventSupport) value);
      } else if ( value instanceof ReportingUser) {
        final ReportingUser user = (ReportingUser) value;
        dependencies.add( new EventDependency( ReportingUser.class, "id", user.getId() ) );

        ensureDependency(
            exportData,
            dependencies,
            new EventDependency( ReportingAccount.class, "id", user.getAccountId() ) );

        exportData.add( user );
      }
    }
  }
}
