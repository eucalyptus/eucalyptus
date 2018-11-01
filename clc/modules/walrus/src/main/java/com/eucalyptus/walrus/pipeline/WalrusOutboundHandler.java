/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.walrus.pipeline;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.stream.ChunkedInput;

import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.walrus.exceptions.HeadExceptionInterface;
import com.eucalyptus.walrus.msgs.CopyObjectResponseType;
import com.eucalyptus.walrus.msgs.CreateBucketResponseType;
import com.eucalyptus.walrus.msgs.PutObjectResponseType;
import com.eucalyptus.walrus.msgs.WalrusDataGetResponseType;
import com.eucalyptus.walrus.msgs.WalrusDataResponseType;
import com.eucalyptus.walrus.msgs.WalrusDeleteResponseType;
import com.eucalyptus.walrus.msgs.WalrusErrorMessageType;
import com.eucalyptus.walrus.msgs.WalrusHeadResponseType;
import com.eucalyptus.walrus.util.WalrusProperties;
import com.eucalyptus.walrus.util.WalrusUtil;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.server.MessageStatistics;
import com.google.common.base.Strings;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

public class WalrusOutboundHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger(WalrusOutboundHandler.class);

  @Override
  public void handleDownstream(final ChannelHandlerContext ctx, final ChannelEvent channelEvent) throws Exception {
    try {
      if (channelEvent instanceof MessageEvent) {
        final MessageEvent event = (MessageEvent) channelEvent;
        if (event.getMessage() != null) {
          if (event.getMessage() instanceof MappingHttpResponse) {
            MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
            BaseMessage msg = (BaseMessage) httpResponse.getMessage();
            if (msg instanceof WalrusDataGetResponseType) {
              Callable<Long> stat = MessageStatistics.startDownstream(ctx.getChannel(), this);
              boolean isDone = this.handleMessage(ctx, event);
              stat.call();

              if (isDone) {
                return;
              }
              // fall through and handle other types
            }
          }
          // handle other types
          Callable<Long> stat = MessageStatistics.startDownstream(ctx.getChannel(), this);
          this.outgoingMessage(ctx, event);
          stat.call();
        }
      }
      ctx.sendDownstream(channelEvent);
    } catch (Exception e) {
      // TODO: zhill - this should be a error message sent downstream
      throw new WebServicesException(e.getMessage(), HttpResponseStatus.BAD_REQUEST);// TODO:GRZE: this is not right; needs to propagate in the right
                                                                                     // direction wrt server vs. client
    }
  }

  @Override
  public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpResponse) {
      MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
      BaseMessage msg = (BaseMessage) httpResponse.getMessage();

      if (msg instanceof PutObjectResponseType) {
        PutObjectResponseType putObjectResponse = (PutObjectResponseType) msg;
        httpResponse.addHeader(HttpHeaders.Names.ETAG, '\"' + putObjectResponse.getEtag() + '\"');
        httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(putObjectResponse.getLastModified()));
        if (putObjectResponse.getVersionId() != null) {
          httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, putObjectResponse.getVersionId());
        }
      } else if (msg instanceof WalrusDataResponseType) {
        WalrusDataResponseType response = (WalrusDataResponseType) msg;
        httpResponse.addHeader(HttpHeaders.Names.ETAG, '\"' + response.getEtag() + '\"');
        httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(response.getLastModified()));
        if (response.getVersionId() != null) {
          httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, response.getVersionId());
        }
      } else if (msg instanceof CopyObjectResponseType) {
        CopyObjectResponseType copyResponse = (CopyObjectResponseType) msg;
        if (copyResponse.getVersionId() != null)
          httpResponse.addHeader("x-amz-version-id", copyResponse.getVersionId());
        if (copyResponse.getCopySourceVersionId() != null)
          httpResponse.addHeader("x-amz-copy-source-version-id", copyResponse.getCopySourceVersionId());
      } else if (msg instanceof EucalyptusErrorMessageType) {
        EucalyptusErrorMessageType errorMessage = (EucalyptusErrorMessageType) msg;
        BaseMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
        if (errMsg instanceof WalrusErrorMessageType) {
          WalrusErrorMessageType walrusErrorMsg = (WalrusErrorMessageType) errMsg;
          httpResponse.setStatus(walrusErrorMsg.getStatus());
        }
        httpResponse.setMessage(errMsg);
      } else if (msg instanceof ExceptionResponseType) {
        ExceptionResponseType errorMessage = (ExceptionResponseType) msg;
        BaseMessage errMsg = WalrusUtil.convertErrorMessage(errorMessage);
        if (errMsg instanceof WalrusErrorMessageType) {
          WalrusErrorMessageType walrusErrorMsg = (WalrusErrorMessageType) errMsg;
          httpResponse.setStatus(walrusErrorMsg.getStatus());
        }
        // Fix for EUCA-2782. If the exception occurred on HEAD request, http response body should be empty
        if (errorMessage.getException() instanceof HeadExceptionInterface) {
          httpResponse.setMessage(null);
        } else {
          httpResponse.setMessage(errMsg);
        }
      } else if (msg instanceof WalrusDeleteResponseType) {
        httpResponse.setStatus(HttpResponseStatus.NO_CONTENT);
        httpResponse.setMessage(null);
      } else if (msg instanceof WalrusHeadResponseType) {
        // This is a HEAD request, don't put a body
        httpResponse.setStatus(HttpResponseStatus.OK);
        httpResponse.setMessage(null);
      } else if (msg instanceof CreateBucketResponseType) {
        httpResponse.setStatus(HttpResponseStatus.OK);
        httpResponse.setMessage(null);
        event.getFuture().addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  public boolean handleMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    return handleOutgoingMessage(ctx, event);
  }

  protected boolean handleOutgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpResponse) {
      MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
      BaseMessage msg = (BaseMessage) httpResponse.getMessage();

      if (msg instanceof WalrusDataGetResponseType) {
        WalrusDataGetResponseType dataResponse = (WalrusDataGetResponseType) msg;
        writeObjectStorageDataGetResponse(dataResponse, ctx);
        return true;
      }
    }
    return false;
  }

  protected void writeObjectStorageDataGetResponse(final WalrusDataGetResponseType response, final ChannelHandlerContext ctx) {
    DefaultHttpResponse httpResponse = createHttpResponse(response);
    if (!Strings.isNullOrEmpty(response.getCorrelationId())) {
      httpResponse.setHeader(WalrusProperties.AMZ_REQUEST_ID, response.getCorrelationId());
    }

    final Channel channel = ctx.getChannel();
    if (channel.isWritable()) {
      ChannelFuture writeFuture = Channels.future(ctx.getChannel());
      List<ChunkedInput> dataStreams = response.getDataInputStream();
      if (dataStreams==null) {
        if ( !httpResponse.containsHeader( HttpHeaders.Names.CONNECTION ) ) {
          httpResponse.addHeader( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE );
        }
      }
      Channels.write(ctx, writeFuture, httpResponse);
      if (dataStreams != null) {
        for (final ChunkedInput dataStream : dataStreams) {
          channel.write(dataStream).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
              dataStream.close();
            }
          });
        }
      } else {
        writeFuture.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  // TODO: zhill - this should all be done in bindings, just need 2-way bindings
  protected DefaultHttpResponse createHttpResponse(WalrusDataGetResponseType reply) {
    DefaultHttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    long contentLength = reply.getSize();
    String contentType = reply.getContentType();
    String etag = reply.getEtag();
    String contentDisposition = reply.getContentDisposition();
    httpResponse.addHeader(HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream");
    if (etag != null) {
      httpResponse.addHeader(HttpHeaders.Names.ETAG, "\"" + etag + "\""); // etag in quotes, per s3-spec.
    }
    httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(reply.getLastModified()));

    if (contentDisposition != null) {
      httpResponse.addHeader("Content-Disposition", contentDisposition);
    }
    String versionId = reply.getVersionId();
    if (versionId != null && !WalrusProperties.NULL_VERSION_ID.equals(versionId)) {
      httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, versionId);
    }
    httpResponse.setHeader(HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));

    // Add user metadata
    for (MetaDataEntry m : reply.getMetaData()) {
      httpResponse.addHeader(WalrusProperties.AMZ_META_HEADER_PREFIX + m.getName(), m.getValue());
    }

    // write extra headers
    if (reply.getByteRangeEnd() != null && reply.getByteRangeStart() != null) {
      httpResponse.addHeader("Content-Range", reply.getByteRangeStart() + "-" + reply.getByteRangeEnd() + "/" + reply.getSize());
    }
    httpResponse.addHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(contentLength));
    return httpResponse;
  }

}
