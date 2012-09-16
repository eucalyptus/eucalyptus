package com.eucalyptus.reporting.art.renderer;

import com.eucalyptus.reporting.ReportFormat;

public interface RendererFactory
{
	public Renderer getRenderer(ReportFormat format);
}
