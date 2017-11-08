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
package com.eucalyptus.reporting.art.generator;

import static com.eucalyptus.reporting.domain.ReportingComputeDomainModel.ReportingComputeZoneDomainModel;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.ComputeCapacityArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.domain.ReportingComputeDomainModel;
import com.google.common.base.Objects;

/**
 *
 */
public class ComputeCapacityArtGenerator implements ArtGenerator {

  @Override
  public ReportArtEntity generateReportArt( final ReportArtEntity report ) {
    final ReportingComputeDomainModel model = ReportingComputeDomainModel.getGlobalComputeDomainModel();

    final ComputeCapacityArtEntity globalCapacity = report.getUsageTotals().getComputeCapacityArtEntity();
    globalCapacity.setSizeS3ObjectAvailableGB( nullSafe(model.getSizeS3ObjectAvailableGB()) );
    globalCapacity.setSizeS3ObjectTotalGB( nullSafe(model.getSizeS3ObjectTotalGB()) );
    globalCapacity.setNumPublicIpsAvailable( nullSafe(model.getNumPublicIpsAvailable()) );
    globalCapacity.setNumPublicIpsTotal( nullSafe(model.getNumPublicIpsTotal()) );
    globalCapacity.setEc2ComputeUnitsAvailable( 0 );
    globalCapacity.setEc2ComputeUnitsTotal( 0 );
    globalCapacity.setEc2DiskUnitsAvailable( 0 );
    globalCapacity.setEc2DiskUnitsTotal( 0 );
    globalCapacity.setEc2MemoryUnitsAvailable( 0 );
    globalCapacity.setEc2MemoryUnitsTotal( 0 );
    globalCapacity.setSizeEbsAvailableGB( 0L );
    globalCapacity.setSizeEbsTotalGB( 0L );

    for ( final String zone : ReportingComputeDomainModel.getZones() ) {
      AvailabilityZoneArtEntity zoneEntity = report.getZones().get( zone );
      if ( zoneEntity == null ) {
        zoneEntity = new AvailabilityZoneArtEntity();
        report.getZones().put( zone, zoneEntity );
      }
      final ComputeCapacityArtEntity zoneCapacity = zoneEntity.getUsageTotals().getComputeCapacityArtEntity();

      final ReportingComputeZoneDomainModel zoneModel = ReportingComputeDomainModel.getZoneComputeDomainModel( zone );
      zoneCapacity.setEc2ComputeUnitsAvailable( nullSafe(zoneModel.getEc2ComputeUnitsAvailable()) );
      zoneCapacity.setEc2ComputeUnitsTotal( nullSafe(zoneModel.getEc2ComputeUnitsTotal()) );
      zoneCapacity.setEc2DiskUnitsAvailable( nullSafe(zoneModel.getEc2DiskUnitsAvailable()) );
      zoneCapacity.setEc2DiskUnitsTotal( nullSafe(zoneModel.getEc2DiskUnitsTotal()) );
      zoneCapacity.setEc2MemoryUnitsAvailable( nullSafe(zoneModel.getEc2MemoryUnitsAvailable()) );
      zoneCapacity.setEc2MemoryUnitsTotal( nullSafe(zoneModel.getEc2MemoryUnitsTotal()) );
      zoneCapacity.setSizeEbsAvailableGB( nullSafe(zoneModel.getSizeEbsAvailableGB()) );
      zoneCapacity.setSizeEbsTotalGB( nullSafe(zoneModel.getSizeEbsTotalGB()) );

      for ( final String vmType : zoneModel.getVmTypes() ) {
        zoneCapacity.setInstancesAvailableForType( vmType, nullSafe(zoneModel.getInstancesAvailableForType(vmType)) );
        zoneCapacity.setInstancesTotalForType( vmType, nullSafe(zoneModel.getInstancesTotalForType(vmType)) );
      }

      globalCapacity.setEc2ComputeUnitsAvailable( globalCapacity.getEc2ComputeUnitsAvailable() + zoneCapacity.getEc2ComputeUnitsAvailable() );
      globalCapacity.setEc2ComputeUnitsTotal( globalCapacity.getEc2ComputeUnitsTotal() + zoneCapacity.getEc2ComputeUnitsTotal() );
      globalCapacity.setEc2DiskUnitsAvailable( globalCapacity.getEc2DiskUnitsAvailable() + zoneCapacity.getEc2DiskUnitsAvailable() );
      globalCapacity.setEc2DiskUnitsTotal( globalCapacity.getEc2DiskUnitsTotal() + zoneCapacity.getEc2DiskUnitsTotal() );
      globalCapacity.setEc2MemoryUnitsAvailable( globalCapacity.getEc2MemoryUnitsAvailable() + zoneCapacity.getEc2MemoryUnitsAvailable() );
      globalCapacity.setEc2MemoryUnitsTotal( globalCapacity.getEc2MemoryUnitsTotal() + zoneCapacity.getEc2MemoryUnitsTotal() );
      globalCapacity.setSizeEbsAvailableGB( globalCapacity.getSizeEbsAvailableGB() + zoneCapacity.getSizeEbsAvailableGB() );
      globalCapacity.setSizeEbsTotalGB( globalCapacity.getSizeEbsTotalGB() + zoneCapacity.getSizeEbsTotalGB() );
      for ( final String vmType : zoneModel.getVmTypes() ) {
        globalCapacity.setInstancesAvailableForType( vmType, nullSafe(globalCapacity.getInstancesAvailableForType(vmType)) + zoneCapacity.getInstancesAvailableForType(vmType) );
        globalCapacity.setInstancesTotalForType( vmType, nullSafe(globalCapacity.getInstancesTotalForType(vmType)) + zoneCapacity.getInstancesTotalForType(vmType) );
      }
    }

    return report;
  }

  private Integer nullSafe( final Integer value ) {
    return Objects.firstNonNull( value, 0 );
  }

  private Long nullSafe( final Long value ) {
    return Objects.firstNonNull( value, 0L );
  }
}
