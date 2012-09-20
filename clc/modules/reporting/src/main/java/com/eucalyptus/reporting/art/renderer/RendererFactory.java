package com.eucalyptus.reporting.art.renderer;

import com.eucalyptus.reporting.ReportFormat;
import com.eucalyptus.reporting.ReportType;
import com.eucalyptus.reporting.art.renderer.document.*;

public class RendererFactory
{
	public static Renderer getRenderer(ReportType type, ReportFormat format)
	{
		final Document document;
		if (format.equals(ReportFormat.CSV)) {
			document = new CsvDocument();
		} else if (format.equals(ReportFormat.HTML)) {
			document = new HtmlDocument();
		} else {
			throw new UnsupportedOperationException("Unsupported format:" + format);
		}
		
		if (type.equals(ReportType.INSTANCE)) {
			return new InstanceRenderer(document);
		} else if (type.equals(ReportType.VOLUME)) {
			return new VolumeRenderer(document);
		} else if (type.equals(ReportType.SNAPSHOT)) {
			return new VolumeSnapshotRenderer(document);
		} else if (type.equals(ReportType.ELASTIC_IP)) {
			return new ElasticIpRenderer(document);
		} else if (type.equals(ReportType.S3)) {
			return new S3Renderer(document);
		} else if (type.equals(ReportType.CAPACITY)) {
			return new ComputeCapacityRenderer(document);
		} else {
			throw new UnsupportedOperationException("Unsupported type:" + type);
		}
	}
}
