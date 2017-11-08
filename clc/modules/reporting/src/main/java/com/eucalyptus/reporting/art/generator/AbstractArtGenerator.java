/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
