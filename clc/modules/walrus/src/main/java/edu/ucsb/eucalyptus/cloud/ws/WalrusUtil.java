package edu.ucsb.eucalyptus.cloud.ws;

import java.util.List;
import javax.persistence.EntityTransaction;
import org.hibernate.criterion.Projections;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.google.common.base.Objects;
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

  /**
   * Return the total size of objects in the Walrus.
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
