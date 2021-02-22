/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.net.HostSpecifier;

/**
 *
 */
@ConfigurableClass(root = "services.rds.worker", description = "Parameters controlling rds")
public class RdsWorkerProperties {
  private static Logger LOG = Logger.getLogger( RdsWorkerProperties.class );

  @ConfigurableField( displayName = "image",
      description = "Machine image for rds instances",
      initial = "NULL",
      changeListener = PropertyChangeListeners.RegexMatchListener.class)
  @PropertyChangeListeners.RegexMatchListener.RegexMatch(
      message = "Image identifier required",
      regex = "[ae]mi-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?")
  public static String IMAGE = "NULL";

  @ConfigurableField( displayName = "keyname",
      description = "Key to use when debugging rds vms",
      changeListener = RdsKeyNameChangeListener.class)
  public static String KEYNAME = null;

  @ConfigurableField( displayName = "ntp_server",
      description = "the address of the NTP server used by rds vms",
      changeListener = RdsNtpServerChangeListener.class
  )
  public static String NTP_SERVER = null;

  public static class RdsKeyNameChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( ConfigurableProperty t, String keyname ) throws ConfigurablePropertyException {
      try {
        if(t.getValue()!=null && !t.getValue().equals(keyname)) {
          if ( keyname != null && !keyname.isEmpty() ) {
            try {
              Ec2Client.getInstance().describeKeyPairs(Accounts.lookupSystemAccountByAlias(
                  RdsSystemAccountProvider.RDS_SYSTEM_ACCOUNT ).getUserId( ), Lists.newArrayList(keyname));
            } catch(final Exception ex) {
              throw new ConfigurablePropertyException("Could not change key name due to: " + ex.getMessage()
                  + ". Do you have keypair " + keyname + " in the "
                  + RdsSystemAccountProvider.RDS_SYSTEM_ACCOUNT + " account?");
            }
          }
        }
      } catch ( final ConfigurablePropertyException e ) {
        throw e;
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not change key name due to: " + e.getMessage());
      }
    }
  }

  public static class RdsNtpServerChangeListener implements PropertyChangeListener {
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
}
