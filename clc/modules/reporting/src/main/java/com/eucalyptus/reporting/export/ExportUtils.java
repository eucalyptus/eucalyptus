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

import java.util.List;
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.eucalyptus.reporting.event_store.ReportingS3BucketCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingS3BucketDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectUsageEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeUsageEvent;
import com.google.common.collect.ImmutableList;

/**
 *
 */
class ExportUtils {

  static final List<Class<? extends ReportingEventSupport>> eventClasses = ImmutableList.of(
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
}
