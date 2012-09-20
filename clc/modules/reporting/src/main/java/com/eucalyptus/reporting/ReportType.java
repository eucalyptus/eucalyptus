package com.eucalyptus.reporting;

import com.eucalyptus.reporting.art.generator.*;

public enum ReportType
{
	INSTANCE   (new InstanceArtGenerator()),
	S3         (new S3ArtGenerator()),
	VOLUME     (new VolumeArtGenerator()),
	SNAPSHOT   (new VolumeSnapshotArtGenerator()),
	ELASTIC_IP (new ElasticIpArtGenerator()),
	COMPUTE    (new ComputeCapacityArtGenerator()),
	;

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
