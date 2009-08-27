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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.state;

import javax.persistence.*;
import java.util.*;

@MappedSuperclass
public abstract class AbstractIsomorph {
  private String userName;
  private String uuid;
  private String displayName;
  private Date birthday;
  @Enumerated(EnumType.STRING)
  private State state;

  public AbstractIsomorph( ) {}

  public AbstractIsomorph( String userName,String displayName ) {
    this.userName = userName;
    this.uuid = UUID.randomUUID().toString();
    this.birthday = new Date();
    this.displayName = displayName;
    this.state = State.NIHIL;
  }

  public abstract Object morph( Object o );

  public String getUserName() {
    return userName;
  }

  public void setUserName( final String userName ) {
    this.userName = userName;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid( final String uuid ) {
    this.uuid = uuid;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName( final String displayName ) {
    this.displayName = displayName;
  }

  public Date getBirthday() {
    return birthday;
  }

  public void setBirthday( final Date birthday ) {
    this.birthday = birthday;
  }

  public State getState() {
    return state;
  }

  public void setState( final State state ) {
    this.state = state;
  }

  public abstract String mapState( );
  public abstract void setMappedState( String state );
}
