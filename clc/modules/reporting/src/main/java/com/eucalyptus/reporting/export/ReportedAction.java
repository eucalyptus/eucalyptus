/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
