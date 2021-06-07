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
package com.eucalyptus.cloudwatch.service;

import com.eucalyptus.auth.AuthContextSupplier;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties;
import com.eucalyptus.cloudwatch.common.internal.domain.InvalidTokenException;
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricUtils;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import com.eucalyptus.cloudwatch.common.msgs.Datapoint;
import com.eucalyptus.cloudwatch.common.msgs.Datapoints;
import com.eucalyptus.cloudwatch.common.msgs.GetMetricStatisticsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.GetMetricStatisticsType;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsResult;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsType;
import com.eucalyptus.cloudwatch.common.msgs.Metric;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.Metrics;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.common.msgs.ResponseMetadata;
import com.eucalyptus.cloudwatch.common.msgs.Statistics;
import com.eucalyptus.cloudwatch.common.policy.CloudWatchPolicySpec;
import com.eucalyptus.cloudwatch.service.queue.metricdata.MetricDataQueue;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.util.async.AsyncExceptions;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;

/**
 *
 */
@ComponentNamed
public class CloudWatchService {

  private static final Logger LOG = Logger.getLogger(CloudWatchService.class);

  public PutMetricDataResponseType putMetricData(PutMetricDataType request)
    throws CloudWatchException {
    PutMetricDataResponseType reply = request.getReply();
    long before = System.currentTimeMillis();
    final Context ctx = Contexts.lookup();

    try {
      // IAM Action Check
      checkActionPermission(CloudWatchPolicySpec.CLOUDWATCH_PUTMETRICDATA, ctx);
      if (CloudWatchConfigProperties.isDisabledCloudWatchService()) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String namespace = CloudWatchServiceFieldValidator.validateNamespace(request.getNamespace(), true);
      MetricType metricType = CloudWatchServiceFieldValidator.getMetricTypeFromNamespace(namespace);
      final Boolean privileged = Contexts.lookup().isPrivileged();
      if (metricType == MetricType.System && !privileged) {
        throw new InvalidParameterValueException("The value AWS/ for parameter Namespace is invalid.");
      }
      final List<MetricDatum> metricData = CloudWatchServiceFieldValidator.validateMetricData(request.getMetricData(), metricType);
      LOG.trace("Namespace=" + namespace);
      LOG.trace("metricData="+metricData);
      MetricDataQueue.getInstance().insertMetricData(ownerFullName.getAccountNumber(), namespace, metricData, metricType);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public ListMetricsResponseType listMetrics(ListMetricsType request)
    throws CloudWatchException {
    ListMetricsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      // IAM Action Check
      checkActionPermission(CloudWatchPolicySpec.CLOUDWATCH_LISTMETRICS, ctx);

      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String namespace = CloudWatchServiceFieldValidator.validateNamespace(request.getNamespace(), false);
      final String metricName = CloudWatchServiceFieldValidator.validateMetricName(request.getMetricName(),
        false);
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionFiltersToMap.INSTANCE
        .apply(CloudWatchServiceFieldValidator.validateDimensionFilters(request.getDimensions()));

      // take all stats updated after two weeks ago
      final Date after = new Date(System.currentTimeMillis() - 2 * 7 * 24 * 60
        * 60 * 1000L);
      final Date before = null; // no bound on time before stats are updated
      // (though maybe 'now')
      final Integer maxRecords = 500; // per the API docs
      final String nextToken = request.getNextToken();
      final List<ListMetric> results;
      try {
        results = ListMetricManager.listMetrics(
          ownerFullName.getAccountNumber(), metricName, namespace,
          dimensionMap, after, before, maxRecords, nextToken);
      } catch (InvalidTokenException e) {
        // not sure why, but this is the message AWS sends (different from the alarm case, different exception too)
        throw new InvalidParameterValueException("Invalid nextToken");
      }

      final Metrics metrics = new Metrics();
      metrics.setMember(Lists.newArrayList(Collections2
        .<ListMetric, Metric>transform(results,
          TransformationFunctions.ListMetricToMetric.INSTANCE)));
      final ListMetricsResult listMetricsResult = new ListMetricsResult();
      listMetricsResult.setMetrics(metrics);
      if (maxRecords != null && results.size() == maxRecords) {
        listMetricsResult.setNextToken(results.get(results.size() - 1)
          .getNaturalId());
      }
      reply.setListMetricsResult(listMetricsResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }
  public GetMetricStatisticsResponseType getMetricStatistics(
    GetMetricStatisticsType request) throws CloudWatchException {
    GetMetricStatisticsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    try {
      // IAM Action Check
      checkActionPermission(CloudWatchPolicySpec.CLOUDWATCH_GETMETRICSTATISTICS, ctx);

      // TODO: parse statistics separately()?
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      Statistics statistics = CloudWatchServiceFieldValidator.validateStatistics(request.getStatistics());
      final String namespace = CloudWatchServiceFieldValidator.validateNamespace(request.getNamespace(), true);
      final String metricName = CloudWatchServiceFieldValidator.validateMetricName(request.getMetricName(),
        true);
      final Date startTime = MetricUtils.stripSeconds(CloudWatchServiceFieldValidator.validateStartTime(request.getStartTime(), true));
      final Date endTime = MetricUtils.stripSeconds(CloudWatchServiceFieldValidator.validateEndTime(request.getEndTime(), true));
      final Integer period = CloudWatchServiceFieldValidator.validatePeriod(request.getPeriod(), true);
      CloudWatchServiceFieldValidator.validateDateOrder(startTime, endTime, "StartTime", "EndTime", true,
        true);
      CloudWatchServiceFieldValidator.validateNotTooManyDataPoints(startTime, endTime, period, 1440L);

      // TODO: null units here does not mean Units.NONE but basically a
      // wildcard.
      // Consider this case.
      final Units units = CloudWatchServiceFieldValidator.validateUnits(request.getUnit(), false);
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionsToMap.INSTANCE
        .apply(CloudWatchServiceFieldValidator.validateDimensions(request.getDimensions()));
      Collection<MetricStatistics> metrics;
      metrics = MetricManager.getMetricStatistics(new MetricManager.GetMetricStatisticsParams(
        ownerFullName.getAccountNumber(), metricName, namespace,
        dimensionMap, CloudWatchServiceFieldValidator.getMetricTypeFromNamespace(namespace), units,
        startTime, endTime, period));
      reply.getGetMetricStatisticsResult().setLabel(metricName);
      ArrayList<Datapoint> datapoints = CloudWatchServiceFieldValidator.convertMetricStatisticsToDatapoints(
        statistics, metrics);
      if (datapoints.size() > 0) {
        Datapoints datapointsReply = new Datapoints();
        datapointsReply.setMember(datapoints);
        reply.getGetMetricStatisticsResult().setDatapoints(datapointsReply);
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public CloudWatchMessage dispatchAction( final CloudWatchMessage request ) throws EucalyptusCloudException {
    final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );
    if ( !Permissions.perhapsAuthorized(CloudWatchPolicySpec.VENDOR_CLOUDWATCH, getIamActionByMessageType( request ), user ) ) {
      throw new CloudWatchAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }

    try {
      @SuppressWarnings( "unchecked" )
      final CloudWatchMessage backendRequest = (CloudWatchMessage) BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final BaseMessage backendResponse = send( backendRequest );
      final CloudWatchMessage response = (CloudWatchMessage) BaseMessages.deepCopy( backendResponse, request.getReply().getClass() );
      final ResponseMetadata metadata = CloudWatchMessage.getResponseMetadata( response );
      if ( metadata != null ) {
        metadata.setRequestId( request.getCorrelationId( ) );
      }
      response.setCorrelationId( request.getCorrelationId( ) );
      return response;
    } catch ( Exception e ) {
      handleRemoteException( e );
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  private static Class getBackendMessageClass( final BaseMessage request ) throws ClassNotFoundException {
    return Class.forName( request.getClass( ).getName( ).replace( ".common.msgs.", ".common.backend.msgs." ) );
  }

  private static BaseMessage send( final BaseMessage request ) throws Exception {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( CloudWatchBackend.class ), request );
    } catch ( NoSuchElementException e ) {
      throw new CloudWatchUnavailableException( "Service Unavailable" );
    } catch ( final FailedRequestException e ) {
      if ( request.getReply( ).getClass( ).isInstance( e.getRequest( ) ) ) {
        return e.getRequest( );
      }
      throw e.getRequest( ) == null ?
          e :
          new CloudWatchException( "InternalError", Role.Receiver, "Internal error " + e.getRequest().getClass().getSimpleName() + ":false" );
    }
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  private void handleRemoteException( final Exception e ) throws EucalyptusCloudException {
    final Optional<AsyncExceptions.AsyncWebServiceError> serviceErrorOption = AsyncExceptions.asWebServiceError( e );
    if ( serviceErrorOption.isPresent( ) ) {
      final AsyncExceptions.AsyncWebServiceError serviceError = serviceErrorOption.get( );
      final String code = serviceError.getCode( );
      final String message = serviceError.getMessage( );
      switch( serviceError.getHttpErrorCode( ) ) {
        case 400:
          throw new CloudWatchClientException( code, message );
        case 403:
          throw new CloudWatchAuthorizationException( code, message );
        case 404:
          throw new CloudWatchNotFoundException( code, message );
        case 503:
          throw new CloudWatchUnavailableException( message );
        default:
          throw new CloudWatchException( code, Role.Receiver, message );
      }
    }
  }

  private static final int DISABLED_SERVICE_FAULT_ID = 1500;
  private boolean alreadyFaulted = false;
  private void faultDisableCloudWatchServiceIfNecessary() {
    if (!alreadyFaulted) {
      Faults.forComponent(CloudWatch.class).havingId(DISABLED_SERVICE_FAULT_ID).withVar("component", "cloudwatch").log();
      alreadyFaulted = true;
    }

  }

  private void checkActionPermission(final String actionType, final Context ctx)
    throws EucalyptusCloudException {
    if (!Permissions.isAuthorized(CloudWatchPolicySpec.VENDOR_CLOUDWATCH, actionType, "",
      ctx.getAccount(), actionType, ctx.getAuthContext())) {
      throw new EucalyptusCloudException("User does not have permission");
    }
  }

  private static void handleException(final Exception e)
    throws CloudWatchException {
    final CloudWatchException cause = Exceptions.findCause(e,
      CloudWatchException.class);
    if (cause != null) {
      throw cause;
    }

    final InternalFailureException exception = new InternalFailureException(
      String.valueOf(e.getMessage()));
    if (Contexts.lookup().hasAdministrativePrivileges()) {
      exception.initCause(e);
    }
    throw exception;
  }
}
