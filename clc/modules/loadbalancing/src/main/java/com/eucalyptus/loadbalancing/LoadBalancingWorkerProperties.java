/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
      description = "EMI containing haproxy and the controller",
      initial = "NULL", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbEmiChangeListener.class)
  public static String IMAGE = "NULL";
  // com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.image

  @ConfigurableField( displayName = "instance_type", 
      description = "instance type for loadbalancer instances",
      initial = "m1.medium",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbInstanceTypeChangeListener.class)
  public static String INSTANCE_TYPE = "m1.medium";
  // com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.instance_type

  @ConfigurableField( displayName = "keyname", 
      description = "keyname to use when debugging loadbalancer VMs",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbKeyNameChangeListener.class)
  public static String KEYNAME = null;
  // com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.keyname

  @ConfigurableField( displayName = "ntp_server", 
      description = "the address of the NTP server used by loadbalancer VMs", 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbNTPServerChangeListener.class
      )
  public static String NTP_SERVER = null;
  // com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.ntp_server

  @ConfigurableField( displayName = "app_cookie_duration",
      description = "duration of app-controlled cookie to be kept in-memory (hours)",
      initial = "24", // 24 hours by default 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbAppCookieDurationChangeListener.class)
  public static String APP_COOKIE_DURATION = "24";
  // com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.app_cookie_duration

  @ConfigurableField( displayName = "expiration_days",
      description = "the days after which the loadbalancer Vms expire",
      initial = "365", // 1 year by default 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbVmExpirationDaysChangeListener.class)
  public static String EXPIRATION_DAYS = "365";
  // com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.expiration_days

  @ConfigurableField(displayName = "init_script",
      description = "bash script that will be executed before service configuration and start up",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = InitScriptChangeListener.class)
  public static String INIT_SCRIPT = null;
  // com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.init_script


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
        throw new ConfigurablePropertyException("Could not change EMI ID due to: " + e.getMessage());
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
            for (LoadBalancer lb:LoadBalancers.listLoadbalancers()){
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

  private static void onPropertyChange(final String emi, final String instanceType,
      final String keyname, String initScript) throws EucalyptusCloudException{
    if (!( Bootstrap.isOperational()  && Topology.isEnabled( Compute.class ) ) )
      return;
    if ((emi!=null && emi.length()>0) ||
        (instanceType!=null && instanceType.length()>0) ||
        (keyname!=null) || (initScript != null) ){
      if(!LoadBalancingWorkflows.modifyServicePropertiesSync(emi, instanceType, 
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
