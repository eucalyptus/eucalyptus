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

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public abstract class EventHandlerChain<T extends LoadbalancingEvent> {
	private static Logger    LOG     = Logger.getLogger( EventHandlerChain.class );
	private final List<EventHandler<T>> handlers =
			Lists.newArrayList();
	
	protected void insert(EventHandler<T> next) {
		handlers.add(next);
	}

	public void execute(T evt){
		for (EventHandler<T> handler : this.handlers){
			try{
				handler.apply(evt);
			}catch(Exception e){
				LOG.warn(String.format("failed handling %s, in handler %s", evt, handler), e);
				// TODO SPARK: rollback?
				Exceptions.toUndeclared(e);
			}
		}
	}
	
	public abstract EventHandlerChain<T> build();
}
