/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.dns;

import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.google.common.net.HostSpecifier;

/**
 *
 */
@ConfigurableClass(root = "services.rds.dns", description = "Parameters controlling rds")
public class RdsDnsProperties {

  private static Logger LOG = Logger.getLogger(RdsDnsProperties.class);

  @ConfigurableField(
      description = "Enable the rds dns resolver.  Note: dns.enabled must also be 'true'",
      initial = "true" )
  public static Boolean RESOLVER_ENABLED = Boolean.TRUE;

  @ConfigurableField(
      description = "rds dns subdomain",
      initial = "rds",
      changeListener = RdsDnsSubdomainChangeListener.class
  )
  public static String SUBDOMAIN = "rds";

  @ConfigurableField(
      description = "rds dns ttl value",
      initial = "60",
      changeListener = RdsDnsTtlChangeListener.class
  )
  public static String TTL = "60";

  public static boolean isResolverEnabled() {
    return Boolean.TRUE.equals(RESOLVER_ENABLED);
  }

  public static String getRdsSubdomain(){
    return SUBDOMAIN;
  }

  public static int getRdsTtl(){
    return Integer.parseInt(TTL);
  }

  public static class RdsDnsSubdomainChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String ) {
          if(!HostSpecifier.isValid(String.format("%s.com", (String) newValue)))
            throw new ConfigurablePropertyException("Malformed domain name");
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Malformed domain name");
      }
    }
  }

  public static class RdsDnsTtlChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try{
        final int ttl = Integer.parseInt((String)newValue);
        if (ttl < 0 || ttl > 604800) {
          throw new ConfigurablePropertyException("Ttl value out of range (0 - 604800)");
        }
      }catch(final Exception ex){
        throw new ConfigurablePropertyException("Malformed ttl value");
      }
    }
  }
}