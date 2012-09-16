package com.eucalyptus.reporting;

import com.eucalyptus.reporting.art.generator.*;
import com.eucalyptus.reporting.art.renderer.*;

public enum ReportType
{
	INSTANCE   (new InstanceArtGenerator(), new InstanceRendererFactory()),
	S3         (null, null),
	VOLUME     (new VolumeArtGenerator(), new VolumeRendererFactory()),
	SNAPSHOT   (null, null),
	ELASTIC_IP (null, null);
	
	private final ArtGenerator generator;
	private final RendererFactory rendererFactory;

	private ReportType(ArtGenerator generator, RendererFactory rendererFactory)
	{
		this.generator = generator;
		this.rendererFactory = rendererFactory;
	}

	public ArtGenerator getGenerator()
	{
		return generator;
	}
	
	public RendererFactory getRendererFactory()
	{
		return this.rendererFactory;
	}
	
}
