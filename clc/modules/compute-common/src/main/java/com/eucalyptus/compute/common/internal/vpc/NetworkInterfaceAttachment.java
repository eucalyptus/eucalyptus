/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.vpc;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import org.hibernate.annotations.Parent;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.compute.common.internal.vm.VmInstance;

/**
 *
 */
@Embeddable
public class NetworkInterfaceAttachment implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum Status {
    attaching,
    attached,
    detaching,
    detached
  }

  protected NetworkInterfaceAttachment( ) {
  }

  protected NetworkInterfaceAttachment( final String attachmentId,
                                        final VmInstance instance,
                                        final String instanceId,
                                        final String instanceOwnerId,
                                        final String natGatewayId,
                                        final Integer deviceIndex,
                                        final Status status,
                                        final Date attachTime,
                                        final Boolean deleteOnTerminate ) {
    this.attachmentId = attachmentId;
    this.instance = instance;
    this.instanceId = instanceId;
    this.instanceOwnerId = instanceOwnerId;
    this.natGatewayId = natGatewayId;
    this.deviceIndex = deviceIndex;
    this.status = status;
    this.attachTime = attachTime;
    this.deleteOnTerminate = deleteOnTerminate;
  }

  /**
   * Create an attachment for an instance
   */
  public static NetworkInterfaceAttachment create( final String attachmentId,
                                                   final VmInstance instance,
                                                   final String instanceId,
                                                   final String instanceOwnerId,
                                                   final Integer deviceIndex,
                                                   final Status status,
                                                   final Date attachTime,
                                                   final Boolean deleteOnTerminate ) {
    return new NetworkInterfaceAttachment(
        attachmentId,
        instance,
        instanceId,
        instanceOwnerId,
        null,
        deviceIndex,
        status,
        attachTime,
        deleteOnTerminate
    );
  }

  /**
   * Create an attachment for a NAT gateway
   */
  public static NetworkInterfaceAttachment create( final String attachmentId,
                                                   final String natGatewayId,
                                                   final Status status ) {
    return new NetworkInterfaceAttachment(
        attachmentId,
        null,
        null,
        AccountIdentifiers.SYSTEM_ACCOUNT,
        natGatewayId,
        1,
        status,
        null,
        false
    );
  }

  @Parent
  private NetworkInterface networkInterface;

  // Persisted by field in parent NetworkInterface
  @Transient
  private VmInstance instance;

  @Column( name = "metadata_attachment_id" )
  private String attachmentId;

  @Column( name = "metadata_att_instance_id" )
  private String instanceId;

  @Column( name = "metadata_att_instance_owner_id" )
  private String instanceOwnerId;

  @Column( name = "metadata_att_nat_gateway_id" )
  private String natGatewayId;

  @Column( name = "metadata_att_device_index" )
  private Integer deviceIndex;

  @Column( name = "metadata_att_status" )
  private Status status;

  @Column( name = "metadata_att_last_status" )
  private Status lastStatus;

  @Column( name = "metadata_att_time" )
  private Date attachTime;

  @Column( name = "metadata_att_delete_on_term" )
  private Boolean deleteOnTerminate;

  public void transitionStatus( final Status status ) {
    if ( status != getStatus( ) ) {
      setLastStatus( getStatus( ) );
      setStatus( status );
    }
  }

  protected NetworkInterface getNetworkInterface() {
    return networkInterface;
  }

  protected void setNetworkInterface( final NetworkInterface networkInterface ) {
    this.networkInterface = networkInterface;
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public void setAttachmentId( final String attachmentId ) {
    this.attachmentId = attachmentId;
  }

  public VmInstance getInstance() {
    return instance;
  }

  public void setInstance( final VmInstance instance ) {
    this.instance = instance;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getInstanceOwnerId() {
    return instanceOwnerId;
  }

  public void setInstanceOwnerId( final String instanceOwnerId ) {
    this.instanceOwnerId = instanceOwnerId;
  }

  public String getNatGatewayId( ) {
    return natGatewayId;
  }

  public void setNatGatewayId( final String natGatewayId ) {
    this.natGatewayId = natGatewayId;
  }

  public Integer getDeviceIndex() {
    return deviceIndex;
  }

  public void setDeviceIndex( final Integer deviceIndex ) {
    this.deviceIndex = deviceIndex;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus( final Status status ) {
    this.status = status;
  }

  public Status getLastStatus( ) {
    return lastStatus;
  }

  public void setLastStatus( final Status lastStatus ) {
    this.lastStatus = lastStatus;
  }

  public Date getAttachTime() {
    return attachTime;
  }

  public void setAttachTime( final Date attachTime ) {
    this.attachTime = attachTime;
  }

  public Boolean getDeleteOnTerminate() {
    return deleteOnTerminate;
  }

  public void setDeleteOnTerminate( final Boolean deleteOnTerminate ) {
    this.deleteOnTerminate = deleteOnTerminate;
  }
}
