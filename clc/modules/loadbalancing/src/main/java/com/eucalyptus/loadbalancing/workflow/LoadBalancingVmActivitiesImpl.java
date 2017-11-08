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

  private static RedisPubSubClient setPolicyClient =
          new RedisPubSubClient("localhost", "set-policy");
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
  public void setPolicy(final String policy) throws LoadBalancingActivityException {
    try {
      setPolicyClient.publish(new Supplier<String>() {
        @Override
        public String get() {
          return policy;
        }
      });
    } catch(final NoSubscriberException ex) {
      throw new LoadBalancingActivityException("No subscriber is found to receive the policy");
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