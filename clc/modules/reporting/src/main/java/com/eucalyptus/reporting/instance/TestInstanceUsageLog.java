package com.eucalyptus.reporting.instance;

public class TestInstanceUsageLog
{
	private static final int NUM_INSTANCES          = 100;
	private static final int NUM_USAGES             = 10000;
	private static final int TIME_USAGE_APART_MS    = 1000;
	private static final int NUM_VERIFY_USAGES      = 5;
	
	private static final double ERROR_MARGIN		= 0.05; //5%

	public void test()
		throws Exception
	{
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();

		/* Generate fake instance data for the test
		 */
		final long maxTimeMs =
			new FalseDataGenerator().generateFakeInstances(NUM_INSTANCES, NUM_USAGES,
					TIME_USAGE_APART_MS);

		
		/* Verify that the fake data returned is correct.
		 * 
		 * The fake data for any instance has arithmetically increasing usage
		 * for each time period. For example, each fake instance uses 1 meg in
		 * the first 1000 ms, 2 megs in the second thousand ms, and so on.
		 * 
		 * This usage pattern has the property that the difference between any
		 * two consecutive periods should be the same as the difference between
		 * any other two consecutive periods, provided all four periods have the
		 * same duration.
		 * 
		 * Find <i>n</i> consecutive equal periods for each instance, verify
		 * that usage is larger in every subsequent period, and verify that the
		 * difference between two consecutive periods is the same as the
		 * difference between each subsequent two consecutive periods.
		 * 
		 * This method was chosen for several reasons. 1) It does not use hard-coded
		 * test data; 2) the property verified (of constant difference between
		 * any two groups of consecutive two periods) would not hold for most
		 * things other than arithmetically increasing sequences; and 3) it
		 * will verify that the logic inside the log mechanism is correct
		 * without using similar logic outside of it.
		 */
		final long subPeriodLength = maxTimeMs / NUM_VERIFY_USAGES;
		LogResultSet rs = usageLog.queryLog(new Period(0l, maxTimeMs));
		while (rs.next()) {
			InstanceAttributes insAttrs = rs.getInstanceAttributes();
			PeriodUsageData pud = rs.getPeriodUsageData();
			UsageData lastUsageData = null;
			UsageData lastUsageDataDiff = null;

			/* Divide the period into <i>n</i> sub-periods of equal duration and
			 * verify that the difference between any two consecutive
			 * sub-periods is equal to the difference between any other two
			 * consecutive sub-periods.
			 */
			for (int j = 0; j < NUM_VERIFY_USAGES; j++) {
				long subPeriodBeginMs = j * subPeriodLength;
				long subPeriodEndMs = subPeriodBeginMs + subPeriodLength;
				UsageData usageData = pud.getUsageData(new Period(
						subPeriodBeginMs, subPeriodEndMs));
				if (j == 1) {
					lastUsageDataDiff = lastUsageData.subtractFrom(usageData);
				} else if (j > 1) {
					UsageData usageDataDiff = lastUsageData
							.subtractFrom(usageData);
					System.out.printf("ins:%s net:%d disk:%d\n",
							insAttrs.getUuid(),
							usageDataDiff.getNetworkIoMegs(),
							usageDataDiff.getDiskIoMegs());
					if (!equalWithinError(usageDataDiff, lastUsageDataDiff,
							ERROR_MARGIN)) {
						throw new RuntimeException("difference check failed");
					}
					lastUsageDataDiff = usageDataDiff;
				}
				lastUsageData = usageData;
			}
		}

		/* Purge fake data
		 */
		usageLog.purgeLog(maxTimeMs);
	}

	/**
	 * @param errorPercent A small sampling error is introduced because the
	 *   boundaries of the samples do not correspond to the boundaries of result
	 *   periods.
	 */
	private static final boolean equalWithinError(UsageData ua, UsageData ub, double errorMargin)
	{
		final double aNetIo = ua.getNetworkIoMegs().doubleValue();
		final double bNetIo = ub.getNetworkIoMegs().doubleValue();
		final double netRatio = aNetIo / bNetIo;
		if (netRatio > 1d+errorMargin
			|| netRatio < 1d-errorMargin)
		{
				return false;
		}
		
		final double aDiskIo = ua.getDiskIoMegs().doubleValue();
		final double bDiskIo = ub.getDiskIoMegs().doubleValue();
		final double diskRatio = aDiskIo / bDiskIo;
		if (diskRatio > 1d+errorMargin
				|| diskRatio < 1d-errorMargin) {
			return false;
		}

		return true;
	}

}
