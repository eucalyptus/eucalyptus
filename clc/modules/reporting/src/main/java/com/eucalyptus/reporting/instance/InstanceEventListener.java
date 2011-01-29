package com.eucalyptus.reporting.instance;

import java.util.*;

import org.hibernate.*;
import org.apache.log4j.*;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.event.*;

public class InstanceEventListener
	implements com.eucalyptus.event.EventListener
{
	private static Logger log = Logger.getLogger( InstanceEventListener.class );

	private static long WRITE_INTERVAL_SECS = 60 * 20; //TODO: configurable

	private final Set<String> recentlySeenUuids;
	private final List<InstanceUsageSnapshot> recentUsageSnapshots;
	private long lastWriteMs;

	public InstanceEventListener()
	{
		this.recentlySeenUuids = new HashSet<String>();
		this.recentUsageSnapshots = new ArrayList<InstanceUsageSnapshot>();
		this.lastWriteMs = 0l;
	}

	public <T extends com.eucalyptus.event.Event> void fireEvent( T e )
	{
	  if( !( e instanceof Event ) ) {
	    return;
	  } else {
	    Event reportingEvent = (Event) e;
  		final long receivedEventMs = this.getCurrentTimeMillis();
  		if (reportingEvent instanceof InstanceEvent) {
  			InstanceEvent event = (InstanceEvent) e;
  
  			final String uuid = event.getUuid();
  		
  			EntityWrapper<InstanceAttributes> entityWrapper =
  				EntityWrapper.get(InstanceAttributes.class);
  			Session sess = null;
  			try {
  				sess = entityWrapper.getSession();
  
  				/* Convert InstanceEvents to internal types. Internal types are
  				 * not exposed because the reporting.instance package won't be
  				 * present in the open src version
  				 */
  				InstanceAttributes insAttrs = new InstanceAttributes(uuid,
  						event.getInstanceId(), event.getInstanceType(), event.getUserId(),
  						event.getAccountId(), event.getClusterName(),
  						event.getAvailabilityZone());
  				InstanceUsageSnapshot insUsageSnapshot = new InstanceUsageSnapshot(uuid,
  						receivedEventMs, event.getCumulativeNetworkIoMegs(),
  						event.getCumulativeDiskIoMegs());
  				System.out.printf("--Snapshot %s %d %d %d\n", uuid, receivedEventMs,
  						event.getCumulativeNetworkIoMegs(), event.getCumulativeDiskIoMegs());
  
  				/* Only write the instance attributes if we don't have them
  				 * already.
  				 */
  				if (! recentlySeenUuids.contains(uuid)) {
  					if (null == sess.get(InstanceAttributes.class, uuid)) {
  						sess.save(insAttrs);
  						log.debug("Wrote Reporting Instance:" + uuid);
  					}
  					recentlySeenUuids.add(uuid);
  				}
  
  				/* Gather all usage snapshots, and write them all to the database
  				 * at once every n secs.
  				 */
  				recentUsageSnapshots.add(insUsageSnapshot);
  
  				if (receivedEventMs > (lastWriteMs + WRITE_INTERVAL_SECS*1000)) {
  					for (InstanceUsageSnapshot ius: recentUsageSnapshots) {
  						sess.save(ius);
  						log.debug("Wrote Instance Usage:" + ius.getUuid() + ":" + ius.getId());
  					}
  					recentUsageSnapshots.clear();
  					lastWriteMs = receivedEventMs;
  				}
  
  				entityWrapper.commit();
  			} catch (Exception ex) {
  				entityWrapper.rollback();
  				log.error(ex);
  			}
  		}
	  }
	}

	//TODO: shutdown hook
	public void flush()
	{
		//TODO: implement
	}

	/**
	 * Get the current time which will be used for recording when an event
	 * occurred. This can be overridden if you have some alternative method
	 * of timekeeping (synchronized, test times, etc).
	 */
	protected long getCurrentTimeMillis()
	{
		return System.currentTimeMillis();
	}
	
}
