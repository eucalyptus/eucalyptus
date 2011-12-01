package com.eucalyptus.reporting.modules.instance;

import java.util.*;

import org.apache.log4j.*;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.*;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

@ConfigurableClass( root = "reporting", description = "Parameters controlling reporting")
public class InstanceEventListener
	implements EventListener<Event>
{
	private static Logger log = Logger.getLogger( InstanceEventListener.class );

	@ConfigurableField( initial = "1200", description = "How often the reporting system writes instance snapshots" )
	public static long DEFAULT_WRITE_INTERVAL_SECS = 1200;

	private final Set<String> recentlySeenUuids;
	private final Map<String, InstanceUsageSnapshot> recentUsageSnapshots;
	private long lastWriteMs;
	
	public InstanceEventListener()
	{
		this.recentlySeenUuids = new HashSet<String>();
		this.recentUsageSnapshots = new HashMap<String, InstanceUsageSnapshot>();
		this.lastWriteMs = 0l;
	}

	public void fireEvent( Event e )
	{
	  final long receivedEventMs = this.getCurrentTimeMillis();
	  if (e instanceof InstanceEvent) {
		  InstanceEvent event = (InstanceEvent) e;
		  log.info("Received instance event:" + event);

		  final String uuid = event.getUuid();
		  if (uuid == null) {
			  log.warn("Received null uuid");
			  return;
		  }
		  
		  /* Retain records of all account and user id's and names encountered
		   * even if they're subsequently deleted.
		   */
		  ReportingAccountDao.getInstance().addUpdateAccount(
				  event.getAccountId(), event.getAccountName());
		  ReportingUserDao.getInstance().addUpdateUser(
				  event.getUserId(), event.getUserName());

		  /* Convert InstanceEvents to internal types. Internal types are not
		   * exposed because the reporting.instance package won't be present
		   * in the open src version
		   */
		  InstanceAttributes insAttrs = new InstanceAttributes(uuid,
				  event.getInstanceId(), event.getInstanceType(),
				  event.getAccountId(), event.getUserId(),
				  event.getClusterName(), event.getAvailabilityZone());
		  InstanceUsageSnapshot insUsageSnapshot = new InstanceUsageSnapshot(
				  uuid, receivedEventMs, event.getCumulativeNetworkIoMegs(),
				  event.getCumulativeDiskIoMegs());

		  
		  /* Write the instance attributes, but only if we don't have it
		   * already.
		   */
		  EntityWrapper<InstanceAttributes> attrEntityWrapper =
			  EntityWrapper.get(InstanceAttributes.class);
		  try {
			  if (! recentlySeenUuids.contains(uuid)) {
				try {
					attrEntityWrapper.getUnique(new InstanceAttributes()
					{
						{
							setUuid(uuid);
						}
					});
				} catch (Exception ex) {
					attrEntityWrapper.add(insAttrs);
					log.info("Wrote Reporting Instance:" + uuid);
				}
				recentlySeenUuids.add(uuid);
			  }
			  attrEntityWrapper.commit();
		  } catch (Exception ex) {
			  attrEntityWrapper.rollback();
			  log.error(ex);
		  }

 
		  /* Gather the latest usage snapshots (they're cumulative, so
		   * intermediate ones don't matter except for granularity), and
		   * write them all to the database at once every n secs.
		   */
		  if (! recentUsageSnapshots.containsKey(uuid)) {
			  recentUsageSnapshots.put(uuid, insUsageSnapshot);
		  } else {
			  InstanceUsageSnapshot oldSnapshot =
				  recentUsageSnapshots.get(uuid);
			  if (oldSnapshot.getTimestampMs() < insUsageSnapshot.getTimestampMs()) {
				  recentUsageSnapshots.put(uuid, insUsageSnapshot);
			  } else {
				  //log, then just continue
				  log.error("Events are arriving out of order");
			  }
		  }

		  EntityWrapper<InstanceUsageSnapshot> entityWrapper =
			  EntityWrapper.get(InstanceUsageSnapshot.class);
		  try {
			  if (receivedEventMs > (lastWriteMs + DEFAULT_WRITE_INTERVAL_SECS*1000)) {
				  for (String key: recentUsageSnapshots.keySet()) {
					  InstanceUsageSnapshot ius = recentUsageSnapshots.get(key);
					  entityWrapper.add(ius);
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
			for (String key : recentUsageSnapshots.keySet()) {
				InstanceUsageSnapshot ius = recentUsageSnapshots.get(key);
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

	
}
