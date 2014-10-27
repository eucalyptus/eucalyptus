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

import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;

/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainDisableVmDatabase extends EventHandlerChain<DisableDBInstanceEvent> {
  private static Logger  LOG = Logger.getLogger( EventHandlerChainDisableVmDatabase.class );

  @Override
  public EventHandlerChain<DisableDBInstanceEvent> build() {
    this.append(new UnsetProperties(this));
    return this;
  }
  
  public static class UnsetProperties extends AbstractEventHandler<DisableDBInstanceEvent> {
    protected UnsetProperties(EventHandlerChain<DisableDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(DisableDBInstanceEvent evt) throws EventHandlerException {
      try{
        final ConfigurableProperty hostProp = 
            PropertyDirectory.getPropertyEntry("cloud.db.appendonlyhost");
        hostProp.setValue("localhost");
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to set hostname property", ex);
      }
      LOG.info("cloud.db.appendonly* properties are updated");
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
  }
}
