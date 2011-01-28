package com.eucalyptus.reporting.storage;

import org.hibernate.*;
import org.apache.log4j.*;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.event.*;
import com.eucalyptus.reporting.instance.*;

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
		Session sess = null;
		try {
			sess = entityWrapper.getSession();
			if (e instanceof S3Event) {
				sess.save(e);
				log.debug("Wrote s3 event to DB");
			} else if (e instanceof EbsEvent) {
				sess.save(e);
				log.debug("Wrote ebs event to DB");
			}
			entityWrapper.commit();
		} catch (Exception ex) {
			entityWrapper.rollback();
			log.error(ex);
		}
	}

}

