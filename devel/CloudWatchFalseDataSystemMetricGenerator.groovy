
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.cloudwatch.PutMetricDataType;
import com.eucalyptus.cluster.callback.CloudWatchHelper;
import com.eucalyptus.cluster.callback.CloudWatchHelper.InstanceInfoProvider;
import com.eucalyptus.component.ServiceConfiguration;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponse;
import edu.ucsb.eucalyptus.msgs.MetricCounterType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsValuesType;
import edu.ucsb.eucalyptus.msgs.MetricsResourceType;
import edu.ucsb.eucalyptus.msgs.SensorsResourceType;


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
      super();
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
      super();
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
      super();
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
      super();
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
      super();
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
      super();
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
  private static class VolumeMetrics {
    
    public String getVolumeId() {
      return volumeId;
    }
    
    public void setVolumeId(String volumeId) {
      this.volumeId = volumeId;
    }
    
    /**
     * Gets the MetricValue associated with "DiskReadOps" (or "VolumeReadOps")
     */
    public MetricValue getDiskReadOps() {
      return diskReadOps;
    }
    
    public void setDiskReadOps(MetricValue diskReadOps) {
      this.diskReadOps = diskReadOps;
    }
    
    /**
     * Gets the MetricValue associated with "DiskWriteOps" (or "VolumeWriteOps")
     */
    public MetricValue getDiskWriteOps() {
      return diskWriteOps;
    }
    
    public void setDiskWriteOps(MetricValue diskWriteOps) {
      this.diskWriteOps = diskWriteOps;
    }
    
    /**
     * Gets the MetricValue associated with "DiskReadBytes" (or "VolumeReadBytes")
     */
    public MetricValue getDiskReadBytes() {
      return diskReadBytes;
    }
    
    public void setDiskReadBytes(MetricValue diskReadBytes) {
      this.diskReadBytes = diskReadBytes;
    }
    
    /**
     * Gets the MetricValue associated with "DiskWriteBytes" (or "VolumeWriteBytes")
     */
   public MetricValue getDiskWriteBytes() {
      return diskWriteBytes;
    }
    
    public void setDiskWriteBytes(MetricValue diskWriteBytes) {
      this.diskWriteBytes = diskWriteBytes;
    }
    
    /**
     * Gets the MetricValue associated with "VolumeQueueLength" (ignored for ephemeral volumes)
     */
    public MetricValue getVolumeQueueLength() {
      return volumeQueueLength;
    }
    
    public void setVolumeQueueLength(MetricValue volumeQueueLength) {
      this.volumeQueueLength = volumeQueueLength;
    }
    
    /**
     * Gets the MetricValue associated with "VolumeTotalReadTime" (ignored for ephemeral volumes)
     */
    public MetricValue getVolumeTotalReadTime() {
      return volumeTotalReadTime;
    }
    
    public void setVolumeTotalReadTime(MetricValue volumeTotalReadTime) {
      this.volumeTotalReadTime = volumeTotalReadTime;
    }
    
    /**
     * Gets the MetricValue associated with "VolumeTotalWriteTime" (ignored for ephemeral volumes)
     */
    public MetricValue getVolumeTotalWriteTime() {
      return volumeTotalWriteTime;
    }
    
    public void setVolumeTotalWriteTime(MetricValue volumeTotalWriteTime) {
      this.volumeTotalWriteTime = volumeTotalWriteTime;
    }
    
    private String volumeId;
    private MetricValue diskReadOps;
    private MetricValue diskWriteOps;
    private MetricValue diskReadBytes;
    private MetricValue diskWriteBytes;
    private MetricValue volumeQueueLength;
    private MetricValue volumeTotalReadTime;
    private MetricValue volumeTotalWriteTime;
  }
  
  /**
   * InstanceMetrics encapsulates several metric values for metrics related to instances.
   */
  private static class InstanceMetrics {
    
    
    /**
     * Gets the MetricValue associated with "CPUUtilization"
     */
    public MetricValue getCpuUtilization() {
      return cpuUtilization;
    }
    
    public void setCpuUtilization(MetricValue cpuUtilization) {
      this.cpuUtilization = cpuUtilization;
    }
    
    /**
     * Gets the MetricValue associated with "NetworkIn"
     */
    public MetricValue getNetworkIn() {
      return networkIn;
    }
    
    public void setNetworkIn(MetricValue networkIn) {
      this.networkIn = networkIn;
    }
    
    /**
     * Gets the MetricValue associated with "NetworkOut"
     */
    public MetricValue getNetworkOut() {
      return networkOut;
    }
    
    public void setNetworkOut(MetricValue networkOut) {
      this.networkOut = networkOut;
    }

    private MetricValue cpuUtilization;
    private MetricValue networkIn;
    private MetricValue networkOut;

  }
  
  /**
   * Instance encapsulates information about instances, including uuid, instanceId, vmType, imageId, whether "monitoring" is on, and all
   * MetricValues associated with the instance.
   */
  private static class Instance {
    
    private String uuid;
    private String instanceId;
    private String imageId;
    private String vmType;
    private boolean monitoring;
    private String userId;
    private String autoscalingGroupName;
    private InstanceMetrics metrics;
    private List<VolumeMetrics> volumeMetricsList;
    
    /**
     * Gets the list of VolumeMetrics associated with this instance.  (This includes ephemeral or EBS volumes associated with the instance).
     */
    public List<VolumeMetrics> getVolumeMetricsList() {
      return volumeMetricsList;
    }
    
    public void setVolumeMetricsList(List<VolumeMetrics> volumeMetricsList) {
      this.volumeMetricsList = volumeMetricsList;
    }
    
    /**
     * Gets the instance uuid.
     */
    public String getUuid() {
      return uuid;
    }
    
    public void setUuid(String uuid) {
      this.uuid = uuid;
    }
    
    /**
     * Gets the instance id.
     */
    public String getInstanceId() {
      return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
    }
    
    /**
     * Gets the image id.
     */
    public String getImageId() {
      return imageId;
    }
    
    public void setImageId(String imageId) {
      this.imageId = imageId;
    }
    
    /**
     * Gets the vm type.
     */
    public String getVmType() {
      return vmType;
    }
    
    public void setVmType(String vmType) {
      this.vmType = vmType;
    }
    
    /**
     * Checks if monitoring is enabled
     */
    public boolean isMonitoring() {
      return monitoring;
    }
    
    public void setMonitoring(boolean monitoring) {
      this.monitoring = monitoring;
    }
    
    /**
     * Gets the user id of the instance owner.
     */
    public String getUserId() {
      return userId;
    }
    
    public void setUserId(String userId) {
      this.userId = userId;
    }
    
    /**
     * Gets the autoscaling group name associated with the instance (null if none)
     */
    public String getAutoscalingGroupName() {
      return autoscalingGroupName;
    }
    
    public void setAutoscalingGroupName(String autoscalingGroupName) {
      this.autoscalingGroupName = autoscalingGroupName;
    }
    
    /**
     * Gets the InstanceMetrics object associated with the instance.
     */
    public InstanceMetrics getMetrics() {
      return metrics;
    }
    
    public void setMetrics(InstanceMetrics metrics) {
      this.metrics = metrics;
    }
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
    public String getEffectiveUserId(String instanceId) throws Exception {
      return instanceMap.get(instanceId).getUserId();
    }

    @Override
    public boolean getMonitoring(String instanceId) {
      return instanceMap.get(instanceId).isMonitoring();
    }
  }

  private static final Logger LOG = Logger.getLogger(CloudWatchFalseDataSystemMetricGenerator.class);
  
  private static final long DEFAULT_NUM_MILLISECONDS_BETWEEN_PUTS = 60000L;
  private static final int DEFAULT_NUM_INSTANCES = 1000;
  private static final long DEFAULT_DURATION_MINS = 60L;
  
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
    final int NUM_EBS_VOLUMES_PER_INSTANCE = 3;
    final double MB_PER_SEC = 1024.0 * 1024.0 / 10000.0;
    DecimalFormat df = new DecimalFormat("00000");
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
      instance.setUserId(Accounts.lookupSystemAdmin().getUserId());
      instance.setAutoscalingGroupName(null);
      
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
  public synchronized void test() throws Exception {
    if (instanceMap.isEmpty()) {
      setupDefaultInstances();
    }
    Date startTime = new Date();
    CloudWatchHelper cloudWatchHelper = new CloudWatchHelper(mockInstanceInfoProvider);
    long numPuts = durationMins * 60000L / numMillisecondsBetweenPuts;
    for (long putNum=0;putNum<numPuts;putNum++) {
      Date currentTime = new Date(startTime.getTime() + numMillisecondsBetweenPuts * putNum);
      long before = System.currentTimeMillis();
      for (String instanceId: instanceMap.keySet()) {
        DescribeSensorsResponse msg = new DescribeSensorsResponse();
        msg.setUser(Accounts.lookupSystemAdmin());
        ArrayList<SensorsResourceType> sensorResources = new ArrayList<SensorsResourceType>();
        Instance instance = instanceMap.get(instanceId);
        sensorResources.add(generateInstanceSensorResource(instance, currentTime, putNum));
        msg.setSensorsResources(sensorResources );
        List<PutMetricDataType> putMetricDataList = cloudWatchHelper.collectMetricData(msg);
        putMetricDataList = CloudWatchHelper.consolidatePutMetricDataList(putMetricDataList);
        ServiceConfiguration serviceConfiguration = CloudWatchHelper.createServiceConfiguration();
        for (PutMetricDataType putMetricData: putMetricDataList) {
          cloudWatchHelper.sendSystemMetric(serviceConfiguration, putMetricData);
        }
        LOG.info("Putting data for instance :" + instanceId);
      }
      long after = System.currentTimeMillis();
      LOG.info("Putting " + instanceMap.size() + " instance data for time:" + currentTime);
      LOG.info("Processing time was " + (after - before) + "ms");
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

new CloudWatchFalseDataSystemMetricGenerator().test();
