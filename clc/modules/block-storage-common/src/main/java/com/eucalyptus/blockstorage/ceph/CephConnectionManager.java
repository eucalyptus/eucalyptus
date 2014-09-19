package com.eucalyptus.blockstorage.ceph;

import java.io.File;
import java.util.Random;

import org.apache.log4j.Logger;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rbd.Rbd;
import com.eucalyptus.blockstorage.ceph.entities.CephInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;

/**
 * Utility class for creating and managing connection to a pool in a Ceph cluster. Encapsulates all elements (Rados, IoCTX, Rbd, pool) of a ceph connection if
 * the caller needs access to a specific element.
 */
public class CephConnectionManager {

	private static Logger LOG = Logger.getLogger(CephConnectionManager.class);
	private static Random randomGenerator = new Random();

	private Rados rados;
	private IoCTX ioContext;
	private Rbd rbd;
	private String pool;

	private CephConnectionManager(CephInfo config, String poolName) {
		rados = new Rados(config.getCephUser());
		try {
			rados.confReadFile(new File(config.getCephConfigFile()));
			rados.connect();
			pool = poolName;
			ioContext = rados.ioCtxCreate(pool);
		} catch (RadosException e) {
			LOG.error("Caught error while establishing connection to ceph cluster", e);
			throw new EucalyptusCephException("Failed to establish connection to ceph cluster", e);
		}
		rbd = new Rbd(ioContext);
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

	public static CephConnectionManager getConnection(CephInfo config, String poolName) {
		return new CephConnectionManager(config, poolName);
	}

	public static CephConnectionManager getRandomVolumePoolConnection(CephInfo config) {
		String[] allPools = getAllVolumePools(config);
		// TODO Implement evaluating strategy to pick a pool. Using a random pool for now
		return new CephConnectionManager(config, allPools[randomGenerator.nextInt(allPools.length)]);
	}

	public static CephConnectionManager getRandomSnapshotPoolConnection(CephInfo config) {
		String[] allPools = getAllSnapshotPools(config);
		// TODO Implement evaluating strategy to pick a pool. Using a random pool for now
		return new CephConnectionManager(config, allPools[randomGenerator.nextInt(allPools.length)]);
	}

	public static String[] getAllVolumePools(CephInfo config) {
		String[] allPools = config.getCephVolumePools().split(",");
		if (allPools != null && allPools.length > 0) {
			return allPools;
		} else {
			LOG.warn("No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephvolumepools=<pool-name>");
			throw new EucalyptusCephException(
					"No ceph pools defined, retry after defining at least one pool using euca-modify-property -p <cluster>.storage.cephvolumepools=<pool-name>");
		}
	}

	public static String[] getAllSnapshotPools(CephInfo config) {
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
