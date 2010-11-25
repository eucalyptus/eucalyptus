package com.eucalyptus.reporting.instance;

import org.hibernate.*;
import org.apache.log4j.*;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.entities.EntityWrapper;

import java.util.*;

/**
 * <p>InstanceUsageLog is a log of historical instance usage information.
 * You can query InstanceUsageLog to gather data for reporting generation.
 * 
 * @author tom.werges
 */
@ConfigurableClass(root="instanceLog", alias="basic", description="Configuration for instance usage sampling and logging", singleton=true)
public class InstanceUsageLog
{
	private static Logger log = Logger.getLogger( InstanceUsageLog.class );

	private static InstanceUsageLog singletonInstance = null;

	private InstanceUsageLog()
	{
	}

	public synchronized InstanceUsageLog getInstanceUsageLog()
	{
		if (singletonInstance==null) {
			singletonInstance = new InstanceUsageLog();
		}
		return singletonInstance;
	}

	/**
	 * <p>Reads instance usage data from the log. The results include both
	 * instance attributes (instance id, type, etc) and resource usage
	 * by instance for some period.
	 * 
	 * <p>The data in the log is not indexed, in order to minimize write time.
	 * As a result, you can't search through the log for specific instances or
	 * specific data; you can only get a full dump between dates. However, there
	 * exist groovy scripts to filter the data, or to insert the data into a
	 * data warehouse for subsequent searching.
	 * 
	 * <p>The usage data in logs is <i>sampled</i>, meaning data is collected
	 * every <i>n</i> seconds and written. As a result, some small error will
	 * be introduced if the boundaries of desired periods (ie months) do not
	 * exactly correspond to the boundaries of the samples. In that case, the
	 * reporting mechanism will be unable to determine how much of the usage in
	 * a sample belongs to which of the two periods whose boundaries it crosses,
	 * so it will assign usage to one period based on a rule.
	 * 
	 * <p>Very recent information (within the prior five minutes, for example)
	 * may not have been acquired yet, in which case, an empty period or a
	 * period with incomplete information may be returned.
	 */
	public LogResultSet queryLog(final Period period)
	{
		if (period == null) throw new IllegalArgumentException("period must not be null");

		log.debug(String.format("getPeriodInstanceUsages periodBeginMs:%d periodsEndMs:%d ",
				period.getBeginningMs(), period.getEndingMs()));

		Iterator resultIter = null;
		EntityWrapper entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		Session sess = null;
		try {
			sess = entityWrapper.getSession();

			resultIter = sess.createQuery(
				"from InstanceAttributes, InstanceUsageSnapshot"
				+ " where InstanceAttributes.uuid = InstanceUsageSnapshot.uuid"
				+ " and InstanceUsageSnapshot.timestampMs > ?"
				+ " and InstanceUsageSnapshot.timestampMs < ?"
				+ " order by InstanceAttributes.uuid")
				.setLong(0, period.getBeginningMs())
				.setLong(1, period.getEndingMs())
				.iterate();

			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			//We can't recover from this.
		}

		return new LogResultSet(resultIter, period);
	}

	/**
	 * Permanently purges data older than a certain timestamp from the log. 
	 */
	public void purgeLog(long earlierThanMs)
	{
		log.info(String.format("purge earlierThan:%d ", earlierThanMs));

		EntityWrapper entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		Session sess = null;
		try {
			sess = entityWrapper.getSession();
			sess.createSQLQuery("DELETE FROM instance_usage_snapshot WHERE timestamp_ms < ?")
				.setLong(0, new Long(earlierThanMs))
				.executeUpdate();

			/* Delete all reporting instances which no longer have even a
			 * a single corresponding instance usage snapshot, using
			 * MySQL's fancy multi-table delete with left outer join syntax.
			 */
			sess.createSQLQuery(
					"DELETE instance_attributes" 
					+ " FROM instance_attributes"
					+ " LEFT OUTER JOIN instance_usage_snapshot"
					+ " ON instance_attributes.uuid = instance_usage_snapshot.uuid"
					+ " WHERE instance_usage_snapshot.uuid IS NULL")
				.executeUpdate();
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
		}
	}

}
