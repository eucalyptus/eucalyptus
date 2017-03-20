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

import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.portal.common.model.ViewInstanceUsageReportResponseType;
import com.eucalyptus.portal.common.model.ViewInstanceUsageReportType;
import com.eucalyptus.portal.common.model.ViewInstanceUsageResult;
import com.eucalyptus.portal.common.model.ViewReservedInstanceUtilizationReportResponseType;
import com.eucalyptus.portal.common.model.ViewReservedInstanceUtilizationReportType;
import com.eucalyptus.portal.common.model.ViewReservedInstanceUtilizationResult;
import com.eucalyptus.portal.common.policy.Ec2ReportsPolicySpec;
import com.eucalyptus.portal.instanceusage.InstanceHourLog;
import com.eucalyptus.portal.instanceusage.InstanceLogs;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Years;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import static java.util.stream.Collectors.*;

@SuppressWarnings( "unused" )
@ComponentNamed
public class Ec2ReportsService {
  private static final Logger LOG = Logger.getLogger( Ec2ReportsService.class );

  public ViewInstanceUsageReportResponseType viewInstanceUsageReport(final ViewInstanceUsageReportType request)
          throws Ec2ReportsServiceException {
    final ViewInstanceUsageReportResponseType response = request.getReply();
    final Context context = checkAuthorized( );
    try {
      final Function<ViewInstanceUsageReportType, Optional<Ec2ReportsServiceException>> requestVerifier = (req) -> {
        if (req.getGranularity() == null)
          return Optional.of(new Ec2ReportsInvalidParameterException("Granularity must be specified"));
        final String granularity = req.getGranularity().toLowerCase();
        if (request.getTimeRangeStart() == null || request.getTimeRangeEnd() == null)
          return Optional.of(new Ec2ReportsInvalidParameterException("time range start and end must be specified"));

        if (!Sets.newHashSet("hourly", "hour", "daily", "day", "monthly", "month").contains(granularity)) {
          return Optional.of(new Ec2ReportsInvalidParameterException("Can't recognize granularity. Valid values are hourly, daily and monthly"));
        }

        if (granularity.equals("hourly") || granularity.equals("hour")) {
          // AWS: time range up to 7 days when using an hourly data granularity
          if (Days.daysBetween(
                  new DateTime(request.getTimeRangeStart().getTime()),
                  new DateTime(request.getTimeRangeEnd().getTime())).getDays() > 7) {
            return Optional.of(new Ec2ReportsInvalidParameterException("time range is allowed up to 7 days when using an hourly data granularity"));
          }
        } else if (granularity.equals("daily") || granularity.equals("day")) {
          // AWS: time range up to 3 months when using a daily data granularity
          if (Months.monthsBetween(
                  new DateTime(request.getTimeRangeStart().getTime()),
                  new DateTime(request.getTimeRangeEnd().getTime())).getMonths() > 3) {
            return Optional.of(new Ec2ReportsInvalidParameterException("time range is allowed up to 3 months when using a daily data granularity"));
          }
        } else {
          // AWS: time range up to 3 years when using a monthly data granularity.
          if (Years.yearsBetween(
                  new DateTime(request.getTimeRangeStart().getTime()),
                  new DateTime(request.getTimeRangeEnd().getTime())).getYears() > 3) {
            return Optional.of(new Ec2ReportsInvalidParameterException("time range is allowed up to 3 years when using a monthly data granularity"));
          }
        }

        if (request.getGroupBy()!=null) {
          if (request.getGroupBy().getType() == null)
            return Optional.of(new Ec2ReportsInvalidParameterException("In group by parameter, type must be specified"));
          final String groupType = request.getGroupBy().getType().toLowerCase();
          final String groupKey = request.getGroupBy().getKey();
          if (!Sets.newHashSet("tag", "tags", "instancetype", "instance_type",
                  "platform", "platforms", "availabilityzone", "availability_zone").contains(groupType)) {
            return Optional.of(new Ec2ReportsInvalidParameterException("supported types in group by parameter are: tag, instance_type, platform, and availability_zone"));
          }
          if ("tag".equals(groupType) || "tags".equals(groupType)) {
            if (groupKey == null) {
              return Optional.of(new Ec2ReportsInvalidParameterException("tag type in group by parameter must also include tag key"));
            }
          }
        }
        return Optional.empty();
      };
      final Optional<Ec2ReportsServiceException> error = requestVerifier.apply(request);
      if (error.isPresent()) {
        throw error.get();
      }

      final String granularity = request.getGranularity();
      final List<InstanceHourLog> logs = Lists.newArrayList();
      if (granularity.equals("hourly") || granularity.equals("hour")) {
        logs.addAll(InstanceLogs.getInstance().queryHourly(context.getAccountNumber(),
                request.getTimeRangeStart(), request.getTimeRangeEnd(), request.getFilters()));
      } else if (granularity.equals("daily") || granularity.equals("day")) {
        logs.addAll(InstanceLogs.getInstance().queryDaily(context.getAccountNumber(),
                request.getTimeRangeStart(), request.getTimeRangeEnd(), request.getFilters()));
      } else {
        logs.addAll(InstanceLogs.getInstance().queryMonthly(context.getAccountNumber(),
                request.getTimeRangeStart(), request.getTimeRangeEnd(), request.getFilters()));
      }

      Collector<InstanceHourLog,?,Map<String,List<InstanceHourLog>>>collector = null;
      Comparator<String> keySorter = String::compareTo; // determines which column appear first
      if(request.getGroupBy() != null ) {
        final String groupType  = request.getGroupBy().getType().toLowerCase();
        final String groupKey = request.getGroupBy().getKey();
        if ("instancetype".equals(groupType) || "instance_type".equals(groupType)) {
          collector = groupingBy((log) -> log.getInstanceType(), toList());
          keySorter = String::compareTo; // sort by type name is natural
        } else if ("platform".equals(groupType) || "platforms".equals(groupType)) {
          collector = groupingBy((log) -> log.getPlatform(), toList());
          keySorter = String::compareTo; // linux comes before windows
        } else if ("availabilityzone".equals(groupType) || "availability_zone".equals(groupType)) {
          collector = groupingBy((log) -> log.getAvailabilityZone(), toList());
          keySorter = String::compareTo;
        } else if ("tag".equals(groupType) || "tags".equals(groupType)) {
          // map instanceId to tag value where tag key = group key
          final Map<String, String> tagValueMap =
                  logs.stream()
                          .collect( groupingBy ((log) -> log.getInstanceId(), toList()))
                          .entrySet().stream()
                          .map( e -> e.getValue().get(0) )
                          .filter(
                                  l -> l.getTags().stream().filter( t -> groupKey.equals(t.getKey()) )
                                          .findAny().isPresent()
                          ).collect(
                          Collectors.toMap(
                                  l -> l.getInstanceId(),
                                  l -> l.getTags().stream().filter( t -> groupKey.equals(t.getKey()) ).findAny().get().getValue()
                          )
                  );
          logs.stream().forEach(
                  l -> {
                    if (!tagValueMap.containsKey(l.getInstanceId()))
                      tagValueMap.put(l.getInstanceId(), "[UNTAGGED]");
                  }
          );
          collector = groupingBy((log) -> tagValueMap.get(log.getInstanceId()), toList());
          keySorter = (s1, s2) -> {
            if("[UNTAGGED]".equals(s1) && "[UNTAGGED]".equals(s2))
              return 0;
            else if ("[UNTAGGED]".equals(s1))
              return -1;
            else if ("[UNTAGGED]".equals(s2))
              return 1;
            else
              return s1.compareTo(s2);
          };
        } else {
          throw new Ec2ReportsInvalidParameterException("supported types in group by parameter are: tag, instance_type, platform, and availability_zone");
        }
      } else {
        collector = groupingBy((log) -> "[INSTANCE HOURS]", toList());
      }
      final Function<Date, AbstractMap.SimpleEntry<Date,Date>> ranger = (logTime) -> {
        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        start.setTime(logTime);
        end.setTime(logTime);
        if (granularity.equals("hourly") || granularity.equals("hour")) {
          end.set(Calendar.HOUR_OF_DAY, end.get(Calendar.HOUR_OF_DAY) +1);
        } else if (granularity.equals("daily") || granularity.equals("day")) {
          end.set(Calendar.DAY_OF_MONTH, start.get(Calendar.DAY_OF_MONTH) + 1);
          start.set(Calendar.HOUR_OF_DAY, 0);
          end.set(Calendar.HOUR_OF_DAY, 0);
        } else {
          end.set(Calendar.MONTH, start.get(Calendar.MONTH) + 1);
          start.set(Calendar.DAY_OF_MONTH, 1);
          start.set(Calendar.HOUR_OF_DAY, 0);
          end.set(Calendar.DAY_OF_MONTH, 1);
          end.set(Calendar.HOUR_OF_DAY, 0);
        }
        for (final int flag : new int[]{ Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND } ) {
          start.set(flag, 0);
          end.set(flag, 0);
        }
        return new AbstractMap.SimpleEntry<Date, Date>(start.getTime(), end.getTime());
      };

      // sum over instance hours whose log_time is the same
        /* e.g.,
           INPUT:
           m1.small -> [(i-1d9eb607,2017-03-16 12:00:00), (i-1d9eb607,2017-03-16 13:00:00), (i-fe4a9384,2017-03-16 13:00:00)]

           OUTPUT:
           m1.small -> [2017-03-16 12:00:00: 1, 2017-03-16 13:00:00: 2]
         */
      final Map<String, List<InstanceHourLog>> logsByGroupKey = logs.stream().collect(collector);
      final Map<String, Map<Date, Long>> hoursAtLogTimeByGroupKey = logsByGroupKey.entrySet().stream()
              .collect(Collectors.toMap(
                      e -> e.getKey(),
                      e -> e.getValue().stream()
                              .collect(
                                      groupingBy( l -> l.getLogTime(), summingLong(l -> l.getHours())) // -> Map<String, Map<Date, Long>
                              )
                      )
              );
      // key -> [(log_time, hours)...]
      final Map<String, List<AbstractMap.SimpleEntry<Date, Long>>> instanceHoursAtLogTime = hoursAtLogTimeByGroupKey.entrySet().stream()
              .collect(Collectors.toMap(
                      e -> e.getKey(),
                      e -> e.getValue().entrySet().stream()
                              .map( e1 -> new AbstractMap.SimpleEntry<Date, Long>( e1.getKey(), e1.getValue()))
                              .collect(toList())
                      )
              );

      final StringBuilder sb = new StringBuilder();
      sb.append("Start Time,End Time");
      for (final String key : instanceHoursAtLogTime.keySet().stream().sorted(keySorter).collect(toList())) {
        sb.append(String.format(",%s", key));
      }

      // fill-in missing logs (with 0 hours) in the table
      final Set<Date> distinctLogs = instanceHoursAtLogTime.values().stream()
              .flatMap(e -> e.stream())
              .map( kv -> kv.getKey() )
              .distinct()
              .collect(toSet());
      for(final String groupKey : instanceHoursAtLogTime.keySet()) {
        final List<AbstractMap.SimpleEntry<Date, Long>> hours = instanceHoursAtLogTime.get(groupKey);
        final Set<Date> currentLogs = hours.stream().map(kv -> kv.getKey()).collect(toSet());
        final List<AbstractMap.SimpleEntry<Date, Long>> normalized = Lists.newArrayList(hours);
        for (final Date missing: Sets.difference(distinctLogs, currentLogs)) {
          normalized.add(new AbstractMap.SimpleEntry<Date, Long>(missing, 0L));
        }
        instanceHoursAtLogTime.put(groupKey, normalized);
      }

      final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      final Map<Date, String> lines = Maps.newHashMap();
      for (final String groupKey : instanceHoursAtLogTime.keySet().stream().sorted(keySorter).collect(toList())) {
        for (final AbstractMap.SimpleEntry<Date, Long> hl : instanceHoursAtLogTime.get(groupKey)) {
          final Date logTime = hl.getKey();
          final Long hours = hl.getValue();
          if (!lines.containsKey(logTime)) {
            lines.put(logTime, String.format("%s,%s,%d", df.format(ranger.apply(logTime).getKey()),
                    df.format(ranger.apply(logTime).getValue()), hours));
          } else {
            lines.put(logTime, String.format("%s,%d", lines.get(logTime), hours));
          }
        }
      }

      lines.entrySet().stream()
              .sorted( (e1, e2) -> e1.getKey().compareTo(e2.getKey()) ) // order by log time
              .map ( e -> e.getValue() )
              .forEach( s -> sb.append(String.format("\n%s", s)));
      final ViewInstanceUsageResult result = new ViewInstanceUsageResult();
      result.setUsageReport(sb.toString());
      response.setResult(result);
    } catch(final Ec2ReportsServiceException ex) {
      throw ex;
    } catch (final Exception ex) {
      handleException(ex);
    }
    return response;
  }

  public ViewReservedInstanceUtilizationReportResponseType viewReservedInstanceUtilizationReport(final ViewReservedInstanceUtilizationReportType request)
          throws Ec2ReportsServiceException {
    final ViewReservedInstanceUtilizationReportResponseType response = request.getReply();
    final Context context = checkAuthorized( );

    final ViewReservedInstanceUtilizationResult result = new ViewReservedInstanceUtilizationResult();
    result.setUtilizationReport("Start Time, End Time, TAG");
    response.setResult(result);
    return response;
  }

  private static Context checkAuthorized( ) throws Ec2ReportsServiceUnauthorizedException {
    final Context ctx = Contexts.lookup( );
    final AuthContextSupplier requestUserSupplier = ctx.getAuthContext( );
    if ( !Permissions.isAuthorized(
            Ec2ReportsPolicySpec.VENDOR_EC2REPORTS,
            "",
            "",
            null,
            getIamActionByMessageType( ),
            requestUserSupplier ) ) {
      throw new Ec2ReportsServiceUnauthorizedException(
              "UnauthorizedOperation",
              "You are not authorized to perform this operation." );
    }
    return ctx;
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static Ec2ReportsServiceException handleException( final Exception e  ) throws Ec2ReportsServiceException {
    Exceptions.findAndRethrow( e, Ec2ReportsServiceException.class );

    LOG.error( e, e );

    final Ec2ReportsServiceException exception = new Ec2ReportsServiceException( "InternalError", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }

  public static <T, A, R> Collector<T, A, R> filtering(
          Predicate<? super T> filter, Collector<T, A, R> downstream) {
    BiConsumer<A, T> accumulator = downstream.accumulator();
    Set<Collector.Characteristics> characteristics = downstream.characteristics();
    return Collector.of(downstream.supplier(), (acc, t) -> {
              if(filter.test(t)) accumulator.accept(acc, t);
            }, downstream.combiner(), downstream.finisher(),
            characteristics.toArray(new Collector.Characteristics[0]));
  };
}
