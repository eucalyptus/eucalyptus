/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
