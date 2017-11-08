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
package com.eucalyptus.reporting.art.entity;

import java.util.Map;
import java.util.Set;
import com.eucalyptus.reporting.art.ArtObject;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 */
public class ComputeCapacityArtEntity implements ArtObject {
  // Global only
  private Long sizeS3ObjectAvailableGB;
  private Long sizeS3ObjectTotalGB;
  private Integer numPublicIpsAvailable;
  private Integer numPublicIpsTotal;

  // Per-AZ, summarized for global
  private Integer ec2ComputeUnitsAvailable;
  private Integer ec2ComputeUnitsTotal;
  private Integer ec2MemoryUnitsAvailable;
  private Integer ec2MemoryUnitsTotal;
  private Integer ec2DiskUnitsAvailable;
  private Integer ec2DiskUnitsTotal;
  private Long    sizeEbsAvailableGB;
  private Long    sizeEbsTotalGB;
  private Map<String,Integer> ec2VmTypeToAvailable = Maps.newHashMap();
  private Map<String,Integer> ec2VmTypeToTotal = Maps.newHashMap();

  public Long getSizeS3ObjectAvailableGB() {
    return sizeS3ObjectAvailableGB;
  }

  public void setSizeS3ObjectAvailableGB( final Long sizeS3ObjectAvailableGB ) {
    this.sizeS3ObjectAvailableGB = sizeS3ObjectAvailableGB;
  }

  public Long getSizeS3ObjectTotalGB() {
    return sizeS3ObjectTotalGB;
  }

  public void setSizeS3ObjectTotalGB( final Long sizeS3ObjectTotalGB ) {
    this.sizeS3ObjectTotalGB = sizeS3ObjectTotalGB;
  }

  public Integer getNumPublicIpsAvailable() {
    return numPublicIpsAvailable;
  }

  public void setNumPublicIpsAvailable( final Integer numPublicIpsAvailable ) {
    this.numPublicIpsAvailable = numPublicIpsAvailable;
  }

  public Integer getNumPublicIpsTotal() {
    return numPublicIpsTotal;
  }

  public void setNumPublicIpsTotal( final Integer numPublicIpsTotal ) {
    this.numPublicIpsTotal = numPublicIpsTotal;
  }

  public Integer getEc2ComputeUnitsAvailable() {
    return ec2ComputeUnitsAvailable;
  }

  public void setEc2ComputeUnitsAvailable( final Integer ec2ComputeUnitsAvailable ) {
    this.ec2ComputeUnitsAvailable = ec2ComputeUnitsAvailable;
  }

  public Integer getEc2ComputeUnitsTotal() {
    return ec2ComputeUnitsTotal;
  }

  public void setEc2ComputeUnitsTotal( final Integer ec2ComputeUnitsTotal ) {
    this.ec2ComputeUnitsTotal = ec2ComputeUnitsTotal;
  }

  public Integer getEc2MemoryUnitsAvailable() {
    return ec2MemoryUnitsAvailable;
  }

  public void setEc2MemoryUnitsAvailable( final Integer ec2MemoryUnitsAvailable ) {
    this.ec2MemoryUnitsAvailable = ec2MemoryUnitsAvailable;
  }

  public Integer getEc2MemoryUnitsTotal() {
    return ec2MemoryUnitsTotal;
  }

  public void setEc2MemoryUnitsTotal( final Integer ec2MemoryUnitsTotal ) {
    this.ec2MemoryUnitsTotal = ec2MemoryUnitsTotal;
  }

  public Integer getEc2DiskUnitsAvailable() {
    return ec2DiskUnitsAvailable;
  }

  public void setEc2DiskUnitsAvailable( final Integer ec2DiskUnitsAvailable ) {
    this.ec2DiskUnitsAvailable = ec2DiskUnitsAvailable;
  }

  public Integer getEc2DiskUnitsTotal() {
    return ec2DiskUnitsTotal;
  }

  public void setEc2DiskUnitsTotal( final Integer ec2DiskUnitsTotal ) {
    this.ec2DiskUnitsTotal = ec2DiskUnitsTotal;
  }

  public Long getSizeEbsAvailableGB() {
    return sizeEbsAvailableGB;
  }

  public void setSizeEbsAvailableGB( final Long sizeEbsAvailableGB ) {
    this.sizeEbsAvailableGB = sizeEbsAvailableGB;
  }

  public Long getSizeEbsTotalGB() {
    return sizeEbsTotalGB;
  }

  public void setSizeEbsTotalGB( final Long sizeEbsTotalGB ) {
    this.sizeEbsTotalGB = sizeEbsTotalGB;
  }

  public Set<String> getVmTypes() {
    return Sets.newTreeSet( Iterables.concat( this.ec2VmTypeToAvailable.keySet(), this.ec2VmTypeToTotal.keySet() ) );
  }

  public Integer getInstancesAvailableForType( final String vmType ) {
    return this.ec2VmTypeToAvailable.get( vmType );
  }

  public void setInstancesAvailableForType( final String vmType, final Integer available ) {
    this.ec2VmTypeToAvailable.put( vmType, available );
  }

  public Integer getInstancesTotalForType( final String vmType ) {
    return this.ec2VmTypeToTotal.get( vmType );
  }

  public void setInstancesTotalForType( final String vmType, final Integer total ) {
    this.ec2VmTypeToTotal.put( vmType, total );
  }
}
