/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.autoscaling.config;

import static com.eucalyptus.autoscaling.activities.ActivityManager.ActivityTask;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.autoscaling.groups.ScalingProcessType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.util.Intervals;
import com.google.common.base.Enums;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
@ConfigurableClass(root = "autoscaling", description = "Parameters controlling auto scaling")
public class AutoScalingConfiguration {
  private static final Logger logger = Logger.getLogger( AutoScalingConfiguration.class );

  @ConfigurableField( initial = "5m", description = "Timeout for a scaling activity.", changeListener = AutoScalingIntervalPropertyChangeListener.class )
  public static volatile String activityTimeout = "5m";

  @ConfigurableField( initial = "42d", description = "Expiry age for scaling activities. Older activities are deleted.", changeListener = AutoScalingIntervalPropertyChangeListener.class )
  public static volatile String activityExpiry = "42d";

  @ConfigurableField( initial = "20", description = "Maximum instances to launch at one time." )
  public static volatile int maxLaunchIncrement = 20;

  @ConfigurableField( initial = "5", description = "Number of times to attempt load balancer registration for each instance." )
  public static volatile int maxRegistrationRetries = 5;

  @ConfigurableField( initial = "5m", description = "Time after which an unavailable zone should be treated as failed", changeListener=AutoScalingIntervalPropertyChangeListener.class )
  public static volatile String zoneFailureThreshold = "5m";

  @ConfigurableField( initial = "1d", description = "Timeout for administrative suspension of scaling activities for a group.", changeListener=AutoScalingIntervalPropertyChangeListener.class )
  public static volatile String suspensionTimeout = "1d";

  @ConfigurableField( initial = "15", description = "Minimum launch attempts for administrative suspension of scaling activities for a group." )
  public static volatile int suspensionLaunchAttemptsThreshold = 15;

  @ConfigurableField( initial = "", description = "Globally suspended scaling processes.", changeListener = AutoScalingSuspendedProcessesPropertyChangeListener.class )
  public static volatile String suspendedProcesses = "";

  @ConfigurableField( initial = "", description = "Suspended scaling tasks.", changeListener = AutoScalingSuspendedTasksPropertyChangeListener.class  )
  public static volatile String suspendedTasks = "";

  @ConfigurableField( initial = "5m", description = "Timeout for termination of untracked auto scaling instances." )
  public static volatile String untrackedInstanceTimeout = "5m";

  @ConfigurableField( initial = "15m", description = "Timeout for a pending instance.", changeListener = AutoScalingIntervalPropertyChangeListener.class )
  public static volatile String pendingInstanceTimeout = "15m";

  @ConfigurableField( initial = "15m", description = "Maximum backoff period for failing activities.", changeListener = AutoScalingIntervalPropertyChangeListener.class )
  public static volatile String activityMaxBackoff = "15m";

  @ConfigurableField( initial = "9s", description = "Initial backoff period for failing activities.", changeListener = AutoScalingIntervalPropertyChangeListener.class )
  public static volatile String activityInitialBackoff = "9s";

  private static AtomicLong activityTimeoutMillis = new AtomicLong( Intervals.parse( activityTimeout, TimeUnit.MINUTES.toMillis( 5 ) ) );
  private static AtomicLong activityExpiryMillis =  new AtomicLong( Intervals.parse( activityExpiry, TimeUnit.DAYS.toMillis( 42 ) ) );
  private static AtomicLong zoneFailureThresholdMillis = new AtomicLong( Intervals.parse( zoneFailureThreshold, TimeUnit.MINUTES.toMillis( 5 ) ) );
  private static AtomicLong suspensionTimeoutMillis = new AtomicLong( Intervals.parse( suspensionTimeout, TimeUnit.DAYS.toMillis( 1 ) ) );
  private static AtomicLong untrackedInstanceTimeoutMillis = new AtomicLong( Intervals.parse( untrackedInstanceTimeout, TimeUnit.MINUTES.toMillis( 5 ) ) );
  private static AtomicLong pendingInstanceTimeoutMillis = new AtomicLong( Intervals.parse( pendingInstanceTimeout, TimeUnit.MINUTES.toMillis( 15 ) ) );
  private static AtomicLong activityMaxBackoffMillis = new AtomicLong( Intervals.parse( activityMaxBackoff, TimeUnit.MINUTES.toMillis( 15 ) ) );
  private static AtomicLong activityInitialBackoffMillis = new AtomicLong( Intervals.parse( activityInitialBackoff, TimeUnit.SECONDS.toMillis( 9 ) ) );
  private static AtomicReference<EnumSet<ScalingProcessType>> suspendedProcessesSet = new AtomicReference<EnumSet<ScalingProcessType>>( toEnumSet( ScalingProcessType.class, suspendedProcesses ) );
  private static AtomicReference<EnumSet<ActivityTask>> suspendedTasksSet = new AtomicReference<EnumSet<ActivityTask>>( toEnumSet( ActivityTask.class, suspendedTasks ) );

  public static int getMaxLaunchIncrement() {
    return maxLaunchIncrement;
  }

  public static int getMaxRegistrationRetries() {
    return maxRegistrationRetries;
  }

  public static int getSuspensionLaunchAttemptsThreshold() {
    return suspensionLaunchAttemptsThreshold;
  }

  public static long getActivityTimeoutMillis() {
    return activityTimeoutMillis.get();
  }

  public static long getActivityExpiryMillis() {
    return activityExpiryMillis.get();
  }

  public static long getZoneFailureThresholdMillis() {
    return zoneFailureThresholdMillis.get();
  }

  public static long getSuspensionTimeoutMillis() {
    return suspensionTimeoutMillis.get();
  }

  public static long getUntrackedInstanceTimeoutMillis() {
    return untrackedInstanceTimeoutMillis.get();
  }

  public static long getPendingInstanceTimeoutMillis() {
    return pendingInstanceTimeoutMillis.get();
  }

  public static long getActivityMaxBackoffMillis() {
    return activityMaxBackoffMillis.get();
  }

  public static long getActivityInitialBackoffMillis() {
    return activityInitialBackoffMillis.get();
  }

  public static EnumSet<ScalingProcessType> getSuspendedProcesses() {
    return suspendedProcessesSet.get();
  }

  public static EnumSet<ActivityTask> getSuspendedTasks() {
    return suspendedTasksSet.get();
  }

  private static <E extends Enum<E>> EnumSet<E> toEnumSet( final Class<E> enumClass,
                                                           final String text ) {
    final List<E> values = Lists.newArrayList( Iterables.filter(
        Iterables.transform(
            Splitter.on( "," ).omitEmptyStrings().trimResults().split( text ),
            Enums.valueOfFunction( enumClass ) ),
        Predicates.notNull() ) );
    return values.isEmpty() ?
      EnumSet.noneOf( enumClass ) :
      EnumSet.copyOf( values );
  }

  public static final class AutoScalingIntervalPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty configurableProperty,
                            final Object newValue ) throws ConfigurablePropertyException {
      try {
        final String fieldName = configurableProperty.getField().getName() + "Millis";
        final Field field = AutoScalingConfiguration.class.getDeclaredField( fieldName );
        final long defaultValue = Intervals.parse( configurableProperty.getDefaultValue() );
        final long value = Intervals.parse( String.valueOf( newValue ), defaultValue );
        field.setAccessible( true );
        logger.info( "Auto scaling configuration updated " + field.getName() + ": " + value + "ms" );
        ((AtomicLong)field.get( null )).set( value );
      } catch ( NoSuchFieldException e ) {
        logger.error( e, e );
      } catch ( ParseException e ) {
        logger.error( e, e );
      } catch ( IllegalAccessException e ) {
        logger.error( e, e );
      }
    }
  }

  public static final class AutoScalingSuspendedProcessesPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty configurableProperty,
                            final Object newValue ) throws ConfigurablePropertyException {
      final EnumSet<ScalingProcessType> scalingProcessTypes = toEnumSet( ScalingProcessType.class, String.valueOf( newValue ) );
      logger.info( "Suspended auto scaling processes updated: " + scalingProcessTypes );
      suspendedProcessesSet.set( scalingProcessTypes );
    }
  }

  public static final class AutoScalingSuspendedTasksPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty configurableProperty,
                            final Object newValue ) throws ConfigurablePropertyException {
      final EnumSet<ActivityTask> activityTasks = toEnumSet( ActivityTask.class, String.valueOf( newValue ) );
      logger.info( "Suspended auto scaling activity tasks updated: " + activityTasks );
      suspendedTasksSet.set( activityTasks );
    }
  }
}
