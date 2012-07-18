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

package edu.ucsb.eucalyptus.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;


public class EucaSemaphoreDirectory {
	private static ConcurrentHashMap<String, EucaSemaphore> semaphoreMap = new ConcurrentHashMap<String, EucaSemaphore>();

	public static EucaSemaphore getSemaphore(String key) {
		EucaSemaphore semaphore = semaphoreMap.putIfAbsent(key, new EucaSemaphore(Integer.MAX_VALUE));
		if (semaphore == null) {
			semaphore = semaphoreMap.get(key);
		}
		return semaphore;
	}

	public static EucaSemaphore getSemaphore(String key, int number) {
		EucaSemaphore semaphore = semaphoreMap.putIfAbsent(key, new EucaSemaphore(number));
		if (semaphore == null) {
			semaphore = semaphoreMap.get(key);
		}
		return semaphore;
	}

	public static EucaSemaphore getSolitarySemaphore(String key) {
		EucaSemaphore semaphore = semaphoreMap.putIfAbsent(key, new EucaSemaphore(1));
		if (semaphore == null) {
			semaphore = semaphoreMap.get(key);
		}
		return semaphore;
	}

	public static void removeSemaphore(String key) {
		if(semaphoreMap.containsKey(key)) {
			semaphoreMap.remove(key);
		}
	}
}
