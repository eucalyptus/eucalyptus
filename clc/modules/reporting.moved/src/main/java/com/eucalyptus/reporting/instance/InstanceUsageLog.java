package com.eucalyptus.reporting.instance;

import org.hibernate.*;
import org.apache.log4j.*;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.entities.EntityWrapper;

import java.util.*;

/**
 * <p>InstanceUsageLog is a log of historical instance usage information.
 * You can query InstanceUsageLog to gather data for reporting generation.
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
	 * Reads instance usage data from the log.
	 *
	 * You can't search through the log for specific instances or
	 * specific data; you can only get a full dump between dates. However, there
	 * exist groovy scripts to filter the data, or to insert the data into
	 * a data warehouse for subsequent searching.
	 *
	 * The usage data in logs is <i>sampled</i>, meaning data is collected
	 * every <i>n</i> seconds and stored. As a result, some small error
	 * will be introduced if the boundaries of desired periods (ie months)
	 * do not exactly correspond to the boundaries of the samples. In that
	 * case, the reporting mechanism will be unable to determine how much
	 * of the usage in a sample belongs to which of the two periods whose
	 * boundaries it crosses, so it will assign usage to one period based on
	 * a rule.
	 *
	 * Very recent information (within the prior five minutes, for example)
	 * may not have been acquired yet, in which case, an empty period or a
	 * period with incomplete information may be returned.
	 * 
	 * An Iterator is returned because the result set can be large and
	 * may not fit in memory.
	 */
	public Iterator<ReportingInstance> getReportingInstances(final Period period)
	{
		if (period == null) throw new IllegalArgumentException("period must not be null");
		final List<ReportingInstance> results = new ArrayList<ReportingInstance>();

		log.debug(String.format("getPeriodInstanceUsages periodBeginMs:%d periodsEndMs:%d ",
				period.getBeginningMs(), period.getEndingMs()));

		EntityWrapper entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		try {
			
			Iterator iter = entityWrapper.createQuery(
				"from ReportingInstance, InstanceUsageSnapshot"
				+ " where ReportingInstance.uuid = InstanceUsageSnapshot.uuid"
				+ " and InstanceUsageSnapshot.timestampMs > ?"
				+ " and InstanceUsageSnapshot.timestampMs < ?")
				.setLong(0, period.getBeginningMs())
				.setLong(1, period.getEndingMs())
				.iterate();

			Map<String, ReportingInstanceImpl> reportingInstances = new HashMap<String, ReportingInstanceImpl>();
			
			while (iter.hasNext()) {
				Object[] row = (Object[]) iter.next();

				InstanceAttributes insAtts = (InstanceAttributes) row[0];
				InstanceUsageSnapshot iu = (InstanceUsageSnapshot) row[1];
				UsageSnapshot uSnapshot = iu.getUsageSnapshot();
				String uuid = insAtts.getUuid();
				
				if (reportingInstances.containsKey(uuid)) {
					reportingInstances.get(uuid).addSnapshot(uSnapshot);
				} else {
					ReportingInstanceImpl repIns = new ReportingInstanceImpl(uuid,
							insAtts.getInstanceId(), insAtts.getInstanceType(),
							insAtts.getUserId(), insAtts.getClusterName(),
							insAtts.getAvailabilityZone());
					repIns.addSnapshot(uSnapshot);
					reportingInstances.put(uuid, repIns);
				}
			}

			results.addAll(reportingInstances.values());

			entityWrapper.commit();

		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
		}
		
		return results.iterator();
	}

}
