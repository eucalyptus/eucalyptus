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
 ************************************************************************/package com.eucalyptus.cluster.service.metrics;

import java.util.Map;

/**
 *
 */
public final class SensorData {
  private final String id;
  private final String uuid;
  private final long created;
  private final Map<String, MetricValues> metricValues;

  public SensorData(
      final String id,
      final String uuid,
      final long created,
      final Map<String, MetricValues> metricValues
  ) {
    this.id = id;
    this.uuid = uuid;
    this.created = created;
    this.metricValues = metricValues;
  }
}
