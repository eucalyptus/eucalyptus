/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.simplequeue.ws;

import com.eucalyptus.binding.BindingException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.simplequeue.common.msgs.QueueUrlGetterSetter;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.ssl.SslHandler;

import java.net.URI;
import java.net.URISyntaxException;

public class SimpleQueueQueueUrlQueryBinding extends SimpleQueueQueryBinding {
  Logger LOG = Logger.getLogger(SimpleQueueQueueUrlQueryBinding.class);

  ChannelPipeline channelPipeline;

  public SimpleQueueQueueUrlQueryBinding(ChannelPipeline channelPipeline) {
    super();
    this.channelPipeline = channelPipeline;
  }

  @Override
  public Object bind(MappingHttpRequest httpRequest) throws BindingException {
    Object result = super.bind(httpRequest);

    if (result instanceof QueueUrlGetterSetter && ((QueueUrlGetterSetter) result).getQueueUrl() == null) {
      String scheme = null;
      String userInfo = null;
      String host = null;
      int port = -1;
      String path = null;
      String query = null;
      String fragment = null;

      String hostAndPort = httpRequest.getHeader(HttpHeaders.Names.HOST);
      if (hostAndPort != null) {
        if (hostAndPort.contains(":")) {
          try {
            port = Integer.parseInt(hostAndPort.substring(hostAndPort.lastIndexOf(":") + 1));
          } catch (NumberFormatException ex) {
            throw new BindingException("Invalid host/port number " + hostAndPort);
          }
          host = hostAndPort.substring(0, hostAndPort.lastIndexOf(":"));
        } else {
          host = hostAndPort;
        }
        // only populate scheme if host is given
        if (channelPipeline.get(SslHandler.class) != null) {
          scheme = "https";
        } else {
          scheme = "http";
        }
      }
      path = httpRequest.getServicePath();
      try {
        ((QueueUrlGetterSetter) result).setQueueUrl(new URI(scheme, userInfo, host, port, path, query, fragment).toString());
      } catch (URISyntaxException e) {
        throw new BindingException("Unable to bind queueUrl, values would be: scheme="+scheme+",userInfo="+userInfo+",host="+host+",port="+port+",path="+path+",query="+query+",fragment="+fragment);
      }
    }
    return result;
  }
}
