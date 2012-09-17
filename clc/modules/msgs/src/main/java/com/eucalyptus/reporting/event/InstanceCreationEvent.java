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

package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import com.eucalyptus.event.Event;

public class InstanceCreationEvent implements Event{
    
    private static final long serialVersionUID = 1L;
    
    private String uuid;
    private String instanceId;
    private String instanceType;
    private String userId;
    private String userName;
    private String accountId;
    private String accountName;
    private String availabilityZone;
    
    public InstanceCreationEvent(String uuid, String instanceId,
	    String instanceType, String userId, String userName, String accountId,
	    String accountName, String availabilityZone) {
	
	assertThat(uuid, notNullValue());
	assertThat(instanceId, notNullValue());
	assertThat(instanceType, notNullValue());
	assertThat(userId, notNullValue());
	assertThat(userName, notNullValue());
	assertThat(accountId, notNullValue());
	assertThat(accountName, notNullValue());

	this.uuid = uuid;
	this.instanceId = instanceId;
	this.instanceType = instanceType;
	this.userId = userId;
	this.userName = userName;
	this.accountId = accountId;
	this.accountName = accountName;
	this.availabilityZone = availabilityZone;

    }

    public String getUuid() {
        return uuid;
    }
    public String getInstanceId() {
        return instanceId;
    }
    public String getInstanceType() {
        return instanceType;
    }
    public String getUserId() {
        return userId;
    }
    public String getUserName() {
        return userName;
    }
    public String getAccountId() {
        return accountId;
    }
    public String getAccountName() {
        return accountName;
    }
 
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    @Override
    public String toString() {
	return "InstanceCreationEvent [uuid=" + uuid + ", instanceId="
		+ instanceId + ", instanceType=" + instanceType + ", userId="
		+ userId + ", userName=" + userName + ", accountId="
		+ accountId + ", accountName=" + accountName
		+ ", availabilityZone=" + availabilityZone + "]";
    }

}
