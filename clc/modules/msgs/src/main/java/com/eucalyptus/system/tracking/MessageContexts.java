/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.system.tracking;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author Sang-Min Park
 *
 */
public class MessageContexts {
   
  private static Map<String, List<MessageCache>> correlationIds = 
      new ConcurrentHashMap<String, List<MessageCache>>();

  public static void remember(final String resourceId, final Class<? extends BaseMessage> msgType, BaseMessage message) {
    List<MessageCache> listIds = null;
    if(! correlationIds.containsKey(resourceId)){
      listIds = Lists.newArrayList();
      correlationIds.put(resourceId, listIds);
    }else
      listIds = correlationIds.get(resourceId);
    
    for(final MessageCache corrId : listIds){
      if(msgType.equals(corrId.getKey())){
        return;
      }
    }
    try{
      listIds.add(new MessageCache(msgType, message ));
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(new EucalyptusCloudException("Correlation Id is in wrong format"));
    }
  }
  
  public static boolean contains(final String resourceId, final Class<? extends BaseMessage> msgType){
    return lookup(resourceId, msgType) != null;
  }
  
  public static BaseMessage lookupLast(final String resourceId, Set<Class> msgTypes) {
    if(! correlationIds.containsKey(resourceId))
      return null;
    
    final List<MessageCache> result = Lists.newArrayList();
    for(final MessageCache corrId : correlationIds.get(resourceId)){
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
  
  public static List<BaseMessage> lookup(final String resourceId, final Set<Class> msgTypes) {
    if(! correlationIds.containsKey(resourceId))
      return Lists.newArrayList();
    
    final List<BaseMessage> result = Lists.newArrayList();
    for(final MessageCache corrId : correlationIds.get(resourceId)){
      if(msgTypes.contains(corrId.getKey()))
        result.add(corrId.getValue());
    }
    return result;
  }
  
  public static BaseMessage lookup(final String resourceId, final Class<? extends BaseMessage> msgType) {
    if(! correlationIds.containsKey(resourceId))
      return null;
    
    for(final MessageCache corrId : correlationIds.get(resourceId)){
      if(msgType.equals(corrId.getKey()))
        return corrId.getValue();
    }
    return null;
  }
  
  public static List<BaseMessage> lookup(final String resourceId){
    if(! correlationIds.containsKey(resourceId))
      return Lists.newArrayList();
    
    return Lists.transform(correlationIds.get(resourceId), new Function<MessageCache, BaseMessage>(){
      @Override
      public BaseMessage apply(MessageCache arg0) {
        return arg0.getValue();
      }
    });
  }
}
