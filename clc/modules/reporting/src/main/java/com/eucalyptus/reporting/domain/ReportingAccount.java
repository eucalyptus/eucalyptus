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
 ************************************************************************/
package com.eucalyptus.reporting.domain;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

/**
 * <p>ReportingAccount is a eucalyptus account stored in the reporting database. The reporting
 * subsystem keeps its own historical record of users and accounts, seperate from the CLC
 * database. This is because the reporting subsystem stores historical information. It will retain
 * user and account information after those users and accounts have been deleted, so you can
 * still generate reports of accounts and users that no longer exist.
 *
 * <p>NOTE: Accounts must be created using the <code>ReportingAccountCrud</code> class. Do not 
 * instantiate one of these or store it in the database directly.
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_account")
public class ReportingAccount implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id @Column(name="account_id", nullable=false)
	private String id;
	@Column(name="account_name", nullable=false)
	private String name;

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingAccountCrud class.
 	 */
	protected ReportingAccount()
	{
		/* Hibenrate will extend this class and overwrite these values. */
		this.id = null;
		this.name = null;
	}
	
	ReportingAccount(String id, String name)
	{
		this.id = id;
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
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		if (name == null) throw new IllegalArgumentException("id cant be null");
		this.name = name;
	}
	
}
