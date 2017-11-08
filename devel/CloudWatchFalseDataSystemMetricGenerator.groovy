package com.eucalyptus.cloudwatch;
/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType
import com.eucalyptus.cluster.callback.reporting.AbsoluteMetricQueue;
import com.eucalyptus.cluster.callback.reporting.CloudWatchHelper;
import com.eucalyptus.cluster.callback.reporting.CloudWatchHelper.InstanceInfoProvider;
import com.eucalyptus.component.ServiceConfiguration;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponse;
import edu.ucsb.eucalyptus.msgs.MetricCounterType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsValuesType;
import edu.ucsb.eucalyptus.msgs.MetricsResourceType;
import edu.ucsb.eucalyptus.msgs.SensorsResourceType;
import com.eucalyptus.cluster.callback.reporting.AbsoluteMetricQueueItem
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * CloudWatchFalseDataSystemMetricGenerator is a class that generates EBS and EC2 metrics for several instances.
 * Tweakable parameters include number of instances, duration (in minutes), and number of milliseconds between "puts". 
 * In addition, instance information can be tweaked, including metric values.  Default values are given as well.
 *
 * The easiest way to run a test is as follows
 * <code>
 * CloudWatchFalseDataSystemMetricGenerator test = new CloudWatchFalseDataSystemMetricGenerator();
 * test.start(); 
 * </code>
 */
public class CloudWatchFalseDataSystemMetricGenerator {

  /**
   * MetricValue is a class that supplies a value to a metric at a given future point.
   */
  private static abstract class MetricValue {
    /**
     * @param n number of 'puts' after the initial put
     * @return value of the function
     */
    public abstract double getValue(long n);
  }

  /**
   * ConstantValue is a MetricValue class that has a constant difference between successive .getValue() calls.
   * (i.e. .getValue(n+1) - .getValue(n) = constant
   */
  private static class ConstantValue extends MetricValue {
    /**
     * Constructor
     * @param constantValue the constant difference between .getValue(n+1) and .getValue(n)
     */
    public ConstantValue(double constantValue) {
      this.constantValue = constantValue;
    }

    private double constantValue;

    @Override
    public double getValue(long n) {
      return constantValue * n;
    }
  }

  /**
   * IncreasingValue is a MetricValue class whose difference between successive .getValue() calls is increasing at a constant rate
   * (i.e. .getValue(1) - .getValue(0) = startVal, .getValue(2) - .getValue(1) = startVal + increment,
   * .getValue(3) - .getValue(2) = startVal + 2 * increment, etc.
   */
  private static class IncreasingValue extends MetricValue {

    private double startVal;
    private double increment;
    /**
     * Constructor
     * @param startVal the value of .getValue(1) - .getValue(0)
     * @param increment the amount each successive difference changes by
     */
    public IncreasingValue(double startVal, double increment) {
      this.startVal = startVal;
      this.increment = increment;
    }

    @Override
    public double getValue(long n) {
      // This is derived from some algebra.
      // Given a function f(n) such that f(n+1) - f(n) = c + dn, where c = startVal, and d = increment
      // We try a given f(n) = a*n^2 + bn (don't need a constant as the differences will cancel), and that works where
      // a = d/2 and b = c - d/2 [You can check that]
      double c = startVal;
      double d = increment;
      double a = d/2;
      double b = c - d/2;
      return n * (a * n + b);
    }
  }

  /**
   * IncreasingBoundedValue is a MetricValue class whose difference between successive .getValue() calls is increasing at a constant rate until
   * the difference hits the upper bound, at which case all future differences are that upper bound...
   * (i.e. .getValue(1) - .getValue(0) = startVal, .getValue(2) - .getValue(1) = startVal + increment,
   * .getValue(3) - .getValue(2) = startVal + 2 * increment, etc... eventually
   * .getValue(n+1) - .getValue(n) = upperBound
   */
  private static class IncreasingBoundedValue extends MetricValue {
    private double startVal;
    private double increment;
    private double upperBound;

    /**
     * Constructor
     * @param startVal the value of .getValue(1) - .getValue(0)
     * @param increment the amount each successive difference changes by
     * @param upperBound the constant (final) value .getValue(n+1) - .getValue(n) eventually becomes
     */
    public IncreasingBoundedValue(double startVal, double increment,
                                  double upperBound) {
      this.startVal = startVal;
      this.increment = increment;
      this.upperBound = upperBound;
      this.initial = new IncreasingValue(startVal, increment);
    }

    private IncreasingValue initial;

    @Override
    public double getValue(long n) {
      // We want a function f(n) such than f(n+1) - f(n) = max(c + dn, ub), where ub is an upper bound.
      // c = startVal, and d = increment
      double c = startVal;
      double d = increment;
      long maxN = (long) Math.ceil(((upperBound - c) / d));
      if (n <= maxN) {
        return initial.getValue(n);
      } else {
        return initial.getValue(maxN) + (n - maxN) * upperBound;
      }
    }
  }

  /**
   * DecreasingValue is a MetricValue class whose difference between successive .getValue() calls is decreasing at a constant rate
   * (i.e. .getValue(1) - .getValue(0) = startVal, .getValue(2) - .getValue(1) = startVal - increment,
   * .getValue(3) - .getValue(2) = startVal - 2 * increment, etc.
   *
   * Please make sure if you use this class you don't let it run long enough for the differences between values
   * to become negative.  AbsoluteMetric stats are cumulative, negative differences imply instance restarts, so
   * unpredictable results may occur.  DecreasingValueBounded with a lower bound of 0 may be a better choice.
   */
  private static class DecreasingValue extends MetricValue {

    private IncreasingValue opposite;

    /**
     * Constructor
     * @param startVal the value of .getValue(1) - .getValue(0)
     * @param decrement the amount each successive difference changes by
     */
    public DecreasingValue(double startVal, double decrement) {
      this.opposite = new IncreasingValue(startVal, -decrement);
    }

    @Override
    public double getValue(long n) {
      return opposite.getValue(n);
    }
  }

  /**
   * DecreasingBoundedValue is a MetricValue class whose difference between successive .getValue() calls is decreasing at a constant rate until
   * the difference hits the lower bound, at which case all future differences are that lower bound...
   * (i.e. .getValue(1) - .getValue(0) = startVal, .getValue(2) - .getValue(1) = startVal - decrement,
   * .getValue(3) - .getValue(2) = startVal - 2 * decrement, etc... eventually
   * .getValue(n+1) - .getValue(n) = lowerBound
   *
   * The lowerBound should be non-negative.
   */
  private static class DecreasingBoundedValue extends MetricValue {

    private IncreasingBoundedValue opposite;

    /**
     * Constructor
     * @param startVal the value of .getValue(1) - .getValue(0)
     * @param decrement the amount each successive difference changes by
     * @param lowerBound the constant (final) value .getValue(n+1) - .getValue(n) eventually becomes
     */
    public DecreasingBoundedValue(double startVal, double decrement, double lowerBound) {
      this.opposite = new IncreasingBoundedValue(startVal, -decrement, lowerBound);
    }

    @Override
    public double getValue(long n) {
      return opposite.getValue(n);
    }
  }

  /**
   * OscillatingValue is a MetricValue class whose difference between successive .getValue() 
   * calls oscillates between minVal and maxVal at a constant rate for a given period.
   * Period should be even, say p, then 
   * .getValue(1) - .getValue(0) = minVal
   * .getValue(p/2 + 1) - .getValue(p/2) = maxVal
   * .getValue(p + 1) - .getValue(p) = minVal
   * .etc
   */
  private static class OscillatingValue extends MetricValue {

    private long periodLength;
    private IncreasingValue goingUp;
    private DecreasingValue goingDown;

    /**
     * Constructor
     *
     * @param minVal the min val
     * @param maxVal the max val
     * @param periodLength the period length
     */
    public OscillatingValue(double minVal, double maxVal, long periodLength) {
      double increment = (maxVal - minVal) / (periodLength / 2);
      this.goingUp = new IncreasingValue(minVal, increment);
      this.goingDown = new DecreasingValue(maxVal, increment);
    }

    public double getValue(long n) {
      long numWholePeriods = n / periodLength;
      double valueAtBeginningOfPeriod = numWholePeriods * (goingUp.getValue(periodLength / 2) + goingDown.getValue(periodLength / 2));
      long nLocal = n % periodLength;
      if (nLocal <= (periodLength / 2)) {
        return valueAtBeginningOfPeriod + goingUp.getValue(nLocal);
      } else {
        return valueAtBeginningOfPeriod + goingUp.getValue(periodLength / 2) + goingDown.getValue(nLocal - periodLength / 2);
      }
    }
  }

  /**
   * CPUUtilization is calculated differently than other metrics.  It is not simply the difference between successive values, it is 
   * the difference between successive values divided by elapsed time, times 100.  This MetricValue class wraps other MetricValue
   * classes and converts the values for use in CPUUtilization
   */
  private static class CPUUtilizationValue extends MetricValue {

    private MetricValue original;
    private long msPerPeriod;
    /** Constructor
     * @param original the original MetricValue
     * @param msPerPeriod the number of milliseconds per period
     */
    public CPUUtilizationValue(MetricValue original,
                               long msPerPeriod) {
      this.original = original;
      this.msPerPeriod = msPerPeriod;
    }

    @Override
    public double getValue(long n) {
      return original.getValue(n) * msPerPeriod / 100.0;
    }
  }

  /**
   * VolumeMetrics encapsulates several metric values for metrics related to volumes.  Volumes with 
   * ids of "root" or "ephemeral-X" will have metric values reported as EC2 metrics.  Volumes with id of
   * "vol-XXX" will be EBS metrics.
   */
  static class VolumeMetrics {

    String volumeId;
    MetricValue diskReadOps;
    MetricValue diskWriteOps;
    MetricValue diskReadBytes;
    MetricValue diskWriteBytes;
    MetricValue volumeQueueLength;
    MetricValue volumeTotalReadTime;
    MetricValue volumeTotalWriteTime;
  }

  /**
   * InstanceMetrics encapsulates several metric values for metrics related to instances.
   */
  static class InstanceMetrics {
    MetricValue cpuUtilization;
    MetricValue networkIn;
    MetricValue networkOut;

  }

  /**
   * Instance encapsulates information about instances, including uuid, instanceId, vmType, imageId, whether "monitoring" is on, and all
   * MetricValues associated with the instance.
   */
  static class Instance {

    String uuid;
    String instanceId;
    String imageId;
    String vmType;
    boolean monitoring;
    String accountNumber;
    String autoscalingGroupName;
    InstanceMetrics metrics;
    List<VolumeMetrics> volumeMetricsList;
    Integer statusCheckedFailed;
    Integer instanceStatusCheckedFailed;
    Integer systemStatusCheckedFailed;

  }

  /**
   * MockInstanceInfoProvider is an InstanceInfoProvider whose InstanceInfo is contained within the objects.
   */
  private static class MockInstanceInfoProvider implements InstanceInfoProvider {

    private Map<String, Instance> instanceMap;
    private Iterable<String> uuidList;

    /**
     * Constructor
     *
     * @param instanceMap map of instance objects (keyed off of instance id)
     * @param uuidList the uuid list (list of "running" instance uuids)
     */
    public MockInstanceInfoProvider(
      Map<String, Instance> instanceMap, Iterable<String> uuidList) {
      super();
      this.instanceMap = instanceMap;
      this.uuidList = uuidList;
    }

    @Override
    public Iterable<String> getRunningInstanceUUIDList() {
      return uuidList;
    }

    @Override
    public String getAutoscalingGroupName(String instanceId) {
      return instanceMap.get(instanceId).getAutoscalingGroupName();
    }

    @Override
    public String getInstanceId(String instanceId) {
      return instanceMap.get(instanceId).getInstanceId();
    }

    @Override
    public String getImageId(String instanceId) {
      return instanceMap.get(instanceId).getImageId();
    }

    @Override
    public String getVmTypeDisplayName(String instanceId) {
      return instanceMap.get(instanceId).getVmType();
    }

    @Override
    public String getAccountNumber(String instanceId) throws Exception {
      return instanceMap.get(instanceId).getAccountNumber();
    }

    @Override
    public Integer getStatusCheckFailed(String instanceId) {
      return instanceMap.get(instanceId).getStatusCheckedFailed();
    }

    @Override
    public Integer getInstanceStatusCheckFailed(String instanceId) {
      return instanceMap.get(instanceId).getInstanceStatusCheckedFailed();
    }

    @Override
    public Integer getSystemStatusCheckFailed(String instanceId) {
      return instanceMap.get(instanceId).getSystemStatusCheckedFailed();
    }

    @Override
    public boolean getMonitoring(String instanceId) {
      return instanceMap.get(instanceId).isMonitoring();
    }
  }

  static final Logger LOG = Logger.getLogger(CloudWatchFalseDataSystemMetricGenerator.class);

  private static final long DEFAULT_NUM_MILLISECONDS_BETWEEN_PUTS = 60000L;
  private static final int DEFAULT_NUM_INSTANCES = 400;
  private static final long DEFAULT_DURATION_MINS = 120L;
  private static final int FOLD_NUMBER = 1;

  private List<String> uuidList = Lists.newArrayList();
  private Map<String, Instance> instanceMap = Maps.newHashMap();
  private MockInstanceInfoProvider mockInstanceInfoProvider = new MockInstanceInfoProvider(instanceMap, uuidList);


  private long durationMins;
  private long numMillisecondsBetweenPuts;

  public CloudWatchFalseDataSystemMetricGenerator() {
    this.durationMins = DEFAULT_DURATION_MINS;
    this.numMillisecondsBetweenPuts = DEFAULT_NUM_MILLISECONDS_BETWEEN_PUTS;
  }


  public void setDurationMins(long durationMins) {
    this.durationMins = durationMins;
  }

  public void setNumMillisecondsBetweenPuts(long numMillisecondsBetweenPuts) {
    this.numMillisecondsBetweenPuts = numMillisecondsBetweenPuts;
  }

  /**
   * Sets up a number of default instance values
   */
  public synchronized void setupDefaultInstances() throws Exception {
    setupDefaultInstances(DEFAULT_NUM_INSTANCES);
  }

  /**
   * Sets up a number of default instance values
   * @param numInstances number of instances to set up
   */
  public synchronized void setupDefaultInstances(long numInstances) throws Exception {
    final int NUM_EBS_VOLUMES_PER_INSTANCE = 2;
    final double MB_PER_SEC = 1024.0 * 1024.0 / 10000.0;
    DecimalFormat df = new DecimalFormat("00000");
    String adminAccountId = Accounts.lookupSystemAdmin().getAccountNumber();
    for (long i=0L;i<numInstances;i++) {
      String uuid = "00000000-0000-0000-0000-0000000" + df.format(i);
      String instanceId = "i-"+df.format(i);
      String vmType = "m1.fake";
      String imageId = "emi-"+df.format(i);
      Instance instance = new Instance();
      instance.setUuid(uuid);
      instance.setInstanceId(instanceId);
      instance.setImageId(imageId);
      instance.setVmType(vmType);
      instance.setMonitoring(true);
      instance.setAccountNumber(adminAccountId);
      instance.setAutoscalingGroupName(null);
      instance.setStatusCheckedFailed(0);
      instance.setSystemStatusCheckedFailed(0);
      instance.setInstanceStatusCheckedFailed(0);
      InstanceMetrics instanceMetrics = new InstanceMetrics();
      double baseFraction = ((double) i )/ numInstances;
      instanceMetrics.setCpuUtilization(new CPUUtilizationValue(new ConstantValue(baseFraction * 100.0), numMillisecondsBetweenPuts));
      instanceMetrics.setNetworkIn(new ConstantValue(baseFraction * 50 * MB_PER_SEC));
      instanceMetrics.setNetworkOut(new ConstantValue(baseFraction * 70 * MB_PER_SEC));
      instance.setMetrics(instanceMetrics);

      List<VolumeMetrics> allVolumeMetrics = Lists.newArrayList();
      // EC2 Disk Metrics
      VolumeMetrics ephemeralDiskMetrics = new VolumeMetrics();
      ephemeralDiskMetrics.setVolumeId("root");
      ephemeralDiskMetrics.setDiskReadOps(new ConstantValue(baseFraction * 10000));
      ephemeralDiskMetrics.setDiskWriteOps(new ConstantValue(baseFraction * 20000));
      ephemeralDiskMetrics.setDiskReadBytes(new ConstantValue(baseFraction * 25 * MB_PER_SEC));
      ephemeralDiskMetrics.setDiskWriteBytes(new ConstantValue(baseFraction * 35 * MB_PER_SEC));
      allVolumeMetrics.add(ephemeralDiskMetrics);

      // Add some EBS metrics
      for (int j = 0; j < NUM_EBS_VOLUMES_PER_INSTANCE; j++) {
        String volumeId = "vol-" + df.format(i) + j;
        VolumeMetrics ebsMetrics = new VolumeMetrics();
        ebsMetrics.setVolumeId(volumeId);
        ebsMetrics.setDiskReadOps(new ConstantValue(baseFraction * 10000));
        ebsMetrics.setDiskWriteOps(new ConstantValue(baseFraction * 20000));
        ebsMetrics.setDiskReadBytes(new ConstantValue(baseFraction * 25 * MB_PER_SEC));
        ebsMetrics.setDiskWriteBytes(new ConstantValue(baseFraction * 35 * MB_PER_SEC));
        ebsMetrics.setVolumeQueueLength(new ConstantValue(0.0));
        ebsMetrics.setVolumeTotalReadTime(new ConstantValue(baseFraction * 2000));
        ebsMetrics.setVolumeTotalWriteTime(new ConstantValue(baseFraction * 1000));
        allVolumeMetrics.add(ebsMetrics);
      }
      instance.setVolumeMetricsList(allVolumeMetrics);
      addInstance(instance);
    }
  }
  /**
   * Adds a specific instance to the test
   * @param instance instance to add
   */
  public synchronized void addInstance(Instance instance) {
    instanceMap.put(instance.getInstanceId(), instance);
    uuidList.add(instance.getUuid());
  }
  /**
   * Runs the test
   * @throws Exception if something goes wrong
   */
  public synchronized void runMyTest() throws Exception {
    if (instanceMap.isEmpty()) {
      setupDefaultInstances();
    }
    Date startTime = new Date();
    CloudWatchHelper cloudWatchHelper = new CloudWatchHelper(mockInstanceInfoProvider);
    long numPuts = durationMins * 60000L / numMillisecondsBetweenPuts;
    UserPrincipal admin = Accounts.lookupSystemAdmin();
    long convertTime = 0L;
    for (long putNum=0;putNum<numPuts;putNum++) {
      Date currentTime = new Date(startTime.getTime() + numMillisecondsBetweenPuts * putNum);
      LOG.fatal("Started putting " + instanceMap.size() + " instance data (fold number = " + FOLD_NUMBER + ", total mins = " + DEFAULT_DURATION_MINS + ") for time:" + currentTime);
      long before = System.currentTimeMillis();
      int numTogether = 0;
      long numPoints = 0L;
      List<AbsoluteMetricQueueItem> absoluteMetricQueueItemList = new ArrayList<AbsoluteMetricQueueItem>();
      for (String instanceId: instanceMap.keySet()) {
        DescribeSensorsResponse msg = new DescribeSensorsResponse();
        msg.setUser(admin);
        ArrayList<SensorsResourceType> sensorResources = new ArrayList<SensorsResourceType>();
        Instance instance = instanceMap.get(instanceId);
        sensorResources.add(generateInstanceSensorResource(instance, currentTime, putNum));
        msg.setSensorsResources(sensorResources );
        absoluteMetricQueueItemList.addAll(cloudWatchHelper.collectMetricData(Collections.singleton(instanceId), msg));
        numTogether = numTogether + 1;
        if (numTogether == FOLD_NUMBER) {
          AbsoluteMetricQueue.getInstance().addQueueItems(absoluteMetricQueueItemList);
          numTogether = 0;
          absoluteMetricQueueItemList.clear()
        }
        //LOG.info("Putting data for instance :" + instanceId);
      }
      absoluteMetricQueueItemList.clear()
      numTogether = 0;
      long after = System.currentTimeMillis();
      LOG.fatal("Finished putting " + instanceMap.size() + " instance data ( " + numPoints + " points) for time:" + currentTime);
      LOG.fatal("Processing time was " + (after - before) + "ms");
      long remainingTime = numMillisecondsBetweenPuts - (after - before);
      if (remainingTime > 0) {
        Thread.sleep(remainingTime);
      }
    }
  }


  /**
   * Generate XML snippet
   * <code>
   * &lt;euca:item&gt;
   *   &lt;euca:resourceName&gt;INSTANCE_ID&lt;/euca:resourceName&gt;
   *   &lt;euca:resourceType&gt;instance&lt;/euca:resourceType&gt;
   *   &lt;euca:resourceUuid&gt;INSTANCE_UUID&lt;/euca:resourceUuid&gt;
   *   &lt;euca:metrics&gt;
   *     ...
   *   &lt;/euca:metrics&gt;
   * &lt;/euca:item&gt;
   * </code>
   * that goes inside a &lt;euca:sensorsResources&gt; block.
   * @param instance the instance
   * @param timestamp the timestamp
   * @param stepNum the step number
   * @return the XML Snippet (SensorsResourceType)
   */
  private SensorsResourceType generateInstanceSensorResource(Instance instance, Date timestamp, long stepNum) {
    SensorsResourceType sensorResource = new SensorsResourceType();
    sensorResource.setResourceName(instance.getInstanceId());
    sensorResource.setResourceType("instance");
    sensorResource.setResourceUuid(instance.getUuid());
    ArrayList<MetricsResourceType> metrics= new ArrayList<MetricsResourceType>();

    // Add CPUUtilization
    if (instance.getMetrics() != null && instance.getMetrics().getCpuUtilization() != null) {
      MetricDimensionsValuesType value = createValue(timestamp, instance.getMetrics().getCpuUtilization().getValue(stepNum));
      MetricDimensionsType dimension = createDimension("default", 1L, Lists.newArrayList(value));
      MetricsResourceType metric = createMetricResource("CPUUtilization", numMillisecondsBetweenPuts, "summation", Lists.newArrayList(dimension));
      metrics.add(metric);
    }


    // Add Network In
    if (instance.getMetrics() != null && instance.getMetrics().getNetworkIn() != null) {
      MetricDimensionsValuesType value = createValue(timestamp, instance.getMetrics().getNetworkIn().getValue(stepNum));
      MetricDimensionsType dimension = createDimension("total", 1L, Lists.newArrayList(value));
      MetricsResourceType metric = createMetricResource("NetworkIn", numMillisecondsBetweenPuts, "summation", Lists.newArrayList(dimension));
      metrics.add(metric);
    }

    // Add Network Out
    if (instance.getMetrics() != null && instance.getMetrics().getNetworkOut() != null) {
      MetricDimensionsValuesType value = createValue(timestamp, instance.getMetrics().getNetworkOut().getValue(stepNum));
      MetricDimensionsType dimension = createDimension("total", 1L, Lists.newArrayList(value));
      MetricsResourceType metric = createMetricResource("NetworkOut", numMillisecondsBetweenPuts, "summation", Lists.newArrayList(dimension));
      metrics.add(metric);
    }

    // Add disk metrics

    // Add DiskReadOps (ephemeral and ebs)
    if (instance.getVolumeMetricsList() != null) {
      ArrayList<MetricDimensionsType> dimensions = Lists.newArrayList();
      for (VolumeMetrics volumeMetrics: instance.getVolumeMetricsList()) {
        if (volumeMetrics != null && volumeMetrics.getDiskReadOps() != null) {
          MetricDimensionsValuesType value = createValue(timestamp, volumeMetrics.getDiskReadOps().getValue(stepNum));
          MetricDimensionsType dimension = createDimension(volumeMetrics.getVolumeId(), 1L, Lists.newArrayList(value));
          dimensions.add(dimension);
        }
      }
      if (dimensions != null && !dimensions.isEmpty()) {
        MetricsResourceType metric = createMetricResource("DiskReadOps", numMillisecondsBetweenPuts, "summation", dimensions);
        metrics.add(metric);
      }
    }

    // Add DiskWriteOps (ephemeral and ebs)
    if (instance.getVolumeMetricsList() != null) {
      ArrayList<MetricDimensionsType> dimensions = Lists.newArrayList();
      for (VolumeMetrics volumeMetrics: instance.getVolumeMetricsList()) {
        if (volumeMetrics != null && volumeMetrics.getDiskWriteOps() != null) {
          MetricDimensionsValuesType value = createValue(timestamp, volumeMetrics.getDiskWriteOps().getValue(stepNum));
          MetricDimensionsType dimension = createDimension(volumeMetrics.getVolumeId(), 1L, Lists.newArrayList(value));
          dimensions.add(dimension);
        }
      }
      if (dimensions != null && !dimensions.isEmpty()) {
        MetricsResourceType metric = createMetricResource("DiskWriteOps", numMillisecondsBetweenPuts, "summation", dimensions);
        metrics.add(metric);
      }
    }

    // Add DiskReadBytes (ephemeral and ebs)
    if (instance.getVolumeMetricsList() != null) {
      ArrayList<MetricDimensionsType> dimensions = Lists.newArrayList();
      for (VolumeMetrics volumeMetrics: instance.getVolumeMetricsList()) {
        if (volumeMetrics != null && volumeMetrics.getDiskReadBytes() != null) {
          MetricDimensionsValuesType value = createValue(timestamp, volumeMetrics.getDiskReadBytes().getValue(stepNum));
          MetricDimensionsType dimension = createDimension(volumeMetrics.getVolumeId(), 1L, Lists.newArrayList(value));
          dimensions.add(dimension);
        }
      }
      if (dimensions != null && !dimensions.isEmpty()) {
        MetricsResourceType metric = createMetricResource("DiskReadBytes", numMillisecondsBetweenPuts, "summation", dimensions);
        metrics.add(metric);
      }
    }

    // Add DiskWriteBytes (ephemeral and ebs)
    if (instance.getVolumeMetricsList() != null) {
      ArrayList<MetricDimensionsType> dimensions = Lists.newArrayList();
      for (VolumeMetrics volumeMetrics: instance.getVolumeMetricsList()) {
        if (volumeMetrics != null && volumeMetrics.getDiskWriteBytes() != null) {
          MetricDimensionsValuesType value = createValue(timestamp, volumeMetrics.getDiskWriteBytes().getValue(stepNum));
          MetricDimensionsType dimension = createDimension(volumeMetrics.getVolumeId(), 1L, Lists.newArrayList(value));
          dimensions.add(dimension);
        }
      }
      if (dimensions != null && !dimensions.isEmpty()) {
        MetricsResourceType metric = createMetricResource("DiskWriteBytes", numMillisecondsBetweenPuts, "summation", dimensions);
        metrics.add(metric);
      }
    }

    // Add VolumeTotalReadTime (ebs)
    if (instance.getVolumeMetricsList() != null) {
      ArrayList<MetricDimensionsType> dimensions = Lists.newArrayList();
      for (VolumeMetrics volumeMetrics: instance.getVolumeMetricsList()) {
        if (volumeMetrics != null && volumeMetrics.getVolumeTotalReadTime() != null) {
          MetricDimensionsValuesType value = createValue(timestamp, volumeMetrics.getVolumeTotalReadTime().getValue(stepNum));
          MetricDimensionsType dimension = createDimension(volumeMetrics.getVolumeId(), 1L, Lists.newArrayList(value));
          dimensions.add(dimension);
        }
      }
      if (dimensions != null && !dimensions.isEmpty()) {
        MetricsResourceType metric = createMetricResource("VolumeTotalReadTime", numMillisecondsBetweenPuts, "summation", dimensions);
        metrics.add(metric);
      }
    }

    // Add VolumeTotalWriteTime (ebs)
    if (instance.getVolumeMetricsList() != null) {
      ArrayList<MetricDimensionsType> dimensions = Lists.newArrayList();
      for (VolumeMetrics volumeMetrics: instance.getVolumeMetricsList()) {
        if (volumeMetrics != null && volumeMetrics.getVolumeTotalWriteTime() != null) {
          MetricDimensionsValuesType value = createValue(timestamp, volumeMetrics.getVolumeTotalWriteTime().getValue(stepNum));
          MetricDimensionsType dimension = createDimension(volumeMetrics.getVolumeId(), 1L, Lists.newArrayList(value));
          dimensions.add(dimension);
        }
      }
      if (dimensions != null && !dimensions.isEmpty()) {
        MetricsResourceType metric = createMetricResource("VolumeTotalWriteTime", numMillisecondsBetweenPuts, "summation", dimensions);
        metrics.add(metric);
      }
    }

    // Add VolumeQueueLength (ebs)
    if (instance.getVolumeMetricsList() != null) {
      ArrayList<MetricDimensionsType> dimensions = Lists.newArrayList();
      for (VolumeMetrics volumeMetrics: instance.getVolumeMetricsList()) {
        if (volumeMetrics != null && volumeMetrics.getVolumeQueueLength() != null) {
          MetricDimensionsValuesType value = createValue(timestamp, volumeMetrics.getVolumeQueueLength().getValue(stepNum));
          MetricDimensionsType dimension = createDimension(volumeMetrics.getVolumeId(), 1L, Lists.newArrayList(value));
          dimensions.add(dimension);
        }
      }
      if (dimensions != null && !dimensions.isEmpty()) {
        MetricsResourceType metric = createMetricResource("VolumeQueueLength", numMillisecondsBetweenPuts, "summation", dimensions);
        metrics.add(metric);
      }
    }

    sensorResource.setMetrics(metrics);
    return sensorResource;
  }

  /**
   * Generate XML snippet
   * <code>
   * &lt;euca:item&gt;
   *   &lt;euca:metricName&gt;METRIC_NAME&lt;/euca:metricName&gt;
   *   &lt;euca:counters&gt;
   *     &lt;euca:item&gt;
   *       &lt;euca:type&gt;COUNTER_TYPE&lt;/euca:type&gt;
   *       &lt;euca:collectionIntervalMs&gt;COLLECTION_INTERVAL_MS&lt;/euca:collectionIntervalMs&gt;
   *          &lt;euca:dimensions&gt;
   *            ...
   *          &lt;/euca:dimensions&gt;
   *        &lt;/euca:item&gt;
   *     &lt;/euca:counters&gt;
   * &lt;/euca:item&gt;
   * </code>
   * that goes inside a &lt;euca:metrics&gt; block.
   *
   * @param metricName the metric name
   * @param collectionIntervalMs the collection interval time in milliseconds
   * @param counterType the counter type
   * @param dimensions the dimensions
   * @return the XML snippet (MetricsResourceType)
   */
  private MetricsResourceType createMetricResource(String metricName, long collectionIntervalMs, String counterType, ArrayList<MetricDimensionsType> dimensions) {
    MetricsResourceType metricsResourceType = new MetricsResourceType();
    metricsResourceType.setMetricName(metricName);
    ArrayList<MetricCounterType> counters = new ArrayList<MetricCounterType>();
    MetricCounterType counter = new MetricCounterType();
    counter.setCollectionIntervalMs(collectionIntervalMs);
    counter.setDimensions(dimensions );
    counter.setType(counterType);
    counters.add(counter);
    metricsResourceType.setCounters(counters);
    return metricsResourceType;
  }

  /**
   * Generate XML snippet
   * <code>
   * &lt;euca:item&gt;
   *   &lt;euca:dimensionName&gt;DIMENSION_NAME&lt;/euca:dimensionName&gt;
   *   &lt;euca:sequenceNum&gt;SEQUENCE_NUM&lt;/euca:sequenceNum&gt;
   *   &lt;euca:values&gt;
   *     ...
   *   &lt;/euca:values&gt;
   * &lt;/euca:item&gt;
   * </code>
   * that goes inside a &lt;euca:dimensions&gt; block.
   *
   * @param dimensionName the dimension name
   * @param sequenceNum the sequence num
   * @param values the values
   * @return the XML snippet (MetricDimensionsType)
   */
  private MetricDimensionsType createDimension(String dimensionName, Long sequenceNum, ArrayList<MetricDimensionsValuesType> values) {
    MetricDimensionsType dimension = new MetricDimensionsType();
    dimension.setDimensionName(dimensionName);
    dimension.setSequenceNum(sequenceNum);
    dimension.setValues(values );
    return dimension;

  }

  /**
   * Generate XML snippet
   * <code>
   * &lt;euca:item&gt;
   *   &lt;euca:timestamp&gt;TIMESTAMP&lt;/euca:timestamp&gt;
   *   &lt;euca:value&gt;METRIC_VALUE&lt;/euca:value&gt;
   * &lt;/euca:item&gt;
   * </code>
   * that goes inside a &lt;euca:values&gt; block.
   *
   * @param timestamp the timestamp
   * @param metricValue the metric value
   * @return the XML snippet (MetricDimensionsValuesType)
   */
  private MetricDimensionsValuesType createValue(Date timestamp, double metricValue) {
    MetricDimensionsValuesType value = new MetricDimensionsValuesType();
    value.setTimestamp(timestamp);
    value.setValue(metricValue);
    return value;
  }
}

new Thread() {
  public void run() {
    try {
      new CloudWatchFalseDataSystemMetricGenerator().runMyTest()
    } catch (Exception e) {
      CloudWatchFalseDataSystemMetricGenerator.LOG.error(e, e);
    }
  }
}.start();
