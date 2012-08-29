/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cluster.callback;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.async.BroadcastCallback;

import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsType;
import edu.ucsb.eucalyptus.msgs.SensorsResourceType;

public class DescribeSensorCallback extends BroadcastCallback<DescribeSensorsType, DescribeSensorsResponseType>{

    private static Logger      LOG = Logger.getLogger( DescribeSensorCallback.class );
    private int historySize; 
    private int collectionIntervalTimeMs;
    ArrayList<String> sensorIds = new ArrayList<String>();
    ArrayList<String> instanceIds = new ArrayList<String>();
    
    public DescribeSensorCallback(int historySize, int collectionIntervalTimeMS, ArrayList<String> sensorIds, ArrayList<String> instanceIds ) {
	    this.historySize = historySize;
	    this.collectionIntervalTimeMs = collectionIntervalTimeMS;
	    this.sensorIds = sensorIds;
	    this.instanceIds = instanceIds;
	
	    DescribeSensorsType msg = new DescribeSensorsType(this.historySize, this.collectionIntervalTimeMs, sensorIds, instanceIds);
	    
	    try {
		msg.setUser(Accounts.lookupUserById(Account.SYSTEM_ACCOUNT));
	    } catch (AuthException e) {
		LOG.error("Unable to find the system user", e);
	    }
	    
	    this.setRequest( msg );
	    
	  }

    @Override
    public void initialize( DescribeSensorsType msg ) {
      try {
        msg.setNameServer( edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getNameserverAddress( ) );
        msg.setClusterControllers( Lists.newArrayList( Clusters.getInstance( ).getClusterAddresses( ) ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    
    @Override
    public BroadcastCallback<DescribeSensorsType, DescribeSensorsResponseType> newInstance() {
	return new DescribeSensorCallback(this.historySize, this.collectionIntervalTimeMs, this.sensorIds, this.instanceIds);
    }

    @Override
    public void fire(DescribeSensorsResponseType msg) {
	
	//TODO : Need to fire the correct usage events to the domain model
	
	for ( SensorsResourceType sensorData : msg.getSensorResources( ) ){
	LOG.debug("sensorData.getResourceName() : " + sensorData.getResourceName()); 
	}
	
    }

}
