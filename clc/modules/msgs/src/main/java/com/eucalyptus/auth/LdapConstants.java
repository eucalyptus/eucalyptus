package com.eucalyptus.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LDAP constants
 * 
 * @author wenye
 *
 */
public interface LdapConstants {
  public static final String DN = "dn";
  // objectClass
  public static final String OBJECT_CLASS = "objectClass";
  public static final String TOP = "top";
  public static final String DC_OBJECT = "dcObject";
  public static final String ORGANIZATION = "organization";
  public static final String PERSON = "person";
  public static final String ORGANIZATIONAL_PERSON  = "organizationalPerson";
  public static final String ORGANIZATIONAL_UNIT = "organizationalUnit";
  public static final String INET_ORG_PERSON = "inetOrgPerson";
  public static final String POSIX_GROUP = "posixGroup";
  // eucalyptus objectClass
  public static final String EUCA_USER = "eucaUser";
  public static final String EUCA_GROUP = "eucaGroup";
  // attributes
  public static final String DC = "dc"; // domain component
  public static final String O = "o"; // organizationName
  public static final String OU = "ou"; // organizationalUnitName
  public static final String CN = "cn"; // commonName
  public static final String SN = "sn"; // surname
  public static final String UID = "uid";
  public static final String GID_NUMBER = "gidNumber";
  public static final String MEMBER_UID = "memberUid";
  public static final String MAIL = "mail";
  public static final String ROLE_OCCUPANT = "roleOccupant";
  public static final String TELEPHONE_NUMBER = "telephoneNumber";
  public static final String DEPARTMENT_NUMBER = "departmentNumber";
  public static final String DESCRIPTION = "description";
  public static final String USER_PASSWORD = "userPassword";
  public static final String EMPLOYEE_TYPE = "employeeType";
  // eucalyptus attributes
  public static final String QUERY_ID = "queryId";
  public static final String SECRET_KEY = "secretKey";
  public static final String TOKEN = "token";
  public static final String IS_ADMINISTRATOR = "isAdministrator";
  public static final String ENABLED = "enabled";
  public static final String EUCA_CERTIFICATE = "eucaCertificate";
  public static final String EUCA_REVOKED_CERTIFICATE = "eucaRevokedCertificate";
  public static final String APPROVED = "approved";
  public static final String CONFIRMED = "confirmed";
  public static final String PASSWORD_EXPIRES = "passwordExpires";
  public static final String CONFIRMATION_CODE = "confirmationCode";
  public static final String PERMISSION = "permission";
  public static final String EUCA_GROUP_ID = "eucaGroupId";
  public static final String TEST_ATTR = "testAttr";
  
  // schema enforcement
  public static final String EUCA_USER_MUSTS[] = { UID, CN, SN };
  public static final String EUCA_GROUP_MUSTS[] = { CN, GID_NUMBER };
  
  // object classes
  public static final String EUCA_USER_OBJECT_CLASSES[] = { TOP, PERSON, ORGANIZATIONAL_PERSON, INET_ORG_PERSON, EUCA_USER };
  public static final String EUCA_GROUP_OBJECT_CLASSES[] = { TOP, POSIX_GROUP, EUCA_GROUP };

}
