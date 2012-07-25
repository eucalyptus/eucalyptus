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

/**
 *
 */
abstract class InstanceEventSupport implements Event
{
  private final String uuid;
  private final String instanceId;
  private final String instanceType;
  private final String userId;
  private final String userName;
  private final String accountId;
  private final String accountName;
  private final String clusterName;
  private final String availabilityZone;

  /**
   * Constructor for InstanceEventSupport.
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
  InstanceEventSupport( final String uuid,
                        final String instanceId,
                        final String instanceType,
                        final String userId,
                        final String userName,
                        final String accountId,
                        final String accountName,
                        final String clusterName,
                        final String availabilityZone ) {
    if (uuid==null) throw new IllegalArgumentException("uuid cant be null");
    if (instanceId==null) throw new IllegalArgumentException("instanceId cant be null");
    if (instanceType==null) throw new IllegalArgumentException("instanceType cant be null");
    if (userId==null) throw new IllegalArgumentException("userId cant be null");
    if (userName==null) throw new IllegalArgumentException("userName cant be null");
    if (accountId==null) throw new IllegalArgumentException("accountId cant be null");
    if (accountName==null) throw new IllegalArgumentException("accountName cant be null");
    if (clusterName==null) throw new IllegalArgumentException("clusterName cant be null");
    if (availabilityZone==null) throw new IllegalArgumentException("availabilityZone cant be null");

    this.uuid = uuid;
    this.instanceId = instanceId;
    this.instanceType = instanceType;
    this.userId = userId;
    this.userName = userName;
    this.accountId = accountId;
    this.accountName = accountName;
    this.clusterName = clusterName;
    this.availabilityZone = availabilityZone;
  }

  InstanceEventSupport( final InstanceEventSupport e ) {
    this(
        e.getUuid(),
        e.getInstanceId(),
        e.getInstanceType(),
        e.getUserId(),
        e.getUserName(),
        e.getAccountId(),
        e.getAccountName(),
        e.getClusterName(),
        e.getAvailabilityZone()
    );
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

  public String getClusterName() {
    return clusterName;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public boolean requiresReliableTransmission() {
    return false;
  }

  public String toString() {
    return String.format( "[uuid:%s,instanceId:%s,instanceType:%s,userId:%s"
        + ",accountId:%s,cluster:%s,zone:%s]",
        getUuid(), getInstanceId(), getInstanceType(), getUserId(), getAccountId(),
        getClusterName(), getAvailabilityZone() );
  }

}
