/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ObjectStorageManagerTest {

  @Test
  public void extractIntervalTest() throws Exception {
    String jobSchedule = "interval: 500000 ";
    jobSchedule = jobSchedule.substring("interval:".length(), jobSchedule.length());
    int intervalSecs = -1;
    try {
      intervalSecs = Integer.parseInt(jobSchedule.trim());
    } catch (NumberFormatException nfe) {
      assertTrue("failed to parse number", false);
    }
    assertTrue("expected parsing to result in 500000", intervalSecs == 500000);
  }

}
