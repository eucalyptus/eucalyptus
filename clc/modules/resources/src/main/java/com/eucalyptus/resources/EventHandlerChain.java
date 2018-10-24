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
package com.eucalyptus.resources;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public abstract class EventHandlerChain<T extends ResourceEvent> {
	private static Logger    LOG     = Logger.getLogger( EventHandlerChain.class );
	private final List<EventHandler<T>> handlers =
			Lists.newArrayList();
	public void append(EventHandler<T> next) {
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
