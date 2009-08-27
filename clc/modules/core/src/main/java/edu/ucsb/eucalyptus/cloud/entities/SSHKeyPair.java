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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "ssh_keypair" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class SSHKeyPair {

  @Id
  @GeneratedValue
  @Column( name = "ssh_keypair_id" )
  private Long id = -1l;
  @Column( name = "ssh_keypair_name" )
  private String name;
  @Lob
  @Column( name = "ssh_keypair_public_key" )
  private String publicKey;
  @Column( name = "ssh_keypair_finger_print" )
  private String fingerPrint;
  @Transient
  public static String NO_KEY_NAME = "";
  @Transient
  public static SSHKeyPair NO_KEY = new SSHKeyPair( "", "", "" );

  public SSHKeyPair() {}

  public SSHKeyPair( final String name ) {
    this.name = name;
  }

  public SSHKeyPair( String name, String fingerPrint, String publicKey )
  {
    this.fingerPrint = fingerPrint;
    this.name = name;
    this.publicKey = publicKey;
  }

  public String getFingerPrint()
  {
    return fingerPrint;
  }

  public void setFingerPrint( String fingerPrint )
  {
    this.fingerPrint = fingerPrint;
  }

  public Long getId()
  {
    return id;
  }

  public void setId( Long id )
  {
    this.id = id;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getPublicKey()
  {
    return publicKey;
  }

  public void setPublicKey( String publicKey )
  {
    this.publicKey = publicKey;
  }

  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    SSHKeyPair that = ( SSHKeyPair ) o;

    if ( !name.equals( that.name ) ) return false;

    return true;
  }

  public int hashCode()
  {
    return name.hashCode();
  }
}
