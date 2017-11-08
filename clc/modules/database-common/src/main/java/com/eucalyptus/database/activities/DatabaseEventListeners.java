/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
