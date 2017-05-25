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
@GroovyAddClassUUID
package com.eucalyptus.cluster.common.msgs

import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

class SensorsResourceType extends EucalyptusData {
  String resourceName
  String resourceType
  String resourceUuid
  ArrayList<MetricsResourceType> metrics = new ArrayList<MetricsResourceType>()
}

class MetricsResourceType extends EucalyptusData {
  String metricName
  ArrayList<MetricCounterType> counters = new ArrayList<MetricCounterType>()
}

class MetricCounterType extends EucalyptusData {
  String type
  Long collectionIntervalMs
  ArrayList<MetricDimensionsType> dimensions = new ArrayList<MetricDimensionsType>()
}

class MetricDimensionsType extends EucalyptusData {
  String dimensionName
  Long sequenceNum
  ArrayList<MetricDimensionsValuesType> values = new ArrayList<MetricDimensionsValuesType>()
}

class MetricDimensionsValuesType extends EucalyptusData {
  Date timestamp
  Double value
}
