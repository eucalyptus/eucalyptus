/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.vpc;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import org.hibernate.annotations.Parent;
import com.eucalyptus.vm.VmInstance;

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
                                        final Integer deviceIndex,
                                        final Status status,
                                        final Date attachTime,
                                        final Boolean deleteOnTerminate ) {
    this.attachmentId = attachmentId;
    this.instance = instance;
    this.instanceId = instanceId;
    this.instanceOwnerId = instanceOwnerId;
    this.deviceIndex = deviceIndex;
    this.status = status;
    this.attachTime = attachTime;
    this.deleteOnTerminate = deleteOnTerminate;
  }

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
        deviceIndex,
        status,
        attachTime,
        deleteOnTerminate
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

  @Column( name = "metadata_att_device_index" )
  private Integer deviceIndex;

  @Column( name = "metadata_att_status" )
  private Status status;

  @Column( name = "metadata_att_time" )
  private Date attachTime;

  @Column( name = "metadata_att_delete_on_term" )
  private Boolean deleteOnTerminate;

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

  @PostLoad
  protected void postLoad( ) {
    this.instance = networkInterface.getInstance( );
  }
}
