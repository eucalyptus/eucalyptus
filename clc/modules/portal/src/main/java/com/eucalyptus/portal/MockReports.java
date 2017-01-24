/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal;

import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.portal.common.model.InstanceUsageFilter;
import com.eucalyptus.portal.common.model.InstanceUsageGroup;
import com.eucalyptus.portal.common.model.ViewInstanceUsageReportType;
import com.eucalyptus.portal.common.model.ViewInstanceUsageResult;
import com.eucalyptus.portal.common.model.ViewMonthlyUsageResult;
import com.eucalyptus.portal.common.model.ViewMonthlyUsageType;
import com.eucalyptus.portal.common.model.ViewUsageResult;
import com.eucalyptus.portal.common.model.ViewUsageType;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MockReports {
  private static final Logger LOG = Logger.getLogger( MockReports.class );

  private static MockReports instance = new MockReports();
  private MockReports() { }
  public static MockReports getInstance() {
    return instance;
  }

  private static final String dir = String.format("%s/doc/billing-sample-reports", BaseDirectory.LIB);

  private static final long MILLISEC_IN_DAY = 86400000;
  private static final long MILLISEC_IN_HOUR = 3600000;


  private enum ReportType {
    monthly_report, aws_usage_report, ec2_usage_report
  }
  private enum ReportGranularity {
    hourly, daily, monthly
  }
  private enum ReportedService {
    ec2, s3, cloudwatch, elb
  }
  private static class SampleFile {
    private ReportType reportType = ReportType.monthly_report;
    private ReportGranularity reportGranularity = ReportGranularity.hourly;
    private ReportedService reportedService = ReportedService.ec2;
    private SampleFile(final ReportType type) {
      this.reportType = type;
    }

    private SampleFile withGranularity(final String granularity) {
      if (granularity.toLowerCase().startsWith("hour"))
        this.reportGranularity = ReportGranularity.hourly;
      else if (granularity.toLowerCase().startsWith("day") || granularity.toLowerCase().startsWith("dai"))
        this.reportGranularity = ReportGranularity.daily;
      else if (granularity.toLowerCase().startsWith("month"))
        this.reportGranularity = ReportGranularity.monthly;
      return this;
    }
    private SampleFile withGranularity(final ReportGranularity granularity) {
      this.reportGranularity = granularity;
      return this;
    }

    private SampleFile withService(final String service) {
      if (service.toLowerCase().startsWith("ec2"))
        this.reportedService = ReportedService.ec2;
      else if (service.toLowerCase().startsWith("s3"))
        this.reportedService = ReportedService.s3;
      else if (service.toLowerCase().startsWith("cloudwatch") || service.toLowerCase().equals("cw"))
        this.reportedService = ReportedService.cloudwatch;
      return this;
    }
    private SampleFile withService(final ReportedService service) {
      this.reportedService = service;
      return this;
    }

    private String getPath() {
      if(ReportType.monthly_report.equals(this.reportType)) {
        return String.format("%s/monthly_report.csv", dir);
      } else if (ReportType.aws_usage_report.equals(this.reportType)) {
        return String.format("%s/aws_usage_%s_%s.csv", dir, this.reportedService, this.reportGranularity);
      } else if (ReportType.ec2_usage_report.equals(this.reportType)) {
        return String.format("%s/ec2_usage.csv");
      }
      return null;
    }

    private ReportType getReportType() {
      return this.reportType;
    }
  }

  public ViewMonthlyUsageResult generateMonthlyReport(final ViewMonthlyUsageType request) {
    final ViewMonthlyUsageResult result = new ViewMonthlyUsageResult();
    final SampleReportReader reader = new SampleReportReader(new SampleFile(ReportType.monthly_report));
    final StringBuilder sb = new StringBuilder();
    try {
      sb.append(reader.getHeaderLine()+"\n");
      final MonthlyUsageReports reports = new MonthlyUsageReports(request);
      sb.append(reader.readLines().stream()
              .map(reports.fromHashableLine)
              .map(reports.substitutor)
              .map(data -> data.toString())
              .reduce( (l1, l2) -> String.format("%s\n%s", l1, l2) )
              .get()
      );
      result.setData(sb.toString());
    } catch (final Exception ex) {
      LOG.error("Failed to generate sample monthly report", ex);
    }
    return result;
  }

  public ViewUsageResult generateAwsUsageReport(final ViewUsageType request) {
    final ViewUsageResult result = new ViewUsageResult();
    final SampleReportReader reader = new SampleReportReader(
            new SampleFile(ReportType.aws_usage_report)
                    .withGranularity(request.getReportGranularity())
                    .withService(request.getServices())
    );

    final StringBuilder sb = new StringBuilder();
    try {
      final HashableLine firstLine = reader.getLine(1);
      final String strStartTime = firstLine.getValue("StartTime");
      final DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
      final Date earliestStartTime = df.parse(strStartTime);
      final AwsUsageReports reports = new AwsUsageReports(request);
      reports.setEarliestTimeInReport(earliestStartTime);

      sb.append(reader.getHeaderLine() + "\n");
      sb.append(reader.readLines().stream()
              .map(reports.fromHashableLine)
              .filter(reports.filter)
              .map(reports.timeAdjustor)
              .filter(reports.endTimeFilter)
              .map(data -> data.toString())
              .reduce( (l1, l2) -> String.format("%s\n%s", l1, l2))
              .get()
      );
      result.setData(sb.toString());
    }catch (final Exception ex) {
      LOG.error("Failed to generate sample aws usage report", ex);
    }
    return result;
  }

  public ViewInstanceUsageResult generateInstanceUsageReport(final ViewInstanceUsageReportType request)
          throws Exception {
    final ViewInstanceUsageResult result = new ViewInstanceUsageResult();

    final DateFormat df = new SimpleDateFormat("MM/dd/yy");
    Date rangeStart = request.getTimeRangeStart();
    rangeStart = df.parse(df.format(rangeStart));

    Date rangeEnd = request.getTimeRangeEnd();
    rangeEnd = df.parse(df.format(rangeEnd));

    long msDiff = rangeEnd.getTime()-rangeStart.getTime();
    if (msDiff < 0)
      throw new Exception("Time range's start date must be before end date");
    if (request.getGranularity()!=null &&
            request.getGranularity().toLowerCase().startsWith("hour")) {
      if (msDiff > (14 * MILLISEC_IN_DAY))
        throw new Exception("Max duration for hourly granularity is 2 weeks");
    }

    final InstanceUsageReport report =  Ec2InstanceUsageReports.getInstance().generate(request.getGranularity(),
            rangeStart,
            rangeEnd,
            request.getGroupBy(),
            request.getFilters() != null ? request.getFilters().getMember()  : Lists.newArrayList()
    );

    final StringBuilder sb = new StringBuilder();
    sb.append("Start Time,End Time");
    for (final String header : report.getHeader()) {
      sb.append(","+header);
    }
    sb.append("\n");

    sb.append(report.getData().stream()
            .map(data -> data.toString())
            .reduce( (l1, l2) -> String.format("%s\n%s", l1, l2))
            .get());

    result.setUsageReport(sb.toString());
    return result;
  }


  public static class SampleReportReader {
    private Map<String, Integer> header = Maps.newHashMap();
    private String sampleFilePath = null;
    private ReportType reportType = ReportType.monthly_report;
    public SampleReportReader(final SampleFile file) {
      this.reportType = file.getReportType();
      this.sampleFilePath = file.getPath();
    }

    public List<HashableLine> readLines() throws Exception {
      if (header == null || header.size() <= 0) {
        buildHeader();
        if (header == null || header.size() <= 0) {
          new Exception("Failed to parse header from the file");
        }
      }
      final BufferedReader reader = new BufferedReader(new FileReader(this.sampleFilePath));
      if(this.reportType == ReportType.monthly_report) {
        return reader.lines().skip(1)
                .map(s -> new HashableLine(header, s, "\",\""))
                .filter(hl -> hl.isValid())
                .collect(Collectors.toList());
      } else if (this.reportType == ReportType.aws_usage_report) {
        return reader.lines().skip(1)
                .map(s -> new HashableLine(header, s, ","))
                .filter(hl -> hl.isValid())
                .collect(Collectors.toList());
      } else {
        return null;
      }
    }

    public HashableLine getLine(final int lineNum) throws Exception {
      if (header == null || header.size() <= 0) {
        buildHeader();
        if (header == null || header.size() <= 0) {
          new Exception("Failed to parse header from the file");
        }
      }

      final BufferedReader reader = new BufferedReader(new FileReader(this.sampleFilePath));
      if(this.reportType == ReportType.monthly_report) {
        return reader.lines().skip(lineNum)
                .map( s -> new HashableLine(header, s, "\",\""))
                .findFirst()
                .get();

      } else if (this.reportType == ReportType.aws_usage_report) {
        return reader.lines().skip(lineNum)
                .map( s -> new HashableLine(header, s, ","))
                .findFirst()
                .get();
      } else {
        return null;
      }
    }

    private void buildHeader() throws Exception {
      final String header = getHeaderLine();
      final String[] headerTokens = header.split(",");
      for (int i = 0; i< headerTokens.length; i++ ) {
        final String unquoted = headerTokens[i].replaceAll("\"", "");
        final String trimmed = unquoted.trim();
        this.header.put(trimmed, i);
      }
    }

    public String getHeaderLine() throws Exception {
      final BufferedReader reader = new BufferedReader(new FileReader(this.sampleFilePath));
      final Optional<String> header = reader.lines().findFirst();
      if (!header.isPresent())
        throw new Exception("Header line is not found");
      return header.get();
    }
  }

  public static class HashableLine {
    private String line = null;
    private Map<String, Integer> header = null;
    private String[] tokens = null;
    public HashableLine(final Map<String, Integer> header, final String line, final String splitRegex) {
      this.header = header;
      this.line = line;
      this.tokens = this.line.split(splitRegex);
    }

    public String getValue(final String fieldName) throws ParseException {
      if (this.tokens.length != this.header.keySet().size()) {
        throw new ParseException("Number of tokens doesn't match the header", -1);
      }
      if (!this.header.keySet().contains(fieldName)) {
        throw new ParseException("No such field is found in header", -1);
      }
      if (this.header.get(fieldName) < 0 || this.header.get(fieldName) >= this.tokens.length) {
        throw new ParseException("Invalid field index in header", this.header.get(fieldName));
      }

      final String token = this.tokens[this.header.get(fieldName)];
      final String unquoted = token.replaceAll("\"", "");
      return unquoted;
    }

    public boolean isValid() {
      if (this.tokens.length != this.header.keySet().size()) {
        return false;
      }
      return true;
    }

    public Set<String> getHeaders() {
      return this.header.keySet();
    }

    @Override
    public String toString() {
      return this.line;
    }
  }


  public static class Ec2InstanceUsageReports {
    private static Ec2InstanceUsageReports instance = new Ec2InstanceUsageReports();
    private Ec2InstanceUsageReports() { }
    public static Ec2InstanceUsageReports getInstance() {
      return instance;
    }

    private List<SimulatedInstance> simInstances = Lists.newArrayList();

    public static final int NUM_SIMULATED_INSTANCES = 1000;
    public static final int NUM_SIMULATED_DAYS = 365;
    public static final int MAX_INSTANCE_DAYS = 60;

    public InstanceUsageReport generate( final String strGranularity, final Date rangeStart,
                                                   final Date rangeEnd, final InstanceUsageGroup groupBy,
                                                   final List<InstanceUsageFilter> filters
    ) {
      if (this.simInstances == null || this.simInstances.size() <= 0 )
        this.simulate();

      if (strGranularity.toLowerCase().startsWith("hour"))
        return generate(rangeStart, rangeEnd, groupBy, filters, rangeStart,
                (d) -> new Date(d.getTime() + MILLISEC_IN_HOUR));
      else if (strGranularity.toLowerCase().startsWith("day") || strGranularity.toLowerCase().startsWith("daily"))
        return generate(rangeStart, rangeEnd, groupBy, filters, Ec2InstanceUsageReports.findBeginningOfNextDay(rangeStart),
                (d) -> Ec2InstanceUsageReports.findBeginningOfNextDay(d));
      else if (strGranularity.toLowerCase().startsWith("month"))
        return generate(rangeStart, rangeEnd, groupBy, filters,
                Ec2InstanceUsageReports.findFirstDayOfNextMonth(rangeStart),
                (d) -> Ec2InstanceUsageReports.findFirstDayOfNextMonth(d));

      throw Exceptions.toUndeclared(new Exception("Invalid granularity specified: " + strGranularity));
    }

    private Set<String> generateHeader(final Date rangeStart,
                                        final Date rangeEnd,
                                        final InstanceUsageGroup groupBy,
                                        final List<InstanceUsageFilter> filters) {
      if(groupBy == null) {
        return Sets.newHashSet("[INSTANCE HOURS]");
      }
      return this.simInstances.stream()
              .filter( (instance) -> instance.apply(rangeStart, rangeEnd) )
              .filter( (instance) -> instance.apply(filters))
              .map( (instance) ->  instance.getGroupKey(groupBy))
              .distinct()
              .collect(Collectors.toSet());
    }

    private InstanceUsageReport  generate(final Date rangeStart,
                                                         final Date rangeEnd,
                                                         final InstanceUsageGroup groupBy,
                                                         final List<InstanceUsageFilter> filters,
                                          final Date beginTime,
                                          Function<Date, Date> advanceTime) {
      final Map<Long, Integer[]> usageMap = Maps.newHashMap();
      final Set<String> headers = this.generateHeader(rangeStart, rangeEnd, groupBy, filters);
      final Map<String, Integer> headerIndex = Maps.newHashMap();
      int idx = 0;
      for(final String h : headers) {
        headerIndex.put(h, idx++);
      }

      Date curDate = beginTime;
      while (curDate.before(rangeEnd)) {
        usageMap.put(curDate.getTime(), new Integer[headers.size()]);
        for(int i = 0; i< headers.size(); i++) {
          usageMap.get(curDate.getTime())[i] = 0;
        }
        curDate = advanceTime.apply(curDate);
      }

      for (final SimulatedInstance instance :
              this.simInstances.stream().filter(inst -> inst.apply(filters)).collect(Collectors.toList())) {
        usageMap.keySet().stream()
                .filter(ts -> instance.apply(ts))
                .forEach(ts -> {
            usageMap.get(ts)[headerIndex.get(instance.getGroupKey(groupBy))]++;
        });
      }
      final InstanceUsageReport report = new InstanceUsageReport();
      report.setHeader(new String[headers.size()]);
      headers.stream()
              .forEach(h -> report.getHeader()[headerIndex.get(h)] = h);
      report.setData(Lists.newArrayListWithCapacity(usageMap.keySet().size()));
      final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      //  2017-01-11 00:00:00
      usageMap.keySet().stream()
              .sorted()
              .forEach((ts) -> {
                final InstanceUsageReportData data = new InstanceUsageReportData();
                final Date begin = new Date(ts);
                final Date end = advanceTime.apply(begin);
                data.startTime = df.format(begin);
                data.endTime = df.format(end);
                data.values = usageMap.get(ts);
                report.getData().add(data);
              });
      return report;
    }

    public static Date findBeginningOfNextDay(final Date time) {
      final Calendar c = Calendar.getInstance();
      c.setTime(time);
      c.add(Calendar.DAY_OF_MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTime();
    }

    public static Date findFirstDayOfNextMonth(final Date time) {
      final Calendar c = Calendar.getInstance();
      c.setTime(time);
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.add(Calendar.MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTime();
    }

    private final Random rand = new Random();

    private final String[] AccountNumbers = new String[]{ "123456789012" };
    private final String[] InstanceTypes = new String[] {
            "m1.small", "t1.micro", "m1.medium", "c1.medium", "m1.large", "m1.xlarge", "c1.xlarge",
            "m2.xlarge", "m3.xlarge", "m2.2xlarge", "m3.2xlarge", "cc1.4xlarge", "m2.4xlarge", "hi1.4xlarge",
            "cc2.8xlarge", "cg1.4xlarge", "cr1.8xlarge", "hs1.8xlarge"
    };
    private final String[] Platforms = new String[] {"linux", "windows"};
    private final String[] Regions = new String[] { "eucalyptus"};
    private final String[] AvailabilityZones = new String[] {"one", "two", "three"};
    private final String[] TagNames = new String[] { "WebServer", "DB", "Compute" };
    private final String[] TagGroups = new String[] { "Dev", "QA", "GTM" };
    private final String[] TagCosts = new String[] { "High", "Medium", "Low" };

    private Function<SimulatedInstance, SimulatedInstance> withStartTime = (instance) -> {
      final Long minStartMs = System.currentTimeMillis()
              - NUM_SIMULATED_DAYS * MILLISEC_IN_DAY;
      final long timeMillis = minStartMs + (long) ((System.currentTimeMillis() - minStartMs) * rand.nextDouble());
      instance.startTime = new Date(timeMillis);
      return instance;
    };

    private Function<SimulatedInstance, SimulatedInstance> withEndTime = (instance) -> {
      final long hours = (long) (MAX_INSTANCE_DAYS * 24 * Math.abs(rand.nextGaussian()));
      final long diff = hours * MILLISEC_IN_HOUR;
       instance.endTime = new Date( instance.startTime.getTime() + diff);
      return instance;
    };

    private Function<SimulatedInstance, SimulatedInstance> withAccount = (instance) -> {
      instance.account = AccountNumbers[rand.nextInt(AccountNumbers.length)];
      return instance;
    };

    private Function<SimulatedInstance, SimulatedInstance> withInstanceType = (instance) -> {
      instance.instanceType = InstanceTypes[rand.nextInt(InstanceTypes.length)];
      return instance;
    };

    private Function<SimulatedInstance, SimulatedInstance> withPlatform =  (instance) -> {
      instance.platform = Platforms[rand.nextInt(Platforms.length)];
      return instance;
    };

    private Function<SimulatedInstance, SimulatedInstance> withRegion = (instance) -> {
      instance.region = Regions[rand.nextInt(Regions.length)];
      return instance;
    };

    private Function<SimulatedInstance, SimulatedInstance> withAvailabilityZone = (instance) -> {
      instance.availabilityZone = AvailabilityZones[rand.nextInt(AvailabilityZones.length)];
      return instance;
    };

    private Function<SimulatedInstance, SimulatedInstance> withTags = (instance) -> {
      instance.tags = new HashSet<AbstractMap.SimpleEntry<String,String>>();
      instance.tags.add( new AbstractMap.SimpleEntry<String, String>("Name",
              TagNames[rand.nextInt(TagNames.length)]
      ));
      instance.tags.add( new AbstractMap.SimpleEntry<String, String>("Group",
              TagGroups[rand.nextInt(TagGroups.length)]
      ));
      instance.tags.add( new AbstractMap.SimpleEntry<String, String>("Cost",
              TagCosts[rand.nextInt(TagCosts.length)]
      ));
      return instance;
    };

    private void simulate() {
      List<SimulatedInstance> instances = Lists.newArrayListWithCapacity(NUM_SIMULATED_INSTANCES);
      for (int i = 0; i< NUM_SIMULATED_INSTANCES; i++) {
        instances.add(new SimulatedInstance());
      }

      this.simInstances = instances.stream()
              .map(withStartTime)
              .map(withEndTime)
              .map(withAccount)
              .map(withInstanceType)
              .map(withPlatform)
              .map(withRegion)
              .map(withAvailabilityZone)
              .map(withTags)
              .collect(Collectors.toList());
    }

    private class SimulatedInstance {
      private Date startTime;
      private Date endTime;
      private String account;
      private String instanceType;
      private String platform;
      private String region;
      private String availabilityZone;
      private Set<AbstractMap.SimpleEntry<String,String>> tags;

      /// was this instance running?
      private boolean apply(final Long timestamp) {
        return this.startTime.getTime() < timestamp &&
                timestamp < this.endTime.getTime();
      }

      private String getGroupKey (final InstanceUsageGroup groupBy) {
        if (groupBy == null)
          return "[INSTANCE HOURS]";

        final String groupType = groupBy.getType();
        final String groupKey = groupBy.getKey();

        if (groupType == null)
          throw Exceptions.toUndeclared("GroupBy type must be specified");

        if (groupType.toLowerCase().startsWith("account"))
            return account;
        else if (groupType.toLowerCase().startsWith("availability"))
            return availabilityZone;
        else if (groupType.toLowerCase().startsWith("instancetype") || groupType.toLowerCase().startsWith("instance type"))
            return instanceType;
        else if (groupType.toLowerCase().startsWith("platform"))
            return platform;
        else if (groupType.toLowerCase().startsWith("region"))
            return region;
        else if (groupType.toLowerCase().startsWith("tag")) {
          if (groupKey == null)
            throw Exceptions.toUndeclared("GroupBy key must be specified for tag type");
          final Optional<String> value = tags.stream()
                  .filter(t -> groupKey.equals(t.getKey()))
                  .map(t -> t.getValue())
                  .findAny();
          if (value.isPresent())
            return value.get();
          else
            return "[UNTAGGED]";
        }
        throw Exceptions.toUndeclared("Unknown group type: " + groupType);
      }

      /// was this instance running?
      private boolean apply(final Date rangeStart, final Date rangeEnd) {
        if(endTime!=null && endTime.before(rangeStart))
          return false;
        if(startTime!=null && startTime.after(rangeEnd))
          return false;
        return true;
      }

      private boolean apply(final List<InstanceUsageFilter> filters) {
        for (final InstanceUsageFilter f : filters) {
          if (!apply(f))
            return false;
        }
        return true;
      }

      private boolean apply(final InstanceUsageFilter filter) {
        final String type = filter.getType().toLowerCase();
        final String key = filter.getKey();
        final String value = filter.getValue();
        if (type.startsWith("tag")) {
          if (this.tags == null)
            return false;
          if (key==null || value==null)  /// this shouldn't be allowed
            return false;
          return this.tags.stream()
                  .filter((tag) -> key.equals(tag.getKey()) &&  value.equals(tag.getValue()))
                  .count() > 0;
        } else if (type.startsWith("region")) {
          return this.region!=null && this.region.equals(key);
        } else if (type.startsWith("instance type") || type.startsWith("instancetype")) {
          return this.instanceType!=null && this.instanceType.equals(key);
        } else if (type.startsWith("account")) {
          return this.account!=null && this.account.equals(key);
        } else if (type.startsWith("platform")) {
          return key!=null && key.toLowerCase().startsWith(this.platform);
        } else {
          return false;
        }
      }
    }
  }

  public static class AwsUsageReports {
    private String service = null;
    private String operation = null;
    private String usageType = null;
    private Date timePeriodFrom = null;
    private Date timePeriodTo = null;
    private String reportGranularity = null;

    private Date earliestTimeInReport = null;
    public AwsUsageReports(final ViewUsageType request) {
      this.service = request.getServices();
      this.operation = request.getOperations();
      if (this.operation != null && this.operation.toLowerCase().equals("all"))
        this.operation = null;
      this.usageType = request.getUsageTypes();
      if (this.usageType != null && this.usageType.toLowerCase().equals("all"))
        this.usageType = null;
      this.timePeriodFrom = request.getTimePeriodFrom();
      this.timePeriodTo = request.getTimePeriodTo();
      this.reportGranularity = request.getReportGranularity();
    }

    public void setEarliestTimeInReport(final Date time) {
      this.earliestTimeInReport = time;
    }

    public Function<HashableLine, AwsUsageReportData> fromHashableLine = (line) -> {
      final AwsUsageReportData data = new AwsUsageReportData();
      try {
        for( final Field field : data.getClass().getFields() ) {
          String fieldName = field.getName();
          if (fieldName == null) {
            continue;
          } else if (fieldName.length() > 1) {
            fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
          } else if (fieldName.length() == 1) {
            fieldName = fieldName.toUpperCase();
          }

          if (line.getHeaders().contains(fieldName)) {
            final String strValue = line.getValue(fieldName);
            if (String.class.equals(field.getType())) {
              field.set(data, strValue);
            } else if (Date.class.equals(field.getType()) && strValue.length() > 0) {
              // 11/03/16 00:00:00
              final DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
              final Date dt = df.parse(strValue);
              field.set(data, dt);
            } else if (Double.class.equals(field.getType()) && strValue.length() > 0) {
              Double dv = Double.parseDouble(strValue);
              field.set(data, dv);
            }
          }
        }
      } catch (final ParseException ex) {
        LOG.error("Failed to parse mock data", ex);
        return null;
      } catch (final IllegalAccessException ex) {
        LOG.error("Failed to parse mock data", ex);
        return null;
      }
      return data;
    };

    public Predicate<AwsUsageReportData> filter = (data) -> {
      if (this.operation!=null && this.operation.length()>0) { // operation specified
        if (data.operation != null && !data.operation.contains(this.operation))
          return false;
      }
      if (this.usageType!=null && this.usageType.length()>0) { // usage type specfied
        if (data.usageType != null && !data.usageType.contains(this.usageType))
          return false;
      }
      return true;
    };

    public Function<AwsUsageReportData, AwsUsageReportData> timeAdjustor = (data) -> {
      if (this.timePeriodFrom == null) {
        return data;
      } else {
        final long timeDiff = this.timePeriodFrom.getTime() - this.earliestTimeInReport.getTime();
        data.startTime = new Date(data.startTime.getTime() + timeDiff);
        data.endTime = new Date(data.endTime.getTime() + timeDiff);
        return data;
      }
    };

    public Predicate<AwsUsageReportData> endTimeFilter = (data) -> {
      if (data.startTime!=null && this.timePeriodTo !=null) {
        if (data.startTime.after(this.timePeriodTo))
          return false;
      }
      return true;
    };
  }

  public static class MonthlyUsageReports {
    private String accountId = null;
    private String accountName = null;
    private int year = 2016;
    private int month = 12;
    public MonthlyUsageReports(final ViewMonthlyUsageType request) {
      try {
        final Context ctx = Contexts.lookup(request.getCorrelationId());
        this.accountId = ctx.getAccountNumber();
        this.accountName = ctx.getAccountAlias();
      }catch(final Exception ex) {
        this.accountId = "0000000000";
        this.accountName = "Eucalyptus";
      }
      this.year = Integer.parseInt(request.getYear());
      this.month = Integer.parseInt(request.getMonth());
    }

    public Function<HashableLine, MonthlyUsageReportData> fromHashableLine = (line) -> {
      final MonthlyUsageReportData data = new MonthlyUsageReportData();
      try {
        for( final Field field : data.getClass().getFields() ) {
          String fieldName = field.getName();
          if (fieldName == null) {
            continue;
          } else if (fieldName.length() > 1) {
            fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
          } else if (fieldName.length() == 1) {
            fieldName = fieldName.toUpperCase();
          }

          if (line.getHeaders().contains(fieldName)) {
            final String strValue = line.getValue(fieldName);
            if (String.class.equals(field.getType())) {
              field.set(data, strValue);
            } else if (Date.class.equals(field.getType()) && strValue.length() > 0) {
              // 2016/12/03 10:34:18
              final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
              final Date dt = df.parse(strValue);
              field.set(data, dt);
            } else if (Double.class.equals(field.getType()) && strValue.length() > 0) {
              Double dv = Double.parseDouble(strValue);
              field.set(data, dv);
            }
          }
        }
      } catch (final ParseException ex) {
        LOG.error("Failed to parse mock data", ex);
        return null;
      } catch (final IllegalAccessException ex) {
        LOG.error("Failed to parse mock data", ex);
        return null;
      }
      return data;
    };

    public Function<MonthlyUsageReportData, MonthlyUsageReportData> substitutor = (data) -> {
      data.payerAccountId = this.accountId;
      data.payerAccountName = this.accountName;
      data.taxationAddress = "6750 Navigator Way Suite 200, Goleta, CA 93117";

      if (data.billingPeriodStartDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.billingPeriodStartDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.billingPeriodStartDate = cal.getTime();
      }
      if (data.billingPeriodEndDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.billingPeriodEndDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.billingPeriodEndDate = cal.getTime();
      }

      if (data.invoiceDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.invoiceDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.invoiceDate = cal.getTime();
      }

      if (data.usageStartDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.usageStartDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1 );
        data.usageStartDate = cal.getTime();
      }

      if (data.usageEndDate != null) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(data.usageEndDate);
        cal.set(Calendar.YEAR, this.year);
        cal.set(Calendar.MONTH, this.month - 1);
        data.usageEndDate = cal.getTime();
      }
      return data;
    };
  }
}
