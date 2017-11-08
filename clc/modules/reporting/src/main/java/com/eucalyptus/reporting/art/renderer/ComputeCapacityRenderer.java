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
class ComputeCapacityRenderer implements Renderer {
  private final Document doc;

  public ComputeCapacityRenderer( final Document doc ) {
    this.doc = doc;
  }

  @Override
  public void render( final ReportArtEntity report, final OutputStream os, final Units units ) throws IOException {
    doc.setWriter(new OutputStreamWriter(os));
    doc.setUnlabeledRowIndent(2);

    doc.open();
    doc.textLine("Capacity Report", 1);
    doc.textLine("Begin:" + new Date(report.getBeginMs()).toString(), 4);
    doc.textLine("End:" + new Date(report.getEndMs()).toString(), 4);
    doc.textLine("Resource Usage Section", 3);
    doc.tableOpen();

    doc.newRow().addValCol("Resource").addValCol("Available").addValCol("Total");
    doc.newRow().addLabelCol(0, "Cloud");

    final ComputeCapacityArtEntity entity = report.getUsageTotals().getComputeCapacityArtEntity();
    doc.addValCol( "S3 Storage" ).addValCol( entity.getSizeS3ObjectAvailableGB() ).addValCol( entity.getSizeS3ObjectTotalGB() ).newRow();
    doc.addValCol( "Elastic IP" ).addValCol( entity.getNumPublicIpsAvailable() ).addValCol( entity.getNumPublicIpsTotal() ).newRow();

    outputZoneCapacities( entity );

    for ( final Map.Entry<String,AvailabilityZoneArtEntity> azEntry : report.getZones().entrySet() ) {
      final ComputeCapacityArtEntity zoneEntity = azEntry.getValue().getUsageTotals().getComputeCapacityArtEntity();
      doc.addLabelCol(0, "Availability Zone: " + azEntry.getKey() );
      outputZoneCapacities( zoneEntity );
    }

    doc.tableClose();
    doc.close();
  }

  private void outputZoneCapacities( final ComputeCapacityArtEntity entity ) throws IOException {
    doc.addValCol( "EBS Storage" ).addValCol( entity.getSizeEbsAvailableGB() ).addValCol( entity.getSizeEbsTotalGB() ).newRow();
    doc.addValCol( "EC2 Compute" ).addValCol( entity.getEc2ComputeUnitsAvailable() ).addValCol( entity.getEc2ComputeUnitsTotal() ).newRow();
    doc.addValCol( "EC2 Disk" ).addValCol( entity.getEc2DiskUnitsAvailable() ).addValCol( entity.getEc2DiskUnitsTotal() ).newRow();
    doc.addValCol( "EC2 Memory" ).addValCol( entity.getEc2MemoryUnitsAvailable() ).addValCol( entity.getEc2MemoryUnitsTotal() ).newRow();

    final Set<String> vmTypes = entity.getVmTypes();
    if ( !vmTypes.isEmpty() ) {
      doc.addLabelCol(1, "VM Types" );
      for ( final String vmType : vmTypes ) {
        doc.addValCol( vmType ).addValCol( entity.getInstancesAvailableForType(vmType) ).addValCol( entity.getInstancesTotalForType(vmType) ).newRow();
      }
    }
  }
}
