package com.eucalyptus.blockstorage.ceph;

import java.io.File;

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
 * 
 * TODO: Implement methods to establish connection to a specific pool and picking a pool based on some strategy
 */
public class CephConnectionManager {

	private static Logger LOG = Logger.getLogger(CephConnectionManager.class);

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

	public static CephConnectionManager getConnection(CephInfo config) {
		String[] allPools = config.getCephPools().split(",");
		if (allPools != null && allPools.length > 0) {
			// TODO implement evaluating the pool strategy and pools to pick the pool. Establish a connection to that pool
			// TODO Using the first pool for now, needs to be changed
			return new CephConnectionManager(config, allPools[0]);
		} else {
			LOG.warn("No ceph pools defined, not sure what to do here");
			throw new EucalyptusCephException("No ceph pools defined");
		}
	}

	public static CephConnectionManager getConnection(CephInfo config, String poolName) {
		return new CephConnectionManager(config, poolName);
	}
}
