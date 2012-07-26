package com.eucalyptus.reporting.event_store;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * @author tom.werges
 */
public class ReportingS3BucketEventStore
{
	private static Logger LOG = Logger.getLogger( ReportingS3BucketEventStore.class );

	private static ReportingS3BucketEventStore instance = null;
	
	public static synchronized ReportingS3BucketEventStore getInstance()
	{
		if (instance == null) {
			instance = new ReportingS3BucketEventStore();
		}
		return instance;
	}
	
	private ReportingS3BucketEventStore()
	{
		
	}

	public void insertS3BucketCreateEvent(String s3BucketName, String userId, String availabilityZone)
	{
		EntityWrapper<ReportingS3BucketCreateEvent> entityWrapper =
			EntityWrapper.get(ReportingS3BucketCreateEvent.class);

		try {
			ReportingS3BucketCreateEvent s3Bucket = new ReportingS3BucketCreateEvent(s3BucketName, userId, availabilityZone);
			entityWrapper.add(s3Bucket);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + s3Bucket);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertS3BucketDeleteEvent(String s3BucketName, long timestampMs)
	{
		EntityWrapper<ReportingS3BucketDeleteEvent> entityWrapper =
			EntityWrapper.get(ReportingS3BucketDeleteEvent.class);

		try {
			ReportingS3BucketDeleteEvent s3Bucket = new ReportingS3BucketDeleteEvent(s3BucketName, timestampMs);
			entityWrapper.add(s3Bucket);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + s3Bucket);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
		
	}
}

