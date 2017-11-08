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