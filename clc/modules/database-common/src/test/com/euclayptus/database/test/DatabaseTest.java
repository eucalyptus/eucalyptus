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
package com.euclayptus.database.test;

import org.junit.Test;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.database.activities.DatabaseEventListeners;
import com.eucalyptus.database.activities.EnableDBInstanceEvent;

/**
 * @author Sang-Min Park
 *
 */
public class DatabaseTest {

  @Test
  public void enableDatabase() {
    final String userName = "eucalyptus";
    final String password = "foobar";
    final String dbId = "postgresql";
    final int port = 5432;
    
    try{
      final EnableDBInstanceEvent evt = new EnableDBInstanceEvent(Accounts.lookupSystemAdmin().getUserId());
      evt.setMasterUserName(userName);
      evt.setMasterUserPassword(password);
      evt.setDbInstanceIdentifier(dbId);
      evt.setPort(port);
      DatabaseEventListeners.getInstance().fire(evt); 
      
      System.out.println("Enabled remote database");
    }catch(final Exception ex) {
      System.out.println("Failed");
      ex.printStackTrace();
    }
  }
}
