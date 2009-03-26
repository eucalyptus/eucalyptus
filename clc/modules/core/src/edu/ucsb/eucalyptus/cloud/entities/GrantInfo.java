/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

@Entity
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
    @Column(name="entity_name")
    private String entityName;
	@Column(name="read")
	private Boolean read;
	@Column(name="write")
	private Boolean write;
	@Column(name="read_acp")
	private Boolean readACP;
	@Column(name="write_acp")
	private Boolean writeACP;

    private static Logger LOG = Logger.getLogger( ObjectInfo.class );

    public GrantInfo(){
        read = write = readACP = writeACP = false;
    }

	public Long getId()
	{
		return this.id;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(Boolean read) {
		this.read = read;
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

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public boolean isWrite() {
		return write;
	}

	public void setWrite(Boolean write) {
		this.write = write;
	}

	public boolean isReadACP() {
		return readACP;
	}

	public void setReadACP(Boolean readACP) {
		this.readACP = readACP;
	}

	public boolean isWriteACP() {
		return writeACP;
	}

	public void setWriteACP(Boolean writeACP) {
		this.writeACP = writeACP;
	}

	public void setFullControl() {
		read = write = readACP = writeACP = true;
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
                        displayName = UserManagement.getUserName(id);
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
					grantInfo.setRead(true);
				}   else if (permission.equals("WRITE")) {
					grantInfo.setWrite(true);
				}   else if (permission.equals("READ_ACP")) {
					grantInfo.setReadACP(true);
				}   else if (permission.equals("WRITE_ACP")) {
					grantInfo.setWriteACP(true);
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
}
