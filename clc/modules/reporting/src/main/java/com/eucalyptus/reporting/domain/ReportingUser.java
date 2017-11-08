/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.reporting.domain;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * <p>ReportingUser is a eucalyptus user stored in the reporting database. The reporting
 * subsystem keeps its own historical record of users and accounts, seperate from the CLC
 * database. This is because the reporting subsystem stores historical information. It will retain
 * user and account information after those users and accounts have been deleted, so you can
 * still generate reports of accounts and users that no longer exist.
 *
 * <p>NOTE: Users must be created using the <code>ReportingUserCrud</code> class. Do not 
 * instantiate one of these or store it in the database directly.
 */
@Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_user")
public class ReportingUser implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id @Column(name="user_id", nullable=false)
	private String id;
	@Column(name="account_id", nullable=false, columnDefinition = "character varying(255) default '' not null")
	private String accountId;
	@Column(name="user_name", nullable=false)
	private String name;

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingUserCrud class.
 	 */
	protected ReportingUser()
	{
		/* Hibenrate will extend this class and overwrite these values. */
		this.id = null;
		this.accountId = null;
		this.name = null;
	}
	
	/**
 	 * <p>Do not instantiate this class directly; use the ReportingUserCrud class.
 	 */
	public ReportingUser(String id, String accountId, String name)
	{
		this.id = id;
		this.accountId = accountId;
		this.name = name;
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		if (id == null) throw new IllegalArgumentException("id cant be null");
		this.id = id;
	}
	
	public String getAccountId()
	{
		return accountId;
	}
	
	public void setAccountId(String accountId)
	{
		if (accountId == null) throw new IllegalArgumentException("accountId cant be null");
		this.accountId = accountId;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		if (name == null) throw new IllegalArgumentException("id cant be null");
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReportingUser other = (ReportingUser) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
}
