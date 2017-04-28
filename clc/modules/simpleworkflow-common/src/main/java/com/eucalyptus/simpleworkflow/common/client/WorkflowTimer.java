/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.simpleworkflow.common.client;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.Topology;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.system.Ats;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class WorkflowTimer implements EventListener<Hertz> {
  private static Logger LOG  = Logger.getLogger( WorkflowTimer.class );

  private static Map<Class<?>, Integer> onetimeWorkflows = Maps.newConcurrentMap();
  private static Set<Class<?>> repeatingWorkflows = Sets.newConcurrentHashSet();
  private static Set<Class<?>> hourlyWorkflows = Sets.newConcurrentHashSet();
  private static Set<Class<?>> dailyWorkflows = Sets.newConcurrentHashSet();
  private static ConcurrentMap<Class<?>, Long> lastExecution = Maps.newConcurrentMap();

  static void addHourlyWorkflow(@Nonnull final Class<?> workflowImpl) {
    hourlyWorkflows.add(workflowImpl);
  }
  static void addDailyWorkflow(@Nonnull final Class<?> workflowImpl) {
    dailyWorkflows.add(workflowImpl);
  }
  static void addRepeatingWorkflow(@Nonnull final Class<?> workflowImpl) {
    lastExecution.put(workflowImpl, System.currentTimeMillis());
    repeatingWorkflows.add(workflowImpl);
  }
  static void addOnceWorkflow(@Nonnull final Class<?> workflowImpl) {
    onetimeWorkflows.put( workflowImpl, 0);
  }

  static List<Class<?>> listHourlyWorkflows() {
    return hourlyWorkflows.stream().collect(Collectors.toList());
  }
  static List<Class<?>> listDailyWorkflows() { return dailyWorkflows.stream().collect(Collectors.toList());}
  static List<Class<?>> listRepeatingWorkflows() { return repeatingWorkflows.stream().collect(Collectors.toList());}

  static final int minute(long timestamp) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(new Date(timestamp));
    return cal.get(Calendar.MINUTE);
  }

  static final int hour(long timestamp) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(new Date(timestamp));
    return cal.get(Calendar.HOUR_OF_DAY);
  }

  static final int day(long timestamp) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(new Date(timestamp));
    return cal.get(Calendar.DAY_OF_YEAR);
  }

  static List<WorkflowStarter> markAndGetRepeatingStarters() {
    try {
      final List<WorkflowStarter> starters = Lists.newArrayList();
      listRepeatingWorkflows().stream().forEach((impl) -> {
        try {
          final Repeating ats = Ats.inClassHierarchy(impl).get(Repeating.class);
          if (Topology.isEnabled(ats.dependsOn())) {
            final Long lastRun = lastExecution.get(impl);
            if (lastRun != null) {
              final DateTime now = new DateTime(System.currentTimeMillis());
              final DateTime sleepBegin = now.minusSeconds(ats.sleepSeconds());
              final Interval interval = new Interval(sleepBegin, now);
              if (!interval.contains(new DateTime(lastRun))) {
                if (lastExecution.replace(impl, lastRun, System.currentTimeMillis())) {
                  starters.add(ats.value().newInstance());
                }
              }
            }
          }
        } catch (InstantiationException ex) {
          ;
        } catch (IllegalAccessException ex) {
          ;
        }
      });
      return starters;
    } catch (final Exception ex) {
      return Lists.newArrayList();
    }

  }

  static List<WorkflowStarter> markAndGetHourlyStarters() {
    try {
      final List<WorkflowStarter> starters = Lists.newArrayList();
      listHourlyWorkflows().stream().forEach( (impl) -> {
        try {
          final Hourly ats = Ats.inClassHierarchy(impl).get(Hourly.class);
          final Long now = System.currentTimeMillis();
          final Long lastRun = lastExecution.get(impl);

          if ((minute(now) == ats.minute()
                  && !Arrays.stream(ats.skipHour()).anyMatch(n -> n==hour(now)))
                  && (lastRun == null || hour(lastRun) != hour(now))) {
            if (lastRun!=null) {
              if(lastExecution.replace(impl, lastRun, now)) {
                starters.add(ats.value().newInstance());
              }
            } else {
              if(lastExecution.putIfAbsent(impl, now) == null) {
                starters.add(ats.value().newInstance());
              }
            }
          }
        } catch (InstantiationException ex) {
          ;
        } catch (IllegalAccessException ex) {
          ;
        }
      });
      return starters;
    } catch (final Exception ex) {
      return Lists.newArrayList();
    }
  }

  static List<WorkflowStarter> markAndGetDailyStarters() {
    try {
      final List<WorkflowStarter> starters = Lists.newArrayList();
      listDailyWorkflows().stream().forEach( impl -> {
        try {
          final Daily ats = Ats.inClassHierarchy(impl).get(Daily.class);
          final Long now = System.currentTimeMillis();
          final Long lastRun = lastExecution.get(impl);
          if (hour(now) == ats.hour() && minute(now) == ats.minute() &&
                  (lastRun == null || day(lastRun) != day(now))) {
            if (lastRun!=null) {
              if(lastExecution.replace(impl, lastRun, now)) {
                starters.add(ats.value().newInstance());
              }
            } else {
              if(lastExecution.putIfAbsent(impl, now) == null) {
                starters.add(ats.value().newInstance());
              }
            }
          }
        } catch (InstantiationException ex) {
          ;
        } catch (IllegalAccessException ex) {
          ;
        }
      });
      return starters;
    } catch (final Exception ex) {
      return Lists.newArrayList();
    }
  }

  static synchronized List<WorkflowStarter> getOnetimeStarters() {
    try {
      List<WorkflowStarter> starters = Lists.newArrayList();
      onetimeWorkflows.keySet().stream().forEach( impl ->  {
        try {
          final Once ats = Ats.inClassHierarchy(impl).get(Once.class);
          if (Topology.isEnabled(ats.dependsOn())) {
            if (onetimeWorkflows.get(impl) < ats.retry()) {
              starters.add(ats.value().newInstance());
            }
          }
        } catch (InstantiationException ex) {
          ;
        } catch (IllegalAccessException ex) {
          ;
        }
      });
      return starters;
    } catch (final Exception ex) {
      return Lists.newArrayList();
    }
  }

  static synchronized void markOneTimeStarterFailure(final WorkflowStarter starter) {
    try {
      for (final Class<?> cls : onetimeWorkflows.keySet()) {
        if (Ats.inClassHierarchy(cls).get(Once.class).value().equals(starter.getClass())) {
          onetimeWorkflows.put(cls, onetimeWorkflows.get(cls)+1);
          break;
        }
      }
    } catch (final Exception ex) {
      ;
    }
  }

  static synchronized  void markOneTimeStarterSuccess(final WorkflowStarter starter) {
    try {
      final Optional<Class<?>> found = onetimeWorkflows.keySet().stream()
              .filter(impl -> Ats.inClassHierarchy(impl).get(Once.class).value().equals(starter.getClass()))
              .findAny();
      if (found.isPresent()) {
        onetimeWorkflows.remove(found.get());
      }
    }catch (final Exception ex) {
      ;
    }
  }

  public static void register() {
    Listeners.register(Hertz.class, new WorkflowTimer());
  }

  @Override
  public void fireEvent(Hertz event) {
    if (!Bootstrap.isOperational() || !BootstrapArgs.isCloudController() || !Topology.isEnabled(SimpleWorkflow.class)) {
      return;
    }

    for (final WorkflowStarter starter : markAndGetDailyStarters()) {
      try {
        starter.start();
      } catch (final Exception ex) {
        LOG.error("Failed to start daily workflow: " + starter.name(), ex);
      }
    }

    for (final WorkflowStarter starter : markAndGetHourlyStarters()) {
      try {
        starter.start();
      } catch (final Exception ex) {
        LOG.error("Failed to start hourly workflow: " + starter.name(), ex);
      }
    }

    for (final WorkflowStarter starter : markAndGetRepeatingStarters()) {
      try {
        starter.start();
      } catch (final  Exception ex) {
        LOG.error("Failed to start repeating workflow: " + starter.name(), ex);
      }
    }

    for (final WorkflowStarter starter : getOnetimeStarters()) {
      try {
        starter.start();
        markOneTimeStarterSuccess(starter);
      } catch (final Exception ex) {
        markOneTimeStarterFailure(starter);
        LOG.error("Failed to start one-time workflow (will retry): " + starter.name(), ex);
      }
    }
  }
}