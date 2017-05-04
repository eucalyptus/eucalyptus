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

import java.util.List;
import com.eucalyptus.util.Pair;

/**
 *
 */
public final class MetricDimension {
  private final String name;
  private final int sequence;
  private final List<Pair<Long, String>> values; // timestamp and value

  public MetricDimension(
      final String name,
      final int sequence,
      final List<Pair<Long, String>> values
  ) {
    this.name = name;
    this.sequence = sequence;
    this.values = values;
  }
}
