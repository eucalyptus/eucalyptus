package com.eucalyptus.cloudwatch.domain;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.CloudWatchException;
import com.eucalyptus.cloudwatch.InvalidNextTokenException;
import com.eucalyptus.cloudwatch.InvalidParameterValueException;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;

public class NextTokenUtils {

  private NextTokenUtils() {
  }

  public static Date getNextTokenCreatedTime(String nextToken, Class<? extends AbstractPersistent> clazz, boolean throwsNextTokenException) throws CloudWatchException{
    Date nextTokenCreated = null;
    if (nextToken != null) {
      Criteria nextTokenCriteria = Entities.createCriteria(clazz);
      nextTokenCriteria.add(Restrictions.eq("naturalId", nextToken));
      AbstractPersistent nextTokenEntity = (AbstractPersistent) nextTokenCriteria.uniqueResult();
      if (nextTokenEntity != null) {
        nextTokenCreated = nextTokenEntity.getCreationTimestamp();
      } else {
        if (throwsNextTokenException) {
          throw new InvalidNextTokenException("The token '" + nextToken +"' was invalid");
        } else {
          // hack (ListMetrics throws InvalidParameterValueException
          throw new InvalidParameterValueException("Invalid nextToken");
        }
      }
    }
    return nextTokenCreated;
  }
  
  public static Criteria addNextTokenConstraints(Integer maxRecords,
      String nextToken, Date nextTokenCreated, Criteria criteria) {
    if (nextTokenCreated != null) {
      // add (WHERE creationTimestamp > nextTokenCreated OR 
      // (creationTimestamp = nextTokenCreated AND naturalId > nextToken
      Junction or = Restrictions.disjunction();
      or.add(Restrictions.gt("creationTimestamp", nextTokenCreated));
      Junction and = Restrictions.conjunction();
      and.add(Restrictions.eq("creationTimestamp", nextTokenCreated));
      and.add(Restrictions.gt("naturalId", nextToken));
      or.add(and);
      criteria = criteria.add(or);
    }
    criteria = criteria.addOrder( Order.asc("creationTimestamp") );
    criteria = criteria.addOrder( Order.asc("naturalId") );
    if (maxRecords != null) {
      criteria = criteria.setMaxResults(maxRecords);
    }
    return criteria;
  }

}
