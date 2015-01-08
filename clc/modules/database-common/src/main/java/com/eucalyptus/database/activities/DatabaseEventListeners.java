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

package com.eucalyptus.database.activities;

import org.apache.log4j.Logger;

import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.resources.EventHandlerChainException;
import com.eucalyptus.util.Exceptions;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class DatabaseEventListeners {

  private static final Logger LOG = Logger.getLogger( DatabaseEventListeners.class );

  private DatabaseEventListeners(){
    DatabaseEventListener.register();
  }
  private static DatabaseEventListeners _instance = new DatabaseEventListeners();
  public static DatabaseEventListeners getInstance(){
    return _instance;
  }

  public void fire(DatabaseEvent evt) throws EventFailedException {
    ListenerRegistry.getInstance().fireThrowableEvent(evt);
  }

  enum DatabaseEventListener implements EventListener<DatabaseEvent>{
    NewDBInstance(NewDBInstanceEvent.class) {
      @Override
      public void fireEvent(DatabaseEvent event) {
        try{
         EventHandlerChains.onNewDBInstance().execute((NewDBInstanceEvent) event);
        }catch(final EventHandlerChainException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }, 
    DeleteDBInstance(DeleteDBInstanceEvent.class) {
      @Override
      public void fireEvent(DatabaseEvent event) {
        try{
          EventHandlerChains.onDeleteDBInstance().execute((DeleteDBInstanceEvent) event);
        }catch(final EventHandlerChainException ex) {
          throw Exceptions.toUndeclared(ex);
        }
      }
    }, 
    EnableDBInstance(EnableDBInstanceEvent.class) {

      @Override
      public void fireEvent(DatabaseEvent event) {
        try{
          EventHandlerChains.onEnableDBInstance().execute((EnableDBInstanceEvent) event);
        }catch(final EventHandlerChainException ex) {
          throw Exceptions.toUndeclared(ex);
        }
      }      
    }, 
    DisableDBInstance(DisableDBInstanceEvent.class) {
      @Override
      public void fireEvent(DatabaseEvent event) {
        try{
          EventHandlerChains.onDisableDBInstance().execute((DisableDBInstanceEvent) event);
        }catch(final EventHandlerChainException ex) {
          throw Exceptions.toUndeclared(ex);
        }
      }
    };

    private final Class<? extends DatabaseEvent> evtType;

    DatabaseEventListener(Class<? extends DatabaseEvent> type){
      evtType = type;
    }
  
    public static void register(){
      for (DatabaseEventListener listener : DatabaseEventListener.values()){
        Listeners.register(listener.evtType, listener);
      }
    }
  }
}
