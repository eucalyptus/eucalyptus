/*
 * Copyright 2009-$year Eucalyptus Systems, Inc.
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
 */

package com.eucalyptus.stats;

import com.eucalyptus.stats.beans.EucaMonitoringMXBean;
import com.eucalyptus.stats.sensors.SensorManagers;
import net.sf.hajdbc.util.SystemProperties;
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
    SystemProperties.setSystemProperty("euca.conf.dir", "/opt/eucalyptus/etc/eucalyptus/cloud.d");
    SystemProperties.setSystemProperty("euca.run.dir", "/opt/eucalyptus/var/run/eucalyptus");
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