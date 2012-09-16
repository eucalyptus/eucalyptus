package com.eucalyptus.reporting.art.renderer;

import com.eucalyptus.reporting.ReportFormat;
import com.eucalyptus.reporting.art.renderer.document.*;

public class InstanceRendererFactory
implements RendererFactory
{
	@Override
	public Renderer getRenderer(ReportFormat format)
	{
		if (format.equals(ReportFormat.HTML)) {
			return new InstanceRenderer(new HtmlDocument());
		} else if (format.equals(ReportFormat.CSV)) {
			return new InstanceRenderer(new CsvDocument());			
		} else {
			throw new UnsupportedOperationException("Format " + format + " unsupported for instance reports");
		}
	}

}
