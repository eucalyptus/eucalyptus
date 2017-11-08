/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth;

/**
 * Exception for auth operations.
 */
public class AuthException extends Exception {

  private static final long serialVersionUID = 1L;
  
  // Auth error types
  public static final String EMPTY_USER_NAME = "Empty user name";
  public static final String EMPTY_USER_ID = "Empty user ID";
  public static final String EMPTY_CANONICAL_ID = "Empty canonical ID";
  public static final String EMPTY_GROUP_NAME = "Empty group name";
  public static final String EMPTY_GROUP_ID = "Empty group ID";
  public static final String EMPTY_ROLE_NAME = "Empty role name";
  public static final String EMPTY_INSTANCE_PROFILE_NAME = "Empty instance profile name";
  public static final String EMPTY_ROLE_ID = "Empty role ID";
  public static final String EMPTY_ACCOUNT_NAME = "Empty account name";
  public static final String EMPTY_ACCOUNT_ID = "Empty account ID";
  public static final String EMPTY_OPENID_PROVIDER_ARN = "Empty openId connect provider ARN";
  public static final String EMPTY_OPENID_PROVIDER_URL = "Empty openId connect provider URL";
  public static final String USER_DELETE_CONFLICT = "User has resources attached and can not be deleted";
  public static final String GROUP_DELETE_CONFLICT = "Group has resources attached and can not be deleted";
  public static final String POLICY_DELETE_CONFLICT = "Policy has resources attached and can not be deleted";
  public static final String ROLE_DELETE_CONFLICT = "Role has resources attached and can not be deleted";
  public static final String ACCOUNT_DELETE_CONFLICT = "Account still has groups and can not be deleted";
  public static final String DELETE_ACCOUNT_ADMIN = "Can not delete account admin";
  public static final String DELETE_SYSTEM_ADMIN = "Can not delete system admin account";
  public static final String USER_CREATE_FAILURE = "Can not create user";
  public static final String USER_DELETE_FAILURE = "Can not delete user";
  public static final String GROUP_CREATE_FAILURE = "Can not create group";
  public static final String GROUP_DELETE_FAILURE = "Can not delete group";
  public static final String ROLE_CREATE_FAILURE = "Can not create role";
  public static final String INSTANCE_PROFILE_CREATE_FAILURE = "Can not create instance profile";
  public static final String POLICY_CREATE_FAILURE = "Can not create policy";
  public static final String ROLE_DELETE_FAILURE = "Can not delete role";
  public static final String ACCOUNT_CREATE_FAILURE = "Can not create account";
  public static final String ACCOUNT_DELETE_FAILURE = "Can not delete account";
  public static final String USER_ALREADY_EXISTS = "User already exists";
  public static final String GROUP_ALREADY_EXISTS = "Group already exists";
  public static final String ROLE_ALREADY_EXISTS = "Role already exists";
  public static final String INSTANCE_PROFILE_ALREADY_EXISTS = "Instance profile already exists";
  public static final String ACCOUNT_ALREADY_EXISTS = "Account already exists";
  public static final String POLICY_ALREADY_EXISTS = "Policy already exists";
  public static final String NO_SUCH_USER = "No such user";
  public static final String NO_SUCH_GROUP = "No such group";
  public static final String NO_SUCH_POLICY = "No such policy";
  public static final String NO_SUCH_ROLE = "No such role";
  public static final String NO_SUCH_INSTANCE_PROFILE = "No such instance profile";
  public static final String NO_SUCH_ACCOUNT = "No such account";
  public static final String NO_SUCH_LOGIN_PROFILE = "No such login profile";
  public static final String USER_GROUP_DELETE = "Can not delete user group";
  public static final String NO_SUCH_CERTIFICATE = "No such certificate";
  public static final String NO_SUCH_KEY = "No such access key";
  public static final String NO_SUCH_OPENID_CONNECT_PROVIDER = "No such openId connect provider";
  public static final String DELETE_SYSTEM_ACCOUNT = "Can not delete system account";
  public static final String ACCESS_DENIED = "Access to the resource is denied";
  public static final String QUOTA_EXCEEDED = "Resource quota is exceeded";
  public static final String SYSTEM_MODIFICATION = "It is not possible to modify the SYSTEM user or account.";
  public static final String INVALID_ID = "Invalid id";
  public static final String INVALID_NAME = "Invalid name";
  public static final String INVALID_PATH = "Invalid path";
  public static final String INVALID_CERT = "Invalid cert";
  public static final String INVALID_DESCRIPTION = "Invalid description";
  public static final String INVALID_PASSWORD = "Invalid password";
  public static final String CONFLICT = "Conflict";
  public static final String EMPTY_POLICY_NAME = "Empty policy name";
  public static final String EMPTY_CERT = "Empty certificate";
  public static final String EMPTY_KEY_ID = "Empty access key id";
  public static final String EMPTY_STATUS = "Empty status";
  public static final String EMPTY_CERT_ID = "Empty certificate id";
  public static final String INVALID_SERVER_CERT_NAME = "Server certificate name is invalid";
  public static final String INVALID_SERVER_CERT_PATH = "Server certificate path is invalid";
  public static final String SERVER_CERT_ALREADY_EXISTS = "Server certificate with the same name already exists";
  public static final String SERVER_CERT_NO_SUCH_ENTITY = "No server certificate with the requested name exist";
  public static final String SERVER_CERT_DELETE_CONFLICT = "Server certificate is attached to subordinate entities";
  public static final String SERVER_CERT_INVALID_FORMAT = "Server certificate/private-key is malformed";
  public static final String INVALID_SIGNATURE ="Signature cannot be verified";
  public static final String INVALID_OPENID_PROVIDER_URL = "OpenID connect provider url is invalid";
  public static final String INVALID_OPENID_PROVIDER_CLIENT_ID = "OpenID connect provider client id is invalid";
  public static final String INVALID_OPENID_PROVIDER_THUMBPRINT = "OpenID connect provider thumbprint is invalid";
  public static final String OPENID_PROVIDER_ALREADY_EXISTS = "OpenID connect provider with the same url already exists";
  public static final String OPENID_PROVIDER_CREATE_FAILURE = "Can not create openid connect provider";
  public AuthException( ) {
    super( );
  }
  
  public AuthException( String message, Throwable cause ) {
    super( message, cause );
  }
  
  public AuthException( String message ) {
    super( message );
  }
  
  public AuthException( Throwable cause ) {
    super( cause );
  }
  
}
