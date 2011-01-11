package com.eucalyptus.reporting.storage;

import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.instance.InstanceAttributes;

public class StorageUsageLog
{
	private static Logger log = Logger.getLogger( StorageUsageLog.class );

	private static StorageUsageLog instance;
	
	private StorageUsageLog()
	{
	}
	
	public static StorageUsageLog getStorageUsageLog()
	{
		if (instance == null) {
			instance = new StorageUsageLog();
		}
		return instance;
	}

	public Iterator<StorageUsageSnapshot> scanLog(Period period)
	{
		EntityWrapper<InstanceAttributes> entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		Session sess = null;
		try {
			sess = entityWrapper.getSession();
			@SuppressWarnings("rawtypes")
			Iterator iter = sess.createQuery(
				"from StorageUsageSnapshot as sus"
				+ " where sus.timestampMs > ?"
				+ " and sus.timestampMs < ?"
				+ " order by sus.timestamp")
				.setLong(0, period.getBeginningMs())
				.setLong(1, period.getEndingMs())
				.iterate();

			return new StorageUsageSnapshotIterator(iter);
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}

	}
	
	private class StorageUsageSnapshotIterator
			implements
				Iterator<StorageUsageSnapshot>
	{
		private final Iterator resultSetIter;

		StorageUsageSnapshotIterator(Iterator resultSetIter)
		{
			this.resultSetIter = resultSetIter;
		}

		@Override
		public boolean hasNext()
		{
			return resultSetIter.hasNext();
		}

		@Override
		public StorageUsageSnapshot next()
		{
			Object[] row = (Object[]) resultSetIter.next();
			return (StorageUsageSnapshot) row[0];
		}

		@Override
		public void remove()
		{
			resultSetIter.remove();
		}

	}

	private static String getAttributeValue(GroupByCriterion criterion,
			StorageUsageSnapshot snapshot)
	{
		switch (criterion) {
			case ACCOUNT:
				return snapshot.getAccountId();
			case USER:
				return snapshot.getOwnerId();
			case CLUSTER:
				return snapshot.getClusterName();
			case AVAILABILITY_ZONE:
				return snapshot.getAvailabilityZone();
			default:
				return snapshot.getOwnerId();
		}
	}


	public Map<String, StorageUsageSummary> scanSummarize(Period period,
			GroupByCriterion criterion)
	{
		final Map<String, StorageUsageSummary> results =
			new HashMap<String, StorageUsageSummary>();

		Iterator<StorageUsageSnapshot> iter = scanLog(period);
		while (iter.hasNext()) {
			StorageUsageSnapshot snapshot = iter.next();
			String critVal = getAttributeValue(criterion, snapshot);
			StorageUsageSummary summary = null;
			if (results.containsKey(critVal)) {
				summary = results.get(critVal);
			} else {
				summary = new StorageUsageSummary();
				results.put(critVal, summary);
			}
			summary.sumFrom(snapshot);
		}
		return results;
	}

	public Map<String, Map<String, StorageUsageSummary>> scanSummarize(
			Period period, GroupByCriterion outerCriterion,
			GroupByCriterion innerCriterion)
	{
		final Map<String, Map<String, StorageUsageSummary>> results =
			new HashMap<String, Map<String, StorageUsageSummary>>();

		Iterator<StorageUsageSnapshot> iter = scanLog(period);
		while (iter.hasNext()) {
			StorageUsageSnapshot snapshot = iter.next();
			String outerCritVal = getAttributeValue(outerCriterion, snapshot);
			Map<String, StorageUsageSummary> innerMap = null;
			if (results.containsKey(outerCritVal)) {
				innerMap = results.get(outerCritVal);
			} else {
				innerMap = new HashMap<String, StorageUsageSummary>();
				results.put(outerCritVal, innerMap);
			}
			String innerCritVal = getAttributeValue(innerCriterion, snapshot);
			StorageUsageSummary summary = null;
			if (innerMap.containsKey(innerCritVal)) {
				summary = innerMap.get(innerCritVal);
			} else {
				summary = new StorageUsageSummary();
				innerMap.put(innerCritVal, summary);
			}
			summary.sumFrom(snapshot);
		}

		return results;
	}

}
