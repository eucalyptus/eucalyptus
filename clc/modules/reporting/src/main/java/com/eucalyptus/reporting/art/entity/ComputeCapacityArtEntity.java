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
package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;

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
}
