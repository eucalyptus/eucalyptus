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
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.loadbalancing.workflow;

import java.util.List;
import org.apache.log4j.Logger;
import com.google.common.base.Supplier;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
public class LoadBalancingVmActivitiesImpl
    implements LoadBalancingVmActivities {
  private static Logger    LOG     = 
      Logger.getLogger(  LoadBalancingVmActivitiesImpl.class );
  
  static class NoSubscriberException extends Exception { }
  
  private static RedisPubSubClient setLoadBalancerClient =
      new RedisPubSubClient("localhost", "set-loadbalancer");
  private static RedisPubSubClient getInstanceStatusClient =
      new RedisPubSubClient("localhost", "get-instance-status");
  private static RedisBlockingListClient getInstanceStatusReplyClient =
      new RedisBlockingListClient("localhost", "get-instance-status-reply");
  private static RedisPubSubClient getCloudwatchMetricsClient =
      new RedisPubSubClient("localhost", "get-cloudwatch-metrics");
  private static RedisBlockingListClient getCloudwatchMetricsReplyClient =
      new RedisBlockingListClient("localhost", "get-cloudwatch-metrics-reply");
  
  
  static class RedisClient {
    private String server = null;
    private Jedis jedis = null;
    public RedisClient(final String server) {
      this.server  = server;
    }
    
    protected Jedis connect() {
      if (jedis == null) {
        jedis = new Jedis(this.server);
      }
      return jedis;
    }
    
    protected void close() {
      if (jedis != null) {
        jedis.quit();
        jedis = null;
      }
    }
  }
  
  static class RedisPubSubClient extends RedisClient {
    public RedisPubSubClient(String server) {
      super(server);
    }
    
    public RedisPubSubClient(String server, String channel) {
      super(server);
      this.channel = channel;
    }
    
    private String channel = "euca-vmservice";
    public void publish(Supplier<String> message) throws NoSubscriberException {
      try {
        final Jedis server = this.connect();
        if( server.publish(channel, message.get()) <= 0 )
          throw new NoSubscriberException();
      }finally{
        this.close();
      }
    }
    
    public void subscribe(JedisPubSub callback) {
      final Jedis server = this.connect();
      server.subscribe(callback, this.channel);
      this.close();
    }
  }
  
  static class RedisBlockingListClient extends RedisClient {
    public RedisBlockingListClient(String server) {
      super(server);
    }
    
    private String defaultKey = null;
    public RedisBlockingListClient(String server, String key) {
      super(server);
      this.defaultKey = key;
    }
    
    public String pop() {
      return this.pop(defaultKey);
    }
    
    public String pop(final String key) {
      return this.pop(key, 0);
    }
    
    public String pop(final String key, final int timeout) {
     try{
       final Jedis server = this.connect();
       final List<String> results = server.blpop(timeout, key);
       
       String result = null;
       if (results != null && !results.isEmpty())
         result = results.get(1);
       return result;
     }finally {
       this.close();
     }
    }
  }

  @Override
  public void setLoadBalancer(final String loadbalancer) throws LoadBalancingActivityException{
   // serialize loadbalancer
    try{
    setLoadBalancerClient.publish(new Supplier<String>() {
      @Override
      public String get() {
        return loadbalancer;
      }
    });
    LOG.debug(String.format("New loadbalancer: %n%s", loadbalancer));
    }catch(final NoSubscriberException ex) {
      throw new LoadBalancingActivityException("No subscriber is found to receive the loadbalanacer");
    }
  }

  @Override
  public String getCloudWatchMetrics() throws LoadBalancingActivityException {
    try{
      getCloudwatchMetricsClient.publish(new Supplier<String> () { 
        @Override
        public String get() {
          return "GetCloudWatchMetrics";
        }
      });
    }catch(final NoSubscriberException ex) {
      throw new LoadBalancingActivityException("No subscriber is found to send the cloudwatch metrics");
    }
    
    String output = null;
    try{
      output = getCloudwatchMetricsReplyClient.pop();
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to receive cloudwatch metrics");
    }
    LOG.debug(String.format("get-cloudwatch-metric result: %n%s", output));
    return output;
  }
  
  @Override
  public String getInstanceStatus() throws LoadBalancingActivityException {
    try{
      getInstanceStatusClient.publish(new Supplier<String>() {
        @Override
        public String get() {
          return "GetInstanceStatus";
        }
      });
    }catch(final NoSubscriberException ex) {
      throw new LoadBalancingActivityException("No subscriber is found to send the instance status");
    }

    String output = null;
    try{
      output = getInstanceStatusReplyClient.pop();
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to receive instance status");
    }

    LOG.debug(String.format("get-instance-status result: %n%s", output));
    return output;
  }
}