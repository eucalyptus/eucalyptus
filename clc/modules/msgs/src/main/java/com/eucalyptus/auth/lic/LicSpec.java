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

package com.eucalyptus.auth.lic;

/**
 * LDAP Integration Configuration (LIC) specification constants.
 */
public class LicSpec {

  public static final String LDAP_SERVICE = "ldap-service";
  public static final String SERVER_URL = "server-url";
  public static final String AUTH_METHOD = "auth-method";
  public static final String AUTH_PRINCIPAL = "auth-principal";
  public static final String AUTH_CREDENTIALS = "auth-credentials";
  public static final String USE_SSL = "use-ssl";
  public static final String IGNORE_SSL_CERT_VALIDATION = "ignore-ssl-cert-validation";
  public static final String ROOT_DN = "root-dn";
  public static final String GROUP_BASE_DN = "base-dn";
  public static final String USER_BASE_DN = "base-dn";
  public static final String ACCOUNTING_GROUPS = "accounting-groups";
  public static final String GROUPS_PARTITION = "groups-partition";
  public static final String SELECT = "select";
  public static final String NOT_SELECT = "not-select";
  public static final String GROUPS_ATTRIBUTE = "member-attribute";
  public static final String GROUPS = "groups";
  public static final String USERS_ATTRIBUTE = "member-attribute";
  public static final String USERS = "users";
  public static final String USER_INFO_ATTRIBUTES = "user-info-attributes";
  public static final String PASSWORD_ATTRIBUTE = "password-attribute";
  public static final String SYNC = "sync";
  public static final String ENABLE_SYNC = "enable";
  public static final String AUTO_SYNC = "auto";
  public static final String SYNC_INTERVAL = "interval";
  public static final String ID_ATTRIBUTE = "id-attribute";
  public static final String ACCOUNTING_GROUP_BASE_DN = "base-dn";
  public static final String SELECTION = "selection";
  public static final String FILTER = "filter";
  public static final String KRB5_CONF = "krb5-conf";
  public static final String USER_AUTH_METHOD = "user-auth-method";
  public static final String CLEAN_DELETION = "clean-deletion";
  public static final String SASL_ID_ATTRIBUTE = "sasl-id-attribute";

}
