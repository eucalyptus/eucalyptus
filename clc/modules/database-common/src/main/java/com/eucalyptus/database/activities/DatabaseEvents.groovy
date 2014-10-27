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
package com.eucalyptus.database.activities
import com.eucalyptus.event.GenericEvent
import com.eucalyptus.resources.ResourceEvent
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
class DatabaseEvent extends ResourceEvent { 
  String userId = null;
}

class DatabaseUserEvent extends DatabaseEvent{ 
  DatabaseUserEvent(final String userId){
    this.userId = userId;
  }
  
  @Override
  public boolean equals(final Object other){
    if (this == other)
      return true;
    else if(other != null && other instanceof DatabaseUserEvent ) {
      final DatabaseUserEvent otherEvt = (DatabaseUserEvent) other;
      return this.getClass().equals(other.getClass()) && this.userId.equals(otherEvt.userId);
    }else {
      return false;
    }
  }
  
  @Override
  public int hashCode(){
    int result = this.getClass().hashCode();
    result = 31 * result + (this.userId != null ? this.userId.hashCode() : 0);
    return result;
  }
}

class DatabaseAdminEvent extends DatabaseEvent{ 
  DatabaseAdminEvent() {
    this.userId = null;
  }
}

/* from AWS RDS createDbInstance api */
class NewDBInstanceEvent extends DatabaseUserEvent { 
  NewDBInstanceEvent(final String userId) {
    super(userId);
  }
  
	int allocatedStorage = 10;
  Boolean autoMinorVersionUpgrade = false;
  String availabilityZone = null;
  int backupRetentionPeriod = 1;
  String characterSetName = null;
  String dbInstanceClass = null;
  String dbInstanceIdentifier = null;
  String dbName = null;
  String dbParameterGroupName = null;
  Collection<String> dbSecurityGroups = Lists.newArrayList();
  String dbSubnetGroupName = null;
  String engine = "postgres";
  String engineVersion = "9.3";
  int iops = -1;
  String licenseModel = "general-public-license";
  String masterUserPassword = null;
  String masterUserName = null;
  Boolean multiAz = false;
  Boolean optionGroupName = false;
  int port = 5432;
  String preferredBackupWindow = null;
  String preferredMaintenanceWindow = null;
  Boolean publiclyAccessible = false;
  Collection<String> tags = Lists.newArrayList();
  Collection<String> vpcSecurityGroupIds = Lists.newArrayList();
  
  
  @Override
  public boolean equals(final Object other){
    if (this == other)
      return true;
    else if(other != null && other instanceof NewDBInstanceEvent ) {
      final NewDBInstanceEvent otherEvt = (NewDBInstanceEvent) other;
      return this.userId.equals(otherEvt.userId) && this.dbInstanceIdentifier.equals(otherEvt.dbInstanceIdentifier);
    }else {
      return false;
    }
  }
  
  @Override
  public int hashCode(){
    int result = this.getClass().hashCode();
    result = 31 * result + (this.userId != null ? this.userId.hashCode() : 0);
    result = 31 * result + (this.dbInstanceIdentifier != null ? this.dbInstanceIdentifier.hashCode() : 0);
    return result;
  }
 }

class DeleteDBInstanceEvent extends DatabaseUserEvent {
  DeleteDBInstanceEvent(final String userId) {
    super(userId);
  }
  String dbInstanceIdentifier;
  String finalDbSnapshotIdentifier;
  Boolean skipFinalSnapshot; 
}

class EnableDBInstanceEvent extends DatabaseUserEvent {
  EnableDBInstanceEvent(final String userId) {
    super(userId);
  }
  String dbInstanceIdentifier;
  String masterUserPassword;
  String masterUserName;
  int port = 5432;
}

class DisableDBInstanceEvent extends DatabaseUserEvent {
  DisableDBInstanceEvent(final String userId) {
    super(userId);
  }
  String dbInstanceIdentifier;  
}