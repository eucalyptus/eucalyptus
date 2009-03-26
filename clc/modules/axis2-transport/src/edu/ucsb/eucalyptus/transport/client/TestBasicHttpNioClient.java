package edu.ucsb.eucalyptus.transport.client;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import java.net.URI;

public class TestBasicHttpNioClient {

  public static void main( String[] args ) throws Throwable {
    URI uri = new URI("http://www.google.com/");
    String scheme = uri.getScheme() == null? "http" : uri.getScheme();
    String host = uri.getHost() == null? "localhost" : uri.getHost();
    int port = uri.getPort() == -1? 80 : uri.getPort();

    NioClient client = new NioClient(host, 80);
    HttpRequest request = new DefaultHttpRequest( HttpVersion.HTTP_1_0, HttpMethod.GET, uri.toASCIIString());
    request.addHeader( HttpHeaders.Names.HOST, host);
    ChannelFuture requestFuture = client.write( request );
    requestFuture.awaitUninterruptibly();
    client.cleanup();
  }
}
