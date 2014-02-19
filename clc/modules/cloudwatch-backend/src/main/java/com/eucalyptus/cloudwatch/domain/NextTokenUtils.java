/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch.domain;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.backend.CloudWatchException;
import com.eucalyptus.cloudwatch.backend.InvalidNextTokenException;
import com.eucalyptus.cloudwatch.backend.InvalidParameterValueException;
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
      criteria.add(or);
    }
    criteria.addOrder( Order.asc("creationTimestamp") );
    criteria.addOrder( Order.asc("naturalId") );
    if (maxRecords != null) {
      criteria.setMaxResults(maxRecords);
    }
    return criteria;
  }

}
