/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SimpleQueueClientManager {
  private static final Logger LOG = Logger
          .getLogger(SimpleQueueClientManager.class);

  private static final ObjectMapper mapper = new ObjectMapper( )
          .setPropertyNamingStrategy( PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE );
  static {
    // to prevent conflicting setter definitions for property "seed"
    mapper.addMixInAnnotations( ClientConfiguration.class, ClientConfigurationMixin.class );
  }

  private static SimpleQueueClientManager instance = new SimpleQueueClientManager();
  private SimpleQueueClientManager() {
    simpleQueueClient = buildClient();
  }
  public static SimpleQueueClientManager getInstance() {
    return instance;
  }

  private AmazonSQS simpleQueueClient = null;
  public AmazonSQS getSimpleQueueClient( ) {
    if (simpleQueueClient == null) {
      simpleQueueClient = buildClient();
    }
    return simpleQueueClient;
  }

  private AmazonSQS buildClient() {
    try {
      return buildClient(BillingAWSCredentialsProvider.BillingUserSupplier.INSTANCE,
              BillingProperties.SQS_CLIENT_CONFIG);
    } catch (final Exception ex) {
      LOG.error("Failed to initialize SQS client", ex);
      return null;
    }
  }

  private AmazonSQS buildClient( final Supplier<User> user, final String text ) throws
          AuthException {
    final AWSCredentialsProvider credentialsProvider =
            new SecurityTokenAWSCredentialsProvider( user );
    final AmazonSQS client = new AmazonSQSClient(
            credentialsProvider,
            buildConfiguration( text )
    );
    client.setEndpoint( ServiceUris.remote( Topology.lookup( SimpleQueue.class ) ).toString( ) );
    return client;
  }

  @SuppressWarnings( "unused" )
  private interface ClientConfigurationMixin {
    @JsonIgnore
    SecureRandom getSecureRandom();
    @JsonIgnore void setSecureRandom(SecureRandom secureRandom);
  }

  /**
   * Parse a JSON format string for AWS SDK for Java ClientConfiguration.
   *
   * @param text The configuration in JSON
   * @return The configuration object
   */
  public static ClientConfiguration buildConfiguration( final String text ) {
    try {
      return Strings.isNullOrEmpty( text ) ?
              new ClientConfiguration( ) :
              mapper.readValue( source( text ), ClientConfiguration.class );
    } catch ( final IOException e ) {
      throw new IllegalArgumentException( "Invalid configuration: " + e.getMessage( ), e );
    }
  }

  private static StringReader source(final String text ) {
    return new StringReader( text ) {
      @Override public String toString( ) { return "property"; } // overridden for better source in error message
    };
  }


  final LoadingCache<String, String> queueUrlCache = CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .build(new CacheLoader<String, String>() {
            @Override
            public String load(final String queueName) throws Exception {
              try {
                final GetQueueUrlRequest req = new GetQueueUrlRequest();
                req.setQueueName(queueName);
                req.setQueueOwnerAWSAccountId(BillingAWSCredentialsProvider.BillingUserSupplier.INSTANCE.get().getAccountNumber());
                final GetQueueUrlResult result = getSimpleQueueClient().getQueueUrl(req);
                return result.getQueueUrl();
              } catch (final Exception ex) {
                throw ex;
              }
            }
          });

  public String getQueueUrl(final String queueName) {
    try {
      return queueUrlCache.get(queueName);
    } catch (final Exception ex) {
      LOG.error("Failed to get queue url", ex);
      return null;
    }
  }

  public boolean queueExists(final String queueName) {
    return getQueueUrl(queueName) != null;
  }

  public void createQueue(final String queueName, final Map<String, String> queueAttributes) throws Exception {
    try {
      final CreateQueueRequest req = new CreateQueueRequest();
      if (queueAttributes != null)
        req.setAttributes(queueAttributes);
      req.setQueueName(queueName);
      if(getSimpleQueueClient().createQueue(req).getQueueUrl() == null)
        throw new Exception("Null queue URL is returned");
    } catch (final AmazonServiceException ex) {
      throw new Exception("Failed to create queue due to service error", ex);
    } catch (final AmazonClientException ex) {
      throw new Exception("Failed to create queue due to client error", ex);
    }
  }

  public List<String> listQueues(final String prefix) throws Exception {
    try {
      final ListQueuesRequest req = new ListQueuesRequest();
      if (prefix!=null)
        req.setQueueNamePrefix(prefix);
      return getSimpleQueueClient().listQueues(req).getQueueUrls();
    } catch (final AmazonServiceException ex) {
      throw new Exception("Failed to list queues due to service error", ex);
    } catch (final AmazonClientException ex) {
      throw new Exception("Failed to list queues due to client error", ex);
    }
  }

  public void deleteQueue(final String queueName) throws Exception {
    try {
      final DeleteQueueRequest req = new DeleteQueueRequest();
      req.setQueueUrl(getQueueUrl(queueName));
      getSimpleQueueClient().deleteQueue(req);
    }catch (final AmazonServiceException ex) {
      throw new Exception("Failed to delete queue due to service error", ex);
    } catch (final AmazonClientException ex) {
      throw new Exception("Failed to delete queue due to client error", ex);
    }
  }

  public void setQueueAttributes(final String queueName, final Map<String, String> queueAttributes) throws Exception {
    try {
      final SetQueueAttributesRequest req = new SetQueueAttributesRequest();
      req.setAttributes(queueAttributes);
      req.setQueueUrl(getQueueUrl(queueName));
      getSimpleQueueClient().setQueueAttributes(req);
    } catch (final AmazonServiceException ex) {
      throw new Exception("Failed to set queue attributes due to service error", ex);
    } catch (final AmazonClientException ex) {
      throw new Exception("Failed to set queue attributes due to client error", ex);
    }
  }

  public void sendMessage(final String queueName, final String message) throws Exception {
    try {
      final SendMessageRequest req = new SendMessageRequest();
      req.setQueueUrl(getQueueUrl(queueName));
      req.setDelaySeconds(0);
      req.setMessageBody(message);
      getSimpleQueueClient().sendMessage(req);
    } catch (final AmazonServiceException ex) {
      throw new Exception("Failed to send message due to service error", ex);
    } catch (final AmazonClientException ex) {
      throw new Exception("Failed to send message due to client error", ex);
    }
  }

  public List<Message> receiveAllMessages(final String queueName, final boolean shouldDelete)
          throws Exception{
    try {
      final List<Message> messages = Lists.newArrayList();
      while (true) {
        final ReceiveMessageRequest req = new ReceiveMessageRequest();
        req.setQueueUrl(getQueueUrl(queueName));
        req.setMaxNumberOfMessages(10);
        req.setWaitTimeSeconds(0);
        req.setVisibilityTimeout(10);

        final ReceiveMessageResult result = getSimpleQueueClient().receiveMessage(req);
        final List<Message> received = result.getMessages();
        if (received == null || received.size() <= 0)
          break;
        messages.addAll(received);
      }

      // TODO: Use PurgeQueue
      if(shouldDelete) {
        for(final List<Message> partition : Iterables.partition(messages, 10)) {
          final DeleteMessageBatchRequest delReq = new DeleteMessageBatchRequest();
          delReq.setQueueUrl(getQueueUrl(queueName));
          delReq.setEntries(
                  partition.stream()
                          .map(m -> new DeleteMessageBatchRequestEntry()
                                  .withId(m.getMessageId())
                                  .withReceiptHandle(m.getReceiptHandle()))
                          .collect(Collectors.toList())
          );
          getSimpleQueueClient().deleteMessageBatch(delReq);
        }
      }
      return messages;
    } catch (final AmazonServiceException ex) {
      throw new Exception("Failed to receive messages due to service error", ex);
    } catch (final AmazonClientException ex) {
      throw new Exception("Failed to receive messages due to client error", ex);
    }
  }
}
