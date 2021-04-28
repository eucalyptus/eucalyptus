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

package com.eucalyptus.loadbalancing;

import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.google.common.net.HostSpecifier;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 */
@ConfigurableClass(root = "services.loadbalancing", description = "Parameters controlling loadbalancing")
public class LoadBalancerDnsRecord {

  public static class ELBDnsChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try {
        if (newValue instanceof String) {
          if (!HostSpecifier.isValid(String.format("%s.com", (String) newValue))) {
            throw new ConfigurablePropertyException("Malformed domain name");
          }
        }
      } catch (final Exception e) {
        throw new ConfigurablePropertyException("Malformed domain name");
      }
    }
  }

  public static class ELBDnsTtlChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try {
        final int ttl = Integer.parseInt((String) newValue);
      } catch (final Exception ex) {
        throw new ConfigurablePropertyException("Malformed ttl value");
      }
    }
  }

  private static Logger LOG = Logger.getLogger(LoadBalancerDnsRecord.class);
  @ConfigurableField(displayName = "loadbalancer_dns_subdomain",
      description = "loadbalancer dns subdomain",
      initial = "lb",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ELBDnsChangeListener.class
  )
  public static String DNS_SUBDOMAIN = "lb";

  @ConfigurableField(displayName = "loadbalancer_dns_ttl",
      description = "loadbalancer dns ttl value",
      initial = "60",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ELBDnsTtlChangeListener.class
  )
  public static String DNS_TTL = "60";

  public static int getLoadbalancerTTL() {
    return Integer.parseInt(DNS_TTL);
  }
}