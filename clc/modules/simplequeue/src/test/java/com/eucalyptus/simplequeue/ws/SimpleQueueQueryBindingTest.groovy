package com.eucalyptus.simplequeue.ws

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.AddPermissionRequest
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.DeleteQueueRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueUrlRequest
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesRequest
import com.amazonaws.services.sqs.model.ListQueuesRequest
import com.amazonaws.services.sqs.model.ReceiveMessageRequest

//TODO: AWS SDK update import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.amazonaws.services.sqs.model.RemovePermissionRequest
import com.amazonaws.services.sqs.model.SendMessageBatchRequest
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest
import com.amazonaws.util.BinaryUtils
import com.amazonaws.util.Md5Utils
import com.eucalyptus.binding.BindingException
import com.eucalyptus.simplequeue.AddPermissionResponseType
import com.eucalyptus.simplequeue.AddPermissionType
import com.eucalyptus.simplequeue.Attribute
import com.eucalyptus.simplequeue.BatchResultErrorEntry
import com.eucalyptus.simplequeue.ChangeMessageVisibilityBatchRequestEntry
import com.eucalyptus.simplequeue.ChangeMessageVisibilityBatchResponseType
import com.eucalyptus.simplequeue.ChangeMessageVisibilityBatchResult
import com.eucalyptus.simplequeue.ChangeMessageVisibilityBatchResultEntry
import com.eucalyptus.simplequeue.ChangeMessageVisibilityBatchType
import com.eucalyptus.simplequeue.ChangeMessageVisibilityResponseType
import com.eucalyptus.simplequeue.ChangeMessageVisibilityType
import com.eucalyptus.simplequeue.CreateQueueResponseType
import com.eucalyptus.simplequeue.CreateQueueResult
import com.eucalyptus.simplequeue.CreateQueueType
import com.eucalyptus.simplequeue.DeleteMessageBatchRequestEntry
import com.eucalyptus.simplequeue.DeleteMessageBatchResponseType
import com.eucalyptus.simplequeue.DeleteMessageBatchResult
import com.eucalyptus.simplequeue.DeleteMessageBatchResultEntry
import com.eucalyptus.simplequeue.DeleteMessageBatchType
import com.eucalyptus.simplequeue.DeleteMessageResponseType
import com.eucalyptus.simplequeue.DeleteMessageType
import com.eucalyptus.simplequeue.DeleteQueueResponseType
import com.eucalyptus.simplequeue.DeleteQueueType
import com.eucalyptus.simplequeue.GetQueueAttributesResponseType
import com.eucalyptus.simplequeue.GetQueueAttributesResult
import com.eucalyptus.simplequeue.GetQueueAttributesType
import com.eucalyptus.simplequeue.GetQueueUrlResponseType
import com.eucalyptus.simplequeue.GetQueueUrlResult
import com.eucalyptus.simplequeue.GetQueueUrlType
import com.eucalyptus.simplequeue.ListDeadLetterSourceQueuesResponseType
import com.eucalyptus.simplequeue.ListDeadLetterSourceQueuesResult
import com.eucalyptus.simplequeue.ListDeadLetterSourceQueuesType
import com.eucalyptus.simplequeue.ListQueuesResponseType
import com.eucalyptus.simplequeue.ListQueuesResult
import com.eucalyptus.simplequeue.ListQueuesType
import com.eucalyptus.simplequeue.Message
import com.eucalyptus.simplequeue.MessageAttribute
import com.eucalyptus.simplequeue.MessageAttributeValue
import com.eucalyptus.simplequeue.PurgeQueueResponseType
import com.eucalyptus.simplequeue.PurgeQueueType
import com.eucalyptus.simplequeue.ReceiveMessageResponseType
import com.eucalyptus.simplequeue.ReceiveMessageResult
import com.eucalyptus.simplequeue.ReceiveMessageType
import com.eucalyptus.simplequeue.RemovePermissionResponseType
import com.eucalyptus.simplequeue.RemovePermissionType
import com.eucalyptus.simplequeue.ResponseMetadata
import com.eucalyptus.simplequeue.SendMessageBatchRequestEntry
import com.eucalyptus.simplequeue.SendMessageBatchResponseType
import com.eucalyptus.simplequeue.SendMessageBatchResult
import com.eucalyptus.simplequeue.SendMessageBatchResultEntry
import com.eucalyptus.simplequeue.SendMessageBatchType
import com.eucalyptus.simplequeue.SendMessageResponseType
import com.eucalyptus.simplequeue.SendMessageResult
import com.eucalyptus.simplequeue.SendMessageType
import com.eucalyptus.simplequeue.SetQueueAttributesResponseType
import com.eucalyptus.simplequeue.SetQueueAttributesType
import com.eucalyptus.util.EucalyptusCloudException
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import com.google.common.base.Strings
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.apache.log4j.Logger
import org.apache.xml.security.utils.Base64
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFactory
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.channel.group.ChannelGroupFuture
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.util.CharsetUtil
import org.jibx.binding.Loader
import org.jibx.runtime.BindingDirectory
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.concurrent.Executors

import static org.jboss.netty.channel.Channels.pipeline
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 * Created by ethomas on 8/23/16.
 */
class SimpleQueueQueryBindingTest extends QueryBindingTestSupport {

  static ByteBuffer strToByteBuffer(String string) {
    return ByteBuffer.wrap(string.getBytes(ChecksumUtils.UTF8));
  }
  static String base64EncodeStr(String string) {
    return Base64.encode(string.getBytes(ChecksumUtils.UTF8));
  }

  static String base64EncodeBin(ByteBuffer byteBuffer) {
    return Base64.encode(byteBuffer.array());
  }

  static bindingsAvailable = false // change to true to test response binding
  static Loader loader
  public static final String QUEUE_URL = 'http://localhost:10000/0000000/myqueue'
  public static final int NETTY_PORT = 10000
  public static final String NETTY_ENDPOINT = "http://localhost:10000"

  @BeforeClass
  static void setup() {
    QueryBindingTestSupport.setup( )
    if ( bindingsAvailable ) {
      loader = new Loader();
      loader.loadBinding("simplequeue-binding.xml", "simplequeue-binding", SimpleQueueQueryBindingTest.class.getResource('/simplequeue-binding.xml').openStream(), null);
      loader.processBindings();
    }
  }

  @Test
  void testValidBinding() {
    URL resource = SimpleQueueQueryBindingTest.getResource('/simplequeue-binding.xml')
    assertValidBindingXml(resource)
  }

  @Test
  void testValidQueryBinding() {
    URL resource = SimpleQueueQueryBindingTest.class.getResource('/simplequeue-binding.xml')
    assertValidQueryBinding(resource)
  }

  @Test
  void testAddPermission() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;
    // AddPermission
    bean = new AddPermissionType(
      queueUrl: QUEUE_URL,
      label: 'testLabel',
      awsAccountId: ['00000000', '00000001'],
      actionName: ['Action1', 'Action2']
    );

    parameterMap =
      [
       'QueueUrl'       : QUEUE_URL,
        Label           : 'testLabel',
        'AWSAccountId.1': '00000000',
        'AWSAccountId.2': '00000001',
        'ActionName.1'  : 'Action1',
        'ActionName.2'  : 'Action2'
      ];
    bindAndAssertObject(mb, AddPermissionType.class, "AddPermission", bean, parameterMap.size());
    bindAndAssertParameters(mb, AddPermissionType.class, "AddPermission", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(AddPermissionResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance( requestId: UUID.randomUUID().toString())
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    AddPermissionRequest addPermissionRequest = new AddPermissionRequest(
      queueUrl: QUEUE_URL,
      label: 'testLabel',
      aWSAccountIds: ['00000000', '00000001'],
      actions: ['Action1', 'Action2']
    )
    sqs.addPermission(addPermissionRequest);
    nettyHttpServer.shutdown();
  }

  @Test
  void testChangeMessageVisibility() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;
    // ChangeMessageVisibility
    bean = new ChangeMessageVisibilityType(
      queueUrl: QUEUE_URL,
      receiptHandle: 'boo',
      visibilityTimeout: 5
    );
    parameterMap =
      [
        'QueueUrl'         : QUEUE_URL,
        'ReceiptHandle'    : 'boo',
        'VisibilityTimeout': '5'
      ];
    bindAndAssertObject(mb, ChangeMessageVisibilityType.class, "ChangeMessageVisibility", bean, parameterMap.size());
    bindAndAssertParameters(mb, ChangeMessageVisibilityType.class, "ChangeMessageVisibility", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(ChangeMessageVisibilityResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString())
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    ChangeMessageVisibilityRequest changeMessageVisibilityRequest = new ChangeMessageVisibilityRequest(
      queueUrl: QUEUE_URL,
      receiptHandle: 'boo',
      visibilityTimeout: 5
    );
    sqs.changeMessageVisibility(changeMessageVisibilityRequest);
    nettyHttpServer.shutdown();
  }

  @Test
  void testChangeMessageVisibilityBatch() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // ChangeMessageVisibilityBatch
    bean = new ChangeMessageVisibilityBatchType(
      queueUrl: QUEUE_URL,
      changeMessageVisibilityBatchRequestEntry: [
        new ChangeMessageVisibilityBatchRequestEntry(
          id: 'myId 1',
          receiptHandle: 'my rh 1',
          visibilityTimeout: 1
        ),
        new ChangeMessageVisibilityBatchRequestEntry(
          id: 'myId 2',
          receiptHandle: 'my rh 2',
          visibilityTimeout: 2
        ),
        new ChangeMessageVisibilityBatchRequestEntry(
          id: 'myId 3',
          receiptHandle: 'my rh 3',
          visibilityTimeout: 3
        )
      ]
    );
    parameterMap =
      [
        'QueueUrl'                                                    : QUEUE_URL,
        'ChangeMessageVisibilityBatchRequestEntry.1.Id'               : 'myId 1',
        'ChangeMessageVisibilityBatchRequestEntry.1.ReceiptHandle'    : 'my rh 1',
        'ChangeMessageVisibilityBatchRequestEntry.1.VisibilityTimeout': '1',
        'ChangeMessageVisibilityBatchRequestEntry.2.Id'               : 'myId 2',
        'ChangeMessageVisibilityBatchRequestEntry.2.ReceiptHandle'    : 'my rh 2',
        'ChangeMessageVisibilityBatchRequestEntry.2.VisibilityTimeout': '2',
        'ChangeMessageVisibilityBatchRequestEntry.3.Id'               : 'myId 3',
        'ChangeMessageVisibilityBatchRequestEntry.3.ReceiptHandle'    : 'my rh 3',
        'ChangeMessageVisibilityBatchRequestEntry.3.VisibilityTimeout': '3'
      ];
    bindAndAssertObject(mb, ChangeMessageVisibilityBatchType.class, "ChangeMessageVisibilityBatch", bean, parameterMap.size());
    bindAndAssertParameters(mb, ChangeMessageVisibilityBatchType.class, "ChangeMessageVisibilityBatch", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(ChangeMessageVisibilityBatchResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      changeMessageVisibilityBatchResult: loadClass(ChangeMessageVisibilityBatchResult.class).newInstance(
        changeMessageVisibilityBatchResultEntry: [
          loadClass(ChangeMessageVisibilityBatchResultEntry.class).newInstance(id: 'my id 1'),
          loadClass(ChangeMessageVisibilityBatchResultEntry.class).newInstance(id: 'my id 3')
        ],
        batchResultErrorEntry: [
          loadClass(BatchResultErrorEntry.class).newInstance(
            id: 'my id 2',
            code: 'code 2',
            message: 'message 2',
            senderFault: false
          )
        ]
      )
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest = new ChangeMessageVisibilityBatchRequest(
      queueUrl: QUEUE_URL,
      entries: [
        new com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry(
          id: 'myId 1',
          receiptHandle: 'my rh 1',
          visibilityTimeout: 1
        ),
        new com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry(
          id: 'myId 2',
          receiptHandle: 'my rh 2',
          visibilityTimeout: 2
        ),
        new com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry(
          id: 'myId 3',
          receiptHandle: 'my rh 3',
          visibilityTimeout: 3
        )
      ]
    );
    com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchResult changeMessageVisibilityBatchResult =
      sqs.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);

    nettyHttpServer.shutdown();

    assertNotNull(changeMessageVisibilityBatchResult);
    assertNotNull(changeMessageVisibilityBatchResult.getFailed());
    assertEquals(changeMessageVisibilityBatchResult.getFailed().size(), 1);
    assertNotNull(changeMessageVisibilityBatchResult.getFailed().get(0));
    assertEquals(changeMessageVisibilityBatchResult.getFailed().get(0).getId(), "my id 2");
    assertEquals(changeMessageVisibilityBatchResult.getFailed().get(0).getCode(), "code 2");
    assertEquals(changeMessageVisibilityBatchResult.getFailed().get(0).getMessage(), "message 2");
    assertEquals(changeMessageVisibilityBatchResult.getFailed().get(0).getSenderFault(), false);

    assertNotNull(changeMessageVisibilityBatchResult.getSuccessful());
    assertEquals(changeMessageVisibilityBatchResult.getSuccessful().size(), 2);
    assertNotNull(changeMessageVisibilityBatchResult.getSuccessful().get(0));
    assertEquals(changeMessageVisibilityBatchResult.getSuccessful().get(0).getId(), "my id 1");
    assertNotNull(changeMessageVisibilityBatchResult.getSuccessful().get(1));
    assertEquals(changeMessageVisibilityBatchResult.getSuccessful().get(1).getId(), "my id 3");

  }
  @Test
  void testCreateQueue() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // CreateQueue
    bean = new CreateQueueType(
      attribute: [
        new Attribute(name: 'A', value: 'B'),
        new Attribute(name: 'C', value: 'D')
      ] as ArrayList<Attribute>,
      queueName: 'queueName'
    );
    parameterMap =
      [
        'Attribute.1.Name' : 'A',
        'Attribute.1.Value': 'B',
        'Attribute.2.Name' : 'C',
        'Attribute.2.Value': 'D',
        'QueueName'        : 'queueName'
      ];
    bindAndAssertObject(mb, CreateQueueType.class, "CreateQueue", bean, parameterMap.size());
    bindAndAssertParameters(mb, CreateQueueType.class, "CreateQueue", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(CreateQueueResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      createQueueResult: loadClass(CreateQueueResult.class).newInstance(
        queueUrl: QUEUE_URL,
      )
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(
      attributes: ['A': 'B', 'C': 'D'],
      queueName: 'queueName'
    );
    com.amazonaws.services.sqs.model.CreateQueueResult createQueueResult = sqs.createQueue(createQueueRequest);
    nettyHttpServer.shutdown();

    assertNotNull(createQueueResult);
    assertEquals(createQueueResult.getQueueUrl(), QUEUE_URL);
  }

  @Test
  void testDeleteMessage() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    //DeleteMessage
    bean = new DeleteMessageType(
      queueUrl: QUEUE_URL,
      receiptHandle: 'receiptHandle'
    );
    parameterMap =
      [
          'QueueUrl'    :   QUEUE_URL,
          'ReceiptHandle': 'receiptHandle'
      ];
    bindAndAssertObject(mb, DeleteMessageType.class, "DeleteMessage", bean, parameterMap.size());
    bindAndAssertParameters(mb, DeleteMessageType.class, "DeleteMessage", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(DeleteMessageResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString())
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(
      queueUrl: QUEUE_URL,
      receiptHandle: 'receiptHandle'
    );
    sqs.deleteMessage(deleteMessageRequest);
    nettyHttpServer.shutdown();

  }

  @Test
  void testDeleteMessageBatch() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    //DeleteMessageBatch
    bean = new DeleteMessageBatchType(
      queueUrl: QUEUE_URL,
      deleteMessageBatchRequestEntry: [
        new DeleteMessageBatchRequestEntry(id: 'del id 1', receiptHandle: 'receiptHandle1'),
        new DeleteMessageBatchRequestEntry(id: 'del id 2', receiptHandle: 'receiptHandle2'),
        new DeleteMessageBatchRequestEntry(id: 'del id 3', receiptHandle: 'receiptHandle3')
      ] as ArrayList<DeleteMessageBatchRequestEntry>
    );
    parameterMap =
      [
        'QueueUrl'                                      : QUEUE_URL,
        'DeleteMessageBatchRequestEntry.1.Id'           : 'del id 1',
        'DeleteMessageBatchRequestEntry.1.ReceiptHandle': 'receiptHandle1',
        'DeleteMessageBatchRequestEntry.2.Id'           : 'del id 2',
        'DeleteMessageBatchRequestEntry.2.ReceiptHandle': 'receiptHandle2',
        'DeleteMessageBatchRequestEntry.3.Id'           : 'del id 3',
        'DeleteMessageBatchRequestEntry.3.ReceiptHandle': 'receiptHandle3'
      ];
    bindAndAssertObject(mb, DeleteMessageBatchType.class, "DeleteMessageBatch", bean, parameterMap.size());
    bindAndAssertParameters(mb, DeleteMessageBatchType.class, "DeleteMessageBatch", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(DeleteMessageBatchResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      deleteMessageBatchResult: loadClass(DeleteMessageBatchResult.class).newInstance(
        deleteMessageBatchResultEntry: [
          loadClass(DeleteMessageBatchResultEntry.class).newInstance(id: 'my id 1'),
          loadClass(DeleteMessageBatchResultEntry.class).newInstance(id: 'my id 3')
        ],
        batchResultErrorEntry: [
          loadClass(BatchResultErrorEntry.class).newInstance(
            id: 'my id 2',
            code: 'code 2',
            message: 'message 2',
            senderFault: false
          )
        ]
      )
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    DeleteMessageBatchRequest deleteMessageBatchRequest = new DeleteMessageBatchRequest(
      queueUrl: QUEUE_URL,
      entries: [
        new com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry(
          id: 'myId 1',
          receiptHandle: 'my rh 1'
        ),
        new com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry(
          id: 'myId 2',
          receiptHandle: 'my rh 2'
        ),
        new com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry(
          id: 'myId 3',
          receiptHandle: 'my rh 3'
        )
      ]
    );
    com.amazonaws.services.sqs.model.DeleteMessageBatchResult deleteMessageBatchResult =
      sqs.deleteMessageBatch(deleteMessageBatchRequest);

    nettyHttpServer.shutdown();

    assertNotNull(deleteMessageBatchResult);
    assertNotNull(deleteMessageBatchResult.getFailed());
    assertEquals(deleteMessageBatchResult.getFailed().size(), 1);
    assertNotNull(deleteMessageBatchResult.getFailed().get(0));
    assertEquals(deleteMessageBatchResult.getFailed().get(0).getId(), "my id 2");
    assertEquals(deleteMessageBatchResult.getFailed().get(0).getCode(), "code 2");
    assertEquals(deleteMessageBatchResult.getFailed().get(0).getMessage(), "message 2");
    assertEquals(deleteMessageBatchResult.getFailed().get(0).getSenderFault(), false);

    assertNotNull(deleteMessageBatchResult.getSuccessful());
    assertEquals(deleteMessageBatchResult.getSuccessful().size(), 2);
    assertNotNull(deleteMessageBatchResult.getSuccessful().get(0));
    assertEquals(deleteMessageBatchResult.getSuccessful().get(0).getId(), "my id 1");
    assertNotNull(deleteMessageBatchResult.getSuccessful().get(1));
    assertEquals(deleteMessageBatchResult.getSuccessful().get(1).getId(), "my id 3");

  }

  @Test
  void testDeleteQueue() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // DeleteQueue
    bean = new DeleteQueueType(
      queueUrl: QUEUE_URL
    );
    parameterMap =
      [
        'QueueUrl' : QUEUE_URL
      ];

    bindAndAssertObject(mb, DeleteQueueType.class, "DeleteQueue", bean, parameterMap.size());
    bindAndAssertParameters(mb, DeleteQueueType.class, "DeleteQueue", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(DeleteQueueResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString())
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    DeleteQueueRequest deleteQueueRequest = new DeleteQueueRequest(
      queueUrl: QUEUE_URL
    );
    sqs.deleteQueue(deleteQueueRequest);
    nettyHttpServer.shutdown();

  }

  @Test
  void testGetQueueAttributes() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // GetQueueAttributes
    bean = new GetQueueAttributesType(
      attributeName: ['A1', 'A2', 'A3'],
      queueUrl: QUEUE_URL
    );
    parameterMap =
      [
        'QueueUrl'       : QUEUE_URL,
        'AttributeName.1': 'A1',
        'AttributeName.2': 'A2',
        'AttributeName.3': 'A3'
      ];
    bindAndAssertObject(mb, GetQueueAttributesType.class, "GetQueueAttributes", bean, parameterMap.size());
    bindAndAssertParameters(mb, GetQueueAttributesType.class, "GetQueueAttributes", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(GetQueueAttributesResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      getQueueAttributesResult: loadClass(GetQueueAttributesResult.class).newInstance(
        attribute: [
          loadClass(Attribute.class).newInstance(name: 'A1', value: 'V1'),
          loadClass(Attribute.class).newInstance(name: 'A2', value: 'V2'),
          loadClass(Attribute.class).newInstance(name: 'A3', value: 'V3')
        ]
      )
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest(
      queueUrl: QUEUE_URL,
      attributeNames: ['A1', 'A2', 'A3']
    );
    com.amazonaws.services.sqs.model.GetQueueAttributesResult getQueueAttributesResult =
      sqs.getQueueAttributes(getQueueAttributesRequest);

    nettyHttpServer.shutdown();

    assertNotNull(getQueueAttributesResult);
    assertNotNull(getQueueAttributesResult.getAttributes());
    assertEquals(getQueueAttributesResult.getAttributes(),['A1':'V1','A2':'V2','A3':'V3']);
  }

  @Test
  void testGetQueueUrl() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // GetQueueUrl
    bean = new GetQueueUrlType(
      queueName: 'queueName',
      queueOwnerAWSAccountId: '000000000'
    );
    parameterMap =
      [
        'QueueName'             : 'queueName',
        'QueueOwnerAWSAccountId': '000000000'
      ];
    bindAndAssertObject(mb, GetQueueUrlType.class, "GetQueueUrl", bean, parameterMap.size());
    bindAndAssertParameters(mb, GetQueueUrlType.class, "GetQueueUrl", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(GetQueueUrlResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      getQueueUrlResult: loadClass(GetQueueUrlResult.class).newInstance(
        queueUrl: QUEUE_URL,
      )
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    GetQueueUrlRequest getQueueUrlRequest = new GetQueueUrlRequest(
      queueName: 'queueName',
      queueOwnerAWSAccountId: '000000000'
    );
    com.amazonaws.services.sqs.model.GetQueueUrlResult getQueueUrlResult = sqs.getQueueUrl(getQueueUrlRequest);
    nettyHttpServer.shutdown();

    assertNotNull(getQueueUrlResult);
    assertEquals(getQueueUrlResult.getQueueUrl(), QUEUE_URL);
  }

  @Test
  void testListDeadLetterSourceQueues() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // ListDeadLetterSourceQueues
    bean = new ListDeadLetterSourceQueuesType(
      queueUrl: QUEUE_URL,
    );
    parameterMap =
      [
          'QueueUrl' : QUEUE_URL
      ];
    bindAndAssertObject(mb, ListDeadLetterSourceQueuesType.class, "ListDeadLetterSourceQueues", bean, parameterMap.size());
    bindAndAssertParameters(mb, ListDeadLetterSourceQueuesType.class, "ListDeadLetterSourceQueues", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(ListDeadLetterSourceQueuesResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      listDeadLetterSourceQueuesResult: loadClass(ListDeadLetterSourceQueuesResult.class).newInstance(
        queueUrl: ['queueUrl1','queueUrl2','queueUrl3']
      )
    );
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest = new ListDeadLetterSourceQueuesRequest(
      queueUrl: QUEUE_URL
    );
    com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesResult listDeadLetterSourceQueuesResult = sqs.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    nettyHttpServer.shutdown();

    assertNotNull(listDeadLetterSourceQueuesResult);
    assertNotNull(listDeadLetterSourceQueuesResult.getQueueUrls());
    assertEquals(listDeadLetterSourceQueuesResult.getQueueUrls(), ['queueUrl1','queueUrl2','queueUrl3']);
  }

  @Test
  void testListQueues() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // ListQueues
    bean = new ListQueuesType(
      queueNamePrefix: 'queue'
    );
    parameterMap = [
      'QueueNamePrefix': 'queue'
    ];
    bindAndAssertObject(mb, ListQueuesType.class, "ListQueues", bean, parameterMap.size());
    bindAndAssertParameters(mb, ListQueuesType.class, "ListQueues", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(ListQueuesResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      listQueuesResult: loadClass(ListQueuesResult.class).newInstance(
        queueUrl: ['queueUrl1','queueUrl2','queueUrl3']
      )
    );
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    ListQueuesRequest listQueuesRequest = new ListQueuesRequest(
      queueNamePrefix: 'queue'
    );
    com.amazonaws.services.sqs.model.ListQueuesResult listQueuesResult = sqs.listQueues(listQueuesRequest);
    nettyHttpServer.shutdown();

    assertNotNull(listQueuesResult);
    assertNotNull(listQueuesResult.getQueueUrls());
    assertEquals(listQueuesResult.getQueueUrls(), ['queueUrl1','queueUrl2','queueUrl3']);
  }

  @Test
  void testPurgeQueue() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // PurgeQueue
    bean = new PurgeQueueType(
      queueUrl: QUEUE_URL,
    );
    parameterMap =
      [
        'QueueUrl' : QUEUE_URL
      ];
    Maps.newHashMap();
    bindAndAssertObject(mb, PurgeQueueType.class, "PurgeQueue", bean, parameterMap.size());
    bindAndAssertParameters(mb, PurgeQueueType.class, "PurgeQueue", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(PurgeQueueResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString())
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
//TODO: AWS SDK update     PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest(
//TODO: AWS SDK update       queueUrl: QUEUE_URL
//TODO: AWS SDK update     );
//TODO: AWS SDK update     sqs.purgeQueue(purgeQueueRequest);
    nettyHttpServer.shutdown();
  }

  @Test
  void testReceiveMessage() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // ReceiveMessage
    bean = new ReceiveMessageType(
        attributeName: ['A1' ,'A2', 'A3', 'A4', 'A5'],
        maxNumberOfMessages : 5,
        messageAttributeName: ['MA1' ,'MA2', 'MA3', 'MA4', 'MA5'],
        queueUrl: QUEUE_URL,
        visibilityTimeout : 4,
        waitTimeSeconds : 3
    );
    parameterMap =         [
        'QueueUrl'                         :  QUEUE_URL,
        'AttributeName.1'                  : 'A1',
        'AttributeName.2'                  : 'A2',
        'AttributeName.3'                  : 'A3',
        'AttributeName.4'                  : 'A4',
        'AttributeName.5'                  : 'A5',
        'MaxNumberOfMessages'              : '5',
        'MessageAttributeName.1'           : 'MA1',
        'MessageAttributeName.2'           : 'MA2',
        'MessageAttributeName.3'           : 'MA3',
        'MessageAttributeName.4'           : 'MA4',
        'MessageAttributeName.5'           : 'MA5',
        'VisibilityTimeout'                : '4',
        'WaitTimeSeconds'                  : '3'
    ];
    bindAndAssertObject(mb, ReceiveMessageType.class, "ReceiveMessage", bean, parameterMap.size());
    bindAndAssertParameters(mb, ReceiveMessageType.class, "ReceiveMessage", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    // first some helper methods
    def attributes = { maxAttrNum, msgNum ->
      def list = new ArrayList();
      for (int attrNum = 1; attrNum <= maxAttrNum; attrNum++) {
        list.add(loadClass(Attribute.class).newInstance(name: 'A' + attrNum, value: 'V' + msgNum + attrNum));
      }
      list;
    }

    def binaryListValue = { maxArrayNum, msgNum ->
      def list = new ArrayList();
      for (int arrayNum = 1; arrayNum <= maxArrayNum; arrayNum++) {
        list.add(base64EncodeStr('bin_array_' + msgNum + '_' + arrayNum));
      }
      list;
    }

    def stringListValue = { maxArrayNum, msgNum ->
      def list = new ArrayList();
      for (int arrayNum = 1; arrayNum <= maxArrayNum; arrayNum++) {
        list.add('str_array_' + msgNum + '_' + arrayNum);
      }
      list;
    }

    def messageAttributes = { maxArrayNum, msgNum ->
      return [
        loadClass(MessageAttribute.class).newInstance(
          name: 'MA1',
          value: loadClass(MessageAttributeValue.class).newInstance(
            dataType: 'Binary',
            binaryListValue: binaryListValue(maxArrayNum, msgNum)
          )
        ),
        loadClass(MessageAttribute.class).newInstance(
          name: 'MA2',
          value: loadClass(MessageAttributeValue.class).newInstance(
            dataType: 'String',
            stringListValue: stringListValue(maxArrayNum, msgNum)
          )
        ),
        loadClass(MessageAttribute.class).newInstance(
          name: 'MA3',
          value: loadClass(MessageAttributeValue.class).newInstance(
            dataType: 'Number',
            stringValue: String.valueOf(msgNum)
          )
        ),
        loadClass(MessageAttribute.class).newInstance(
          name: 'MA4',
          value: loadClass(MessageAttributeValue.class).newInstance(
            dataType: 'Binary',
            binaryValue: base64EncodeStr('bin_' + msgNum)
          )
        ),
        loadClass(MessageAttribute.class).newInstance(
          name: 'MA5',
          value: loadClass(MessageAttributeValue.class).newInstance(
            dataType: 'String',
            stringValue: 'str_' + msgNum
          )
        )
      ];
    }

    def message = {msgNum ->
      return loadClass(Message.class).newInstance(
        messageId: 'message id ' + msgNum,
        receiptHandle: 'receipt handle ' + msgNum,
        body: 'body ' + msgNum,
        attribute: attributes(5, msgNum),
        messageAttribute: messageAttributes(5, msgNum)
      );
    }
    def response = loadClass(ReceiveMessageResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      receiveMessageResult: loadClass(ReceiveMessageResult.class).newInstance(
        message: [
          message(1),
          message(2),
          message(3),
          message(4),
          message(5)
        ]
      )
    );

    // now calculate checksums
    response.receiveMessageResult.message.each { msg->
      msg.mD5OfBody = ChecksumUtils.calculateMessageBodyMd5(msg.body);
      msg.mD5OfMessageAttributes = ChecksumUtils.calculateMessageAttributesMd5(
        ChecksumUtils.messageAttributesToMap(msg.messageAttribute)
      );
    }

    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
      attributeNames: ['A1' ,'A2', 'A3', 'A4', 'A5'],
      maxNumberOfMessages : 5,
//TODO: AWS SDK update       messageAttributeNames: ['MA1' ,'MA2', 'MA3', 'MA4', 'MA5'],
      queueUrl: QUEUE_URL,
      visibilityTimeout : 4,
      waitTimeSeconds : 3
    );
    com.amazonaws.services.sqs.model.ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest);
    nettyHttpServer.shutdown();

    assertNotNull(receiveMessageResult);
    assertNotNull(receiveMessageResult.getMessages());
    assertEquals(receiveMessageResult.getMessages().size(), 5);
    int ctr = 1;
    for (com.amazonaws.services.sqs.model.Message awsMessage: receiveMessageResult.getMessages()) {
      assertNotNull(awsMessage.getBody());
      assertEquals(awsMessage.getBody(), 'body ' + ctr);
      // no need to check md5, already done on instantiation
      assertNotNull(awsMessage.getAttributes());
      assertEquals(awsMessage.getAttributes().size(), 5);
      for (int attrCtr = 1; attrCtr <=5; attrCtr++) {
        assertNotNull(awsMessage.getAttributes().get('A' + attrCtr));
        assertEquals(awsMessage.getAttributes().get('A' + attrCtr), 'V' + ctr + attrCtr);
      }
//TODO: AWS SDK update       // 5 message attributes:
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes());
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().size(), 5);
//TODO: AWS SDK update       // 1) binary list
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes().get('MA1'));
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA1').getDataType(), 'Binary');
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes().get('MA1').getBinaryListValues());
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA1').getBinaryListValues().size(), 5);
//TODO: AWS SDK update       for (int msgAttrCtr = 1; msgAttrCtr <=5; msgAttrCtr++) {
//TODO: AWS SDK update         ByteBuffer byteBuffer = awsMessage.getMessageAttributes().get('MA1').getBinaryListValues().get(msgAttrCtr - 1);
//TODO: AWS SDK update         assertNotNull(byteBuffer);
//TODO: AWS SDK update         assertTrue(Arrays.equals(byteBuffer.array(), new String('bin_array_' + ctr + "_" + msgAttrCtr).getBytes(ChecksumUtils.UTF8)));
//TODO: AWS SDK update       }
//TODO: AWS SDK update       // 2) string list
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes().get('MA2'));
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA2').getDataType(), 'String');
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes().get('MA2').getStringListValues());
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA2').getStringListValues().size(), 5);
//TODO: AWS SDK update       for (int msgAttrCtr = 1; msgAttrCtr <=5; msgAttrCtr++) {
//TODO: AWS SDK update         String string = awsMessage.getMessageAttributes().get('MA2').getStringListValues().get(msgAttrCtr - 1);
//TODO: AWS SDK update         assertNotNull(string);
//TODO: AWS SDK update         assertEquals(string, 'str_array_' + ctr + "_" + msgAttrCtr);
//TODO: AWS SDK update       }
//TODO: AWS SDK update       // 3) number
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes().get('MA3'));
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA3').getDataType(), 'Number');
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA3').getStringValue(), String.valueOf(ctr));
//TODO: AWS SDK update       // 4) binary
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes().get('MA4'));
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA4').getDataType(), 'Binary');
//TODO: AWS SDK update       ByteBuffer byteBuffer = awsMessage.getMessageAttributes().get('MA4').getBinaryValue();
//TODO: AWS SDK update       assertNotNull(byteBuffer);
//TODO: AWS SDK update       assertTrue(Arrays.equals(byteBuffer.array(), new String('bin_' + ctr).getBytes(ChecksumUtils.UTF8)));
//TODO: AWS SDK update       // 5) string
//TODO: AWS SDK update       assertNotNull(awsMessage.getMessageAttributes().get('MA5'));
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA5').getDataType(), 'String');
//TODO: AWS SDK update       assertEquals(awsMessage.getMessageAttributes().get('MA5').getStringValue(), 'str_' + ctr);

      ctr++;
    }
  }

  @Test
  void testRemovePermission() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;
    // RemovePermission
    bean = new RemovePermissionType(
      queueUrl: QUEUE_URL,
      label: 'testLabel'
    );

    parameterMap =
      [
       'QueueUrl'       : QUEUE_URL,
        Label           : 'testLabel'
      ];
    bindAndAssertObject(mb, RemovePermissionType.class, "RemovePermission", bean, parameterMap.size());
    bindAndAssertParameters(mb, RemovePermissionType.class, "RemovePermission", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(RemovePermissionResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance( requestId: UUID.randomUUID().toString())
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    RemovePermissionRequest removePermissionRequest = new RemovePermissionRequest(
      queueUrl: QUEUE_URL,
      label: 'testLabel'    )
    sqs.removePermission(removePermissionRequest);
    nettyHttpServer.shutdown();
  }

  static class SendHelper {
    static def binaryMessageAttributeValue = { msgNum, type ->
      // type -> "Type" for SendMessage(Batch)Type
      //         "Request" for SendMessage(Batch)Request (aws)

      ByteBuffer value = strToByteBuffer("binary" + msgNum);
      if (type.equals("Type")) {
        return new MessageAttributeValue(
          dataType: 'Binary',
          binaryValue: base64EncodeBin(value)
        );
//TODO: AWS SDK update       } else if (type.equals("Request")) {
//TODO: AWS SDK update         return new com.amazonaws.services.sqs.model.MessageAttributeValue(
//TODO: AWS SDK update           dataType: 'Binary',
//TODO: AWS SDK update           binaryValue: value
//TODO: AWS SDK update         );
      }
      return null;
    }

    static def stringMessageAttributeValue = { msgNum, type ->
      // type -> "Type" for SendMessage(Batch)Type
      //         "Request" for SendMessage(Batch)Request (aws)

      String value = "string" + msgNum;
      if (type.equals("Type")) {
        return new MessageAttributeValue(
          dataType: 'String',
          stringValue: value
        );
//TODO: AWS SDK update       } else if (type.equals("Request")) {
//TODO: AWS SDK update         return new com.amazonaws.services.sqs.model.MessageAttributeValue(
//TODO: AWS SDK update           dataType: 'String',
//TODO: AWS SDK update           stringValue: value
//TODO: AWS SDK update         );
      }
      return null;
    }

    static def numberMessageAttributeValue = { msgNum, type ->
      // type -> "Type" for SendMessage(Batch)Type
      //         "Request" for SendMessage(Batch)Request (aws)

      String value = '' + msgNum;
      if (type.equals("Type")) {
        return new MessageAttributeValue(
          dataType: 'Number',
          stringValue: value
        );
//TODO: AWS SDK update       } else if (type.equals("Request")) {
//TODO: AWS SDK update         return new com.amazonaws.services.sqs.model.MessageAttributeValue(
//TODO: AWS SDK update           dataType: 'Number',
//TODO: AWS SDK update           stringValue: value
//TODO: AWS SDK update         );
      }
      return null;
    }

    static def binaryListMessageAttributeValue = { msgNum, type ->
      // type -> "Type" for SendMessage(Batch)Type
      //         "Request" for SendMessage(Batch)Request (aws)

      ByteBuffer value1 = strToByteBuffer("binary" + msgNum + '1');
      ByteBuffer value2 = strToByteBuffer("binary" + msgNum + '2');
      ByteBuffer value3 = strToByteBuffer("binary" + msgNum + '3');
      if (type.equals("Type")) {
        return new MessageAttributeValue(
          dataType: 'Binary',
          binaryListValue: [base64EncodeBin(value1), base64EncodeBin(value2), base64EncodeBin(value3)]
        );
//TODO: AWS SDK update       } else if (type.equals("Request")) {
//TODO: AWS SDK update         return new com.amazonaws.services.sqs.model.MessageAttributeValue(
//TODO: AWS SDK update           dataType: 'Binary',
//TODO: AWS SDK update           binaryListValues: [value1, value2, value3]
//TODO: AWS SDK update         );
      }
      return null;
    }

    static def stringListMessageAttributeValue = { msgNum, type ->
      // type -> "Type" for SendMessage(Batch)Type
      //         "Request" for SendMessage(Batch)Request (aws)

      String value1 = "string" + msgNum + '1';
      String value2 = "string" + msgNum + '2';
      String value3 = "string" + msgNum + '3';
      if (type.equals("Type")) {
        return new MessageAttributeValue(
          dataType: 'String',
          stringListValue: [value1, value2, value3]
        );
//TODO: AWS SDK update       } else if (type.equals("Request")) {
//TODO: AWS SDK update         return new com.amazonaws.services.sqs.model.MessageAttributeValue(
//TODO: AWS SDK update           dataType: 'String',
//TODO: AWS SDK update           stringListValues: [value1, value2, value3]
//TODO: AWS SDK update         );
      }
      return null;
    }

    static def paramMessageAttributes = { prefix, messageAttributes ->
      def map = [ : ];
      messageAttributes.each { messageAttribute ->
        // cheating because we know the attribute name
        String ctr = messageAttribute.name.substring(2);
        map.put(prefix + 'MessageAttribute.' + ctr + '.Name', messageAttribute.name);
        map.put(prefix + 'MessageAttribute.' + ctr + '.Value.DataType', messageAttribute.value.dataType);
        if (!Strings.isNullOrEmpty(messageAttribute.value.stringValue)) {
          map.put(prefix + 'MessageAttribute.' + ctr + '.Value.StringValue', messageAttribute.value.stringValue);
        }
        if (!Strings.isNullOrEmpty(messageAttribute.value.binaryValue)) {
          map.put(prefix + 'MessageAttribute.' + ctr + '.Value.BinaryValue', messageAttribute.value.binaryValue);
        }
        if (messageAttribute.value.binaryListValue != null && !messageAttribute.value.binaryListValue.isEmpty()) {
          for (int innerCtr = 1; innerCtr <= messageAttribute.value.binaryListValue.size(); innerCtr++) {
            map.put(prefix + 'MessageAttribute.' + ctr + '.Value.BinaryListValue.' + innerCtr, messageAttribute.value.binaryListValue.get(innerCtr - 1));
          }
        }
        if (messageAttribute.value.stringListValue != null && !messageAttribute.value.stringListValue.isEmpty()) {
          for (int innerCtr = 1; innerCtr <= messageAttribute.value.stringListValue.size(); innerCtr++) {
            map.put(prefix + 'MessageAttribute.' + ctr + '.Value.StringListValue.' + innerCtr, messageAttribute.value.stringListValue.get(innerCtr - 1));
          }
        }
      }
      map;
    }

  }
  @Test
  void testSendMessage() {

    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // SendMessage
    bean = new SendMessageType(
      delaySeconds: 5,
      messageBody: 'This is the body of the message',
      queueUrl: QUEUE_URL,
      messageAttribute: [
        new MessageAttribute(name: 'MA1', value: SendHelper.binaryMessageAttributeValue(0, "Type")),
        new MessageAttribute(name: 'MA2', value: SendHelper.stringMessageAttributeValue(0, "Type")),
        new MessageAttribute(name: 'MA3', value: SendHelper.numberMessageAttributeValue(0, "Type")),
        new MessageAttribute(name: 'MA4', value: SendHelper.binaryListMessageAttributeValue(0, "Type")),
        new MessageAttribute(name: 'MA5', value: SendHelper.stringListMessageAttributeValue(0, "Type"))
      ]
    );
    parameterMap = [
      'QueueUrl'    : QUEUE_URL,
      'DelaySeconds': '5',
      'MessageBody' : 'This is the body of the message'
    ];
    parameterMap.putAll(SendHelper.paramMessageAttributes('', bean.messageAttribute));
    bindAndAssertObject(mb, SendMessageType.class, "SendMessage", bean, parameterMap.size());
    bindAndAssertParameters(mb, SendMessageType.class, "SendMessage", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(SendMessageResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      sendMessageResult: loadClass(SendMessageResult.class).newInstance(
        messageId: 'messageId0',
        mD5OfMessageBody: ChecksumUtils.calculateMessageBodyMd5(bean.messageBody),
        mD5OfMessageAttributes: ChecksumUtils.calculateMessageAttributesMd5(
          ChecksumUtils.messageAttributesToMap(bean.messageAttribute)
        )
      )
    )

    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    SendMessageRequest sendMessageRequest = new SendMessageRequest(
      delaySeconds: 5,
      messageBody: 'This is the body of the message',
      queueUrl: QUEUE_URL,
//TODO: AWS SDK update       messageAttributes: [
//TODO: AWS SDK update         'MA1': SendHelper.binaryMessageAttributeValue(0, "Request"),
//TODO: AWS SDK update         'MA2': SendHelper.stringMessageAttributeValue(0, "Request"),
//TODO: AWS SDK update         'MA3': SendHelper.numberMessageAttributeValue(0, "Request"),
//TODO: AWS SDK update         'MA4': SendHelper.binaryListMessageAttributeValue(0, "Request"),
//TODO: AWS SDK update         'MA5': SendHelper.stringListMessageAttributeValue(0, "Request")
//TODO: AWS SDK update       ]
    );
    com.amazonaws.services.sqs.model.SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);
    nettyHttpServer.shutdown();

    // no need to check checksums, client does that for you, just check non-checksum fields
    assertNotNull(sendMessageResult);
    assertEquals(sendMessageResult.getMessageId(), 'messageId0');
  }

  @Test
  void testSendMessageBatch() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;

    // SendMessageBatch
    bean = new SendMessageBatchType(
      queueUrl: QUEUE_URL,
      sendMessageBatchRequestEntry: []
    );
    for (int i = 1; i <= 5; i++) {
      bean.sendMessageBatchRequestEntry.add(
        new SendMessageBatchRequestEntry(
          id: "id" + i,
          delaySeconds: i,
          messageAttribute: [
            new MessageAttribute(name: 'MA1', value: SendHelper.binaryMessageAttributeValue(i, "Type")),
            new MessageAttribute(name: 'MA2', value: SendHelper.stringMessageAttributeValue(i, "Type")),
            new MessageAttribute(name: 'MA3', value: SendHelper.numberMessageAttributeValue(i, "Type")),
            new MessageAttribute(name: 'MA4', value: SendHelper.binaryListMessageAttributeValue(i, "Type")),
            new MessageAttribute(name: 'MA5', value: SendHelper.stringListMessageAttributeValue(i, "Type"))
          ],
          messageBody: 'This is the body of message ' + i
        )
      );
    }
    parameterMap = [
      'QueueUrl'                   : QUEUE_URL
    ];
    for (int i = 1; i <= 5; i++) {
      parameterMap.put("SendMessageBatchRequestEntry." + i + ".Id", 'id' + i);
      parameterMap.put("SendMessageBatchRequestEntry." + i + ".DelaySeconds", '' + i);
      parameterMap.put("SendMessageBatchRequestEntry." + i + ".MessageBody", 'This is the body of message ' + i);
      parameterMap.putAll(SendHelper.paramMessageAttributes("SendMessageBatchRequestEntry." + i + ".", bean.sendMessageBatchRequestEntry.get(i - 1).messageAttribute));
    }
    bindAndAssertObject(mb, SendMessageBatchType.class, "SendMessageBatch", bean, parameterMap.size());
    bindAndAssertParameters(mb, SendMessageBatchType.class, "SendMessageBatch", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(SendMessageBatchResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance(requestId: UUID.randomUUID().toString()),
      sendMessageBatchResult: loadClass(SendMessageBatchResult.class).newInstance(
        sendMessageBatchResultEntry: [
          loadClass(SendMessageBatchResultEntry.class).newInstance(
            id: 'id2',
            messageId: 'messageId2',
            mD5OfMessageBody: ChecksumUtils.calculateMessageBodyMd5(bean.sendMessageBatchRequestEntry.get(1).messageBody),
            mD5OfMessageAttributes: ChecksumUtils.calculateMessageAttributesMd5(
              ChecksumUtils.messageAttributesToMap(bean.sendMessageBatchRequestEntry.get(1).messageAttribute)
            )
          ),
          loadClass(SendMessageBatchResultEntry.class).newInstance(
            id: 'id4',
            messageId: 'messageId4',
            mD5OfMessageBody: ChecksumUtils.calculateMessageBodyMd5(bean.sendMessageBatchRequestEntry.get(3).messageBody),
            mD5OfMessageAttributes: ChecksumUtils.calculateMessageAttributesMd5(
              ChecksumUtils.messageAttributesToMap(bean.sendMessageBatchRequestEntry.get(3).messageAttribute)
            )
          )
        ],
        batchResultErrorEntry: [
          loadClass(BatchResultErrorEntry.class).newInstance(
            code: 'code1',
            id: 'id1',
            message: 'message1',
            senderFault: false
          ),
          loadClass(BatchResultErrorEntry.class).newInstance(
            code: 'code3',
            id: 'id3',
            message: 'message3',
            senderFault: true
          ),
          loadClass(BatchResultErrorEntry.class).newInstance(
            code: 'code5',
            id: 'id5',
            message: 'message5',
            senderFault: false
          )
        ]
      )
    )

    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    SendMessageBatchRequest sendMessageBatchRequest = new SendMessageBatchRequest(
      queueUrl: QUEUE_URL,
      entries: []
    );
    for (int i = 1; i <= 5; i++) {
      sendMessageBatchRequest.entries.add(
        new com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry(
          id: "id" + i,
          delaySeconds: i,
//TODO: AWS SDK update           messageAttributes: [
//TODO: AWS SDK update             'MA1': SendHelper.binaryMessageAttributeValue(i, "Request"),
//TODO: AWS SDK update             'MA2': SendHelper.stringMessageAttributeValue(i, "Request"),
//TODO: AWS SDK update             'MA3': SendHelper.numberMessageAttributeValue(i, "Request"),
//TODO: AWS SDK update             'MA4': SendHelper.binaryListMessageAttributeValue(i, "Request"),
//TODO: AWS SDK update             'MA5': SendHelper.stringListMessageAttributeValue(i, "Request")
//TODO: AWS SDK update           ],
          messageBody: 'This is the body of message ' + i
        )
      );
    }

    com.amazonaws.services.sqs.model.SendMessageBatchResult sendMessageBatchResult = sqs.sendMessageBatch(sendMessageBatchRequest);
    nettyHttpServer.shutdown();

    assertNotNull(sendMessageBatchResult);
    assertNotNull(sendMessageBatchResult.getFailed());
    assertEquals(sendMessageBatchResult.getFailed().size(), 3);
    assertNotNull(sendMessageBatchResult.getFailed().get(0));
    assertEquals(sendMessageBatchResult.getFailed().get(0).getCode(), 'code1');
    assertEquals(sendMessageBatchResult.getFailed().get(0).getId(), 'id1');
    assertEquals(sendMessageBatchResult.getFailed().get(0).getMessage(), 'message1');
    assertEquals(sendMessageBatchResult.getFailed().get(0).getSenderFault(), false);
    assertNotNull(sendMessageBatchResult.getFailed().get(1));
    assertEquals(sendMessageBatchResult.getFailed().get(1).getCode(), 'code3');
    assertEquals(sendMessageBatchResult.getFailed().get(1).getId(), 'id3');
    assertEquals(sendMessageBatchResult.getFailed().get(1).getMessage(), 'message3');
    assertEquals(sendMessageBatchResult.getFailed().get(1).getSenderFault(), true);
    assertNotNull(sendMessageBatchResult.getFailed().get(2));
    assertEquals(sendMessageBatchResult.getFailed().get(2).getCode(), 'code5');
    assertEquals(sendMessageBatchResult.getFailed().get(2).getId(), 'id5');
    assertEquals(sendMessageBatchResult.getFailed().get(2).getMessage(), 'message5');
    assertEquals(sendMessageBatchResult.getFailed().get(2).getSenderFault(), false);
    assertNotNull(sendMessageBatchResult.getSuccessful());
    assertEquals(sendMessageBatchResult.getSuccessful().size(), 2);
    assertNotNull(sendMessageBatchResult.getSuccessful().get(0));
    assertEquals(sendMessageBatchResult.getSuccessful().get(0).getId(), 'id2');
    assertEquals(sendMessageBatchResult.getSuccessful().get(0).getMessageId(), 'messageId2');
    // checksums already validated
    assertNotNull(sendMessageBatchResult.getSuccessful().get(1));
    assertEquals(sendMessageBatchResult.getSuccessful().get(1).getId(), 'id4');
    assertEquals(sendMessageBatchResult.getSuccessful().get(1).getMessageId(), 'messageId4');
    // checksums already validated
  }


  @Test
  void testSetQueueAttribute() {
    SimpleQueueQueryBinding mb = getSimpleQueueQueryBinding()
    Object bean;
    Map<String, String> parameterMap;
    // SetQueueAttributes
    bean = new SetQueueAttributesType(
      queueUrl: QUEUE_URL,
      attribute: [
        new Attribute( name: 'A', value: 'B'),
        new Attribute( name: 'C', value: 'D')
      ]
    );
    parameterMap =
        [
            'QueueUrl'                   : QUEUE_URL,
            'Attribute.1.Name'           : 'A',
            'Attribute.1.Value'          : 'B',
            'Attribute.2.Name'           : 'C',
            'Attribute.2.Value'          : 'D'
        ];
    bindAndAssertObject(mb, SetQueueAttributesType.class, "SetQueueAttributes", bean, parameterMap.size());
    bindAndAssertParameters(mb, SetQueueAttributesType.class, "SetQueueAttributes", bean, parameterMap);

    Assume.assumeTrue( bindingsAvailable )

    //now test response
    def response = loadClass(SetQueueAttributesResponseType.class).newInstance(
      responseMetadata: loadClass(ResponseMetadata.class).newInstance( requestId: UUID.randomUUID().toString())
    )
    NettyHttpServer nettyHttpServer = new NettyHttpServer(NETTY_PORT, getXMLString(response));
    nettyHttpServer.start();
    AmazonSQSClient sqs = new AmazonSQSClient(new BasicAWSCredentials("", ""));
    sqs.setEndpoint(NETTY_ENDPOINT);
    SetQueueAttributesRequest setQueueAttributesRequest = new SetQueueAttributesRequest(
      queueUrl: QUEUE_URL,
      attributes: ['A':'B', 'C':'D']
    )
    sqs.setQueueAttributes(setQueueAttributesRequest);
    nettyHttpServer.shutdown();
  }

  private SimpleQueueQueryBinding getSimpleQueueQueryBinding() {
    URL resource = SimpleQueueQueryBindingTest.class.getResource('/simplequeue-binding.xml')
    return new SimpleQueueQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass(String operationName)
        throws BindingException {
        createTestBindingFromXml(resource, operationName)
      }

      @Override
      protected void validateBinding(com.eucalyptus.binding.Binding currentBinding,
                                     String operationName, Map<String, String> params, BaseMessage eucaMsg)
        throws BindingException {
        // Validation requires compiled bindings
      }
    }
  }

  static class NettyHttpServer {
    int port;
    String result;

    NettyHttpServer(int port, String result) {
      this.port = port
      this.result = result
    }

    static final ChannelGroup allChannels = new DefaultChannelGroup("test-server");
    ChannelFactory factory;
    public void start() throws Exception {
       factory = new NioServerSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool());

      // Configure the server.
      ServerBootstrap bootstrap = new ServerBootstrap(factory);

      // Set up the event pipeline factory.
      bootstrap.setPipelineFactory(new PipelineFactory());

      // Bind and start to accept incoming connections.
      bootstrap.bind(new InetSocketAddress(port));
    }

    public void shutdown() {
      ChannelGroupFuture future = allChannels.close();
      future.awaitUninterruptibly();
      factory.releaseExternalResources();
    }


    public class PipelineFactory implements ChannelPipelineFactory {

      public PipelineFactory() {
      }

      public ChannelPipeline getPipeline() {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = pipeline();

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

        pipeline.addLast("handler", new NettyServerHandler());
        return pipeline;
      }
    }

    public class NettyServerHandler extends SimpleChannelUpstreamHandler {

      @Override
      public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        NettyHttpServer.allChannels.add(e.getChannel());
      }
      @Override
      public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.addHeader(CONTENT_TYPE, "text/xml; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer(result + "\r\n", CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  String getXMLString(Object obj) {
    //    // There are some class loader and casting issues here, so we get the factory class name evilly
    String factoryClassNameWithPipes = (String) obj.getClass().getDeclaredField(BindingDirectory.BINDINGLIST_NAME).get(null);
    String factoryClassName = factoryClassNameWithPipes.substring(1, factoryClassNameWithPipes.length() - 1);
    def bfact = loader.loadClass(factoryClassName).newInstance();
    def mctx = bfact.createMarshallingContext();

    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    mctx.setOutput(bOut, "UTF-8");
    mctx.marshalDocument(obj);
    return new String(bOut.toByteArray());
  }


  static class ChecksumUtils {

    static def messageAttributesToMap(Collection messageAttributes) {
      if (messageAttributes == null) return null;
      Map map = new HashMap();
      for (Object messageAttribute: messageAttributes) {
        map.put(messageAttribute.getName(), messageAttribute.getValue());
      }
      return map;
    }

    // BEGIN CODE FROM Amazon AWS SDK 1.11.28-SNAPSHOT, file: com.amazonaws.services.sqs.MessageMD5ChecksumHandler

    /**
     * Returns the hex-encoded MD5 hash String of the given message body.
     */
    private static final Logger LOG = Logger.getLogger(SimpleQueueQueryBindingTest.class);
    private static final int INTEGER_SIZE_IN_BYTES = 4;
    private static final byte STRING_TYPE_FIELD_INDEX = 1;
    private static final byte BINARY_TYPE_FIELD_INDEX = 2;
    private static final byte STRING_LIST_TYPE_FIELD_INDEX = 3;
    private static final byte BINARY_LIST_TYPE_FIELD_INDEX = 4;


    private static String calculateMessageBodyMd5(String messageBody) throws EucalyptusCloudException {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Message body: " + messageBody);
      }
      byte[] expectedMd5;
      try {
        expectedMd5 = Md5Utils.computeMD5Hash(messageBody.getBytes(UTF8));
      } catch (Exception e) {
        throw new EucalyptusCloudException("Unable to calculate the MD5 hash of the message body. " + e.getMessage(),
          e);
      }
      String expectedMd5Hex = BinaryUtils.toHex(expectedMd5);
      if (LOG.isTraceEnabled()) {
        LOG.trace("Expected  MD5 of message body: " + expectedMd5Hex);
      }
      return expectedMd5Hex;
    }

    /**
     * Returns the hex-encoded MD5 hash String of the given message attributes.
     */
    private static String calculateMessageAttributesMd5(def messageAttributes) throws EucalyptusCloudException {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Message attribtues: " + messageAttributes);
      }
      List<String> sortedAttributeNames = new ArrayList<String>(messageAttributes.keySet());
      Collections.sort(sortedAttributeNames);

      MessageDigest md5Digest = null;
      try {
        md5Digest = MessageDigest.getInstance("MD5");

        for (String attrName : sortedAttributeNames) {
          def attrValue = messageAttributes.get(attrName);

          // Encoded Name
          updateLengthAndBytes(md5Digest, attrName);
          // Encoded Type
          updateLengthAndBytes(md5Digest, attrValue.getDataType());

          // Encoded Value
          if (attrValue.getStringValue() != null) {
            md5Digest.update(STRING_TYPE_FIELD_INDEX);
            updateLengthAndBytes(md5Digest, attrValue.getStringValue());
          } else if (attrValue.getBinaryValue() != null) {
            md5Digest.update(BINARY_TYPE_FIELD_INDEX);
            // Eucalyptus stores the value as a Base 64 encoded string.  Convert to byte buffer
            ByteBuffer byteBuffer = ByteBuffer.wrap(org.apache.xml.security.utils.Base64.decode(attrValue.getBinaryValue()));
            updateLengthAndBytes(md5Digest, byteBuffer);
          } else if (attrValue.getStringListValue() != null && attrValue.getStringListValue().size() > 0) {
            md5Digest.update(STRING_LIST_TYPE_FIELD_INDEX);
            for (String strListMember : attrValue.getStringListValue()) {
              updateLengthAndBytes(md5Digest, strListMember);
            }
          } else if (attrValue.getBinaryListValue() != null && attrValue.getBinaryListValue().size() > 0) {
            md5Digest.update(BINARY_LIST_TYPE_FIELD_INDEX);
            for (String byteListMember : attrValue.getBinaryListValue()) {
              // Eucalyptus stores the value as a Base 64 encoded string.  Convert to byte buffer
              ByteBuffer byteBuffer = ByteBuffer.wrap(org.apache.xml.security.utils.Base64.decode(byteListMember));
              updateLengthAndBytes(md5Digest, byteBuffer);
            }
          }
        }
      } catch (Exception e) {
        throw new EucalyptusCloudException("Unable to calculate the MD5 hash of the message attributes. "
          + e.getMessage(), e);
      }

      String expectedMd5Hex = BinaryUtils.toHex(md5Digest.digest());
      if (LOG.isTraceEnabled()) {
        LOG.trace("Expected  MD5 of message attributes: " + expectedMd5Hex);
      }
      return expectedMd5Hex;
    }

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input String and the actual utf8-encoded byte values.
     */
    private static void updateLengthAndBytes(MessageDigest digest, String str) throws UnsupportedEncodingException {
      byte[] utf8Encoded = str.getBytes(UTF8);
      ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(utf8Encoded.length);
      digest.update(lengthBytes.array());
      digest.update(utf8Encoded);
    }

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input ByteBuffer and all the bytes it contains.
     */
    private static void updateLengthAndBytes(MessageDigest digest, ByteBuffer binaryValue) {
      ByteBuffer readOnlyBuffer = binaryValue.asReadOnlyBuffer();
      int size = readOnlyBuffer.remaining();
      ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(size);
      digest.update(lengthBytes.array());
      digest.update(readOnlyBuffer);
    }

    // From com.amazonaws.util.StringUtils:

    private static final String DEFAULT_ENCODING = "UTF-8";

    public static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);

    // END CODE FROM Amazon AWS SDK 1.11.28-SNAPSHOT


  }

  def loadClass(Class clazz) {
    return loader.loadClass(clazz.getName());
  }

}
