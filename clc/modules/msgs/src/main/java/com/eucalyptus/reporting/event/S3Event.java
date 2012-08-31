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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.reporting.event;

import com.eucalyptus.event.Event;

public class S3Event
	implements Event
{
	private final boolean   objectOrBucket;
	private final boolean   createOrDelete;
	private final long      sizeMegs;
	private final String    ownerId;
	private final String    ownerName;
	private final String    accountId;
	private final String    accountName;

	/**
	 * <p>Constructor indicating S3 Object event.
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
	 *  
	 * @param createOrDelete true if the object is being created, false deleted.
	 * @param sizeMegs the size of the object being created or deleted.
     */
	public S3Event(boolean createOrDelete, long sizeMegs, String ownerId,
			String ownerName, String accountId, String accountName)
	{
		super();
		if (sizeMegs < 0) throw new IllegalArgumentException("sizeMegs for s3 objects cannot be negative");
		if (ownerId==null) throw new IllegalArgumentException("ownerId cant be null");
		if (accountId==null) throw new IllegalArgumentException("ownerId cant be null");

		this.objectOrBucket = true;
		this.createOrDelete = createOrDelete;
		this.sizeMegs = sizeMegs;
		this.ownerId = ownerId;
		this.ownerName = ownerName;
		this.accountId = accountId;
		this.accountName = accountName;
	}
	
	/**
	 * <p>Constructor indicating S3 bucket event.
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
	 *  
	 * @param createOrDelete true if the bucket is being created, false deleted.
     */
	public S3Event(boolean createOrDelete, String ownerId,	String ownerName,
			String accountId, String accountName)
	{
		super();
		if (ownerId==null) throw new IllegalArgumentException("ownerId cant be null");
		if (accountId==null) throw new IllegalArgumentException("ownerId cant be null");

		this.objectOrBucket = false;
		this.createOrDelete = createOrDelete;
		this.sizeMegs = 0l;
		this.ownerId = ownerId;
		this.ownerName = ownerName;
		this.accountId = accountId;
		this.accountName = accountName;
	}

	
	public boolean isObjectOrBucket()
	{
		return objectOrBucket;
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

	public String toString()
	{
		return String.format("[isObj:%b,isCreate:%b,sizeMegs:%d,ownerId:%s,ownerName:%s,accountId:%s,accountName:%s]",
					objectOrBucket, createOrDelete, sizeMegs, ownerId, ownerName, accountId, accountName);
	}
}

