/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.troubleshooting.checker;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.eucalyptus.records.Logs;
import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Eucalyptus;

public class MemoryCheck {
	private final static Logger LOG = Logger.getLogger(MemoryCheck.class);

	private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private static final int OUT_OF_MEMORY_FAULT_ID = 1004;
	private final static long DEFAULT_POLL_INTERVAL = 1 * 1000;
	private static final Class <? extends ComponentId> DEFAULT_COMPONENT_ID_CLASS = Eucalyptus.class;

	private MemoryCheck() {

	}

	public static ScheduledFuture<?> start(GarbageCollectionChecker checker) {
		return pool.scheduleWithFixedDelay(checker, 0, checker.pollInterval, TimeUnit.MILLISECONDS);
	}

	// Someone should be calling this, currently no one is. Might be a nice thing to say hello to in the service shutdown hooks. Although might complicate stuff
	// when multiple services using it
	public static void shutdown() {
		pool.shutdownNow();
	}

	public static class GarbageCollectionChecker implements Runnable {
		private double ratio;
		private long pollInterval;
		private Class <? extends ComponentId> componentIdClass;

		private boolean alreadyFaulted = false;

		public GarbageCollectionChecker(double ratio) {
			this.ratio = ratio;
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentIdClass = DEFAULT_COMPONENT_ID_CLASS;
		}

		public GarbageCollectionChecker(double ratio, Class<? extends ComponentId> componentIdClass,
				long pollInterval) {
			this.ratio = ratio;
			this.pollInterval = pollInterval;
			this.componentIdClass = componentIdClass;
		}

		@Override
		public void run() {
			List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
			boolean noOldGenBeans = true;
			if (null != beans) {
				for (MemoryPoolMXBean bean : beans) {
					String name = bean.getName();
					if (!name.contains("Old Gen")) continue;
					if (bean.getType() != MemoryType.HEAP) continue;
					noOldGenBeans = false;

					MemoryPoolMXBean oldGen = bean;
					// Look at usage, max available and peak usage collection yield for old gen
					String cmsInfo = getConcurrentMarkSweepInfo();
					MemoryUsage gc = oldGen.getCollectionUsage( );
					MemoryUsage usage = oldGen.getUsage( );
					MemoryUsage peak = oldGen.getPeakUsage( );
					double ogLastMaxRatio = ((double) gc.getUsed()) / ((double) usage.getMax());
					double ogPeakRatio = ((double) usage.getUsed()) / ((double) peak.getUsed());
					double ogLastRatio = ((double) gc.getUsed()) / ((double) usage.getUsed());
					double ogLastPeakRatio = ((double) gc.getUsed()) / ((double) peak.getUsed());
					Logs.extreme().debug("Memory info: ogLastMaxRatio = " + ogLastMaxRatio + ", ogPeakRatio = " + ogLastRatio + ", ogLastRatio = " + ogLastMaxRatio + ", ogLastPeakRatio = " + ogLastPeakRatio + ", " + cmsInfo);
					if (ogLastMaxRatio > ratio) {
						if (!alreadyFaulted) {
							Faults.forComponent(Eucalyptus.class).havingId(OUT_OF_MEMORY_FAULT_ID).withVar("component", Eucalyptus.INSTANCE.getFaultLogPrefix()).log();
							alreadyFaulted = true;
						}
					}
				}
			} else {
				// nothing to check
			}
			if (noOldGenBeans) {
				LOG.warn("Unable to find any mxbeans for Old-Gen usage");
			}
		}

		private String getConcurrentMarkSweepInfo() {
			List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans( );
			if (null != beans) {
				for (GarbageCollectorMXBean bean : beans) {
					String name = bean.getName();
//					if (!name.contains("MarkSweep")) continue;
					if (!name.contains("ConcurrentMarkSweep")) continue;
					GarbageCollectorMXBean cms = bean;
				    return "CMS:" + cms.getName() + " " + cms.getCollectionCount() + "/" + cms.getCollectionTime() + "msec";
				}
			}
			return "CMS: no info available (can't find 'ConcurrentMarkSweep' MXBean)";
		}
	}

}
