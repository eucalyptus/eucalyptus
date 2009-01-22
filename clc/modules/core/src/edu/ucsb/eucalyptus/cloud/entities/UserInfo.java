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
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;
import java.util.zip.Adler32;

@Entity
@Table( name = "Users" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class UserInfo {

	@Id
	@GeneratedValue
	@Column( name = "user_id" )
	private Long id = -1l;
	@Column( name = "user_name" )
	private String userName;
	@Column( name = "user_query_id" )
	private String queryId;
	@Column( name = "user_email" )
	private String email;
	@Column( name = "user_secretkey" )
	private String secretKey;
	@OneToMany( cascade = CascadeType.ALL )
	@JoinTable(
			name = "user_has_certificates",
			joinColumns = { @JoinColumn( name = "user_id" ) },
			inverseJoinColumns = @JoinColumn( name = "cert_info_id" )
	)
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	private List<CertificateInfo> certificates = new ArrayList<CertificateInfo>();
  @OneToMany( cascade = CascadeType.ALL )
  @JoinTable(
      name = "user_has_sshkeys",
      joinColumns = { @JoinColumn( name = "user_id" ) },
      inverseJoinColumns = @JoinColumn( name = "ssh_keypair_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<SSHKeyPair> keyPairs = new ArrayList<SSHKeyPair>();
  @OneToMany( cascade = CascadeType.ALL )
  @JoinTable(
      name = "user_has_volumes",
      joinColumns = { @JoinColumn( name = "user_id" ) },
      inverseJoinColumns = @JoinColumn( name = "volume_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<VolumeInfo> volumes = new ArrayList<VolumeInfo>();
  @OneToMany( cascade = CascadeType.ALL )
  @JoinTable(
      name = "user_has_volumes",
      joinColumns = { @JoinColumn( name = "user_id" ) },
      inverseJoinColumns = @JoinColumn( name = "volume_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<SnapshotInfo> snapshots = new ArrayList<SnapshotInfo>();
	@OneToMany( cascade = CascadeType.ALL )
	@JoinTable(
			name = "user_has_network_groups",
			joinColumns = { @JoinColumn( name = "user_id" ) },
			inverseJoinColumns = @JoinColumn( name = "network_group_id" )
	)
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	private List<NetworkRulesGroup> networkRulesGroup = new ArrayList<NetworkRulesGroup>();
	@Column( name = "user_real_name" )
	private String realName;
	@Column( name = "user_reservation_id" )
	private Long reservationId;
	@Column( name = "user_b_crypted_password" )
	private String bCryptedPassword;
	@Column( name = "user_telephone_number" )
	private String telephoneNumber;
	@Column( name = "user_affiliation" )
	private String affiliation;
	@Column( name = "user_project_description" )
	private String projectDescription;
	@Column( name = "user_project_pi_name" )
	private String projectPIName;
	@Column( name = "user_confirmation_code" )
	private String confirmationCode;
	@Column( name = "user_certificate_code" )
	private String certificateCode;
	@Column( name = "user_is_approved" )
	private Boolean isApproved;
	@Column( name = "user_is_confirmed" )
	private Boolean isConfirmed;
	@Column( name = "user_is_enabled" )
	private Boolean isEnabled;
	@Column( name = "user_is_admin" )
	private Boolean isAdministrator;
	@Column( name = "password_expires" )
	private Long passwordExpires;
	@Column( name = "user_temporary_password")
	private String temporaryPassword;

	public UserInfo() {}

	public UserInfo( String userName )
	{
		this.userName = userName;
	}

  public List<VolumeInfo> getVolumes() {
    return volumes;
  }

  public void setVolumes( final List<VolumeInfo> volumes ) {
    this.volumes = volumes;
  }

  public List<SnapshotInfo> getSnapshots() {
    return snapshots;
  }

  public void setSnapshots( final List<SnapshotInfo> snapshots ) {
    this.snapshots = snapshots;
  }

  public String getQueryId()
	{
		return queryId;
	}

	public void setQueryId( final String queryId )
	{
		this.queryId = queryId;
	}

	public Long getId()
	{
		return id;
	}

	public String getSecretKey()
	{
		return secretKey;
	}

	public void setUserName( String userName )
	{
		this.userName = userName;
	}

	public void setSecretKey( String secretKey )
	{
		this.secretKey = secretKey;
	}

	public String getUserName()
	{
		return userName;
	}

	public List<SSHKeyPair> getKeyPairs()
	{
		return keyPairs;
	}

	public void setKeyPairs( List<SSHKeyPair> keyPairs )
	{
		this.keyPairs = keyPairs;
	}

	public List<CertificateInfo> getCertificates()
	{
		return certificates;
	}

	public void setCertificates( List<CertificateInfo> certificates )
	{
		this.certificates = certificates;
	}

	public String getAffiliation()
	{
		return affiliation;
	}

	public void setAffiliation( String affiliation )
	{
		this.affiliation = affiliation;
	}

	public String getBCryptedPassword()
	{
		return bCryptedPassword;
	}

	public void setBCryptedPassword( String bCryptedPassword )
	{
		this.bCryptedPassword = bCryptedPassword;
	}

	public String getConfirmationCode()
	{
		return confirmationCode;
	}

	public void setConfirmationCode( String confirmationCode )
	{
		this.confirmationCode = confirmationCode;
	}

	public String getCertificateCode()
	{
		return certificateCode;
	}

	public void setCertificateCode( String certificateCode )
	{
		this.certificateCode = certificateCode;
	}

	public Boolean isAdministrator()
	{
		return isAdministrator;
	}

	public void setIsAdministrator( Boolean administrator )
	{
		isAdministrator = administrator;
	}

	public Long getPasswordExpires()
	{
		return passwordExpires;
	}

	public void setPasswordExpires( Long passwordExpires )
	{
		this.passwordExpires = passwordExpires;
	}

	public String getTemporaryPassword ()
	{
		return this.temporaryPassword;
	}

	public void setTemporaryPassword ( String password )
	{
		this.temporaryPassword = password;
	}

	public Boolean isApproved()
	{
		return isApproved;
	}

	public void setIsApproved( Boolean approved )
	{
		isApproved = approved;
	}

	public Boolean isConfirmed()
	{
		return isConfirmed;
	}

	public void setIsConfirmed( Boolean confirmed )
	{
		isConfirmed = confirmed;
	}

	public Boolean isEnabled()
	{
		return isEnabled;
	}

	public void setIsEnabled( Boolean enabled )
	{
		isEnabled = enabled;
	}

	public String getProjectDescription()
	{
		return projectDescription;
	}

	public void setProjectDescription( String projectDescription )
	{
		this.projectDescription = projectDescription;
	}

	public String getProjectPIName()
	{
		return projectPIName;
	}

	public void setProjectPIName( String projectPIName )
	{
		this.projectPIName = projectPIName;
	}

	public String getRealName()
	{
		return realName;
	}

	public void setRealName( String realName )
	{
		this.realName = realName;
	}

	public String getTelephoneNumber()
	{
		return telephoneNumber;
	}

	public void setTelephoneNumber( String telephoneNumber )
	{
		this.telephoneNumber = telephoneNumber;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail( String email )
	{
		this.email = email;
	}

	public List<NetworkRulesGroup> getNetworkRulesGroup()
	{
		return networkRulesGroup;
	}

	public void setNetworkRulesGroup( final List<NetworkRulesGroup> networkRulesGroup )
	{
		this.networkRulesGroup = networkRulesGroup;
	}

	public Long getReservationId()
	{
		return reservationId++;
	}

	public void setReservationId( Long reservationId )
	{
		this.reservationId = reservationId;
	}

  public boolean equals( final Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		UserInfo userInfo = ( UserInfo ) o;

		if ( !userName.equals( userInfo.userName ) ) return false;

		return true;
	}

	public int hashCode()
	{
		return userName.hashCode();
	}

  public static UserInfo named( String userId ) throws EucalyptusCloudException {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique( new UserInfo( userId ) );
    } finally {
      db.commit();
    }
    return user;
  }

  public static String getUserNumber( final String userName ) {
    Adler32 hash = new Adler32();
    hash.reset();
    hash.update( userName.getBytes(  ) );
    String userNumber = String.format( "%012d", hash.getValue() );
    return userNumber;
  }
}

