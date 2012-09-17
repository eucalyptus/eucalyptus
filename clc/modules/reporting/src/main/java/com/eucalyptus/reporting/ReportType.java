package com.eucalyptus.reporting;

import com.eucalyptus.reporting.art.generator.*;
import com.eucalyptus.reporting.art.renderer.*;

public enum ReportType
{
	INSTANCE   (new InstanceArtGenerator()),
	S3         (new S3ArtGenerator()),
	VOLUME     (new VolumeArtGenerator()),
	SNAPSHOT   (new VolumeSnapshotArtGenerator()),
	ELASTIC_IP (new ElasticIpArtGenerator());
	
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
