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
	private final List<EventHandler<T>> handlers =
			Lists.newArrayList();
	protected void insert(EventHandler<T> next) {
		handlers.add(next);
	}

	public void execute(T evt) throws EventHandlerChainException{
		LinkedList<EventHandler<T>> reverseHandler = Lists.newLinkedList();
		for (EventHandler<T> handler : this.handlers){
			try{
				reverseHandler.addFirst(handler); // failed handler will rollback too
				handler.apply(evt);
				if(handler.skipRemaining()){
					LOG.info("skipping the remaining handlers");
					break;
				}
			}catch(Exception e){
				LOG.warn("starting to rollback");
				final String msg = e.getMessage()!=null ? e.getMessage() : 
					String.format("failed handling %s at %s", evt, handler);
				final EventHandlerChainException toThrow = 
						new EventHandlerChainException(msg, e, true);
				for (EventHandler<T> h : reverseHandler){
					try{
						h.rollback();
					}catch(Exception ex){
						LOG.warn("rollback failed at " + h.toString(), ex);
						toThrow.setRollback(false); // fail once, rollback status is set to false
					}
				}
				LOG.info("finished rollback");
				throw toThrow;
			}
		}
	}
	
	public abstract EventHandlerChain<T> build();
	
	@SuppressWarnings("unchecked")
	public <HT> HT findHandler(Class<HT> handlerType){
		for (EventHandler<T> handler : this.handlers){
			if (handler.getClass().isAssignableFrom(handlerType)){
				return (HT) handler;
			}
		}
		throw new NoSuchElementException();
	}
}
