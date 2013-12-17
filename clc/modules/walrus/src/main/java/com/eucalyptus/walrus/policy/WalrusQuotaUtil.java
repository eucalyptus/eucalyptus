/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.walrus.policy;

import java.util.List;
import javax.persistence.EntityTransaction;
import org.hibernate.criterion.Projections;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.walrus.entities.BucketInfo;
import com.eucalyptus.walrus.entities.ObjectInfo;
import com.google.common.base.Objects;

public class WalrusQuotaUtil {

  public static long countBucketByAccount( String accountId ) throws AuthException {
    EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
    BucketInfo searchBucket = new BucketInfo();
    searchBucket.setOwnerId(accountId);
    try {
      List<BucketInfo> bucketInfoList = db.query(searchBucket);
      db.commit();
      return bucketInfoList.size();
    } catch (Exception e) {
      db.rollback();
      throw new AuthException("Failed to search bucket", e);
    }
  }
  
  public static long countBucketByUser( String userId ) throws AuthException {
    EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
    BucketInfo searchBucket = new BucketInfo();
    searchBucket.setUserId(userId);
    try {
      List<BucketInfo> bucketInfoList = db.query(searchBucket);
      db.commit();
      return bucketInfoList.size();
    } catch (Exception e) {
      db.rollback();
      throw new AuthException("Failed to search bucket", e);
    }
  }
  
  public static long countBucketObjectNumber(String bucketName) throws AuthException {
    EntityWrapper<ObjectInfo> db = EntityWrapper.get(ObjectInfo.class);
    ObjectInfo searchObjectInfo = new ObjectInfo();
    searchObjectInfo.setBucketName(bucketName);
    searchObjectInfo.setDeleted(false);
    searchObjectInfo.setLast(true);
    try {
      List<ObjectInfo> objectInfos = db.query(searchObjectInfo);
      db.commit();
      return objectInfos.size();
    } catch (Exception e) {
      db.rollback();
      throw new AuthException("Failed to search object", e);
    }
  }
  
  public static long countBucketSize(String bucketName) throws AuthException {
    EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
    BucketInfo searchBucket = new BucketInfo(bucketName);
    try {
      long size = 0;
      List<BucketInfo> bucketInfoList = db.query(searchBucket);
      if (bucketInfoList.size() > 0) {
        size = bucketInfoList.get(0).getBucketSize();
      }
      db.commit();
      return size;
    } catch (Exception e) {
      db.rollback();
      throw new AuthException("Failed to search bucket", e);
    }
  }
  
  public static long countTotalObjectSizeByAccount(String accountId) throws AuthException {
    EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
    BucketInfo searchBucket = new BucketInfo();
    searchBucket.setOwnerId(accountId);
    try {
      List<BucketInfo> bucketInfoList = db.query(searchBucket);
      long size = 0;
      for (BucketInfo b : bucketInfoList) {
        size += b.getBucketSize();
      }
      db.commit();
      return size;
    } catch (Exception e) {
      db.rollback();
      throw new AuthException("Failed to search bucket", e);
    }
  }
  
  public static long countTotalObjectSizeByUser(String userId) throws AuthException {
    EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
    BucketInfo searchBucket = new BucketInfo();
    searchBucket.setUserId(userId);
    try {
      List<BucketInfo> bucketInfoList = db.query(searchBucket);
      long size = 0;
      for (BucketInfo b : bucketInfoList) {
        size += b.getBucketSize();
      }
      db.commit();
      return size;
    } catch (Exception e) {
      db.rollback();
      throw new AuthException("Failed to search bucket", e);
    }
  }

  /**
   * Return the total size in bytes of objects in the Walrus.
   *
   * @return The size or -1 if the size could not be determined.
   */
  public static long countTotalObjectSize() {
    long size = -1;
    final EntityTransaction db = Entities.get( BucketInfo.class );
    try {
      size = Objects.firstNonNull( (Number) Entities.createCriteria( BucketInfo.class )
          .setProjection( Projections.sum( "bucketSize" ) )
          .setReadOnly( true )
          .uniqueResult(), 0 ).longValue();
      db.commit();
    } catch (Exception e) {
      db.rollback();
    }
    return size;
  }
}
