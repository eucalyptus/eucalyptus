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
 ************************************************************************/

package edu.ucsb.eucalyptus.cloud.ws;

import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.BucketInfo;
import edu.ucsb.eucalyptus.cloud.entities.ObjectInfo;

public class WalrusUtil {

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
  
}
