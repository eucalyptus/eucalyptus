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
