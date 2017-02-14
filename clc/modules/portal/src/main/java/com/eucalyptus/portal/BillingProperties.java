/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.portal.awsusage.SimpleQueueClientManager;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ConfigurableClass(root = "services.billing", description = "Parameters controlling billing service")
public class BillingProperties {
  private static final String DEFAULT_SQS_CLIENT_CONFIG =
          "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}";

  private static final String DEFAULT_SENSOR_QUEUE_ATTRIBUTES =
          "{\"DelaySeconds\": \"0\", \"MaximumMessageSize\": \"262144\", " +
                  "\"MessageRetentionPeriod\": \"10800\", \"ReceiveMessageWaitTimeSeconds\": \"0\", " +
                  "\"VisibilityTimeout\": \"120\"}";

  private static final String DEFAULT_SWF_CLIENT_CONFIG =
          "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}";

  private static final String DEFAULT_SWF_ACTIVITY_WORKER_CONFIG =
          "{\"PollThreadCount\": 4, \"TaskExecutorThreadPoolSize\": 32, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  private static final String DEFAULT_SWF_WORKFLOW_WORKER_CONFIG =
          "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 4, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  @ConfigurableField( description = "Enable billing's data collection and aggregation" )
  public static Boolean ENABLED = Boolean.TRUE;

  @ConfigurableField(
          initial = DEFAULT_SQS_CLIENT_CONFIG,
          description = "JSON configuration for the billing SQS client",
          changeListener = ClientConfigurationValidatingChangeListener.class )
  public static volatile String SQS_CLIENT_CONFIG = DEFAULT_SQS_CLIENT_CONFIG;


  @ConfigurableField(
          initial = DEFAULT_SENSOR_QUEUE_ATTRIBUTES,
          description = "JSON attributes for the sensor queue",
          changeListener = SensorQueueAttributesChangeListener.class)
  public static String SENSOR_QUEUE_ATTRIBUTES = DEFAULT_SENSOR_QUEUE_ATTRIBUTES;

  @ConfigurableField(
          initial = "BillingDomain",
          description = "The simple workflow service domain for billing",
          changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_DOMAIN = "BillingDomain";

  @ConfigurableField(
          initial = "BillingTasks",
          description = "The simple workflow service task list for billing",
          changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_TASKLIST = "BillingTasks";

  @ConfigurableField(
          initial = DEFAULT_SWF_CLIENT_CONFIG,
          description = "JSON configuration for the billing simple workflow client",
          changeListener = Config.ClientConfigurationValidatingChangeListener.class )
  public static volatile String SWF_CLIENT_CONFIG = DEFAULT_SWF_CLIENT_CONFIG;

  @ConfigurableField(
          initial = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG,
          description = "JSON configuration for the billing workflow activity worker",
          changeListener = Config.ActivityWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_ACTIVITY_WORKER_CONFIG = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG;

  @ConfigurableField(
          initial = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG,
          description = "JSON configuration for the billing workflow decision worker",
          changeListener = Config.WorkflowWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_WORKFLOW_WORKER_CONFIG = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG;


  public static String SENSOR_QUEUE_NAME = "BillingSensorQueue";

  public static final class ClientConfigurationValidatingChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue != null && !newValue.toString( ).trim( ).isEmpty( ) ) try {
        SimpleQueueClientManager.buildConfiguration( newValue.toString( ).trim( ) );
      } catch ( final IllegalArgumentException e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
    }
  }

  public static final class SensorQueueAttributesChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue != null && !newValue.toString( ).trim( ).isEmpty( ) ) try {
        SimpleQueueClientManager.getInstance().setQueueAttributes( SENSOR_QUEUE_NAME,
                getQueueAttributes(newValue.toString()) );
      } catch ( final IllegalArgumentException e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      } catch (final IOException e) {
        throw new ConfigurablePropertyException( "Invalid JSON: "+ e.getMessage( ) );
      } catch (final Exception e) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
    }
  }

  public static Map<String, String> getQueueAttributes() throws IOException {
    return getQueueAttributes(SENSOR_QUEUE_ATTRIBUTES);
  }

  public static Map<String, String> getQueueAttributes(final String strAttributes) throws IOException {
    final ObjectMapper mapper = new ObjectMapper( );
    final TypeReference<HashMap<String,String>> typeRef
            = new TypeReference<HashMap<String,String>>() {};
    return mapper.readValue(strAttributes.trim(), typeRef);
  }
}
