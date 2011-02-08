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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth.principal;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.principal.credential.HmacPrincipal;
import com.eucalyptus.auth.principal.credential.X509Principal;

/**
 * @author decker
 *
 */
public abstract interface User extends BasePrincipal, X509Principal, HmacPrincipal {
  public abstract Boolean isSystem( );
  public abstract Boolean isAdministrator( );
  public abstract void setAdministrator( Boolean admin );
  public abstract Boolean isEnabled( );
  public abstract void setEnabled( Boolean enabled );
  public abstract String getToken( );
  public abstract boolean checkToken( String testToken );
  public abstract User getDelegate( );
  public abstract String getPassword( );
  public abstract void setPassword( String password );
  public static final User SYSTEM = new User() { //NOTE:GRZE: this is transitional.  needed for internal communication.
    @Override public String getName( ) {
      return "eucalyptus";
    }
    @Override public X509Certificate getX509Certificate( ) { return null; }
    @Override public Boolean isSystem( ) { return true; }
    @Override public Boolean isAdministrator( ) { return true; }
    @Override public Boolean isEnabled( ) { return true; }
    @Override public List<X509Certificate> getAllX509Certificates( ) { return null; }
    @Override public void setX509Certificate( X509Certificate cert ) {}    
    @Override public void revokeX509Certificate( ) {}    
    @Override public BigInteger getNumber( ) { return BigInteger.ZERO; }
    @Override public void revokeSecretKey( ) {}
    @Override public String getQueryId( ) { return null; }
    @Override public String getSecretKey( ) { return null; }
    @Override public void setQueryId( String queryId ) {}
    @Override public void setSecretKey( String secretKey ) {}
    @Override public void setAdministrator( Boolean admin ) {}
    @Override public void setEnabled( Boolean enabled ) {}
    @Override public String getToken( ) { return null; }
    @Override public boolean checkToken( String testToken ) { return true; }
    @Override public User getDelegate( ) { return null; }
    @Override public String getPassword( ) { return null; }
    @Override public void setPassword( String password ) {}
  };
}
