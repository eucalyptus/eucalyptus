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
 *
 */
@ConfigurableClass(root = "services.loadbalancing", description = "Parameters controlling loadbalancing")
public class LoadBalancerDnsRecord {

	public static class ELBDnsChangeListener implements PropertyChangeListener {
	   @Override
	   public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
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
	       
	public static class ELBDnsTtlChangeListener implements PropertyChangeListener {
	    @Override
	    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
	      try{
	        final int ttl = Integer.parseInt((String)newValue);
	      }catch(final Exception ex){
	       throw new ConfigurablePropertyException("Malformed ttl value"); 
	      }
	    }
	}

	private static Logger    LOG     = Logger.getLogger( LoadBalancerDnsRecord.class );
	@ConfigurableField( displayName = "loadbalancer_dns_subdomain",
			description = "loadbalancer dns subdomain",
			initial = "lb",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ELBDnsChangeListener.class
			)
	public static String DNS_SUBDOMAIN = "lb";
	
	@ConfigurableField( displayName = "loadbalancer_dns_ttl",
	    description = "loadbalancer dns ttl value",
	    initial = "60",
	    readonly = false,
	    type = ConfigurableFieldType.KEYVALUE,
	    changeListener = ELBDnsTtlChangeListener.class
	    )
	public static String DNS_TTL = "60";
	public static int getLoadbalancerTTL(){
	  return Integer.parseInt(DNS_TTL);
	}
}