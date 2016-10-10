/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.util.Intervals;

/**
 *
 */
@ConfigurableClass(root = "services.simpleworkflow", description = "Parameters controlling Simple Workflow")
public class SimpleWorkflowProperties {

  private static final Logger logger = Logger.getLogger( SimpleWorkflowProperties.class );

  @ConfigurableField( initial = "true", description = "Service available for internal/administrator use only." )
  public static volatile boolean systemOnly = true;

  @ConfigurableField( initial = "10000", description = "Maximum number of activity types for each domain." )
  public static volatile int activityTypesPerDomain = 10000;

  @ConfigurableField( initial = "10000", description = "Maximum number of workflow types for each domain." )
  public static volatile int workflowTypesPerDomain = 10000;

  @ConfigurableField( initial = "100000", description = "Maximum number of open workflow executions for each domain." )
  public static volatile int openWorkflowExecutionsPerDomain = 100000;

  @ConfigurableField( initial = "1000", description = "Maximum number of open activity tasks for each workflow execution." )
  public static volatile int openActivityTasksPerWorkflowExecution = 1000;

  @ConfigurableField( initial = "1000", description = "Maximum number of open timers for each workflow execution." )
  public static volatile int openTimersPerWorkflowExecution = 1000;

  @ConfigurableField( initial = "25000", description = "Maximum number of events per workflow execution." )
  public static volatile int workflowExecutionHistorySize = 25000;

  @ConfigurableField(
      initial = "365d",
      description = "Maximum workflow execution time.",
      changeListener = SimpleWorkflowIntervalPropertyChangeListener.class )
  public static volatile String workflowExecutionDuration = "365d";

  @ConfigurableField(
      initial = "90d",
      description = "Maximum workflow execution history retention time.",
      changeListener = SimpleWorkflowIntervalPropertyChangeListener.class )
  public static volatile String workflowExecutionRetentionDuration = "90d";

  @ConfigurableField(
      initial = "30d",
      description = "Deprecated activity type retention time.",
      changeListener = SimpleWorkflowIntervalPropertyChangeListener.class )
  public static volatile String deprecatedActivityTypeRetentionDuration = "30d";

  @ConfigurableField(
      initial = "1d",
      description = "Deprecated workflow type minimum retention time.",
      changeListener = SimpleWorkflowIntervalPropertyChangeListener.class )
  public static volatile String deprecatedWorkflowTypeRetentionDuration = "1d";

  @ConfigurableField(
      initial = "1d",
      description = "Deprecated domain minimum retention time.",
      changeListener = SimpleWorkflowIntervalPropertyChangeListener.class )
  public static volatile String deprecatedDomainRetentionDuration = "1d";

  private static AtomicLong workflowExecutionDurationMillis =
      new AtomicLong( Intervals.parse( workflowExecutionDuration, TimeUnit.DAYS.toMillis( 365 ) ) );

  private static AtomicLong workflowExecutionRetentionDurationMillis =
      new AtomicLong( Intervals.parse( workflowExecutionRetentionDuration, TimeUnit.DAYS.toMillis( 90 ) ) );

  private static AtomicLong deprecatedActivityTypeRetentionDurationMillis =
      new AtomicLong( Intervals.parse( deprecatedActivityTypeRetentionDuration, TimeUnit.DAYS.toMillis( 30 ) ) );

  private static AtomicLong deprecatedWorkflowTypeRetentionDurationMillis =
      new AtomicLong( Intervals.parse( deprecatedWorkflowTypeRetentionDuration, TimeUnit.DAYS.toMillis( 1 ) ) );

  private static AtomicLong deprecatedDomainRetentionDurationMillis =
      new AtomicLong( Intervals.parse( deprecatedDomainRetentionDuration, TimeUnit.DAYS.toMillis( 1 ) ) );

  public static boolean isSystemOnly() {
    return systemOnly;
  }

  public static int getActivityTypesPerDomain() {
    return activityTypesPerDomain;
  }

  public static int getWorkflowTypesPerDomain() {
    return workflowTypesPerDomain;
  }

  public static int getOpenWorkflowExecutionsPerDomain() {
    return openWorkflowExecutionsPerDomain;
  }

  public static int getOpenActivityTasksPerWorkflowExecution() {
    return openActivityTasksPerWorkflowExecution;
  }

  public static int getOpenTimersPerWorkflowExecution() {
    return openTimersPerWorkflowExecution;
  }

  public static int getWorkflowExecutionHistorySize() {
    return workflowExecutionHistorySize;
  }

  public static long getWorkflowExecutionDurationMillis() {
    return workflowExecutionDurationMillis.get();
  }

  public static long getWorkflowExecutionRetentionDurationMillis() {
    return workflowExecutionRetentionDurationMillis.get();
  }

  public static long getDeprecatedActivityTypeRetentionDurationMillis() {
    return deprecatedActivityTypeRetentionDurationMillis.get();
  }

  public static long getDeprecatedWorkflowTypeRetentionDurationMillis() {
    return deprecatedWorkflowTypeRetentionDurationMillis.get();
  }

  public static long getDeprecatedDomainRetentionDurationMillis() {
    return deprecatedDomainRetentionDurationMillis.get();
  }

  public static final class SimpleWorkflowIntervalPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty configurableProperty,
                            final Object newValue ) throws ConfigurablePropertyException {
      try {
        final String fieldName = configurableProperty.getField().getName() + "Millis";
        final Field field = SimpleWorkflowProperties.class.getDeclaredField( fieldName );
        final long defaultValue = Intervals.parse( configurableProperty.getDefaultValue() );
        final long value = Intervals.parse( String.valueOf( newValue ), defaultValue );
        field.setAccessible( true );
        logger.info( "Simple workflow configuration updated " + field.getName() + ": " + value + "ms" );
        ((AtomicLong)field.get( null )).set( value );
      } catch ( NoSuchFieldException | ParseException | IllegalAccessException e ) {
        logger.error( e, e );
      }
    }
  }
}
