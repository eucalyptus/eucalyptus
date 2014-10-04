package com.eucalyptus.blockstorage.ceph;

import java.io.File;
import java.util.Random;

import org.apache.log4j.Logger;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rbd.Rbd;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;

/**
 * Utility class for creating and managing connection to a pool in a Ceph cluster. Encapsulates all elements (Rados, IoCTX, Rbd, pool) of a ceph connection if
 * the caller needs access to a specific element.
 */
public class CephRbdConnectionManager {

	private static Logger LOG = Logger.getLogger(CephRbdConnectionManager.class);
	private static Random randomGenerator = new Random();
	private static final String KEYRING = "keyring";

	private Rados rados;
	private IoCTX ioContext;
	private Rbd rbd;
	private String pool;

	private CephRbdConnectionManager(CephRbdInfo config, String poolName) {
		try {
			rados = new Rados(config.getCephUser());
			rados.confSet(KEYRING, config.getCephKeyringFile());
			rados.confReadFile(new File(config.getCephConfigFile()));
			rados.connect();
			pool = poolName;
			ioContext = rados.ioCtxCreate(pool);
			rbd = new Rbd(ioContext);
		} catch (RadosException e) {
			disconnect();
			LOG.warn("Unable to connect to Ceph cluster", e);
			throw new EucalyptusCephException("Failed to connect to pool " + pool
					+ " in Ceph cluster. Verify Ceph cluster health, privileges of Ceph user assigned to Eucalyptus, Ceph parameters configured in Eucalyptus "
					+ config.toString() + " and retry operation", e);
		}
	}

	public Rados getRados() {
		return rados;
	}

	public IoCTX getIoContext() {
		return ioContext;
	}

	public Rbd getRbd() {
		return rbd;
	}

	public String getPool() {
		return pool;
	}

	public void disconnect() {
		if (rados != null && ioContext != null) {
			rados.ioCtxDestroy(ioContext);
			rados = null;
			ioContext = null;
			rbd = null;
			pool = null;
		}
	}

	public static CephRbdConnectionManager getConnection(CephRbdInfo config, String poolName) {
		return new CephRbdConnectionManager(config, poolName);
	}

	public static CephRbdConnectionManager getRandomVolumePoolConnection(CephRbdInfo config) {
		String[] allPools = getAllVolumePools(config);
		// TODO Implement evaluating strategy to pick a pool. Using a random pool for now
		return new CephRbdConnectionManager(config, allPools[randomGenerator.nextInt(allPools.length)]);
	}

	public static CephRbdConnectionManager getRandomSnapshotPoolConnection(CephRbdInfo config) {
		String[] allPools = getAllSnapshotPools(config);
		// TODO Implement evaluating strategy to pick a pool. Using a random pool for now
		return new CephRbdConnectionManager(config, allPools[randomGenerator.nextInt(allPools.length)]);
	}

	public static String[] getAllVolumePools(CephRbdInfo config) {
		String[] allPools = config.getCephVolumePools().split(",");
		if (allPools != null && allPools.length > 0) {
			return allPools;
		} else {
			LOG.warn("No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephvolumepools=<pool-name>");
			throw new EucalyptusCephException(
					"No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephvolumepools=<pool-name>");
		}
	}

	public static String[] getAllSnapshotPools(CephRbdInfo config) {
		String[] allPools = config.getCephSnapshotPools().split(",");
		if (allPools != null && allPools.length > 0) {
			return allPools;
		} else {
			LOG.warn("No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephsnapshotpools=<pool-name>");
			throw new EucalyptusCephException(
					"No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephsnapshotpools=<pool-name>");
		}
	}
}
