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
package com.eucalyptus.database.activities;

import org.apache.log4j.Logger;

import com.eucalyptus.resources.EventHandlerChain;
/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChains {
    private static Logger LOG  = Logger.getLogger( EventHandlerChains.class );
    public static EventHandlerChain<NewDBInstanceEvent> onNewDBInstance(){
      return (new EventHandlerChainCreateDbInstance()).build();
    }
    public static EventHandlerChain<DeleteDBInstanceEvent> onDeleteDBInstance(){
      return (new EventHandlerChainDeleteDbInstance()).build();
    }
    public static EventHandlerChain<EnableDBInstanceEvent> onEnableDBInstance() {
      return (new EventHandlerChainEnableVmDatabase()).build();
    }
    public static EventHandlerChain<DisableDBInstanceEvent> onDisableDBInstance() {
      return (new EventHandlerChainDisableVmDatabase()).build();
    }
}
