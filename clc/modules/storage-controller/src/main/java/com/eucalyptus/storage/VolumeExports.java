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

package com.eucalyptus.storage;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

import edu.ucsb.eucalyptus.cloud.entities.VolumeExportRecord;
import edu.ucsb.eucalyptus.cloud.entities.VolumeToken;

/**
 * Manages the collection of Volume Exports and their associated entity state and tokens.
 * This is the interface point for the SC to the volume export metadata.
 * @author zhill
 *
 */
public class VolumeExports {
	private static final Logger LOG = Logger.getLogger(VolumeExports.class);
	
	private static ScheduledExecutorService cleanupService = null;
	private static final VolumeToken exampleInvalidToken = new VolumeToken(false); //Use the same example, no need to regen each time.
	
	public static void initialize() {
		cleanupService= Executors.newSingleThreadScheduledExecutor();

		//Run in 1 minute and every 5 minutes thereafter
		cleanupService.scheduleAtFixedRate(new Runnable () {
			@Override
			public void run() {
				//Query the DB for invalid tokens, then delete them.
				EntityTransaction db = Entities.get(VolumeToken.class);								
				List<VolumeToken> tokens = Entities.query(exampleInvalidToken);
				try {
					boolean skip = false;
					//Could also use Entities.deleteAllMatching()...need to look at that as well
					for(VolumeToken t : tokens) {
						skip = false;
						for(VolumeExportRecord r : t.getExportRecords()) {
							//Ensure all records are inactive
							if(r.getIsActive()) {
								skip=true;
							}
						}
						if(!skip) {
							Entities.delete(t);
						}
					}
				} finally {
					if(db.isActive()) {
						try {
							db.commit();
						} catch(final Throwable t) {
							LOG.error("VolumeExport cleanup thread commit failed: " + t.getMessage());
							db.rollback();
						}
					}
				}
				
			}
		}, 1, 5, TimeUnit.MINUTES);
	}
	
	public static void shutdown() {
		LOG.trace("Shutting down VolumeExport checker threads");
		//cleanupService.shutdown();
	}
	
	/*
	 * TODO:
	 * Implement a state machine for a volume export
	 * 
	 * States:
	 * 1. initialized
	 * 2. exported
	 * 3. unexported
	 * 
	 * Transitions:
	 * Start -> initialized (do Issue)
	 * initialized -> exported (add valid export)
	 * exported -> exported (invalidate export, but count(ExtantExports) > 0)
	 * exported -> unexported (invalidate export(s), count(ExtantExports) == 0)
	 * unexported -> unexported (attempt export)
	 * unexported -> unexported (attampt unexport)
	 * Valid_issued -> Invalid (invalidate all exports and token)
	 */
	
	
	
	public static VolumeToken issueToken(String volumeId) {
		EntityTransaction db = Entities.get(VolumeToken.class);
		try {
			VolumeToken token = new VolumeToken();
			token.setToken(Crypto.generateSessionToken());
			token.setVolumeId(volumeId);
			token.setIsValid(true);			
			Entities.persist(token);
			db.commit();			
			return token;
		} catch(Exception e) {
			LOG.error("Error creating new volume token for " + volumeId);
		} finally {
			if(db.isActive()) {
				db.rollback();
			}
		}
		return null;
	}
	
	/**
	 * Add/request a new export record added for the specified token. Will return false if not possible due to token invalid
	 * @param volumeId
	 * @param token
	 * @param ip
	 * @param iqn
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public static boolean addExport(final String volumeId, final String token, final String ip, final String iqn) throws EucalyptusCloudException {
		final VolumeToken requestToken = new VolumeToken(volumeId);
		requestToken.setToken(token);
		
		Function<VolumeToken, VolumeToken> checkToken = new Function<VolumeToken, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeToken reqToken) {
				VolumeToken tok = null;
				EntityTransaction db = Entities.get(VolumeToken.class);
				try {
					tok = Entities.uniqueResult(reqToken);
					Entities.merge(tok);					
					//Do the update of export.
					VolumeExportRecord record = new VolumeExportRecord();
					record.setToken(tok);
					record.setHostIp(ip);
					record.setHostIqn(iqn);
					tok.addExportRecord(new VolumeExportRecord(volumeId, tok, ip, iqn));
					Entities.persist(record);
					return tok;
				} catch (TransactionException e) {
					LOG.error("Volume Export: " + volumeId + " Transaction error. Rolling back");
					
				} catch (NoSuchElementException e) {
					LOG.error("Invalid Token received for volume " + volumeId);
					
				} finally {
					if(db.isActive()) {
						db.rollback();
					}
				}
				
				return tok;
			}
		};
		
		try {
			Entities.asTransaction(VolumeToken.class, checkToken).apply(requestToken);
			return true;
		} catch(Exception e) {
			return false;
		}		
	}
	
	/**
	 * Remove an extant export record
	 * @param volumeId
	 * @param token
	 * @param hostIp
	 * @param hostIqn
	 * @return
	 */
	public static boolean removeExport(String volumeId, String token, String hostIp, String hostIqn) {
		EntityTransaction db = Entities.get(VolumeToken.class);
		try {
			VolumeToken foundToken = Entities.uniqueResult(new VolumeToken(volumeId, token, true));
			if(foundToken.getIsValid()) {					
				//TODO: make this cleaner, but still should not delete token after use, semantics are for token to follow vm attachment lifecycle
				LOG.debug("Valid token found, invalidating token on unexport " + volumeId);
				foundToken.setIsValid(Boolean.FALSE);
				db.commit();
				return true;
			} else {
				LOG.warn("Attempted to remove export with invalid token for volume " + volumeId);
			}
		} catch(Exception e) {
			LOG.error("Error checking token for volume " + volumeId + " : " + e.getMessage());
			LOG.debug("Exception caught invalidating token for vol: " + volumeId,e);
		} finally {
			if(db.isActive()) {
				db.rollback();
			}
		}
		LOG.warn("Cannot remove export for volume " + volumeId);
		return false;
	}
	
	//For admin stuff
	public static void flushAllTokens() {
		EntityTransaction db = Entities.get(VolumeToken.class);
		try {
			Entities.deleteAll(VolumeToken.class);
		} catch(Throwable e) {
			LOG.error("Error flushing volume tokens: " + e.getMessage());
		} finally {
			db.commit();
		}		
	}
	
	public boolean checkToken(String volumeId, String token) throws EucalyptusCloudException {
		EntityTransaction db = Entities.get(VolumeToken.class);
		try {
			VolumeToken tok = new VolumeToken();
			tok.setToken(token);
			VolumeToken t = Entities.uniqueResult(tok);
			return t.getIsValid();						
		} catch(TransactionException e) {
			LOG.error("Transaction exception checking", e);
			throw new EucalyptusCloudException(e);
		} catch(NoSuchElementException e) {
			return false;
		} finally {
			db.rollback();
		}
	}
		
	public boolean invalidateToken(String volumeId, String token) throws EucalyptusCloudException {
		EntityTransaction db = Entities.get(VolumeToken.class);
		try {
			VolumeToken foundToken = Entities.uniqueResult(new VolumeToken(volumeId, token, true));
			if(foundToken.getIsValid()) {					
				//TODO: make this cleaner, but still should not delete token after use, semantics are for token to follow vm attachment lifecycle
				//LOG.debug("Valid token found, invalidating after one use for volume " + volumeId);
				//Entities.delete(foundToken);
				db.commit();					
				return true;
			}
		} catch(Exception e) {
			LOG.error("Error checking token for volume " + volumeId + " : " + e.getMessage());
			LOG.debug("Exception caught invalidating token for vol: " + volumeId,e);
		} finally {
			if(db.isActive()) {
				db.rollback();
			}
		}			
		return false;
	}
}
