/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.msgs.AccessControlListType;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.msgs.Grantee;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import edu.ucsb.eucalyptus.util.UserManagement;

import javax.persistence.*;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.Users;

@Entity
@PersistenceContext(name="eucalyptus_walrus")
@Table( name = "Grants" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class GrantInfo {
	@Id
	@GeneratedValue
	@Column( name = "grant_id" )
	private Long id = -1l;
	@Column(name="user_id")
	private String userId;
	@Column(name="grantGroup")
	private String grantGroup;
	@Column(name="allow_read")
	private Boolean canRead;
	@Column(name="allow_write")
	private Boolean canWrite;
	@Column(name="allow_read_acp")
	private Boolean canReadACP;
	@Column(name="allow_write_acp")
	private Boolean canWriteACP;

	private static Logger LOG = Logger.getLogger( ObjectInfo.class );

	public GrantInfo(){
		canRead = canWrite = canReadACP = canWriteACP = false;
	}

	public Long getId()
	{
		return this.id;
	}

	public boolean canRead() {
		return canRead;
	}

	public void setCanRead(Boolean canRead) {
		this.canRead = canRead;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getGrantGroup() {
		return grantGroup;
	}

	public void setGrantGroup(String grantGroup) {
		this.grantGroup = grantGroup;
	}

	public boolean canWrite() {
		return canWrite;
	}

	public void setCanWrite(Boolean canWrite) {
		this.canWrite = canWrite;
	}

	public boolean canReadACP() {
		return canReadACP;
	}

	public void setCanReadACP(Boolean canReadACP) {
		this.canReadACP = canReadACP;
	}

	public boolean isWriteACP() {
		return canWriteACP;
	}

	public void setCanWriteACP(Boolean writeACP) {
		this.canWriteACP = writeACP;
	}

	public void setFullControl() {
		canRead = canWrite = canReadACP = canWriteACP = true;
	}

	public static void addGrants(String ownerId, List<GrantInfo>grantInfos, AccessControlListType accessControlList) {
		ArrayList<Grant> grants = accessControlList.getGrants();
		if (grants.size() > 0) {
			for (Grant grant: grants) {
				String permission = grant.getPermission();
				if(permission.equals("private")) {
					setFullControl(ownerId, grantInfos);
					continue;
				}
				GrantInfo grantInfo = new GrantInfo();
				Grantee grantee = grant.getGrantee();
				if(grantee.getCanonicalUser() != null) {
					String displayName = grantee.getCanonicalUser().getDisplayName();
					if(displayName == null || displayName.length() == 0) {
						String id = grantee.getCanonicalUser().getID();
						if(id == null || id.length() == 0)
							continue;
						try {
							displayName = Users.lookupQueryId(id).getName();
						} catch ( NoSuchUserException e ) {
              LOG.warn(e,e);
            }
						if(displayName == null)
							continue;
					}
					grantInfo.setUserId(displayName);
				} else {
					grantInfo.setGrantGroup(grantee.getGroup().getUri());
				}
				if (permission.equals("FULL_CONTROL")) {
					grantInfo.setFullControl();
				}   else if (permission.equals("READ")) {
					grantInfo.setCanRead(true);
				}   else if (permission.equals("WRITE")) {
					grantInfo.setCanWrite(true);
				}   else if (permission.equals("READ_ACP")) {
					grantInfo.setCanReadACP(true);
				}   else if (permission.equals("WRITE_ACP")) {
					grantInfo.setCanWriteACP(true);
				}
				grantInfos.add(grantInfo);
			}
		} else {
			setFullControl(ownerId, grantInfos);
		}
	}

	public static void setFullControl(String userId, List<GrantInfo> grantInfos) {
		GrantInfo grantInfo = new GrantInfo();
		grantInfo.setUserId(userId);
		grantInfo.setFullControl();
		grantInfos.add(grantInfo);
	}

	public void setPermission(String permission) {
		if("FULL_CONTROL".equals(permission)) {
			setFullControl();
		} else if("READ".equals(permission)) {
			setCanRead(true);
		} else if("WRITE".equals(permission)) {
			setCanWrite(true);
		} else if("READ_ACP".equals(permission)) {
			setCanReadACP(true);
		} else if("WRITE_ACP".equals(permission)) {
			setCanWriteACP(true);
		}
	}
}
