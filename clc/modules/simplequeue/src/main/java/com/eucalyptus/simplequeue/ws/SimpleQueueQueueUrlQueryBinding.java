/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.simplequeue.ws;

import com.eucalyptus.binding.BindingException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.simplequeue.SimpleQueueMessageWithQueueUrl;
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

    if (result instanceof SimpleQueueMessageWithQueueUrl) {
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
        ((SimpleQueueMessageWithQueueUrl) result).setQueueUrl(new URI(scheme, userInfo, host, port, path, query, fragment).toString());
      } catch (URISyntaxException e) {
        throw new BindingException("Unable to bind queueUrl, values would be: scheme="+scheme+",userInfo="+userInfo+",host="+host+",port="+port+",path="+path+",query="+query+",fragment="+fragment);
      }
    }
    return result;
  }
}
