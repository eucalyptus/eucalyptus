/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.activities;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public abstract class EventHandlerChain<T extends LoadbalancingEvent> {
	private static Logger    LOG     = Logger.getLogger( EventHandlerChain.class );
	private final List<EventHandler<? super T>> handlers =
			Lists.newArrayList();
  LinkedList<EventHandler<? super T>> reverseHandler = 
      Lists.newLinkedList();
	protected void insert(EventHandler<? super T> next) {
		handlers.add(next);
	}

	public void execute(T evt) throws EventHandlerChainException {
		for (EventHandler<? super T> handler : this.handlers){
			try{
				handler.checkVersion(evt);
				reverseHandler.addFirst(handler); // failed handler will rollback too
				handler.apply(evt);
				if(handler.skipRemaining()){
					LOG.info("skipping the remaining handlers");
					break;
				}
			}catch(final Exception e){
			  final String msg = e.getMessage()!=null ? e.getMessage() : String.format("failed handling %s at %s", evt, handler);
			  final EventHandlerChainException toThrow = 
		        new EventHandlerChainException(msg, e, true);
			  rollback();
				throw toThrow;
			}
		}
	}
	
	public void rollback() {
	  LOG.warn("starting to rollback");
    for (EventHandler<? super T> h : reverseHandler){
      try{
        h.rollback();
      }catch(Exception ex){
        ;
      }
    }
    LOG.info("finished rollback");
	}
	
	public abstract EventHandlerChain<T> build();
	
	@SuppressWarnings("unchecked")
	public <HT> HT findHandler(Class<HT> handlerType){
		for (EventHandler<? super T> handler : this.handlers){
			if (handler.getClass().isAssignableFrom(handlerType)){
				return (HT) handler;
			}
		}
		throw new NoSuchElementException();
	}
}
