/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
 * Please contact Eucalyptus Systems, Inc., 6750 Navigator Way, Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.util.metrics;

public enum MonitoredAction {
  // CW
  CLUSTER_SIZE("Cluster:Timing:dataBatch.size"),
  CLUSTER_DEAL_WITH_ABSOLUTE_METRICS("Cluster:Timing:dataBatch.dealWithAbsoluteMetrics():time"),
  CLUSTER_FOLD_METRICS("Cluster:Timing:dataBatch.foldMetrics():time"),
  CLUSTER_CONVERT_TO_PUT_METRIC_DATA_LIST("Cluster:Timing:dataBatch.convertToPutMetricDataList():time"),
  CLUSTER_CONSOLIDATE_PUT_METRIC_DATA_LIST("Cluster:Timing:dataBatch.consolidatePutMetricDataList():time"),
  CLUSTER_LIST_METRIC_MANAGER_CALL_PUT_METRIC_DATA("Cluster:Timing:ListMetricManager.callPutMetricData():time"),
  CLUSTER_TIMING("Cluster:Timing:time"),
  PUT_DATA_QUEUE_SIZE("PutMetricDataQueue:Timing:dataBatch.size"),
  PUT_DATA_QUEUE_CONVERT("PutMetricDataQueue:Timing:dataBatch.convertToSimpleDataBatch():time"),
  PUT_DATA_QUEUE_AGGREGATE("PutMetricDataQueue:Timing:dataBatch.aggregate():time"),
  PUT_DATA_QUEUE_MERTIC_ADD_BATCH("PutMetricDataQueue:Timing:dataBatch.MetricManager.addMetricBatch():time"),
  PUT_DATA_QUEUE_MERTIC_QUEUE_ADDALL("PutMetricDataQueue:Timing:ListMetricQueue.addAll():time"),
  PUT_DATA_TIMING("PutMetricDataQueue:Timing:time"),
  LIST_METRIC_SIZE("ListMetricQueue:Timing:dataBatch.size"),
  LIST_METRIC_PRUNE("ListMetricQueue:Timing:dataBatch.pruneDuplicates:time"),
  LIST_METRIC_CONVERT("ListMetricQueue:Timing:convertToListMetrics:time"),
  LIST_METRIC_MERTIC_ADD_BATCH("ListMetricQueue:Timing:ListMetricManager.addMetricBatch:time"),
  LIST_METRIC_TIMING("ListMetricQueue:Timing:time"),
  // Storage
  CREATE_VOLUME("Create Volume"),
  CREATE_VOLUME_FROM_SNAPSHOT("Create Volume from Snapshot"),
  CREATE_SNAPSHOT("Create Snapshot"),
  EXPORT_VOLUME("Export Volume"),
  UNEXPORT_VOLUME("Unexport Volume"),
  DELETE_VOLUME("Delete Volume"),
  DELETE_SNAPSHOT("Delete Snapshot");
  
  String name;
  private MonitoredAction(String name) {
    this.name = name;
  }
  
  @Override
  public String toString() { return name; }
}