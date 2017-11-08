/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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