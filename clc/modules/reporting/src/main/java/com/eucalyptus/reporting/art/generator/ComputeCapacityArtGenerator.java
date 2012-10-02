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
