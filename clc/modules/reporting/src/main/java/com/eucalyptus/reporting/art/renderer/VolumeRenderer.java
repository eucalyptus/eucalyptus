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
        	.addValCol("Size (" + units.getSizeUnit().toString() + ")").addValCol("Vol Duration");
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
                       	doc.newRow().addValCol(volume.getVolumeId())
                       			.addValCol("cumul,");
                       	addUsageCols(doc, volume.getUsage(), units);
                       	/* Add a separate line for each attachment, below the volume line */
                       	for (String instanceId: volume.getInstanceAttachments().keySet()) {
                       		VolumeUsageArtEntity usage = volume.getInstanceAttachments().get(instanceId);
                           	doc.newRow().addEmptyValCols(6).addValCol(volume.getVolumeId())
                           			.addValCol(instanceId);
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
		doc.addValCol(UnitUtil.convertTime(entity.getDurationMs(), TimeUnit.MS, units.getTimeUnit()));
		return doc;
	}

}
