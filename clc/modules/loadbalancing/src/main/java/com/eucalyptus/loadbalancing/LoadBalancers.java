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

package com.eucalyptus.loadbalancing;

import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.entities.Entities;


public class LoadBalancers {
	private static Logger    LOG     = Logger.getLogger( LoadBalancers.class );
	public static LoadBalancer addLoadbalancer(UserFullName user, String lbName) throws LoadBalancingException
	{
		return LoadBalancers.addLoadbalancer(user,  lbName, null);
	}
	
	public static LoadBalancer addLoadbalancer(UserFullName user, String lbName, String scheme) throws LoadBalancingException {
		 final EntityTransaction db = Entities.get( LoadBalancer.class );
		 try {
		        try {
		        	if(Entities.uniqueResult( LoadBalancer.named( user, lbName )) != null)
		        		throw new LoadBalancingException(LoadBalancingException.DUPLICATE_LOADBALANCER_EXCEPTION);
		        } catch ( NoSuchElementException e ) {
		        	final LoadBalancer lb = LoadBalancer.newInstance(user, lbName);
		        	if(scheme!=null)
		        		lb.setScheme(scheme);
		        	Entities.persist( lb );
		          	db.commit( );
		          	return lb;
		        }
		    } catch ( Exception ex ) {
		    	db.rollback( );
		    	LOG.error("failed to persist a new loadbalancer", ex);
		    	throw new LoadBalancingException("Failed to persist a new load-balancer because of: " + ex.getMessage(), ex);
		  }
		  throw new LoadBalancingException("Failed to create a new load-balancer instance");
	}
	
	public static void deleteLoadbalancer(UserFullName user, String lbName) throws LoadBalancingException {
		final EntityTransaction db = Entities.get( LoadBalancer.class );
		try{
			final LoadBalancer lb = Entities.uniqueResult( LoadBalancer.named(user, lbName));	
			Entities.delete(lb);
			db.commit();
		}catch (NoSuchElementException e){
			throw new LoadBalancingException("No loadbalancer is found with name = "+lbName, e);
		}catch (Exception e){
			db.rollback();
			LOG.error("failed to delete a loadbalancer", e);
			throw new LoadBalancingException("Failed to delete the loadbalancer "+lbName, e);
		}
	}
}
