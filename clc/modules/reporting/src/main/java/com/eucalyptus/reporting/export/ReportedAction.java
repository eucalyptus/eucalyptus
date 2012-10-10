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
package com.eucalyptus.reporting.export;

import java.util.Date;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;

/**
 * Represents a reported action / event.
 */
public class ReportedAction {

  private String eventUuid;
  private Date created;
  private Date occurred;
  private String accountId;
  private String accountName;
  private String userId;
  private String userName;
  private String uuid;
  private String id;
  private String version;
  private String action;
  private String type;
  private String subType;
  private String instanceUuid;
  private String volumeUuid;
  private Long size;
  private String scope;

  public ReportedAction() {
  }

  ReportedAction( final ReportingEventSupport reportingEventSupport ) {
    setEventUuid( reportingEventSupport.getId() );
    setCreated( reportingEventSupport.getCreationTimestamp() );
    setOccurred( new Date( reportingEventSupport.getTimestampMs() ) );
  }

  public String getEventUuid() {
    return eventUuid;
  }

  public void setEventUuid( final String eventUuid ) {
    this.eventUuid = eventUuid;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated( final Date created ) {
    this.created = created;
  }

  public Date getOccurred() {
    return occurred;
  }

  public void setOccurred( final Date occurred ) {
    this.occurred = occurred;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId( final String accountId ) {
    this.accountId = accountId;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName( final String accountName ) {
    this.accountName = accountName;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId( final String userId ) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName( final String userName ) {
    this.userName = userName;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid( final String uuid ) {
    this.uuid = uuid;
  }

  public String getId() {
    return id;
  }

  public void setId( final String id ) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion( final String version ) {
    this.version = version;
  }

  public String getAction() {
    return action;
  }

  public void setAction( final String action ) {
    this.action = action;
  }

  public String getType() {
    return type;
  }

  public void setType( final String type ) {
    this.type = type;
  }

  public String getSubType() {
    return subType;
  }

  public void setSubType( final String subType ) {
    this.subType = subType;
  }

  public String getInstanceUuid() {
    return instanceUuid;
  }

  public void setInstanceUuid( final String instanceUuid ) {
    this.instanceUuid = instanceUuid;
  }

  public String getVolumeUuid() {
    return volumeUuid;
  }

  public void setVolumeUuid( final String volumeUuid ) {
    this.volumeUuid = volumeUuid;
  }

  public Long getSize() {
    return size;
  }

  public void setSize( final Long size ) {
    this.size = size;
  }

  public String getScope() {
    return scope;
  }

  public void setScope( final String scope ) {
    this.scope = scope;
  }}
