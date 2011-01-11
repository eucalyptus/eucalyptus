package com.eucalyptus.reporting.storage;

import org.hibernate.*;
import org.apache.log4j.*;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.event.*;
import com.eucalyptus.reporting.instance.*;
import com.eucalyptus.reporting.star_queue.StarReceiver;

public class StorageEventPoller
{
	private static Logger log = Logger.getLogger( StorageEventPoller.class );

	private final StarReceiver receiver;
	
	public StorageEventPoller(StarReceiver receiver)
	{
		this.receiver = receiver;
	}

	public void createEventSummary()
	{
		EntityWrapper entityWrapper = EntityWrapper.get(S3BucketCreateEvent.class);
		Session sess = null;
		try {
			sess = entityWrapper.getSession();
			entityWrapper.commit();
		} catch (Exception ex) {
			entityWrapper.rollback();
			log.error(ex);
		}
		
	}
	
}

