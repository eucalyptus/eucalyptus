package com.eucalyptus.blockstorage.entities;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;

/**
 * Global configuration information that is common
 * to all SC instances, regardless of cluster or instance
 *
 */
@ConfigurableClass(root = "storage", description = "Basic storage controller configuration.")
public class BlockStorageGlobalConfiguration {
	private static final int DEFAULT_GLOBAL_TOTAL_SNAPSHOT_SIZE_GB = 50;
	
	@ConfigurableField( description = "Maximum total snapshot capacity (GB)", displayName = "Maximum total size allowed for snapshots")
	public static Integer global_total_snapshot_size_limit_gb = DEFAULT_GLOBAL_TOTAL_SNAPSHOT_SIZE_GB;

	/*
	@EntityUpgrade(entities = { BlockStorageGlobalConfiguration.class }, since = Version.v4_0_0, value = Storage.class)
	public static void upgrade3_4_To4_0() throws Exception {
		//Set the max snapshot size from Walrus in 3.4.x
		BlockStorageGlobalConfiguration config = getConfiguration();
		WalrusInfo walrusConfig = WalrusInfo.getWalrusInfo();
		config.setGlobalTotalSnapshotSizeLimitGB(walrusConfig.getStorageMaxTotalSnapshotSizeInGb());
		
		try {
			Transactions.save(config);
		} catch(TransactionException e) {
			LOG.error("Error saving upgrade global osg configuration");
		}
	}
	*/
}
