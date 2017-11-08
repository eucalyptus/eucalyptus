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

import com.eucalyptus.stats.beans.EucaMonitoringMXBean;
import com.eucalyptus.stats.sensors.SensorManagers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.List;


//Don't run automatically, only in manual junit runs from ide, etc.
//Uses local file-system resources that may not be available in an automated CI system
@Ignore
public class SensorsTest {

  @MXBean
  public static class TestSensor implements EucaMonitoringMXBean {
    public TestSensor() {}

    public String getName() {
      return "TestSensor1";
    }

    public Double getMetricValue() {
      return 100.05d;
    }

    public String getState() {
      return null;
    }
  }

  TestSensor fakeSensor;
  ObjectName name;

  @BeforeClass
  public static void preSuiteSetup() {
    System.setProperty("euca.conf.dir", "/opt/eucalyptus/etc/eucalyptus/cloud.d");
    System.setProperty("euca.run.dir", "/opt/eucalyptus/var/run/eucalyptus");
  }

  @Before
  public void setup() throws Exception {
    if(fakeSensor == null) fakeSensor = new TestSensor();
    if(name == null) name = new ObjectName("com.eucalyptus.monitoring:Type=TestSensor,Name=TestSensor1");
    initMbeans();
  }

  @After
  public void tearDown() throws Exception {
    flushMbeans();
  }

  private void initMbeans() throws Exception {
    MBeanServer mbeans = ManagementFactory.getPlatformMBeanServer();
    mbeans.registerMBean(fakeSensor, name);
    for(String dom : mbeans.getDomains()) {
      System.out.println("Found domain: " + dom);
    }
    for(MBeanAttributeInfo attr : mbeans.getMBeanInfo(name).getAttributes()) {
      System.out.println("Found attr: " + attr.getName() + " - " + attr.getType());
    }
  }

  private void flushMbeans() throws Exception {
    MBeanServer mbeans = ManagementFactory.getPlatformMBeanServer();
    mbeans.unregisterMBean(name);
  }


  @Test
  public void basicTest() throws Exception {
    SensorManager sensors = SensorManagers.getInstance();
    sensors.init(new EventEmitterService());
    sensors.start();
    sensors.pollAll(); //force execution now
    List<SystemMetric> metrics = sensors.getMetrics();

    for(SystemMetric m : metrics) {
      assert(m != null && m.getSensor() != null);
      System.out.println(m.toString());
    }
    sensors.stop();
  }

  @Test
  public void threadTest() throws Exception {
    SensorManager sensors = SensorManagers.getInstance();
    sensors.init(new EventEmitterService());
    sensors.start();
    System.out.println("Sleeping to wait for sensor data");
    Thread.sleep(20 * 1000l); //sleep for 2x the interval of 10s.
    System.out.println("Reading data");
    List<SystemMetric> metrics = sensors.getMetrics();

    for(SystemMetric m : metrics) {
      assert(m != null && m.getSensor() != null);
      System.out.println(m.toString());
    }
    sensors.stop();
  }

  @Test
  public void serviceTest() throws Exception {
    SensorManager sensors = SensorManagers.getInstance();
    sensors.init(EventEmitterService.getInstance());
    sensors.start();

    //Wait for execution
    Thread.sleep(60 * 1000l);
    sensors.stop();
  }

}