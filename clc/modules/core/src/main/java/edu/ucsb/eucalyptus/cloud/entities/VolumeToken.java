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

package edu.ucsb.eucalyptus.cloud.entities;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

@Entity 
@javax.persistence.Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "volume_tokens" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class VolumeToken extends AbstractPersistent {
	private static final Logger LOG = Logger.getLogger(VolumeToken.class);		
	private static final long serialVersionUID = 1L;
	private static final Integer TX_RETRIES = 3;

	@Column(name="token", unique=true, nullable = false)
	private String token;

	@ManyToOne
	@JoinColumn( name = "volume", nullable=true)	
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private VolumeInfo volume;
	
	@Column(name="is_valid")
	private Boolean isValid;
	
	@OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "token" )
	private List<VolumeExportRecord> exportRecords;
	
	public VolumeToken() {
		volume = null;
		token = null;
	}
	
	public VolumeToken(VolumeInfo vol) {
		this.volume = vol;
		this.token = null;
		this.exportRecords = null;
		this.isValid = null;
	}
	
	public VolumeToken(VolumeInfo vol, String token, boolean valid) {
		this.volume = vol;
		this.token = token;
		this.exportRecords = null;
		this.isValid = valid;
	}
	
	public VolumeToken(boolean isValid) {
		this.volume = null;
		this.token = null;
		this.exportRecords = null;
		this.isValid = isValid;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Boolean getIsValid() {
		return isValid;
	}

	public void setIsValid(Boolean isValid) {
		this.isValid = isValid;
	}

	public List<VolumeExportRecord> getExportRecords() {
		return exportRecords;
	}

	public void setExportRecords(List<VolumeExportRecord> exportRecords) {
		this.exportRecords = exportRecords;
	}
	
	public VolumeInfo getVolume() {
		return volume;
	}

	public void setVolume(VolumeInfo volumeInfo) {
		this.volume = volumeInfo;
	}
	  
	/**
	 * Create a new token for the requested volume. This does NOT guarantee uniqueness, the caller
	 * must guarantee that only on token is generated per request
	 * @param vol
	 * @return
	 */
	public static VolumeToken generate(final VolumeInfo vol) throws EucalyptusCloudException {
		Function<VolumeInfo, VolumeToken> addToken = new Function<VolumeInfo, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeInfo src) {
				try {
					VolumeInfo volRecord = Entities.merge(src);
					VolumeToken token = new VolumeToken();
					token.setVolume(volRecord);
					token.setIsValid(true);
					token.setToken(Crypto.generateSessionToken());					
					return token;
				} catch(Exception e) {
					LOG.error("Error creating new volume token for " + vol.getVolumeId());
				} 
				return null;						
			}
		};
		
		try {
			return Entities.asTransaction(VolumeInfo.class, addToken, VolumeToken.TX_RETRIES).apply(vol);
		} catch(RuntimeException e) {
			LOG.error("Failed to create new token for volume " + vol.getVolumeId() + " Msg: " + e.getMessage(), e);
			throw new EucalyptusCloudException("Failed new token creation.",e);
		}
	}
	
	public VolumeExportRecord getValidExport(String ip, String iqn) throws EucalyptusCloudException {		
		EntityTransaction db = Entities.get(VolumeToken.class);
		try {
			VolumeToken tokenEntity = Entities.merge(this);		
			for(VolumeExportRecord rec : tokenEntity.getExportRecords()) {
				if(rec.getIsActive() && rec.getHostIp().equals(ip) && rec.getHostIqn().equals(iqn)) {
					db.commit();
					return rec;
				}
			}
			db.commit();
		} catch(Exception e) {
			LOG.error("Error when checking for valid export to " + ip + " and " + iqn + " for volume " + this.getVolume().getVolumeId() + " and token " + this.getToken());
			throw new EucalyptusCloudException("Failed to check for valid export",e);
		} finally {
			if(db.isActive()) { 
				db.rollback();
			}
		}
		return null;
	}
	
	/**
	 * Does this token have any associated active export records
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public boolean hasActiveExports() throws EucalyptusCloudException {
		try {
			return Iterables.any(this.getExportRecords(), new Predicate<VolumeExportRecord>() {
				@Override
				public boolean apply(VolumeExportRecord rec) {
					return rec.getIsActive();
				}
			});
		} catch(Exception e) {
			LOG.error("Error when checking for active exports volume " + this.getVolume().getVolumeId() + " and token " + this.getToken());
			throw new EucalyptusCloudException("Failed to check for valid export",e);
		} 
	}
	
	/**
	 * Return true if and only if this token's only active export is for the given ip/iqn pair.
	 * Does not restrict to a single export record, but if multiple exist for same ip/iqn it will
	 * still return true 
	 * @param ip
	 * @param iqn
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public boolean hasOnlyExport(final String ip, final String iqn) throws EucalyptusCloudException {
		try {
			return Iterables.all(this.getExportRecords(), new Predicate<VolumeExportRecord>() {
				@Override
				public boolean apply(VolumeExportRecord rec) {
					if(rec.getIsActive()) {
						return rec.getHostIp().equals(ip) && rec.getHostIqn().equals(iqn);
					} else {
						//Return true if not an active export, since we don't care which host it is for.
						return true;
					}
				}
			});
		} catch(Exception e) {
			LOG.error("Error checking for only export on " + ip + " : " + iqn + ". Error:" + e.getMessage());
			throw new EucalyptusCloudException(e);
		}
	}
	
	
	/**
	 * Invalidate the export for this token for the given ip and iqn
	 * Does not remove any info, just sets invalidate
	 * @param ip
	 * @param iqn
	 */
	public void invalidateExport(final String ip, final String iqn) throws EucalyptusCloudException {
		Function<VolumeToken, VolumeToken> deactivateExport = new Function<VolumeToken, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeToken tok){
				VolumeToken tokenEntity = Entities.merge(tok);			
				try {
					for(VolumeExportRecord rec : tokenEntity.getExportRecords()) {
						if(rec.getIsActive() && rec.getHostIp().equals(ip) && rec.getHostIqn().equals(iqn)) {						
							rec.setIsActive(Boolean.FALSE);
							break;
						}
					}
					
					Predicate<VolumeExportRecord> notActive = new Predicate<VolumeExportRecord>() {
						@Override
						public boolean apply(VolumeExportRecord record) {
							return !record.getIsActive();
						}						
					};
					
					//If no records are active, then invalidate the token
					if(Iterators.all(tokenEntity.getExportRecords().iterator(),notActive)) {
						//Invalidate the token as well.
						tok.setIsValid(Boolean.FALSE);
					}
					Entities.flush(tokenEntity);
					return tokenEntity;
				} catch(Exception e) {
					LOG.error("Could not invalidate export record for volume " + tok.getVolume().getVolumeId() + " token " + tok.getToken() + " ip " + ip + " iqn " + iqn, e);				
				}				
				return null;
			}		
		};
		
		try {
			if(Entities.asTransaction(VolumeExportRecord.class, deactivateExport).apply(this) == null) {
				throw new Exception("Failed to invalidate export, got null result from deactivation");
			}
		} catch(Exception e) {
			LOG.error("Failed to invalidate export: " + e.getMessage(),e);
			throw new EucalyptusCloudException("Failed to invalidate export");
		}
	}
	
	/**
	 * Add a new export for this token
	 * @param ip - the ip of the client exported to
	 * @param iqn - the iqn for the client exported to
	 * @param connectionString - the connection string for this export
	 */
	public void addExport(final String ip, final String iqn, final String connectionString) throws EucalyptusCloudException {		
		Function<VolumeToken, VolumeToken> createExport = new Function<VolumeToken, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeToken token) {
				VolumeToken tok = null;				
				tok = Entities.merge(token);
				try {
					if(tok.getValidExport(ip, iqn) == null || !connectionString.equals(tok.getValidExport(ip, iqn).getConnectionString())) {
						LOG.trace("Adding new volume export record to token");
						VolumeExportRecord record = new VolumeExportRecord();
						record.setToken(tok);
						record.setVolume(tok.getVolume());
						record.setHostIp(ip);
						record.setHostIqn(iqn);
						record.setConnectionString(connectionString);
						record.setIsActive(true);						
						Entities.flush(record);
						tok.exportRecords.add(record);
						return tok;
					}											
				} catch(Exception e) {
					LOG.error("Error adding new export record", e);
				}
				return tok;
			}
		};
		
		try {
			Entities.asTransaction(VolumeToken.class, createExport).apply(this);
		} catch(Exception e) {
			LOG.error("Failed to add export");
		}
	}

	public void invalidateAllExportsAndToken() throws EucalyptusCloudException {
		Function<VolumeToken, VolumeToken> invalidateToken = new Function<VolumeToken, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeToken tok){
				VolumeToken tokenEntity = Entities.merge(tok);			
				try {
					for(VolumeExportRecord rec : tokenEntity.getExportRecords()) {						
						rec.setIsActive(Boolean.FALSE);
					}
					tok.setIsValid(Boolean.FALSE);
					return tokenEntity;
				} catch(Exception e) {
					LOG.error("Could not invalidate export record for volume " + tok.getVolume().getVolumeId() + " token " + tok.getToken(), e);
				} 								
				return null;
			}
		};
		
		try {
			if(Entities.asTransaction(VolumeExportRecord.class, invalidateToken).apply(this) == null) {
				throw new Exception("Failed to invalidate export, got null result from deactivation");
			}
		} catch(Exception e) {
			LOG.error("Failed to invalidate export: " + e.getMessage(),e);
			throw new EucalyptusCloudException("Failed to invalidate export");
		}		
	}
}
