/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.stats;

import com.google.common.collect.Maps;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

//Don't run automatically, only in manual junit runs from ide, etc.
//Uses local file-system resources that may not be available in an automated CI system
@Ignore
public class EventEmitterServiceTest {

  private SystemMetric eventGen(String name) {
    Map<String, Object> testResults = Maps.newHashMap();
    testResults.put("fakekeyString","fakeresult1");
    testResults.put("fakekeyInteger", 100);
    testResults.put("fakekeyDouble", 150.50);
    return new SystemMetric(name,
        null,
        "description is here",
        testResults,
        100l);
  }

  @Test
  public void basicMonitoringServiceTest() throws Exception {
    EventEmitterService service = new EventEmitterService();
    service.start();

    for (int i = 0; i < 100; i++) {
      if(!service.offer(eventGen("testservice" + i))) {
        throw new Exception("Error submitting event");
      }
    }

    service.softStop();
  }
}