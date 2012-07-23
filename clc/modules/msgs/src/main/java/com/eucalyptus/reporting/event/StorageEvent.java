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

@SuppressWarnings("serial")
public class StorageEvent
	implements Event
{
	public enum EventType { EbsVolume, EbsSnapshot };
	
	private final EventType eventType;
	private final boolean   createOrDelete;
	private final long      sizeMegs;
	private final String    ownerId;
	private final String    ownerName;
	private final String    accountId;
	private final String    accountName;
	private final String    clusterName;
	private final String    availabilityZone;
	
    /**
     * Constructor for StorageEvent. 
     *
     * NOTE: We must include separate userId, username, accountId, and
     *  accountName with each event sent, even though the names can be looked
     *  up using ID's. We must include this redundant information, for
     *  several reasons. First, the reporting subsystem may run on a totally
     *  separate machine outside of eucalyptus (data warehouse configuration)
     *  so it may not have access to the regular eucalyptus database to lookup
     *  usernames or account names. Second, the reporting subsystem stores
     *  <b>historical</b> information, and its possible that usernames and
     *  account names can change, or their users or accounts can be deleted.
     *  Thus we need the user name or account name at the time an event was
     *  sent.
     */
	public StorageEvent(EventType eventType, boolean createOrDelete,
			long sizeMegs, String ownerId, String ownerName, String accountId,
			String accountName, String clusterName,	String availabilityZone)
	{
		if (eventType==null) throw new IllegalArgumentException("eventType cant be null");
		if (ownerId==null) throw new IllegalArgumentException("ownerId cant be null");
		if (ownerName==null) throw new IllegalArgumentException("ownerName cant be null");
		if (accountId==null) throw new IllegalArgumentException("accountId cant be null");
		if (accountName==null) throw new IllegalArgumentException("accountName cant be null");
		if (clusterName==null) throw new IllegalArgumentException("clusterName cant be null");
		if (availabilityZone==null) throw new IllegalArgumentException("availabilityZone cant be null");
		if (sizeMegs < 0) throw new IllegalArgumentException("Storage size cannot be negative");

		this.eventType        = eventType;
		this.createOrDelete   = createOrDelete;
		this.sizeMegs         = sizeMegs;
		this.ownerId          = ownerId;
		this.ownerName        = ownerName;
		this.accountId        = accountId;
		this.accountName      = accountName;
		this.clusterName      = clusterName;
		this.availabilityZone = availabilityZone;
	}

	public EventType getEventType()
	{
		return eventType;
	}

	public boolean isCreateOrDelete()
	{
		return createOrDelete;
	}

	public long getSizeMegs()
	{
		return sizeMegs;
	}

	public String getOwnerId()
	{
		return ownerId;
	}
	
	public String getOwnerName()
	{
		return ownerName;
	}

	public String getAccountId()
	{
		return accountId;
	}
	
	public String getAccountName()
	{
		return accountName;
	}

	public String getClusterName()
	{
		return clusterName;
	}

	public String getAvailabilityZone()
	{
		return availabilityZone;
	}

	@Override
	public boolean requiresReliableTransmission()
	{
		return true;
	}
	
    public String toString()
    {
        return String.format("[type:%s,isCreate:%b,sizeMegs:%d,ownerId:%s,ownerName:%s,accountId:%s,accountName:%s,clusterName:%s,zone:%s]",
                    eventType, createOrDelete, sizeMegs, ownerId, ownerName, accountId, accountName, clusterName, availabilityZone);
    }

}
