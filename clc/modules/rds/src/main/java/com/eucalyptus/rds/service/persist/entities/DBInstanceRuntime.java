/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.hibernate.annotations.Parent;
import com.eucalyptus.rds.service.persist.views.DBInstanceRuntimeView;

/**
 *
 */
@Embeddable
public class DBInstanceRuntime implements DBInstanceRuntimeView, Serializable {
  private static final long serialVersionUID = 1L;

  @Parent
  private DBInstance parent;

  @Column( name = "rds_db_run_sys_subnet_id" )
  private String systemSubnetId;

  @Column( name = "rds_db_run_sys_vpc_id" )
  private String systemVpcId;

  @Column( name = "rds_db_run_sys_volume_id" )
  private String systemVolumeId;

  @Column( name = "rds_db_run_sys_stack_id" )
  private String stackId;

  @Column( name = "rds_db_run_sys_instance_id" )
  private String systemInstanceId;

  @Column( name = "rds_db_run_user_subnet_id" )
  private String userSubnetId;

  @Column( name = "rds_db_run_user_eni_id" )
  private String userNetworkInterfaceId;

  @Column( name = "rds_db_run_public_ip" )
  private String publicIp;

  @Column( name = "rds_db_run_public_ip_alloc_id" )
  private String publicIpAllocationId;

  @Column( name = "rds_db_run_private_ip" )
  private String privateIp;

  protected DBInstanceRuntime() {
  }

  public DBInstanceRuntime(final DBInstance parent) {
    this.parent = parent;
  }

  public DBInstance getParent() {
    return parent;
  }

  public void setParent(final DBInstance parent) {
    this.parent = parent;
  }

  public String getSystemSubnetId() {
    return systemSubnetId;
  }

  public void setSystemSubnetId(final String systemSubnetId) {
    this.systemSubnetId = systemSubnetId;
  }

  public String getSystemVpcId() {
    return systemVpcId;
  }

  public void setSystemVpcId(final String systemVpcId) {
    this.systemVpcId = systemVpcId;
  }

  public String getSystemVolumeId() {
    return systemVolumeId;
  }

  public void setSystemVolumeId(final String systemVolumeId) {
    this.systemVolumeId = systemVolumeId;
  }

  public String getSystemInstanceId() {
    return systemInstanceId;
  }

  public void setSystemInstanceId(final String systemInstanceId) {
    this.systemInstanceId = systemInstanceId;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(final String stackId) {
    this.stackId = stackId;
  }

  public String getUserSubnetId() {
    return userSubnetId;
  }

  public void setUserSubnetId(final String userSubnetId) {
    this.userSubnetId = userSubnetId;
  }

  public String getUserNetworkInterfaceId() {
    return userNetworkInterfaceId;
  }

  public void setUserNetworkInterfaceId(final String userNetworkInterfaceId) {
    this.userNetworkInterfaceId = userNetworkInterfaceId;
  }

  public String getPublicIp() {
    return publicIp;
  }

  public void setPublicIp(final String publicIp) {
    this.publicIp = publicIp;
  }

  public String getPublicIpAllocationId() {
    return publicIpAllocationId;
  }

  public void setPublicIpAllocationId(final String publicIpAllocationId) {
    this.publicIpAllocationId = publicIpAllocationId;
  }

  public String getPrivateIp() {
    return privateIp;
  }

  public void setPrivateIp(final String privateIp) {
    this.privateIp = privateIp;
  }
}
