package com.eucalyptus.reporting.instance;

import java.util.*;

import org.apache.log4j.*;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.*;

public class InstanceEventListener
	implements EventListener<Event>
{
	private static Logger log = Logger.getLogger( InstanceEventListener.class );

	private static final long DEFAULT_WRITE_INTERVAL_SECS = 60 * 20; //TODO: configurable

	private final Set<String> recentlySeenUuids;
	private final List<InstanceUsageSnapshot> recentUsageSnapshots;
	private long lastWriteMs;
	private long writeIntervalMs;
	
	public InstanceEventListener()
	{
		this.recentlySeenUuids = new HashSet<String>();
		this.recentUsageSnapshots = new ArrayList<InstanceUsageSnapshot>();
		this.lastWriteMs = 0l;
		this.writeIntervalMs = DEFAULT_WRITE_INTERVAL_SECS * 1000;
	}

	public void fireEvent( Event e )
	{
	  final long receivedEventMs = this.getCurrentTimeMillis();
	  if (e instanceof InstanceEvent) {
		  log.info("Received instance event");
		  InstanceEvent event = (InstanceEvent) e;

		  final String uuid = event.getUuid();
	
		  EntityWrapper<InstanceAttributes> entityWrapper =
			  EntityWrapper.get(InstanceAttributes.class);
		  try {

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

			  /* Only write the instance attributes if we don't have them
			   * already.
			   */
			  if (! recentlySeenUuids.contains(uuid)) {
				try {
					entityWrapper.getUnique(new InstanceAttributes()
					{
						{
							setUuid(uuid);
						}
					});
				} catch (Exception ex) {
					entityWrapper.add(insAttrs);
					log.info("Wrote Reporting Instance:" + uuid);
				}
				recentlySeenUuids.add(uuid);
			  }

			  /* Gather all usage snapshots, and write them all to the database
			   * at once every n secs.
			   */
			  recentUsageSnapshots.add(insUsageSnapshot);

			  if (receivedEventMs > (lastWriteMs + getWriteIntervalMs())) {
				  for (InstanceUsageSnapshot ius: recentUsageSnapshots) {
					  entityWrapper.recast(InstanceUsageSnapshot.class).add(ius);
					  log.info("Wrote Instance Usage:" + ius.getUuid() + ":" + ius.getEntityId());
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

	//TODO: shutdown hook
	public void flush()
	{
		EntityWrapper<InstanceUsageSnapshot> entityWrapper =
			EntityWrapper.get(InstanceUsageSnapshot.class);
		try {
			for (InstanceUsageSnapshot ius : recentUsageSnapshots) {
				entityWrapper.add(ius);
				log.info("Wrote Instance Usage:" + ius.getUuid() + ":"
						+ ius.getEntityId());
			}
			recentUsageSnapshots.clear();
			entityWrapper.commit();
		} catch (Exception ex) {
			entityWrapper.rollback();
			log.error(ex);
		}
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

	
	public long getWriteIntervalMs()
	{
		return writeIntervalMs;
	}

	public void setWriteIntervalMs(long writeIntervalMs)
	{
		this.writeIntervalMs = writeIntervalMs;
	}

}
