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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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

import com.eucalyptus.auth.UserCredentialProvider;

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
                        try {
                          displayName = UserCredentialProvider.getUserName( id );
                        } catch ( GeneralSecurityException e ) {
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
