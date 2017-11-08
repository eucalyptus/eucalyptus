/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudwatch.common.internal.domain;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;

public class NextTokenUtils {

  private NextTokenUtils() {
  }

  public static Date getNextTokenCreatedTime(String nextToken, Class<? extends AbstractPersistent> clazz) throws InvalidTokenException {
    Date nextTokenCreated = null;
    if (nextToken != null) {
      Criteria nextTokenCriteria = Entities.createCriteria(clazz);
      nextTokenCriteria.add(Restrictions.eq("naturalId", nextToken));
      AbstractPersistent nextTokenEntity = (AbstractPersistent) nextTokenCriteria.uniqueResult();
      if (nextTokenEntity != null) {
        nextTokenCreated = nextTokenEntity.getCreationTimestamp();
      } else {
        throw new InvalidTokenException("The token '" + nextToken +"' was invalid");
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
