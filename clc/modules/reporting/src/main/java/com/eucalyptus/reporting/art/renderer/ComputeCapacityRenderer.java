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
package com.eucalyptus.reporting.art.renderer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.ComputeCapacityArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.renderer.document.Document;
import com.eucalyptus.reporting.units.Units;

/**
 *
 */
public class ComputeCapacityRenderer implements Renderer {
  private final Document doc;

  public ComputeCapacityRenderer( final Document doc ) {
    this.doc = doc;
  }

  @Override
  public void render( final ReportArtEntity report, final OutputStream os, final Units units ) throws IOException {
    doc.setWriter(new OutputStreamWriter(os));

    doc.open();
    doc.textLine("Capacity Report", 1);
    doc.textLine("Begin:" + new Date(report.getBeginMs()).toString(), 4);
    doc.textLine("End:" + new Date(report.getEndMs()).toString(), 4);
    doc.textLine("Resource Usage Section", 3);
    doc.tableOpen();

    doc.newRow().addValCol("Resource").addValCol("Available").addValCol("Total");
    doc.newRow().addLabelCol(1, "Cloud");

    final ComputeCapacityArtEntity entity = report.getUsageTotals().getComputeCapacityArtEntity();
    doc.addValCol( "S3 Storage" ).addValCol( entity.getSizeS3ObjectAvailableGB() ).addValCol( entity.getSizeS3ObjectTotalGB() ).newRow();
    doc.addValCol( "Elastic IP" ).addValCol( entity.getNumPublicIpsAvailable().longValue() ).addValCol( entity.getNumPublicIpsTotal().longValue() ).newRow();

    outputZoneCapacities( entity );

    for ( final Map.Entry<String,AvailabilityZoneArtEntity> azEntry : report.getZones().entrySet() ) {
      final ComputeCapacityArtEntity zoneEntity = azEntry.getValue().getUsageTotals().getComputeCapacityArtEntity();
      doc.addLabelCol(1, "Availability Zone: " + azEntry.getKey() );
      outputZoneCapacities( zoneEntity );
    }

    doc.tableClose();
    doc.close();
  }

  private void outputZoneCapacities( final ComputeCapacityArtEntity entity ) throws IOException {
    doc.addValCol( "EBS Storage" ).addValCol( entity.getSizeEbsAvailableGB() ).addValCol( entity.getSizeEbsTotalGB() ).newRow();
    doc.addValCol( "EC2 Compute" ).addValCol( entity.getEc2ComputeUnitsAvailable().longValue() ).addValCol( entity.getEc2ComputeUnitsTotal().longValue() ).newRow();
    doc.addValCol( "EC2 Disk" ).addValCol( entity.getEc2DiskUnitsAvailable().longValue() ).addValCol( entity.getEc2DiskUnitsTotal().longValue() ).newRow();
    doc.addValCol( "EC2 Memory" ).addValCol( entity.getEc2MemoryUnitsAvailable().longValue() ).addValCol( entity.getEc2MemoryUnitsTotal().longValue() ).newRow();

    final Set<String> vmTypes = entity.getVmTypes();
    if ( !vmTypes.isEmpty() ) {
      doc.addLabelCol(2, "VM Types" );
      for ( final String vmType : vmTypes ) {
        doc.addValCol( vmType ).addValCol( entity.getInstancesAvailableForType(vmType).longValue() ).addValCol( entity.getInstancesTotalForType(vmType).longValue() ).newRow();
      }
    }
  }
}
