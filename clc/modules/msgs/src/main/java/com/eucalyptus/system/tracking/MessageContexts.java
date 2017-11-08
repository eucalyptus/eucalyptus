/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.system.tracking;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author Sang-Min Park
 *
 */
public class MessageContexts {
   
  private static final Logger LOG = Logger.getLogger(MessageContexts.class);
  
  private static Cache<String, List<MessageCache>> correlationIds =
      CacheBuilder.newBuilder()
      .maximumSize(3000)
      .expireAfterAccess(1, TimeUnit.HOURS)
      .build();
  
  public static synchronized void remember(final String resourceId, final Class<? extends BaseMessage> msgType, BaseMessage message) {
    List<MessageCache> listIds = null;
    listIds = correlationIds.getIfPresent(resourceId);
    if(listIds == null){
      listIds = Lists.newArrayList();
      correlationIds.put(resourceId, listIds);
    }
    int duplicate = -1;
    for(int i=0; i<listIds.size(); i++){
      final MessageCache corrId = listIds.get(i);
      if(msgType.equals(corrId.getKey())){
        duplicate = i;
        break;
      }
    }
    if(duplicate>=0)
      listIds.remove(duplicate);
    
    try{
      listIds.add(new MessageCache(msgType, message ));
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(new EucalyptusCloudException("Correlation Id is in wrong format"));
    }
  }
  
  public static synchronized boolean contains(final String resourceId, final Class<? extends BaseMessage> msgType){
    return lookup(resourceId, msgType) != null;
  }
  
  public static synchronized BaseMessage lookupLast(final String resourceId, Set<Class> msgTypes) {   
    List<MessageCache> messages = correlationIds.getIfPresent(resourceId);
    if(messages == null)
      return null;
    
    final List<MessageCache> result = Lists.newArrayList();
    for(final MessageCache corrId : messages){
      if(msgTypes.contains(corrId.getKey()))
        result.add(corrId);
    }
    if(result.size()<=0)
      return null;
    
    BaseMessage lastMsg = result.get(0).getValue();
    long lastTs = result.get(0).getCreationTime().getTime();
    for(final MessageCache corrId : result){
      if(corrId.getCreationTime().getTime() > lastTs){
        lastMsg = corrId.getValue();
        lastTs = corrId.getCreationTime().getTime();
      }
    }
    return lastMsg;
  }
  
  public static synchronized List<BaseMessage> lookup(final String resourceId, final Set<Class> msgTypes) {
    List<MessageCache> messages = correlationIds.getIfPresent(resourceId);
    if(messages == null)
      return Lists.newArrayList();
    
    final List<BaseMessage> result = Lists.newArrayList();
    for(final MessageCache corrId : messages){
      if(msgTypes.contains(corrId.getKey()))
        result.add(corrId.getValue());
    }
    return result;
  }
  
  public static synchronized BaseMessage lookup(final String resourceId, final Class<? extends BaseMessage> msgType) {
    List<MessageCache> messages = correlationIds.getIfPresent(resourceId);
    if(messages == null)
      return null;
    for(final MessageCache corrId : messages){
      if(msgType.equals(corrId.getKey()))
        return corrId.getValue();
    }
    return null;
  }
  
  public static synchronized List<BaseMessage> lookup(final String resourceId){
    List<MessageCache> messages = correlationIds.getIfPresent(resourceId);
    if(messages == null)
      return Lists.newArrayList();
 
    return Lists.transform(messages, new Function<MessageCache, BaseMessage>(){
      @Override
      public BaseMessage apply(MessageCache arg0) {
        return arg0.getValue();
      }
    });
  }
}
