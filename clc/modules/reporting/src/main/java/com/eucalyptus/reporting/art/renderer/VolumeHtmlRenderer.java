package com.eucalyptus.reporting.art.renderer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeUsageArtEntity;
import com.eucalyptus.reporting.units.SizeUnit;
import com.eucalyptus.reporting.units.TimeUnit;
import com.eucalyptus.reporting.units.UnitUtil;
import com.eucalyptus.reporting.units.Units;

public class VolumeHtmlRenderer
	implements HtmlRenderer
{
	private static final int LABEL_WIDTH = 50;
	private static final int VALUE_WIDTH = 80;
	
	public VolumeHtmlRenderer()
	{
		
	}
	
	@Override
	public void render(ReportArtEntity report, OutputStream os, Units units)
			throws IOException
	{
        Writer writer = new OutputStreamWriter(os);
        writer.write("<html><body>\n");
        writer.write("<h1>Volume Report</h1>\n");
        writer.write("<h4>Begin:" + ms2Date(report.getBeginMs()) + "</h4>\n");
        writer.write("<h4>End:" + ms2Date(report.getEndMs()) + "</h4>\n");
        
        writer.write("<table border=0>\n");
        writer.write((new Row(VALUE_WIDTH)).addEmptyCols(6, LABEL_WIDTH)
        		.addCol("Instance Id").addCol("Volume Id").addCol("# Vol").addCol("Size (" + units.toString() + ")")
        		.addCol("Vol Duration").toString());
        for(String zoneName : report.getZones().keySet()) {
        	AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
            writer.write((new VolRow()).addCol("Zone: " + zoneName, LABEL_WIDTH, 3, "left").addEmptyCols(3, LABEL_WIDTH)
            		.addCol("cumul.")
            		.addCol("cumul.")
            		.addUsageCols(zone.getUsageTotals().getVolumeTotals(), units)
            		.toString());
            for (String accountName: zone.getAccounts().keySet()) {
              	AccountArtEntity account = zone.getAccounts().get(accountName);
                writer.write((new VolRow()).addEmptyCols(2,LABEL_WIDTH)
                   		.addCol("Account: " + accountName, LABEL_WIDTH, 3, "left").addEmptyCols(1, LABEL_WIDTH)
                   		.addCol("cumul.")
                   		.addCol("cumul.")
                   		.addUsageCols(account.getUsageTotals().getVolumeTotals(),units)
                   		.toString());
                for (String userName: account.getUsers().keySet()) {
                   	UserArtEntity user = account.getUsers().get(userName);
                       writer.write((new VolRow()).addEmptyCols(3,LABEL_WIDTH)
                       		.addCol("User: " + userName, LABEL_WIDTH, 3, "left")
                       		.addCol("cumul.")
                       		.addCol("cumul.")
                       		.addUsageCols(user.getUsageTotals().getVolumeTotals(),units)
                       		.toString());
                    for (String volumeUuid: user.getVolumes().keySet()) {
                       	VolumeArtEntity volume = user.getVolumes().get(volumeUuid);
                       	writer.write((new VolRow()).addEmptyCols(6,LABEL_WIDTH)
                       			.addCol(volume.getVolumeId())
                       			.addCol("cumul,")
                       			.addUsageCols(volume.getUsage(), units)
                       			.toString());
                       	/* Add a separate line for each attachment, below the volume line */
                       	for (String instanceId: volume.getInstanceAttachments().keySet()) {
                       		VolumeUsageArtEntity usage = volume.getInstanceAttachments().get(instanceId);
                           	writer.write((new VolRow()).addEmptyCols(6,LABEL_WIDTH)
                           			.addCol(volume.getVolumeId())
                           			.addCol(instanceId)
                           			.addUsageCols(usage, units)
                           			.toString());
                       		
                       	}
                    }
                }
            }
        }
        writer.write("</table>\n");

        writer.write("</body></html>\n");
        writer.flush();		
     
	}

	private static class VolRow
	extends Row
	{
	
	public VolRow()
	{
		super(VALUE_WIDTH);
	}
		
    public VolRow addCol(String val)
    {
        return addCol(val, VALUE_WIDTH, 1, "center");
    }

    public VolRow addCol(Long val)
    {
        return addCol((val==null)?null:val.toString(), VALUE_WIDTH, 1, "center");
    }

    public VolRow addCol(Double val)
    {
        return addCol((val==null)?null:String.format("%3.1f", val), VALUE_WIDTH, 1, "center");
    }

    public VolRow addCol(String val, int width, int colspan, String align)
    {
    	super.addCol(val, width, colspan, align);
        return this;
    }

    public VolRow addEmptyCols(int num, int width)
    {
    	super.addEmptyCols(num, width);
        return this;
    }

	public Row addUsageCols(VolumeUsageArtEntity entity, Units units)
	{
		addCol(entity.getVolumeCnt());
		addCol(UnitUtil.convertSize(entity.getSizeGB(), SizeUnit.GB, units.getSizeUnit()));
		addCol(UnitUtil.convertTime(entity.getDurationMs(), TimeUnit.MS, units.getTimeUnit()));
		return this;
	}
	
}
	
	
	private static String ms2Date(long ms)
	{
		return new Date(ms).toString();
	}
	
}
