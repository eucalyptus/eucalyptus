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

package com.eucalyptus.bootstrap;

import com.eucalyptus.component.ComponentId;

public class ShutdownHook implements Comparable<ShutdownHook>{

	private Runnable runnable;
	private ComponentId componentId;

	public ShutdownHook(ComponentId id, Runnable r) {
		componentId = id;
		runnable = r;
	}
	
	public ShutdownHook(Runnable r) {		
		runnable = r;
	}

	@Override
	public int compareTo(ShutdownHook o) {
		if (o.getComponentId() == null) {
			return -1;
		}
		ComponentId id = o.getComponentId();
		if(!componentId.isAlwaysLocal() && !componentId.isCloudLocal())
			return -1;
		if(componentId.isCloudLocal()) {
			if(!id.isCloudLocal() && !id.isAlwaysLocal()) {
				return 1;
			} else {
				return -1;
			}
		}
		if(componentId.isAlwaysLocal()) {
			if(!id.isCloudLocal() && !id.isAlwaysLocal()) {
				return 1;
			} else if(id.isCloudLocal()) {
				return 1;
			} else {
				return -1;
			}
		}		
		return 0;
	}

	public Runnable getRunnable() {
		return runnable;
	}

	public ComponentId getComponentId() {
		return componentId;
	}
	
}
