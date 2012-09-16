package com.eucalyptus.reporting.art.renderer;

import java.io.IOException;
import java.io.OutputStream;

import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.units.Units;

public interface Renderer
{
	public void render(ReportArtEntity report, OutputStream os, Units units)
		throws IOException;
}
