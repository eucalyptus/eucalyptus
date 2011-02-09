package com.eucalyptus.reporting.storage;

import org.apache.log4j.Logger;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.event.EbsEvent;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.EventListener;
import com.eucalyptus.reporting.event.S3BucketCreateEvent;
import com.eucalyptus.reporting.event.S3Event;

public class StorageEventListener
	implements EventListener
{
	private static Logger log = Logger.getLogger( StorageEventListener.class );

	public StorageEventListener()
	{
	}

	public void receiveEvent(Event e)
	{
		EntityWrapper entityWrapper = EntityWrapper.get(S3BucketCreateEvent.class);
		try {
			if (e instanceof S3Event) {
			  entityWrapper.save( e );
				log.debug("Wrote s3 event to DB");
			} else if (e instanceof EbsEvent) {
			  entityWrapper.save(e);
				log.debug("Wrote ebs event to DB");
			}
			entityWrapper.commit();
		} catch (Exception ex) {
			entityWrapper.rollback();
			log.error(ex);
		}
	}

}

