/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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