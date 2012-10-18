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
package com.eucalyptus.reporting.art.generator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityTransaction;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingAccountDao;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.domain.ReportingUserDao;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 *
 */
public abstract class AbstractArtGenerator implements ArtGenerator {

  protected static final String TIMESTAMP_MS = "timestampMs";

  protected ReportingUser getUserById( final String userId ) {
    return ReportingUserDao.getInstance().getReportingUser( userId );
  }

  protected ReportingAccount getAccountById( final String accountId ) {
    return ReportingAccountDao.getInstance().getReportingAccount( accountId );
  }

  protected ReportingUser getUserById( final Map<String, ReportingUser> reportingUsersById,
                                       final String userId ) {
    ReportingUser reportingUser;
    if ( reportingUsersById.containsKey( userId ) ) {
      reportingUser = reportingUsersById.get( userId );
    } else {
      reportingUser = getUserById( userId );
      reportingUsersById.put( userId, reportingUser );
    }
    return reportingUser;
  }

  protected String getAccountNameById( final Map<String, String> accountNamesById,
                                       final String accountId ) {
    String accountName;
    if ( accountNamesById.containsKey( accountId ) ) {
      accountName = accountNamesById.get( accountId );
    } else {
      final ReportingAccount account = getAccountById( accountId );
      accountName = account == null ? null : account.getName();
      accountNamesById.put( accountId, accountName );
    }
    return accountName;
  }

  protected Criterion between( final Long beginInclusive, final Long endExclusive ) {
    return Restrictions.conjunction()
        .add( Restrictions.ge( TIMESTAMP_MS, beginInclusive ) )
        .add( before( endExclusive ) );
  }

  protected Criterion before( final Long endExclusive ) {
    return Restrictions.lt( TIMESTAMP_MS, endExclusive );
  }

  protected <KT,ET extends ReportingEventSupport> Predicate<ET> buildTimestampMap(
      final ReportArtEntity report,
      final Map<KT,List<Long>> keyToTimesMap,
      final Function<ET,KT> keyBuilder ) {
    return new Predicate<ET>(){
      @Override
      public boolean apply( final ET event ) {
        if ( event.getTimestampMs() <= report.getEndMs() ) {
          final KT key = keyBuilder.apply( event );
          List<Long> endTimes = keyToTimesMap.get( key );
          if ( endTimes == null ) {
            endTimes = Lists.newArrayList( event.getTimestampMs() );
            keyToTimesMap.put( key, endTimes );
          } else {
            endTimes.add( event.getTimestampMs() );
          }
          Collections.sort( endTimes );
        } else {
          return false; // end of relevant data
        }
        return true;
      }
    };
  }

  protected <KT> Long findTimeAfter( final Map<KT, List<Long>> keyToEndTimesMap,
                                     final KT key,
                                     final Long startTime ) {
    Long timeAfter = Long.MAX_VALUE;

    final List<Long> endTimesForKey = keyToEndTimesMap.get( key );
    if ( endTimesForKey != null ) {
      for ( final Long endTime : endTimesForKey ) {
        if ( endTime > startTime ) {
          timeAfter = endTime;
          break;
        }
      }
    }

    return timeAfter;
  }

  @SuppressWarnings( "unchecked" )
  protected <ET> void foreach( final Class<ET> eventClass,
                               final Criterion criterion,
                               final boolean ascending,
                               final Predicate<? super ET> callback ) {
    final EntityTransaction transaction = Entities.get( eventClass );
    ScrollableResults results = null;
    try {
      results = Entities.createCriteria( eventClass )
          .setReadOnly( true )
          .setCacheable( false )
          .setCacheMode( CacheMode.IGNORE )
          .setFetchSize( 100 )
          .add( criterion )
          .addOrder( ascending ? Order.asc( TIMESTAMP_MS ) : Order.desc( TIMESTAMP_MS ) )
          .scroll( ScrollMode.FORWARD_ONLY );

      while ( results.next() ) {
        final ET event = (ET) results.get( 0 );
        if ( !callback.apply( event ) ) {
          break;
        }
        Entities.evict( event );
      }
    } finally {
      if (results != null) try { results.close(); } catch( Exception e ) { }
      transaction.rollback();
    }
  }


}
