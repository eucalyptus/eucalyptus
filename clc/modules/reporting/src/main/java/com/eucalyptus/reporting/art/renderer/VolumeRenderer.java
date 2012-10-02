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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.reporting.art.renderer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeUsageArtEntity;
import com.eucalyptus.reporting.art.renderer.document.Document;
import com.eucalyptus.reporting.units.SizeUnit;
import com.eucalyptus.reporting.units.TimeUnit;
import com.eucalyptus.reporting.units.UnitUtil;
import com.eucalyptus.reporting.units.Units;

class VolumeRenderer
	implements Renderer
{
	private Document doc;
	
	public VolumeRenderer(Document doc)
	{
		this.doc = doc;
	}
	
	
	@Override
	public void render(ReportArtEntity report, OutputStream os, Units units)
			throws IOException
	{
        doc.setWriter(new OutputStreamWriter(os));
        
        doc.open();
        doc.textLine("Volume Report", 1);
        doc.textLine("Begin:" + new Date(report.getBeginMs()).toString(), 4);
        doc.textLine("End:" + new Date(report.getEndMs()).toString(), 4);
        doc.textLine("Resource Usage Section", 3);
        doc.tableOpen();

        doc.newRow().addValCol("Instance Id").addValCol("Volume Id").addValCol("# Vol")
        	.addValCol("Size (" + units.getSizeUnit().toString() + ")").addValCol("GB-Days");
        for(String zoneName : report.getZones().keySet()) {
        	AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
            doc.newRow().addLabelCol(0, "Zone: " + zoneName).addValCol("cumul.").addValCol("cumul.");
            addUsageCols(doc, zone.getUsageTotals().getVolumeTotals(), units);
            for (String accountName: zone.getAccounts().keySet()) {
              	AccountArtEntity account = zone.getAccounts().get(accountName);
                doc.newRow().addLabelCol(1, "Account: " + accountName).addValCol("cumul.").addValCol("cumul.");
                addUsageCols(doc, account.getUsageTotals().getVolumeTotals(),units);
                for (String userName: account.getUsers().keySet()) {
                   	UserArtEntity user = account.getUsers().get(userName);
                       doc.newRow().addLabelCol(2, "User: " + userName)
                       		.addValCol("cumul.").addValCol("cumul.");
                       addUsageCols(doc, user.getUsageTotals().getVolumeTotals(),units);
                    for (String volumeUuid: user.getVolumes().keySet()) {
                       	VolumeArtEntity volume = user.getVolumes().get(volumeUuid);
                       	doc.newRow().addValCol("cumul.")
                       			.addValCol(volume.getVolumeId());
                       	addUsageCols(doc, volume.getUsage(), units);
                       	/* Add a separate line for each attachment, below the volume line */
                       	for (String instanceId: volume.getInstanceAttachments().keySet()) {
                       		VolumeUsageArtEntity usage = volume.getInstanceAttachments().get(instanceId);
                           	doc.newRow().addValCol(instanceId)
                           			.addValCol(volume.getVolumeId());
                           	addUsageCols(doc, usage, units);
                       		
                       	}
                    }
                }
            }
        }
        doc.tableClose();
        doc.close();
     
	}

	public static Document addUsageCols(Document doc, VolumeUsageArtEntity entity, Units units)
		throws IOException
	{
		doc.addValCol(entity.getVolumeCnt());
		doc.addValCol(UnitUtil.convertSize(entity.getSizeGB(), SizeUnit.GB, units.getSizeUnit()));
		doc.addValCol(UnitUtil.convertSizeTime(entity.getGBSecs(), SizeUnit.GB,
				units.getSizeUnit(), TimeUnit.SECS, units.getTimeUnit()));
		return doc;
	}

}
