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

import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpUsageArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.art.renderer.document.Document;
import com.eucalyptus.reporting.units.TimeUnit;
import com.eucalyptus.reporting.units.UnitUtil;
import com.eucalyptus.reporting.units.Units;

class ElasticIpRenderer
	implements Renderer
{
	private Document doc;
	
	public ElasticIpRenderer(Document doc)
	{
		this.doc = doc;
	}
	
	
	@Override
	public void render(ReportArtEntity report, OutputStream os, Units units)
			throws IOException
	{
        doc.setWriter(new OutputStreamWriter(os));
        doc.setUnlabeledRowIndent( 2 );
        
        doc.open();
        doc.textLine("Elastic IP Report", 1);
        doc.textLine("Begin:" + new Date(report.getBeginMs()).toString(), 4);
        doc.textLine("End:" + new Date(report.getEndMs()).toString(), 4);
        doc.textLine("Resource Usage Section", 3);
        doc.tableOpen();

        doc.newRow().addValCol("Elastic IP").addValCol("Instance ID").addValCol("# IPs")
        	.addValCol("Duration ("+units.labelForTime()+")");
        for (String accountName: report.getAccounts().keySet()) {
        	AccountArtEntity account = report.getAccounts().get(accountName);
        	doc.newRow().addLabelCol(0, "Account: " + accountName).addValCol("cumul.").addValCol("cumul.");
        	addUsageCols(doc, account.getUsageTotals().getElasticIpTotals(),units);
        	for (String userName: account.getUsers().keySet()) {
        		UserArtEntity user = account.getUsers().get(userName);
        		doc.newRow().addLabelCol(1, "User: " + userName)
        			.addValCol("cumul.").addValCol("cumul.");
        		addUsageCols(doc, user.getUsageTotals().getElasticIpTotals(),units);
        		for (String ip: user.getElasticIps().keySet()) {
        			ElasticIpArtEntity elasticIp = user.getElasticIps().get(ip);
        			doc.newRow().addValCol(ip)
        				.addValCol("cumul.");
        			addUsageCols(doc, elasticIp.getUsage(), units);
        			/* Add a separate line for each attachment, below the volume line */
        			for (String instanceId: elasticIp.getInstanceAttachments().keySet()) {
        				ElasticIpUsageArtEntity usage = elasticIp.getInstanceAttachments().get(instanceId);
        				doc.newRow().addValCol(ip)
        					.addValCol(instanceId);
        				addUsageCols(doc, usage, units);

        			}
        		}
        	}
        }
    	doc.tableClose();
    	doc.close();
	}
     

	public static Document addUsageCols(Document doc, ElasticIpUsageArtEntity entity, Units units)
		throws IOException
	{
		doc.addValCol(entity.getIpNum());
		doc.addValCol(UnitUtil.convertTime(entity.getDurationMs(), TimeUnit.MS, units.getTimeUnit()));
		return doc;
	}
}
