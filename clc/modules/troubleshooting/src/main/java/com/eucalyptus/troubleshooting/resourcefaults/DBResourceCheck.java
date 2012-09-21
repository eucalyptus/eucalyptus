package com.eucalyptus.troubleshooting.resourcefaults;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ConnectionPoolDefinitionIF;
import org.logicalcobwebs.proxool.ConnectionPoolStatisticsIF;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.admin.SnapshotIF;

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

public class DBResourceCheck extends Thread {
	private final static Logger LOG = Logger.getLogger(DBResourceCheck.class);
	private final static long POLL_TIME = 5 * 1000; 
	@Override
	public void run() {
		while (true) {
			try {
			PrintWriter out = new PrintWriter(new FileWriter(BaseDirectory.LOG.getChildFile("db-stats"), true));
			LOG.info("Number of db aliases = " + ((ProxoolFacade.getAliases() != null) ? -1 : ProxoolFacade.getAliases().length));
			for (String alias: ProxoolFacade.getAliases()) {
				ConnectionPoolDefinitionIF definition = ProxoolFacade.getConnectionPoolDefinition(alias);
				SnapshotIF statistics = ProxoolFacade.getSnapshot(alias, true);
				out.println("alias="+definition.getAlias() + ",maxConnectionCount="+definition.getMaximumConnectionCount());
				out.println("activeConnectionCount="+statistics.getActiveConnectionCount() + ",availableConnectionCount="+statistics.getAvailableConnectionCount() + ",connectionCount="+statistics.getConnectionCount());
			}
			out.close();
			} catch (Exception ex) {
				LOG.error(ex);
			}
			try {
				Thread.sleep(POLL_TIME);
			} catch (InterruptedException ex) {
				LOG.warn("Polling thread interrupted");
			}
		}
	}
	
}
