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

/**
 *
 */
public class ComputeCapacityArtGenerator implements ArtGenerator {

  @Override
  public ReportArtEntity generateReportArt( final ReportArtEntity report ) {
    final ReportingComputeDomainModel model = ReportingComputeDomainModel.getGlobalComputeDomainModel();

    final ComputeCapacityArtEntity globalCapacity = report.getUsageTotals().getComputeCapacityArtEntity();
    globalCapacity.setSizeS3ObjectAvailableGB( model.getSizeS3ObjectAvailableGB() );
    globalCapacity.setSizeS3ObjectTotalGB( model.getSizeS3ObjectTotalGB() );
    globalCapacity.setNumPublicIpsAvailable( model.getNumPublicIpsAvailable() );
    globalCapacity.setNumPublicIpsTotal( model.getNumPublicIpsTotal() );
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
      zoneCapacity.setEc2ComputeUnitsAvailable( zoneModel.getEc2ComputeUnitsAvailable() );
      zoneCapacity.setEc2ComputeUnitsTotal( zoneModel.getEc2ComputeUnitsTotal() );
      zoneCapacity.setEc2DiskUnitsAvailable( zoneModel.getEc2DiskUnitsAvailable() );
      zoneCapacity.setEc2DiskUnitsTotal( zoneModel.getEc2DiskUnitsTotal() );
      zoneCapacity.setEc2MemoryUnitsAvailable( zoneModel.getEc2MemoryUnitsAvailable() );
      zoneCapacity.setEc2MemoryUnitsTotal( zoneModel.getEc2MemoryUnitsTotal() );
      zoneCapacity.setSizeEbsAvailableGB( zoneModel.getSizeEbsAvailableGB() );
      zoneCapacity.setSizeEbsTotalGB( zoneModel.getSizeEbsTotalGB() );

      globalCapacity.setEc2ComputeUnitsAvailable( globalCapacity.getEc2ComputeUnitsAvailable() + zoneCapacity.getEc2ComputeUnitsAvailable() );
      globalCapacity.setEc2ComputeUnitsTotal( globalCapacity.getEc2ComputeUnitsTotal() + zoneCapacity.getEc2ComputeUnitsTotal() );
      globalCapacity.setEc2DiskUnitsAvailable( globalCapacity.getEc2DiskUnitsAvailable() + zoneCapacity.getEc2DiskUnitsAvailable() );
      globalCapacity.setEc2DiskUnitsTotal( globalCapacity.getEc2DiskUnitsTotal() + zoneCapacity.getEc2DiskUnitsTotal() );
      globalCapacity.setEc2MemoryUnitsAvailable( globalCapacity.getEc2MemoryUnitsAvailable() + zoneCapacity.getEc2MemoryUnitsAvailable() );
      globalCapacity.setEc2MemoryUnitsTotal( globalCapacity.getEc2MemoryUnitsTotal() + zoneCapacity.getEc2MemoryUnitsTotal() );
      globalCapacity.setSizeEbsAvailableGB( globalCapacity.getSizeEbsAvailableGB() + zoneCapacity.getSizeEbsAvailableGB() );
      globalCapacity.setSizeEbsTotalGB( globalCapacity.getSizeEbsTotalGB() + zoneCapacity.getSizeEbsTotalGB() );
    }

    return report;
  }
}
