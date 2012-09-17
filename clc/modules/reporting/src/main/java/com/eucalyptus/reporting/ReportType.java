package com.eucalyptus.reporting;

import com.eucalyptus.reporting.art.generator.*;
import com.eucalyptus.reporting.art.renderer.*;

public enum ReportType
{
	INSTANCE   (new InstanceArtGenerator()),
	S3         (null),
	VOLUME     (new VolumeArtGenerator()),
	SNAPSHOT   (null),
	ELASTIC_IP (null);
	
	private final ArtGenerator generator;

	private ReportType(ArtGenerator generator)
	{
		this.generator = generator;
	}

	public ArtGenerator getGenerator()
	{
		return generator;
	}
	
}
