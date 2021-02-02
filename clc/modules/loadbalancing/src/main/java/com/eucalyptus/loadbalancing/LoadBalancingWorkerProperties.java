/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingWorkflows;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.net.HostSpecifier;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;


/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ConfigurableClass(root = "services.loadbalancing.worker", description = "Parameters controlling loadbalancing")
public class LoadBalancingWorkerProperties {
  private static Logger  LOG     = Logger.getLogger( LoadBalancingWorkerProperties.class );

  @ConfigurableField( displayName = "image", 
      description = "Machine image containing haproxy and the controller",
      initial = "NULL", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbEmiChangeListener.class)
  public static String IMAGE = "NULL";

  @ConfigurableField( displayName = "instance_type", 
      description = "instance type for loadbalancer instances",
      initial = "t2.nano",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbInstanceTypeChangeListener.class)
  public static String INSTANCE_TYPE = "t2.nano";

  @ConfigurableField( displayName = "keyname", 
      description = "keyname to use when debugging loadbalancer VMs",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbKeyNameChangeListener.class)
  public static String KEYNAME = null;

  @ConfigurableField( displayName = "ntp_server", 
      description = "the address of the NTP server used by loadbalancer VMs", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbNTPServerChangeListener.class
      )
  public static String NTP_SERVER = null;

  @ConfigurableField( displayName = "app_cookie_duration",
      description = "duration of app-controlled cookie to be kept in-memory (hours)",
      initial = "24", // 24 hours by default 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbAppCookieDurationChangeListener.class)
  public static String APP_COOKIE_DURATION = "24";

  @ConfigurableField( displayName = "expiration_days",
      description = "the days after which the loadbalancer Vms expire",
      initial = "365", // 1 year by default 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbVmExpirationDaysChangeListener.class)
  public static String EXPIRATION_DAYS = "365";

  @ConfigurableField(displayName = "init_script",
      description = "bash script that will be executed before service configuration and start up",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = InitScriptChangeListener.class)
  public static String INIT_SCRIPT = null;

  @ConfigurableField( displayName = "failure_threshold_for_recycle",
          description = "number of activity failure that will trigger recycling workers",
          initial = "24", // 24 hours by default
          readonly = false,
          type = ConfigurableFieldType.KEYVALUE,
          changeListener = FailureThresholdChangeListener.class)
  public static String FAILURE_THRESHOLD_FOR_RECYCLE = "24";

  @ConfigurableField( displayName = "use_elastic_ip",
          description = "flag to indicate the workers use elastic IP",
          initial = "false", // 24 hours by default
          readonly = false,
          type = ConfigurableFieldType.KEYVALUE,
          changeListener = UseElasticIpChangeListener.class)
  public static String USE_ELASTIC_IP = "false";

  public static boolean useElasticIp() {
    return Boolean.parseBoolean(USE_ELASTIC_IP);
  }

  public static class UseElasticIpChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
            throws ConfigurablePropertyException {
       if (!("true".equals(newValue) || "false".equals(newValue))) {
        throw new ConfigurablePropertyException("The value must be true or false");
      }
    }
  }

  public static class FailureThresholdChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
            throws ConfigurablePropertyException {
      try{
        final int threshold = Integer.parseInt(newValue);
      }catch(final Exception ex) {
        throw new ConfigurablePropertyException("The value must be number type");
      }
    }
  }

  public static class InitScriptChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try {
        // init script can be empty
        if (t.getValue() != null && !t.getValue().equals(newValue))
          onPropertyChange(null, null, null, (String) newValue);
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Could not change init script", e);
      }
    }
  }

  public static class ElbEmiChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String  ) {
          if(t.getValue()!=null && ! t.getValue().equals(newValue))
            onPropertyChange((String)newValue, null, null, null);
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change image ID due to: " + e.getMessage());
      }
    }
  }

  public static class ElbInstanceTypeChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String ) {
          if( newValue.equals( "" ) )
            throw new EucalyptusCloudException("Instance type cannot be unset");
          if(t.getValue()!=null && ! t.getValue().equals(newValue))
            onPropertyChange(null, (String)newValue, null, null);
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change instance type due to: " + e.getMessage());
      }
    }
  }

  public static class ElbKeyNameChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( ConfigurableProperty t, String keyname ) throws ConfigurablePropertyException {
      try {
        if(t.getValue()!=null && !t.getValue().equals(keyname)) {
          if ( keyname != null && !keyname.isEmpty() ) {
            // find out if there are any old elbs are deployed
            boolean oldElbExist = false;
            for (LoadBalancer lb: LoadBalancerHelper.listLoadbalancers()){
              if (!lb.useSystemAccount()) {
                oldElbExist = true;
                break;
              }
            }
            try {
              Ec2Client.getInstance().describeKeyPairs(Accounts.lookupSystemAccountByAlias(
                  AccountIdentifiers.ELB_SYSTEM_ACCOUNT ).getUserId( ), Lists.newArrayList(keyname));
            } catch(Exception ex) {
              throw new ConfigurablePropertyException("Could not change key name due to: " + ex.getMessage() 
              + ". Do you have keypair " + keyname + " that belongs to "
              + AccountIdentifiers.ELB_SYSTEM_ACCOUNT + " account?");
            }
            if (oldElbExist) {
              try {
                Ec2Client.getInstance().describeKeyPairs(null,
                    Lists.newArrayList(keyname));
              } catch(Exception ex) {
                throw new ConfigurablePropertyException("Could not change key name due to: " + ex.getMessage()
                + ". Do you have keypair " + keyname + " that belongs to system account?");
              }
            }
          }
          onPropertyChange(null, null, keyname, null);
        }
      } catch ( final ConfigurablePropertyException e ) {
        throw e;
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change key name due to: " + e.getMessage());
      }
    }
  }

  public static class ElbVmExpirationDaysChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange(ConfigurableProperty t, String newValue)
        throws ConfigurablePropertyException {
      try{
        final int newExp = Integer.parseInt(newValue);
        if(newExp <= 0 )
          throw new Exception();
      }catch(final Exception ex) {
        throw new ConfigurablePropertyException("The value must be number type and bigger than 0");
      }
    }
  }

  private static void onPropertyChange(final String machineImageIdentifier, final String instanceType,
      final String keyname, String initScript) throws EucalyptusCloudException{
    if (!( Bootstrap.isOperational()  && Topology.isEnabled( Compute.class ) ) )
      return;
    if ((machineImageIdentifier!=null && machineImageIdentifier.length()>0) ||
        (instanceType!=null && instanceType.length()>0) ||
        (keyname!=null) || (initScript != null) ){
      if(!LoadBalancingWorkflows.modifyServicePropertiesSync(machineImageIdentifier, instanceType,
          keyname, initScript)) {
        throw new EucalyptusCloudException("Failed to modify properties. Check log files for error details");
      }
    }
  } 

  public static class ElbNTPServerChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String ) {
          if(((String) newValue).contains(",")){
            final String[] addresses = ((String)newValue).split(",");
            if((addresses.length-1) != StringUtils.countOccurrencesOf((String) newValue, ","))
              throw new EucalyptusCloudException("Invalid address");

            for(final String address : addresses){
              if(!HostSpecifier.isValid(String.format("%s.com",address)))
                throw new EucalyptusCloudException("Invalid address");
            }
          }else{
            final String address = (String) newValue;
            if( !address.equals("") ){
              if(!HostSpecifier.isValid(String.format("%s.com", address)))
                throw new EucalyptusCloudException("Invalid address");
            }
          }
        }else
          throw new EucalyptusCloudException("Address is not string type");
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change ntp server address", e);
      }
    }
  } 

  public static class ElbAppCookieDurationChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String ) {
          try{
            final int appCookieDuration = Integer.parseInt((String)newValue);
            if(appCookieDuration <= 0)
              throw new Exception();
          }catch(final NumberFormatException ex){
            throw new ConfigurablePropertyException("Duration must be in number type and bigger than 0 (in hours)");
          }
        }
      }catch (final ConfigurablePropertyException ex){
        throw ex;
      }catch (final Exception ex) {
        throw new ConfigurablePropertyException("Could not change ELB app cookie duration", ex);
      }
    }
  }
}
