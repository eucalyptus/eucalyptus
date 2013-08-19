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

package com.eucalyptus.blockstorage.entities;

import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "Volumes")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class VolumeInfo extends AbstractPersistent {
	private static final Logger LOG = Logger.getLogger(VolumeInfo.class);

	@Column(name = "volume_user_name")
	private String userName;
	@Column(name = "volume_name", unique = true)
	private String volumeId;
	@Column(name = "sc_name")
	private String scName;
	@Column(name = "size")
	private Integer size; // in GB
	@Column(name = "status")
	private String status;
	@Column(name = "create_time")
	private Date createTime;
	@Column(name = "zone")
	private String zone;
	@Column(name = "snapshot_id")
	private String snapshotId;

	@NotFound( action = NotFoundAction.IGNORE )
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "volume", orphanRemoval=true, cascade = CascadeType.ALL)
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	private List<VolumeToken> attachmentTokens;

	public VolumeInfo() {
		this.scName = StorageProperties.NAME;
	}

	public VolumeInfo(String volumeId) {
		this();
		this.volumeId = volumeId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getVolumeId() {
		return volumeId;
	}

	public void setVolumeId(String volumeId) {
		this.volumeId = volumeId;
	}

	public String getScName() {
		return scName;
	}

	public void setScName(String scName) {
		this.scName = scName;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(String snapshotId) {
		this.snapshotId = snapshotId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VolumeInfo other = (VolumeInfo) obj;
		if (scName == null) {
			if (other.scName != null)
				return false;
		} else if (!scName.equals(other.scName))
			return false;
		if (volumeId == null) {
			if (other.volumeId != null)
				return false;
		} else if (!volumeId.equals(other.volumeId))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return scName.hashCode() + volumeId.hashCode();
	}

	public List<VolumeToken> getAttachmentTokens() {
		return attachmentTokens;
	}

	public void setAttachmentTokens(List<VolumeToken> attachmentTokens) {
		this.attachmentTokens = attachmentTokens;
	}

	private static String decryptToken(String tokenCipher) throws EucalyptusCloudException { 	
		//Decrypt the token with the Cloud's private key.
		try {
			return BlockStorageUtil.decryptWithCloud(tokenCipher);
		} catch(EucalyptusCloudException e) {
			LOG.error("Failed to decrypt token: " + tokenCipher);
			throw e;
		}
	}
	
	/**
	 * Returns the last 4 chars prefixed by '*' to meet the same length as rawToken.
	 * For printing the token in the logs. Just shows '**********ad4e'
	 * @param rawToken
	 * @return
	 */
	private static String redactToken(String rawToken) {
		String tokenSuffix = new String(rawToken).substring(rawToken.length() - 4);
		StringBuilder redact = new StringBuilder();
		for(int i = 0; i < rawToken.length() - 4; i++) {
			redact.append('*');
		}
		redact.append(tokenSuffix);
		return redact.toString();
	}
	
	/**
	 * Returns either the single valid attachment token for the volume, or null
	 * if none exists. Throws an exception if multiple valid tokens are found
	 * 
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public VolumeToken getAttachmentTokenIfValid(final String encryptedTokenValue) throws EucalyptusCloudException {				
		try {
			String decryptedToken = decryptToken(encryptedTokenValue);
			VolumeToken tok = getCurrentValidToken();
			if(tok == null) {
				LOG.warn("Got request for attachment token, no valid token found for volume  " + this.getVolumeId());
				throw new EucalyptusCloudException("No valid token found");
			} else {
				if(!decryptedToken.equals(tok.getToken())){
					LOG.error("Token requested is not valid token for volume " + this.getVolumeId() + " request token: " + redactToken(decryptedToken) + " current token: " + redactToken(tok.getToken()));
					throw new EucalyptusCloudException("Requested token is not the current valid token");
				} else {
					return tok;
				}
			}
		} catch(Exception e) {
			LOG.error("Error checking for valid attachment token",e);
			throw new EucalyptusCloudException(e);
		}
	}
	
	/**
	 * Return the valid token if it exists or null if not.
	 * @return
	 */
	public VolumeToken getCurrentValidToken() throws EucalyptusCloudException {		
		Function<VolumeInfo, VolumeToken> getValidToken = new Function<VolumeInfo, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeInfo src) {				
				try {
					VolumeInfo volEntity = Entities.merge(src);
					for(VolumeToken tok : volEntity.getAttachmentTokens()) {
						if(tok.getIsValid()) {
							return tok;
						}
					}				
				} catch(Exception e) {
					LOG.trace("Failed while looking up valid token found for volume " + src.getVolumeId(), e);
				}
				return null;
			}
		};
		return Entities.asTransaction(VolumeInfo.class, getValidToken).apply(this);		
	}
	
	/**
	 * Get an attachment token, if valid one exists return it, otherwise create a new one.
	 * This should only be used in very specific cases. Normally, the semantics are to either
	 * explicitly create a new token or check an existing one.
	 * @param ip
	 * @param iqn
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public synchronized VolumeToken getOrCreateAttachmentToken() throws EucalyptusCloudException {
		VolumeToken token = null;
		Function<VolumeInfo, VolumeToken> checkAddToken = new Function<VolumeInfo, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeInfo src) {
				src = Entities.merge(src);
				try {					
					if(src.getCurrentValidToken() != null) {
						return src.getCurrentValidToken();
					} else {						
						VolumeToken tok = VolumeToken.generate(src);
						Entities.flush(tok);
						src.getAttachmentTokens().add(tok);
						return tok;
					}					
				} catch(Exception e) {
					LOG.error("Error creating new volume token for " + volumeId);
				}
				return null;				
			}
		};		
		token = Entities.asTransaction(VolumeInfo.class, checkAddToken).apply(this);
		return token;
	}

	/**
	 * Invalidates the export if it exists on the current valid token and invalidates the
	 * token if there are no remaining exports on the token.
	 * 
	 * Throws an exception if no valid token exists, if the export is already invalid, or
	 * invalidation fails.
	 * 
	 * @param tokenValue - the token string to use to lookup an active token
	 * @param nodeIp - the node IP to use for lookup of export record
	 * @param nodeIqn - the iqn for the lookup of export record
	 */
	public void invalidateExport(final String tokenValue, final String nodeIp, final String nodeIqn) throws EucalyptusCloudException {
		Function<VolumeInfo, VolumeToken> invalidateToken = new Function<VolumeInfo, VolumeToken>() {
			@Override
			public VolumeToken apply(VolumeInfo src) {
				try {
					VolumeInfo volEntity = Entities.merge(src);
					VolumeToken token = volEntity.getAttachmentTokenIfValid(tokenValue);
					token.invalidateExport(nodeIp, nodeIqn);
					Entities.flush(token);
					return token;
				} catch(Exception e) {
					LOG.error("Could not invalidate requested export with token " + tokenValue + " to " + nodeIp + " with iqn " + nodeIqn);
				}
				return null;
			}
		};
		
		if(Entities.asTransaction(VolumeInfo.class, invalidateToken).apply(this) == null) {
			throw new EucalyptusCloudException("Failed on invalidation of token");
		}
	}
}
