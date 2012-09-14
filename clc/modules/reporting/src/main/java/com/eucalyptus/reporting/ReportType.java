package com.eucalyptus.reporting;

import com.eucalyptus.reporting.art.generator.*;
import com.eucalyptus.reporting.art.renderer.*;

public enum ReportType
{
	INSTANCE   (new InstanceArtGenerator(), new InstanceHtmlRenderer()),
	S3         (null,null),
	VOLUME     (new VolumeArtGenerator(), new VolumeHtmlRenderer()),
	SNAPSHOT   (null,null),
	ELASTIC_IP (null,null);
	
	private final HtmlRenderer renderer;
	private final ArtGenerator generator;

	private ReportType(ArtGenerator generator, HtmlRenderer renderer)
	{
		this.renderer = renderer;
		this.generator = generator;
	}

	public HtmlRenderer getRenderer()
	{
		return renderer;
	}

	public ArtGenerator getGenerator()
	{
		return generator;
	}
	
}
