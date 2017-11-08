/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
