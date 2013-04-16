/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.art.generator

import org.junit.Test
import static org.junit.Assert.*
import com.eucalyptus.reporting.domain.ReportingComputeDomainModel
import com.eucalyptus.reporting.art.entity.ReportArtEntity
import java.text.SimpleDateFormat
import com.google.common.collect.Sets
import com.eucalyptus.reporting.art.entity.ComputeCapacityArtEntity

/**
 * 
 */
class ComputeCapacityArtGeneratorTest {

  @Test
  void testGeneration() {
    ReportingComputeDomainModel global = ReportingComputeDomainModel.getGlobalComputeDomainModel();
    global.setNumPublicIpsAvailable( 123 )
    global.setNumPublicIpsTotal( 150 )
    global.setSizeS3ObjectAvailableGB( 71 )
    global.setSizeS3ObjectTotalGB( 100 )

    ReportingComputeDomainModel.ReportingComputeZoneDomainModel zone1Model =
      ReportingComputeDomainModel.getZoneComputeDomainModel( "zone1" );
    zone1Model.setSizeEbsAvailableGB( 50 )
    zone1Model.setSizeEbsTotalGB( 75 )
    zone1Model.setEc2ComputeUnitsAvailable( 20 )
    zone1Model.setEc2ComputeUnitsTotal( 25 )
    zone1Model.setEc2MemoryUnitsAvailable( 10 )
    zone1Model.setEc2MemoryUnitsTotal( 15 )
    zone1Model.setEc2DiskUnitsAvailable( 2000 )
    zone1Model.setEc2DiskUnitsTotal( 5000 )
    zone1Model.setInstancesAvailableForType( "small", 5 )
    zone1Model.setInstancesTotalForType( "small", 10 )
    zone1Model.setInstancesAvailableForType( "big", 2 )
    zone1Model.setInstancesTotalForType( "big", 3 )

    ReportingComputeDomainModel.ReportingComputeZoneDomainModel zone2Model =
      ReportingComputeDomainModel.getZoneComputeDomainModel( "zone2" );
    zone2Model.setSizeEbsAvailableGB( 100 )
    zone2Model.setSizeEbsTotalGB( 150 )
    zone2Model.setEc2ComputeUnitsAvailable( 40 )
    zone2Model.setEc2ComputeUnitsTotal( 50 )
    zone2Model.setEc2MemoryUnitsAvailable( 20 )
    zone2Model.setEc2MemoryUnitsTotal( 30 )
    zone2Model.setEc2DiskUnitsAvailable( 4000 )
    zone2Model.setEc2DiskUnitsTotal( 10000 )
    zone2Model.setInstancesAvailableForType( "small", 2 )
    zone2Model.setInstancesTotalForType( "small", 8 )
    zone2Model.setInstancesAvailableForType( "big", 1 )
    zone2Model.setInstancesTotalForType( "big", 2 )

    ComputeCapacityArtGenerator generator = new ComputeCapacityArtGenerator();
    ReportArtEntity art = generator.generateReportArt( new ReportArtEntity( millis("2012-09-01T00:00:00"), millis("2012-09-01T00:00:00") ) )

    ComputeCapacityArtEntity cloudCapacity = art.getUsageTotals().getComputeCapacityArtEntity();
    assertEquals( "Cloud ips available", 123, cloudCapacity.getNumPublicIpsAvailable() )
    assertEquals( "Cloud ips total", 150, cloudCapacity.getNumPublicIpsTotal() )
    assertEquals( "Cloud s3 available", 71, cloudCapacity.getSizeS3ObjectAvailableGB() )
    assertEquals( "Cloud s3 total", 100, cloudCapacity.getSizeS3ObjectTotalGB() )
    assertEquals( "Cloud ebs available", 150, cloudCapacity.getSizeEbsAvailableGB() )
    assertEquals( "Cloud ebs total", 225, cloudCapacity.getSizeEbsTotalGB() )
    assertEquals( "Cloud compute available", 60, cloudCapacity.getEc2ComputeUnitsAvailable() )
    assertEquals( "Cloud compute total", 75, cloudCapacity.getEc2ComputeUnitsTotal() )
    assertEquals( "Cloud memory available", 30, cloudCapacity.getEc2MemoryUnitsAvailable() )
    assertEquals( "Cloud memory total", 45, cloudCapacity.getEc2MemoryUnitsTotal() )
    assertEquals( "Cloud disk available", 6000, cloudCapacity.getEc2DiskUnitsAvailable() )
    assertEquals( "Cloud disk total", 15000, cloudCapacity.getEc2DiskUnitsTotal() )
    assertEquals( "Cloud small vmtype available", 7, cloudCapacity.getInstancesAvailableForType("small") )
    assertEquals( "Cloud small vmtype total", 18, cloudCapacity.getInstancesTotalForType("small") )
    assertEquals( "Cloud big vmtype available", 3, cloudCapacity.getInstancesAvailableForType("big") )
    assertEquals( "Cloud big vmtype total", 5, cloudCapacity.getInstancesTotalForType("big") )

    assertEquals( "availability zones", Sets.newHashSet( "zone1", "zone2" ), art.getZones().keySet() )
    ComputeCapacityArtEntity zone1ComputeCapacity = art.getZones().get( "zone1" ).getUsageTotals().getComputeCapacityArtEntity();
    assertEquals( "Zone1 ebs available", 50, zone1ComputeCapacity.getSizeEbsAvailableGB() )
    assertEquals( "Zone1 ebs total", 75, zone1ComputeCapacity.getSizeEbsTotalGB() )
    assertEquals( "Zone1 compute available", 20, zone1ComputeCapacity.getEc2ComputeUnitsAvailable() )
    assertEquals( "Zone1 compute total", 25, zone1ComputeCapacity.getEc2ComputeUnitsTotal() )
    assertEquals( "Zone1 memory available", 10, zone1ComputeCapacity.getEc2MemoryUnitsAvailable() )
    assertEquals( "Zone1 memory total", 15, zone1ComputeCapacity.getEc2MemoryUnitsTotal() )
    assertEquals( "Zone1 disk available", 2000, zone1ComputeCapacity.getEc2DiskUnitsAvailable() )
    assertEquals( "Zone1 disk total", 5000, zone1ComputeCapacity.getEc2DiskUnitsTotal() )
    assertEquals( "Zone1 small vmtype available", 5, zone1ComputeCapacity.getInstancesAvailableForType("small") )
    assertEquals( "Zone1 small vmtype total", 10, zone1ComputeCapacity.getInstancesTotalForType("small") )
    assertEquals( "Zone1 big vmtype available", 2, zone1ComputeCapacity.getInstancesAvailableForType("big") )
    assertEquals( "Zone1 big vmtype total", 3, zone1ComputeCapacity.getInstancesTotalForType("big") )

    ComputeCapacityArtEntity zone2ComputeCapacity = art.getZones().get( "zone2" ).getUsageTotals().getComputeCapacityArtEntity();
    assertEquals( "Zone2 ebs available", 100, zone2ComputeCapacity.getSizeEbsAvailableGB() )
    assertEquals( "Zone2 ebs total", 150, zone2ComputeCapacity.getSizeEbsTotalGB() )
    assertEquals( "Zone2 compute available", 40, zone2ComputeCapacity.getEc2ComputeUnitsAvailable() )
    assertEquals( "Zone2 compute total", 50, zone2ComputeCapacity.getEc2ComputeUnitsTotal() )
    assertEquals( "Zone2 memory available", 20, zone2ComputeCapacity.getEc2MemoryUnitsAvailable() )
    assertEquals( "Zone2 memory total", 30, zone2ComputeCapacity.getEc2MemoryUnitsTotal() )
    assertEquals( "Zone2 disk available", 4000, zone2ComputeCapacity.getEc2DiskUnitsAvailable() )
    assertEquals( "Zone2 disk total", 10000, zone2ComputeCapacity.getEc2DiskUnitsTotal() )
    assertEquals( "Zone2 small vmtype available", 2, zone2ComputeCapacity.getInstancesAvailableForType("small") )
    assertEquals( "Zone2 small vmtype total", 8, zone2ComputeCapacity.getInstancesTotalForType("small") )
    assertEquals( "Zone2 big vmtype available", 1, zone2ComputeCapacity.getInstancesAvailableForType("big") )
    assertEquals( "Zone2 big vmtype total", 2, zone2ComputeCapacity.getInstancesTotalForType("big") )

    //new ComputeCapacityRenderer( new CsvDocument() ).render( art, System.out, Units.getDefaultDisplayUnits() )
  }

  private long millis( String timestamp ) {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    sdf.parse( timestamp ).getTime()
  }

}
