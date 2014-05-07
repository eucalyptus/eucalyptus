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
package com.eucalyptus.imaging.worker;
import com.google.common.base.Function;
/**
 * @author Sang-Min Park
 *
 */
public abstract class AbstractAction {
  private Function<Class<? extends AbstractAction>, AbstractAction> actionLookup = null;
  private String actionGroupId = null;
  public AbstractAction(Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId) {
    actionLookup = lookup;
    this.actionGroupId = groupId;
  }
  public abstract boolean apply() throws ImagingServiceActionException;
  public abstract void rollback() throws ImagingServiceActionException;
  public abstract String getResult();
  
  public String getResult(Class<? extends AbstractAction> actionClass){
    try{
      return actionLookup.apply(actionClass).getResult();
    }catch(final Exception ex){
      return null;
    }
  }
  
  public String getGroupId(){
    return this.actionGroupId;
  }
  
  @Override
  public String toString(){
    return String.format("Imaging Service launch action: %s-%s", this.actionGroupId, this.getClass().toString());
  }
}
