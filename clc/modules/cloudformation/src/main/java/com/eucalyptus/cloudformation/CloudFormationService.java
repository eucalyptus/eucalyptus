/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cloudformation;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.cloudformation.common.policy.CloudFormationPolicySpec;
import com.eucalyptus.cloudformation.config.CloudFormationProperties;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.entity.StackEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.template.PseudoParameterValues;
import com.eucalyptus.cloudformation.template.Template;
import com.eucalyptus.cloudformation.template.TemplateParser;
import com.eucalyptus.cloudformation.template.url.S3Helper;
import com.eucalyptus.cloudformation.template.url.WhiteListURLMatcher;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflow;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowClient;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowDescriptionTemplate;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflow;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowClient;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowDescriptionTemplate;
import com.eucalyptus.cloudformation.workflow.MonitorCreateStackWorkflow;
import com.eucalyptus.cloudformation.workflow.MonitorCreateStackWorkflowClient;
import com.eucalyptus.cloudformation.workflow.MonitorCreateStackWorkflowDescriptionTemplate;
import com.eucalyptus.cloudformation.workflow.StartTimeoutPassableWorkflowClientFactory;
import com.eucalyptus.cloudformation.workflow.WorkflowClientManager;
import com.eucalyptus.cloudformation.ws.StackWorkflowTags;
import com.eucalyptus.component.*;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesResponseType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.SslSetup;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.IO;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.netflix.glisten.InterfaceBasedWorkflowClient;
import com.netflix.glisten.WorkflowClientFactory;
import com.netflix.glisten.WorkflowDescriptionTemplate;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;


import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.SSLHandshakeException;

@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class CloudFormationService {

  @ConfigurableField(initial = "", description = "The value of AWS::Region and value in CloudFormation ARNs for Region")
  public static volatile String REGION = "";

  @ConfigurableField(initial = "*.s3.amazonaws.com", description = "A comma separated white list of domains (other than Eucalyptus S3 URLs) allowed by CloudFormation URL parameters")
  public static volatile String URL_DOMAIN_WHITELIST = "*s3.amazonaws.com";

  private static final String NO_ECHO_PARAMETER_VALUE = "****";

  private static final String STACK_ID_PREFIX = "arn:aws:cloudformation:";

  private static final Logger LOG = Logger.getLogger(CloudFormationService.class);

  public CancelUpdateStackResponseType cancelUpdateStack( CancelUpdateStackType request ) throws CloudFormationException {
    return request.getReply( );
  }

  public CreateStackResponseType createStack( final CreateStackType request ) throws CloudFormationException {
    CreateStackResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String userId = user.getUserId();
      final String accountId = user.getAccount().getAccountNumber();
      final String accountName = user.getAccount().getName();
      final String stackName = request.getStackName();
      final String templateBody = request.getTemplateBody();
      final String templateUrl = request.getTemplateURL();
      if (stackName == null) throw new ValidationErrorException("Stack name is null");
      if (!stackName.matches("^[\\p{Alpha}][\\p{Alnum}-]*$")) {
        throw new ValidationErrorException("Stack name " + stackName + " must contain only letters, numbers, dashes and start with an alpha character.");
      }
      if (stackName.length() > Limits.STACK_NAME_MAX_LENGTH_CHARS) {
        throw new ValidationErrorException("Stack name " + stackName + " must be no longer than " + Limits.STACK_NAME_MAX_LENGTH_CHARS + " characters.");
      }

      if (templateBody == null && templateUrl == null) throw new ValidationErrorException("Either TemplateBody or TemplateURL must be set.");
      if (templateBody != null && templateUrl != null) throw new ValidationErrorException("Exactly one of TemplateBody or TemplateURL must be set.");
      List<Parameter> parameters = null;
      if (request.getParameters() != null && request.getParameters().getMember() != null) {
        parameters = request.getParameters().getMember();
      }

      final String stackIdLocal = UUID.randomUUID().toString();
      final String stackId = STACK_ID_PREFIX + REGION + ":" + accountId + ":stack/"+stackName+"/"+stackIdLocal;
      final PseudoParameterValues pseudoParameterValues = new PseudoParameterValues();
      pseudoParameterValues.setAccountId(accountId);
      pseudoParameterValues.setStackName(stackName);
      pseudoParameterValues.setStackId(stackId);
      if (request.getNotificationARNs() != null && request.getNotificationARNs().getMember() != null) {
        ArrayList<String> notificationArns = Lists.newArrayList();
        for (String notificationArn: request.getNotificationARNs().getMember()) {
          notificationArns.add(notificationArn);
        }
        pseudoParameterValues.setNotificationArns(notificationArns);
      }
      pseudoParameterValues.setRegion(REGION);
      final List<String> defaultRegionAvailabilityZones = describeAvailabilityZones(userId);
      final Map<String, List<String>> availabilityZones = Maps.newHashMap();
      availabilityZones.put(REGION, defaultRegionAvailabilityZones);
      availabilityZones.put("",defaultRegionAvailabilityZones); // "" defaults to the default region
      pseudoParameterValues.setAvailabilityZones(availabilityZones);
      final ArrayList<String> capabilities = Lists.newArrayList();
      if (request.getCapabilities() != null && request.getCapabilities().getMember() != null) {
        capabilities.addAll(request.getCapabilities().getMember());
      }


      if (templateBody != null) {
        if (templateBody.getBytes().length > Limits.REQUEST_TEMPLATE_BODY_MAX_LENGTH_BYTES) {
          throw new ValidationErrorException("Template body may not exceed " + Limits.REQUEST_TEMPLATE_BODY_MAX_LENGTH_BYTES + " bytes in a request.");
        }
      }

      if (request.getTags()!= null && request.getTags().getMember() != null) {
        for (Tag tag: request.getTags().getMember()) {
          if (Strings.isNullOrEmpty(tag.getKey()) || Strings.isNullOrEmpty(tag.getValue())) {
            throw new ValidationErrorException("Tags can not be null or empty");
          } else if (tag.getKey().startsWith("aws:") ) {
            throw new ValidationErrorException("Invalid tag key.  \"aws:\" is a reserved prefix.");
          } else if (tag.getKey().startsWith("euca:") ) {
            throw new ValidationErrorException("Invalid tag key.  \"euca:\" is a reserved prefix.");
          }
        }
      }

      final String templateText = (templateBody != null) ? templateBody : extractTemplateTextFromURL(templateUrl, user);

      final Template template = new TemplateParser().parse(templateText, parameters, capabilities, pseudoParameterValues);


      final Supplier<StackEntity> allocator = new Supplier<StackEntity>() {
        @Override
        public StackEntity get() {
          try {
            StackEntity stackEntity = new StackEntity();
            StackEntityHelper.populateStackEntityWithTemplate(stackEntity, template);
            stackEntity.setStackName(stackName);
            stackEntity.setStackId(stackId);
            stackEntity.setNaturalId(stackIdLocal);
            stackEntity.setAccountId(accountId);
            stackEntity.setTemplateBody(templateText);
            stackEntity.setStackStatus(StackEntity.Status.CREATE_IN_PROGRESS);
            stackEntity.setStackStatusReason("User initiated");
            stackEntity.setDisableRollback(request.getDisableRollback() == Boolean.TRUE); // null -> false
            stackEntity.setCreationTimestamp(new Date());
            if (request.getCapabilities() != null && request.getCapabilities().getMember() != null) {
              stackEntity.setCapabilitiesJson(StackEntityHelper.capabilitiesToJson(capabilities));
            }
            if (request.getNotificationARNs()!= null && request.getNotificationARNs().getMember() != null) {
              stackEntity.setNotificationARNsJson(StackEntityHelper.notificationARNsToJson(request.getNotificationARNs().getMember()));
            }

            if (request.getTags()!= null && request.getTags().getMember() != null) {
              stackEntity.setTagsJson(StackEntityHelper.tagsToJson(request.getTags().getMember()));
            }
            stackEntity.setRecordDeleted(Boolean.FALSE);
            stackEntity = StackEntityManager.addStack(stackEntity);

            // TODO: Arguably everything after here should be considered not part of the allocation of the stack entity

            String onFailure;
            if (request.getOnFailure() != null && !request.getOnFailure().isEmpty()) {
              if (!request.getOnFailure().equals("ROLLBACK") && !request.getOnFailure().equals("DELETE") &&
                !request.getOnFailure().equals("DO_NOTHING")) {
                throw new ValidationErrorException("Value '" + request.getOnFailure() + "' at 'onFailure' failed to satisfy " +
                  "constraint: Member must satisfy enum value set: [ROLLBACK, DELETE, DO_NOTHING]");
              } else {
                onFailure = request.getOnFailure();
              }
            } else {
              onFailure = (request.getDisableRollback() == Boolean.TRUE) ? "DO_NOTHING" : "ROLLBACK";
            }

            for (ResourceInfo resourceInfo: template.getResourceInfoMap().values()) {
              StackResourceEntity stackResourceEntity = new StackResourceEntity();
              stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
              stackResourceEntity.setDescription(""); // TODO: maybe on resource info?
              stackResourceEntity.setResourceStatus(StackResourceEntity.Status.NOT_STARTED);
              stackResourceEntity.setStackId(stackId);
              stackResourceEntity.setStackName(stackName);
              stackResourceEntity.setRecordDeleted(Boolean.FALSE);
              StackResourceEntityManager.addStackResource(stackResourceEntity);
            }

            StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(stackId, stackName, accountId, accountName);
            Long timeoutInSeconds = (request.getTimeoutInMinutes() != null && request.getTimeoutInMinutes()> 0 ? 60L * request.getTimeoutInMinutes() : null);
            StartTimeoutPassableWorkflowClientFactory createStackWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient( ), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
            WorkflowDescriptionTemplate createStackWorkflowDescriptionTemplate = new CreateStackWorkflowDescriptionTemplate();
            InterfaceBasedWorkflowClient<CreateStackWorkflow> createStackWorkflowClient = createStackWorkflowClientFactory
              .getNewWorkflowClient(CreateStackWorkflow.class, createStackWorkflowDescriptionTemplate, stackWorkflowTags, timeoutInSeconds, null);

            CreateStackWorkflow createStackWorkflow = new CreateStackWorkflowClient(createStackWorkflowClient);
            createStackWorkflow.createStack(stackEntity.getStackId(), stackEntity.getAccountId(), stackEntity.getResourceDependencyManagerJson(), userId, onFailure);
            StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
              StackWorkflowEntity.WorkflowType.CREATE_STACK_WORKFLOW,
              CloudFormationProperties.SWF_DOMAIN,
              createStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
              createStackWorkflowClient.getWorkflowExecution().getRunId());

            WorkflowClientFactory monitorCreateStackWorkflowClientFactory = new WorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
            WorkflowDescriptionTemplate monitorCreateStackWorkflowDescriptionTemplate = new MonitorCreateStackWorkflowDescriptionTemplate();
            InterfaceBasedWorkflowClient<MonitorCreateStackWorkflow> monitorCreateStackWorkflowClient = monitorCreateStackWorkflowClientFactory
              .getNewWorkflowClient(MonitorCreateStackWorkflow.class, monitorCreateStackWorkflowDescriptionTemplate, stackWorkflowTags);

            MonitorCreateStackWorkflow monitorCreateStackWorkflow = new MonitorCreateStackWorkflowClient(monitorCreateStackWorkflowClient);
            monitorCreateStackWorkflow.monitorCreateStack(stackEntity.getStackId(),  stackEntity.getAccountId(), stackEntity.getResourceDependencyManagerJson(), userId, onFailure);


            StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
              StackWorkflowEntity.WorkflowType.MONITOR_CREATE_STACK_WORKFLOW,
              CloudFormationProperties.SWF_DOMAIN,
              monitorCreateStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
              monitorCreateStackWorkflowClient.getWorkflowExecution().getRunId());

            return stackEntity;
          } catch ( CloudFormationException e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      };

      final StackEntity stackEntity = RestrictedTypes.allocateUnitlessResource(allocator);
      CreateStackResult createStackResult = new CreateStackResult();
      createStackResult.setStackId(stackId);
      reply.setCreateStackResult(createStackResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  private static String extractTemplateTextFromURL(String templateUrl, User user) throws ValidationErrorException {
    final URL url;
    try {
      url = new URL(templateUrl);
    } catch (MalformedURLException e) {
      throw new ValidationErrorException("Invalid template url " + templateUrl);
    }
    // First try straight HTTP GET if url is in whitelist
    boolean inWhitelist = WhiteListURLMatcher.urlIsAllowed(url, URL_DOMAIN_WHITELIST);
    if (inWhitelist) {
      InputStream templateIn = null;
      try {
        final URLConnection connection = SslSetup.configureHttpsUrlConnection( url.openConnection( ) );
        templateIn = connection.getInputStream( );
        long contentLength = connection.getContentLengthLong( );
        if ( contentLength > Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES) {
          throw new ValidationErrorException("Template URL exceeds maximum byte count, " + Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES);
        }
        final byte[] templateData = ByteStreams.toByteArray( new BoundedInputStream( templateIn, Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES + 1 ) );
        if ( templateData.length > Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES) {
          throw new ValidationErrorException("Template URL exceeds maximum byte count, " + Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES);
        }
        return new String( templateData, StandardCharsets.UTF_8 );
      } catch ( UnknownHostException ex ) {
        throw new ValidationErrorException("Invalid template url " + templateUrl);
      } catch ( SSLHandshakeException ex ) {
        throw new ValidationErrorException("HTTPS connection error for " + templateUrl );
      } catch (IOException ex) {
        if ( Strings.nullToEmpty( ex.getMessage( ) ).startsWith( "HTTPS hostname wrong" ) ) {
          throw new ValidationErrorException( "HTTPS connection failed hostname verification for " + templateUrl );
        }
        LOG.info("Unable to connect to whitelisted URL, trying S3 instead");
        LOG.debug(ex, ex);
      } finally {
        IO.close( templateIn );
      }
    }

    // Otherwise, assume the URL is a eucalyptus S3 url...
    String[] validHostBucketSuffixes = new String[]{"walrus", "objectstorage"};
    String[] validServicePaths = new String[]{ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH, ComponentIds.lookup(ObjectStorage.class).getServicePath()};
    String[] validDomains = new String[]{DomainNames.externalSubdomain().relativize( Name.root ).toString( )};
    S3Helper.BucketAndKey bucketAndKey = S3Helper.getBucketAndKeyFromUrl(url, validServicePaths, validHostBucketSuffixes, validDomains);
    try ( final EucaS3Client eucaS3Client = EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( user ) ) ) {
      if (eucaS3Client.getObjectMetadata(bucketAndKey.getBucket(), bucketAndKey.getKey()).getContentLength() > Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES) {
        throw new ValidationErrorException("Template URL exceeds maximum byte count, " + Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES);
      }
      return eucaS3Client.getObjectContent(
          bucketAndKey.getBucket( ),
          bucketAndKey.getKey( ),
          (int) Limits.REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES );
    } catch (Exception ex) {
      LOG.debug("Error getting s3 object content: " + bucketAndKey.getBucket() + "/" + bucketAndKey.getKey());
      LOG.debug(ex, ex);
      throw new ValidationErrorException("Template url is an S3 URL to a non-existent or unauthorized bucket/key.  (bucket=" + bucketAndKey.getBucket() + ", key=" + bucketAndKey.getKey());
    }
  }


  private static List<String> describeAvailabilityZones(String userId) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Eucalyptus.class);
    DescribeAvailabilityZonesType describeAvailabilityZonesType = new DescribeAvailabilityZonesType();
    describeAvailabilityZonesType.setEffectiveUserId(userId);
    DescribeAvailabilityZonesResponseType describeAvailabilityZonesResponseType =
      AsyncRequests.<DescribeAvailabilityZonesType,DescribeAvailabilityZonesResponseType>
        sendSync(configuration, describeAvailabilityZonesType);
    List<String> availabilityZones = Lists.newArrayList();
    for (ClusterInfoType clusterInfoType: describeAvailabilityZonesResponseType.getAvailabilityZoneInfo()) {
      availabilityZones.add(clusterInfoType.getZoneName());

    }
    return availabilityZones;
  }

  public DeleteStackResponseType deleteStack( final DeleteStackType request ) throws CloudFormationException {
    DeleteStackResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String userId = user.getUserId();
      final String accountId = user.getAccount().getAccountNumber();
      final String stackName = request.getStackName();
      if (stackName == null) throw new ValidationErrorException("Stack name is null");
      StackEntity stackEntity = StackEntityManager.getNonDeletedStackByNameOrId(stackName, accountId);
      if ( stackEntity == null && ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ) {
        stackEntity = StackEntityManager.getNonDeletedStackByNameOrId( stackName, null );
      }
      if ( stackEntity != null ) {
        if ( !RestrictedTypes.filterPrivileged( ).apply( stackEntity ) ) {
          throw new AccessDeniedException( "Not authorized." );
        }
        final String stackAccountId = stackEntity.getAccountId( );

        // check to see if there has been a delete workflow.  If one exists and is still going on, just quit:
        boolean existingOpenDeleteWorkflow = false;
        List<StackWorkflowEntity> deleteWorkflows = StackWorkflowEntityManager.getStackWorkflowEntities(stackEntity.getStackId(), StackWorkflowEntity.WorkflowType.DELETE_STACK_WORKFLOW);
        if ( deleteWorkflows != null && !deleteWorkflows.isEmpty( ) ) {
          if (deleteWorkflows.size() > 1) {
            throw new ValidationErrorException("More than one delete workflow exists for " + stackEntity.getStackId()); // TODO: InternalFailureException (?)
          }
          // see if the workflow is open
          try {
            AmazonSimpleWorkflow simpleWorkflowClient = WorkflowClientManager.getSimpleWorkflowClient();
            StackWorkflowEntity deleteStackWorkflowEntity = deleteWorkflows.get(0);
            DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest = new DescribeWorkflowExecutionRequest();
            describeWorkflowExecutionRequest.setDomain(deleteStackWorkflowEntity.getDomain());
            WorkflowExecution execution = new WorkflowExecution();
            execution.setRunId(deleteStackWorkflowEntity.getRunId());
            execution.setWorkflowId(deleteStackWorkflowEntity.getWorkflowId());
            describeWorkflowExecutionRequest.setExecution(execution);
            WorkflowExecutionDetail workflowExecutionDetail = simpleWorkflowClient.describeWorkflowExecution(describeWorkflowExecutionRequest);
            if ("OPEN".equals(workflowExecutionDetail.getExecutionInfo().getExecutionStatus())) {
              existingOpenDeleteWorkflow = true;
            }
          } catch (Exception ex) {
            LOG.error("Unable to get status of delete workflow for " + stackEntity.getStackId() + ", assuming not open");
            LOG.debug(ex);
          }
        }
        if (!existingOpenDeleteWorkflow) {
          String stackId = stackEntity.getStackId();
          StackWorkflowTags stackWorkflowTags =
              new StackWorkflowTags(stackId, stackName, stackAccountId, AccountFullName.getInstance(stackAccountId).getAccountName( ) );

          WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
          WorkflowDescriptionTemplate workflowDescriptionTemplate = new DeleteStackWorkflowDescriptionTemplate();
          InterfaceBasedWorkflowClient<DeleteStackWorkflow> client = workflowClientFactory
            .getNewWorkflowClient(DeleteStackWorkflow.class, workflowDescriptionTemplate, stackWorkflowTags);
          DeleteStackWorkflow deleteStackWorkflow = new DeleteStackWorkflowClient(client);
          deleteStackWorkflow.deleteStack(stackId, stackAccountId, stackEntity.getResourceDependencyManagerJson(), userId);
          StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackEntity.getStackId(),
            StackWorkflowEntity.WorkflowType.DELETE_STACK_WORKFLOW,
            CloudFormationProperties.SWF_DOMAIN,
            client.getWorkflowExecution().getWorkflowId(),
            client.getWorkflowExecution().getRunId());
        }
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DescribeStackEventsResponseType describeStackEvents( final DescribeStackEventsType request ) throws CloudFormationException {
    DescribeStackEventsResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      User user = ctx.getUser();
      String accountId = user.getAccount().getAccountNumber();
      String stackName = request.getStackName();
      if (stackName == null) throw new ValidationErrorException("Stack name is null");
      checkStackPermission( ctx, stackName, accountId );
      ArrayList<StackEvent> stackEventList = StackEventEntityManager.getStackEventsByNameOrId( stackName, accountId );
      if ( stackEventList.isEmpty( ) && ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ) {
        stackEventList = StackEventEntityManager.getStackEventsByNameOrId( stackName, null );
      }
      StackEvents stackEvents = new StackEvents();
      stackEvents.setMember(stackEventList);
      DescribeStackEventsResult describeStackEventsResult = new DescribeStackEventsResult();
      describeStackEventsResult.setStackEvents(stackEvents);
      reply.setDescribeStackEventsResult(describeStackEventsResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DescribeStackResourceResponseType describeStackResource(DescribeStackResourceType request)
      throws CloudFormationException {
    DescribeStackResourceResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      final String stackName = request.getStackName();
      if (stackName == null) throw new ValidationErrorException("Stack name is null");
      checkStackPermission( ctx, stackName, accountId );
      final String logicalResourceId = request.getLogicalResourceId();
      if (logicalResourceId == null) throw new ValidationErrorException("logicalResourceId is null");
      final StackResourceEntity stackResourceEntity = StackResourceEntityManager.describeStackResource(
          ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ? null : accountId,
          stackName,
          logicalResourceId );
      final StackResourceDetail stackResourceDetail = new StackResourceDetail();
      stackResourceDetail.setDescription(stackResourceEntity.getDescription());
      stackResourceDetail.setLastUpdatedTimestamp(stackResourceEntity.getLastUpdateTimestamp());
      stackResourceDetail.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
      stackResourceDetail.setMetadata(stackResourceEntity.getMetadataJson());
      stackResourceDetail.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
      stackResourceDetail.setResourceStatus(stackResourceEntity.getResourceStatus() == null ? null : stackResourceEntity.getResourceStatus().toString());
      stackResourceDetail.setResourceStatusReason(stackResourceEntity.getResourceStatusReason());
      stackResourceDetail.setResourceType(stackResourceEntity.getResourceType());
      stackResourceDetail.setStackId(stackResourceEntity.getStackId());
      stackResourceDetail.setStackName(stackResourceEntity.getStackName());
      final DescribeStackResourceResult describeStackResourceResult = new DescribeStackResourceResult();
      describeStackResourceResult.setStackResourceDetail(stackResourceDetail);
      reply.setDescribeStackResourceResult(describeStackResourceResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DescribeStackResourcesResponseType describeStackResources(final DescribeStackResourcesType request)
      throws CloudFormationException {
    DescribeStackResourcesResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      final String stackName = request.getStackName();
      final String logicalResourceId = request.getLogicalResourceId();
      final String physicalResourceId = request.getPhysicalResourceId();
      if (stackName != null && logicalResourceId != null) {
        throw new ValidationErrorException("Only one of StackName or LogicalResourceId can be set");
      }
      if (stackName == null && logicalResourceId == null) {
        throw new ValidationErrorException("StackName or LogicalResourceId must be set");
      }
      ArrayList<StackResource> stackResourceList = Lists.newArrayList();
      List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.describeStackResources(
          ctx.isAdministrator( ) && stackName!=null && stackName.startsWith( STACK_ID_PREFIX ) ? null : accountId,
          stackName,
          physicalResourceId,
          logicalResourceId);
      if (stackResourceEntityList != null && !stackResourceEntityList.isEmpty()) {
        checkStackPermission( ctx, stackResourceEntityList.get( 0 ).getStackId( ), accountId );
        for (StackResourceEntity stackResourceEntity: stackResourceEntityList) {
          StackResource stackResource = new StackResource();
          stackResource.setDescription(stackResourceEntity.getDescription());
          stackResource.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
          stackResource.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
          stackResource.setResourceStatus(stackResourceEntity.getResourceStatus().toString());
          stackResource.setResourceStatusReason(stackResourceEntity.getResourceStatusReason());
          stackResource.setResourceType(stackResourceEntity.getResourceType());
          stackResource.setStackId(stackResourceEntity.getStackId());
          stackResource.setStackName(stackResourceEntity.getStackName());
          stackResource.setTimestamp(stackResourceEntity.getLastUpdateTimestamp());
          stackResourceList.add(stackResource);
        }
      }
      DescribeStackResourcesResult describeStackResourcesResult = new DescribeStackResourcesResult();
      StackResources stackResources = new StackResources();
      stackResources.setMember(stackResourceList);
      describeStackResourcesResult.setStackResources(stackResources);
      reply.setDescribeStackResourcesResult(describeStackResourcesResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DescribeStacksResponseType describeStacks(DescribeStacksType request)
      throws CloudFormationException {
    DescribeStacksResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      final String stackName = request.getStackName();
      final List<StackEntity> stackEntities = StackEntityManager.describeStacks(
          ctx.isAdministrator( ) && stackName!=null && ("verbose".equals(stackName) || stackName.startsWith( STACK_ID_PREFIX )) ? null : accountId,
          ctx.isAdministrator( ) && "verbose".equals(stackName) ? null : stackName );
      final ArrayList<Stack> stackList = new ArrayList<Stack>();
      for ( final StackEntity stackEntity : Iterables.filter( stackEntities, RestrictedTypes.filterPrivileged( ) ) ) {
        Stack stack = new Stack();
        if (stackEntity.getCapabilitiesJson() != null && !stackEntity.getCapabilitiesJson().isEmpty()) {
          ResourceList capabilities = new ResourceList();
          ArrayList<String> member = StackEntityHelper.jsonToCapabilities(stackEntity.getCapabilitiesJson());
          capabilities.setMember(member);
          stack.setCapabilities(capabilities);
        }
        stack.setCreationTime(stackEntity.getCreateOperationTimestamp());
        stack.setDescription(stackEntity.getDescription());
        stack.setStackName(stackEntity.getStackName());
        stack.setDisableRollback(stackEntity.getDisableRollback()); // TODO: how do we handle onFailure(?) field
        stack.setLastUpdatedTime(stackEntity.getLastUpdateTimestamp());
        if (stackEntity.getNotificationARNsJson() != null && !stackEntity.getNotificationARNsJson().isEmpty()) {
          ResourceList notificationARNs = new ResourceList();
          ArrayList<String> member = StackEntityHelper.jsonToNotificationARNs(stackEntity.getNotificationARNsJson());
          notificationARNs.setMember(member);
          stack.setNotificationARNs(notificationARNs);
        }

        if (stackEntity.getOutputsJson() != null && !stackEntity.getOutputsJson().isEmpty()) {
          boolean somethingNotReady = false;
          ArrayList<StackEntity.Output> stackEntityOutputs = StackEntityHelper.jsonToOutputs(stackEntity.getOutputsJson());
          ArrayList<Output> member = Lists.newArrayList();
          for (StackEntity.Output stackEntityOutput: stackEntityOutputs) {
            if (!stackEntityOutput.isReady()) {
              somethingNotReady = true;
              break;
            }  else if (stackEntityOutput.isAllowedByCondition()) {
              Output output = new Output();
              output.setDescription(stackEntityOutput.getDescription());
              output.setOutputKey(stackEntityOutput.getKey());
              output.setOutputValue(stackEntityOutput.getStringValue());
              member.add(output);
            }
          }
          if (!somethingNotReady) {
            Outputs outputs = new Outputs();
            outputs.setMember(member);
            stack.setOutputs(outputs);
          }
        }

        if (stackEntity.getParametersJson() != null && !stackEntity.getParametersJson().isEmpty()) {
          ArrayList<StackEntity.Parameter> stackEntityParameters = StackEntityHelper.jsonToParameters(stackEntity.getParametersJson());
          ArrayList<Parameter> member = Lists.newArrayList();
          for (StackEntity.Parameter stackEntityParameter: stackEntityParameters) {
            Parameter parameter = new Parameter();
            parameter.setParameterKey(stackEntityParameter.getKey());
            parameter.setParameterValue(stackEntityParameter.isNoEcho()
              ? NO_ECHO_PARAMETER_VALUE : stackEntityParameter.getStringValue());
            member.add(parameter);
          }
          Parameters parameters = new Parameters();
          parameters.setMember(member);
          stack.setParameters(parameters);
        }

        stack.setStackId(stackEntity.getStackId());
        stack.setStackName(stackEntity.getStackName());
        stack.setStackStatus(stackEntity.getStackStatus().toString());
        stack.setStackStatusReason(stackEntity.getStackStatusReason());

        if (stackEntity.getTagsJson() != null && !stackEntity.getTagsJson().isEmpty()) {
          Tags tags = new Tags();
          ArrayList<Tag> member = StackEntityHelper.jsonToTags(stackEntity.getTagsJson());
          tags.setMember(member);
          stack.setTags(tags);
        }
        stack.setTimeoutInMinutes(stackEntity.getTimeoutInMinutes());
        stackList.add(stack);
      }
      DescribeStacksResult describeStacksResult = new DescribeStacksResult();
      Stacks stacks = new Stacks();
      stacks.setMember(stackList );
      describeStacksResult.setStacks(stacks );
      reply.setDescribeStacksResult(describeStacksResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public EstimateTemplateCostResponseType estimateTemplateCost(EstimateTemplateCostType request)
      throws CloudFormationException {
    return request.getReply();
  }

  public GetStackPolicyResponseType getStackPolicy(final GetStackPolicyType request) throws CloudFormationException {
    GetStackPolicyResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      final String stackName = request.getStackName();
      if (stackName == null) {
        throw new ValidationErrorException("StackName must not be null");
      }
      checkStackPermission( ctx, stackName, accountId );
      final StackEntity stackEntity = StackEntityManager.getAnyStackByNameOrId(
          stackName,
          ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ? null : accountId );
      if (stackEntity == null) {
        throw new ValidationErrorException("Stack " + stackName + " does not exist");
      }
      GetStackPolicyResult getStackPolicyResult = new GetStackPolicyResult();
      getStackPolicyResult.setStackPolicyBody(stackEntity.getStackPolicy());
      reply.setGetStackPolicyResult(getStackPolicyResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public GetTemplateResponseType getTemplate(final GetTemplateType request)
      throws CloudFormationException {
    GetTemplateResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      final String stackName = request.getStackName();
      if (stackName == null) {
        throw new ValidationErrorException("StackName must not be null");
      }
      checkStackPermission( ctx, stackName, accountId );
      final StackEntity stackEntity = StackEntityManager.getAnyStackByNameOrId(
          stackName,
          ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ? null : accountId );
      if (stackEntity == null) {
        throw new ValidationErrorException("Stack " + stackName + " does not exist");
      }
      GetTemplateResult getTemplateResult = new GetTemplateResult();
      getTemplateResult.setTemplateBody(stackEntity.getTemplateBody());
      reply.setGetTemplateResult(getTemplateResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public ListStackResourcesResponseType listStackResources(ListStackResourcesType request)
      throws CloudFormationException {
    ListStackResourcesResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      final String stackName = request.getStackName();
      if (stackName == null) {
        throw new ValidationErrorException("StackName must not be null");
      }
      checkStackPermission( ctx, stackName, accountId );
      ArrayList<StackResourceSummary> stackResourceSummaryList = Lists.newArrayList();
      List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.listStackResources(
          ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ? null : accountId,
          stackName );
      if (stackResourceEntityList != null) {
        for (StackResourceEntity stackResourceEntity: stackResourceEntityList) {
          StackResourceSummary stackResourceSummary = new StackResourceSummary();
          stackResourceSummary.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
          stackResourceSummary.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
          stackResourceSummary.setResourceStatus(stackResourceEntity.getResourceStatus().toString());
          stackResourceSummary.setResourceStatusReason(stackResourceEntity.getResourceStatusReason());
          stackResourceSummary.setResourceType(stackResourceEntity.getResourceType());
          stackResourceSummary.setLastUpdatedTimestamp(stackResourceEntity.getLastUpdateTimestamp());
          stackResourceSummaryList.add(stackResourceSummary);
        }
      }
      ListStackResourcesResult listStackResourcesResult = new ListStackResourcesResult();
      StackResourceSummaries stackResourceSummaries = new StackResourceSummaries();
      stackResourceSummaries.setMember(stackResourceSummaryList);
      listStackResourcesResult.setStackResourceSummaries(stackResourceSummaries);
      reply.setListStackResourcesResult(listStackResourcesResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }
  
  public ListStacksResponseType listStacks(ListStacksType request)
      throws CloudFormationException {
    ListStacksResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      final ResourceList stackStatusFilter = request.getStackStatusFilter();
      final List<StackEntity.Status> statusFilterList = Lists.newArrayList();
      if (stackStatusFilter != null && stackStatusFilter.getMember() != null) {
        for (String statusFilterStr: stackStatusFilter.getMember()) {
          try {
            statusFilterList.add(StackEntity.Status.valueOf(statusFilterStr));
          } catch (Exception ex) {
            throw new ValidationErrorException("Invalid value for StackStatus " + statusFilterStr);
          }
        }
      }

      // TODO: support next token
      List<StackEntity> stackEntities = StackEntityManager.listStacks(accountId, statusFilterList);
      ArrayList<StackSummary> stackSummaryList = new ArrayList<StackSummary>();
      for ( final StackEntity stackEntity : Iterables.filter( stackEntities, RestrictedTypes.filterPrivileged( ) ) ) {
        StackSummary stackSummary = new StackSummary();
        stackSummary.setCreationTime(stackEntity.getCreateOperationTimestamp());
        stackSummary.setDeletionTime(stackEntity.getDeleteOperationTimestamp());
        stackSummary.setLastUpdatedTime(stackEntity.getLastUpdateOperationTimestamp());
        stackSummary.setStackId(stackEntity.getStackId());
        stackSummary.setStackName(stackEntity.getStackName());
        stackSummary.setStackStatus(stackEntity.getStackStatus().toString());
        stackSummary.setTemplateDescription(stackEntity.getDescription());
        stackSummaryList.add(stackSummary);
      }
      ListStacksResult listStacksResult = new ListStacksResult();
      StackSummaries stackSummaries = new StackSummaries();
      stackSummaries.setMember(stackSummaryList);
      listStacksResult.setStackSummaries(stackSummaries);
      reply.setListStacksResult(listStacksResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public SetStackPolicyResponseType setStackPolicy(SetStackPolicyType request)
      throws CloudFormationException {
    SetStackPolicyResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final User user = ctx.getUser();
      final String accountId = user.getAccount().getAccountNumber();
      // TODO: validate policy
      final String stackName = request.getStackName();
      final String stackPolicyBody = request.getStackPolicyBody();
      if (request.getStackPolicyURL() != null) {
        throw new ValidationErrorException("StackPolicyURL is not supported");
      }
      if (stackName == null) throw new ValidationErrorException("Stack name is null");
      // body could be null (?) (i.e. remove policy)
      StackEntity stackEntity = StackEntityManager.getAnyStackByNameOrId(
          stackName,
          ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ? null : accountId );
      if (stackEntity == null) {
        throw new ValidationErrorException("Stack " + stackName + " does not exist");
      }
      if ( !RestrictedTypes.filterPrivileged( ).apply( stackEntity ) ) {
        throw new AccessDeniedException( "Not authorized." );
      }
      stackEntity.setStackPolicy(stackPolicyBody);
      StackEntityManager.updateStack(stackEntity);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public UpdateStackResponseType updateStack(UpdateStackType request)
      throws CloudFormationException {
    return request.getReply();
  }

  public ValidateTemplateResponseType validateTemplate(ValidateTemplateType request)
    throws CloudFormationException {
    ValidateTemplateResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      // IAM Action Check
      checkActionPermission(CloudFormationPolicySpec.CLOUDFORMATION_VALIDATETEMPLATE, ctx);
      final User user = ctx.getUser();
      final String userId = user.getUserId();
      final String accountId = user.getAccount().getAccountNumber();
      final String templateBody = request.getTemplateBody();
      final String templateUrl = request.getTemplateURL();
      String stackName = "stackName"; // just some value to make the validate code work
      if (templateBody == null && templateUrl == null) throw new ValidationErrorException("Either TemplateBody or TemplateURL must be set.");
      if (templateBody != null && templateUrl != null) throw new ValidationErrorException("Exactly one of TemplateBody or TemplateURL must be set.");

      if (templateBody != null) {
        if (templateBody.getBytes().length > Limits.REQUEST_TEMPLATE_BODY_MAX_LENGTH_BYTES) {
          throw new ValidationErrorException("Template body may not exceed " + Limits.REQUEST_TEMPLATE_BODY_MAX_LENGTH_BYTES + " bytes in a request.");
        }
      }
      String templateText = (templateBody != null) ? templateBody : extractTemplateTextFromURL(templateUrl, user);
      final String stackIdLocal = UUID.randomUUID().toString();
      final String stackId = "arn:aws:cloudformation:" + REGION + ":" + accountId + ":stack/"+stackName+"/"+stackIdLocal;
      final PseudoParameterValues pseudoParameterValues = new PseudoParameterValues();
      pseudoParameterValues.setAccountId(accountId);
      pseudoParameterValues.setStackName(stackName);
      pseudoParameterValues.setStackId(stackId);
      ArrayList<String> notificationArns = Lists.newArrayList();
      pseudoParameterValues.setRegion(REGION);
      final List<String> defaultRegionAvailabilityZones = describeAvailabilityZones(userId);
      final Map<String, List<String>> availabilityZones = Maps.newHashMap();
      availabilityZones.put(REGION, defaultRegionAvailabilityZones);
      availabilityZones.put("",defaultRegionAvailabilityZones); // "" defaults to the default region
      pseudoParameterValues.setAvailabilityZones(availabilityZones);
      List<Parameter> parameters = Lists.newArrayList();
      final ValidateTemplateResult validateTemplateResult = new TemplateParser().validateTemplate(templateText, parameters, pseudoParameterValues);
      reply.setValidateTemplateResult(validateTemplateResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  private static void handleException(final Exception e)
    throws CloudFormationException {
    final CloudFormationException cause = Exceptions.findCause(e,
      CloudFormationException.class);
    if (cause != null) {
      throw cause;
    }

    LOG.error( e, e );

    final InternalFailureException exception = new InternalFailureException(
      String.valueOf(e.getMessage()));
    if (Contexts.lookup().hasAdministrativePrivileges()) {
      exception.initCause(e);
    }
    throw exception;
  }

  private static void checkStackPermission( Context ctx, String stackName, String accountId ) throws AccessDeniedException {
    StackEntity stackEntity = StackEntityManager.getAnyStackByNameOrId( stackName, accountId );
    if ( stackEntity == null && ctx.isAdministrator( ) && stackName.startsWith( STACK_ID_PREFIX ) ) {
      stackEntity = StackEntityManager.getAnyStackByNameOrId( stackName, null );
    }
    if ( stackEntity != null && !RestrictedTypes.filterPrivileged().apply( stackEntity ) ) {
      throw new AccessDeniedException( "Not authorized." );
    }
  }

  private static void checkActionPermission(final String actionType, final Context ctx)
    throws AccessDeniedException {
    if (!Permissions.isAuthorized(CloudFormationPolicySpec.VENDOR_CLOUDFORMATION, actionType, "",
      ctx.getAccount(), actionType, ctx.getAuthContext())) {
      throw new AccessDeniedException("User does not have permission");
    }
  }

}
