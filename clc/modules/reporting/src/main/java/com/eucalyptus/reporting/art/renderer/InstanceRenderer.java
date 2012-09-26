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

import java.io.*;
import java.util.Date;
import java.util.Map;

import com.eucalyptus.reporting.art.entity.*;
import com.eucalyptus.reporting.art.renderer.document.Document;
import com.eucalyptus.reporting.units.SizeUnit;
import com.eucalyptus.reporting.units.TimeUnit;
import com.eucalyptus.reporting.units.UnitUtil;
import com.eucalyptus.reporting.units.Units;

class InstanceRenderer implements Renderer {
  private final Document doc;

  public InstanceRenderer( final Document doc )
  {
    this.doc = doc;
  }

  @Override
  public void render( final ReportArtEntity report,
                      final OutputStream os,
                      final Units units ) throws IOException
  {
        doc.setWriter(new OutputStreamWriter(os));

        doc.open();
        doc.textLine("Instance Report", 1);
        doc.textLine("Begin:" + new Date(report.getBeginMs()).toString(), 4);
        doc.textLine("End:" + new Date(report.getEndMs()).toString(), 4);
        doc.textLine("Resource Usage Section", 3);
        doc.tableOpen();
        doc.newRow().addEmptyValCols(5)
            .addValCol("Net Total " + units.getSizeUnit(), 2, "center")
            .addValCol("Disk " + units.getSizeUnit(), 2, "center")
            .addValCol("Disk IOPS", 2, "center")
            .addValCol("Disk Time", 2, "center");
        doc.newRow().addValCol("InstanceId")
            .addValCol("Type").addValCol("#").addValCol("Time").addValCol("CpuUsage%")
            .addValCol("In").addValCol("Out")
            .addValCol("Read").addValCol("Write")
            .addValCol("Read").addValCol("Write")
            .addValCol("Read").addValCol("Write");
        for( final String zoneName : report.getZones().keySet() ) {
          final AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
            doc.newRow().addLabelCol(0, "Zone: " + zoneName)
                .addValCol("cumul.")
                .addValCol("cumul.");
            addUsageCols(doc, zone.getUsageTotals().getInstanceTotals(), units);
            for (String accountName: zone.getAccounts().keySet()) {
                AccountArtEntity account = zone.getAccounts().get(accountName);
                doc.newRow().addLabelCol(1, "Account: " + accountName)
                      .addValCol("cumul.")
                      .addValCol("cumul.");
                addUsageCols(doc, account.getUsageTotals().getInstanceTotals(),units);
                for (String userName: account.getUsers().keySet()) {
                    UserArtEntity user = account.getUsers().get(userName);
                    doc.newRow().addLabelCol(2, "User: " + userName)
                          .addValCol("cumul.")
                          .addValCol("cumul.");
                    addUsageCols(doc, user.getUsageTotals().getInstanceTotals(),units);
                    for (String instanceUuid: user.getInstances().keySet()) {
                        InstanceArtEntity instance = user.getInstances().get(instanceUuid);
                        doc.newRow().addValCol(instance.getInstanceId())
                            .addValCol(instance.getInstanceType());
                        addUsageCols(doc, instance.getUsage(), units);
                    }
                }
            }
        }
        doc.tableClose();

        doc.textLine("Instance Running Times Section", 3);

        doc.tableOpen();
        doc.newRow()
            .addValCol("m1.small", 2, "center")
            .addValCol("c1.medium", 2, "center")
            .addValCol("m1.large", 2, "center")
            .addValCol("c1.large", 2, "center")
            .addValCol("m1.xlarge", 2, "center");
        doc.newRow()
            .addValCol("num", 1, "center").addValCol("time", 1, "center")
            .addValCol("num", 1, "center").addValCol("time", 1, "center")
            .addValCol("num", 1, "center").addValCol("time", 1, "center")
            .addValCol("num", 1, "center").addValCol("time", 1, "center")
            .addValCol("num", 1, "center").addValCol("time", 1, "center");
        for(String zoneName : report.getZones().keySet()) {
          AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
            doc.newRow().addLabelCol(0, "Zone: " + zoneName);
              addTimeCols(doc, zone.getUsageTotals(), units);
            for (String accountName: zone.getAccounts().keySet()) {
                AccountArtEntity account = zone.getAccounts().get(accountName);
                doc.newRow().addLabelCol(1, "Account: " + accountName);
                addTimeCols(doc, account.getUsageTotals(),units);
                for (String userName: account.getUsers().keySet()) {
                  UserArtEntity user = account.getUsers().get(userName);
                    doc.newRow().addLabelCol(2, "User: " + userName);
                    addTimeCols(doc, user.getUsageTotals(),units);
                }
            }
        }
        doc.tableClose();
        doc.close();
  }

  public static Document addUsageCols( final Document doc,
                                       final InstanceUsageArtEntity entity,
                                       final Units units )
    throws IOException
  {
    doc.addValCol((long)entity.getInstanceCnt());
    doc.addValCol(UnitUtil.convertTime(entity.getDurationMs(), TimeUnit.MS, units.getTimeUnit()));
    doc.addValCol(entity.getCpuUtilizationMs()==null?null:((double)entity.getCpuUtilizationMs()/(double)entity.getDurationMs()));
    doc.addValCol(UnitUtil.convertSize(entity.getNetTotalInMegs(), SizeUnit.MB, units.getSizeUnit()));
    doc.addValCol(UnitUtil.convertSize(entity.getNetTotalOutMegs(), SizeUnit.MB, units.getSizeUnit()));
    doc.addValCol(UnitUtil.convertSize(entity.getDiskReadMegs(), SizeUnit.MB, units.getSizeUnit()));
    doc.addValCol(UnitUtil.convertSize(entity.getDiskWriteMegs(), SizeUnit.MB, units.getSizeUnit()));
    doc.addValCol(entity.getDiskReadOps()==null?null:entity.getDiskReadOps()/(entity.getDurationMs()/1000));
    doc.addValCol(entity.getDiskWriteOps()==null?null:entity.getDiskWriteOps()/(entity.getDurationMs()/1000));
    doc.addValCol(UnitUtil.convertTime(entity.getDiskReadTime(), TimeUnit.MS, TimeUnit.SECS));
    doc.addValCol(UnitUtil.convertTime(entity.getDiskWriteTime(), TimeUnit.MS, TimeUnit.SECS));

    return doc;
  }

  public static Document addTimeCols(Document doc, UsageTotalsArtEntity totals, Units units)
    throws IOException
  {
    final Map<String,InstanceUsageArtEntity> typeTotals = totals.getTypeTotals();
    for (String type : new String[] {"m1.small", "c1.medium", "m1.large", "c1.large", "m1.xlarge"}) {
      if (typeTotals.containsKey(type)) {
        doc.addValCol((long)typeTotals.get(type).getInstanceCnt());
        doc.addValCol(UnitUtil.convertTime(typeTotals.get(type).getDurationMs(), TimeUnit.MS, units.getTimeUnit()));
      } else {
        doc.addValCol("0");
        doc.addValCol("0");
      }
    }
    return doc;
  }

}
