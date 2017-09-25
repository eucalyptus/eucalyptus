/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.common.msgs;

public class NcDescribeResourceResponseType extends CloudNodeMessage {

  private String nodeStatus;
  private Boolean migrationCapable;
  private String iqn;
  private Integer memorySizeMax;
  private Integer memorySizeAvailable;
  private Integer diskSizeMax;
  private Integer diskSizeAvailable;
  private Integer numberOfCoresMax;
  private Integer numberOfCoresAvailable;
  private String publicSubnets;
  private String hypervisor;

  public String getNodeStatus( ) {
    return nodeStatus;
  }

  public void setNodeStatus( String nodeStatus ) {
    this.nodeStatus = nodeStatus;
  }

  public Boolean getMigrationCapable( ) {
    return migrationCapable;
  }

  public void setMigrationCapable( Boolean migrationCapable ) {
    this.migrationCapable = migrationCapable;
  }

  public String getIqn( ) {
    return iqn;
  }

  public void setIqn( String iqn ) {
    this.iqn = iqn;
  }

  public Integer getMemorySizeMax( ) {
    return memorySizeMax;
  }

  public void setMemorySizeMax( Integer memorySizeMax ) {
    this.memorySizeMax = memorySizeMax;
  }

  public Integer getMemorySizeAvailable( ) {
    return memorySizeAvailable;
  }

  public void setMemorySizeAvailable( Integer memorySizeAvailable ) {
    this.memorySizeAvailable = memorySizeAvailable;
  }

  public Integer getDiskSizeMax( ) {
    return diskSizeMax;
  }

  public void setDiskSizeMax( Integer diskSizeMax ) {
    this.diskSizeMax = diskSizeMax;
  }

  public Integer getDiskSizeAvailable( ) {
    return diskSizeAvailable;
  }

  public void setDiskSizeAvailable( Integer diskSizeAvailable ) {
    this.diskSizeAvailable = diskSizeAvailable;
  }

  public Integer getNumberOfCoresMax( ) {
    return numberOfCoresMax;
  }

  public void setNumberOfCoresMax( Integer numberOfCoresMax ) {
    this.numberOfCoresMax = numberOfCoresMax;
  }

  public Integer getNumberOfCoresAvailable( ) {
    return numberOfCoresAvailable;
  }

  public void setNumberOfCoresAvailable( Integer numberOfCoresAvailable ) {
    this.numberOfCoresAvailable = numberOfCoresAvailable;
  }

  public String getPublicSubnets( ) {
    return publicSubnets;
  }

  public void setPublicSubnets( String publicSubnets ) {
    this.publicSubnets = publicSubnets;
  }

  public String getHypervisor( ) {
    return hypervisor;
  }

  public void setHypervisor( String hypervisor ) {
    this.hypervisor = hypervisor;
  }
}
