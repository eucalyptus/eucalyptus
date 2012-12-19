/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.hajdbc.ColumnProperties;
import net.sf.hajdbc.util.Strings;

/**
 * Dialect for <a href="http://postgresql.org">PostgreSQL</a>.
 * @author  Paul Ferraro
 * @since   1.1
 */
@SuppressWarnings("nls")
public class PostgreSQLDialect extends StandardDialect
{
	/**
	 * PostgreSQL uses a schema search path to locate unqualified table names.
	 * The default search path is [$user,public], where $user is the current user.
	 * @see net.sf.hajdbc.dialect.StandardDialect#getDefaultSchemas(java.sql.DatabaseMetaData)
	 */
	@Override
	public List<String> getDefaultSchemas(DatabaseMetaData metaData) throws SQLException
	{
		Connection connection = metaData.getConnection();
		Statement statement = connection.createStatement();
		
		ResultSet resultSet = statement.executeQuery("SHOW search_path");
		
		resultSet.next();
		
		String[] schemas = resultSet.getString(1).split(Strings.COMMA);
		
		resultSet.close();
		statement.close();
		
		List<String> schemaList = new ArrayList<String>(schemas.length);
		
		for (String schema: schemas)
		{
			schemaList.add(schema.equals("$user") ? metaData.getUserName() : schema);
		}
		
		return schemaList;
	}

	/**
	 * PostgreSQL uses the native type OID to identify BLOBs.
	 * However the JDBC driver incomprehensibly maps OIDs to INTEGERs.
	 * The PostgreSQL JDBC folks claim this intentional.
	 * @see net.sf.hajdbc.dialect.StandardDialect#getColumnType(net.sf.hajdbc.ColumnProperties)
	 */
	@Override
	public int getColumnType(ColumnProperties properties)
	{
		return properties.getNativeType().equalsIgnoreCase("oid") ? Types.BLOB : properties.getType();
	}

	/**
	 * Versions &gt;=8.1 of the PostgreSQL JDBC driver return incorrect values for DatabaseMetaData.getExtraNameCharacters().
	 * @see net.sf.hajdbc.dialect.StandardDialect#getIdentifierPattern(java.sql.DatabaseMetaData)
	 */
	@Override
	public Pattern getIdentifierPattern(DatabaseMetaData metaData) throws SQLException
	{
		if ((metaData.getDriverMajorVersion() >= 8) && (metaData.getDriverMinorVersion() >= 1))
		{
			return Pattern.compile("[A-Za-z\\0200-\\0377_][A-Za-z\\0200-\\0377_0-9\\$]*");
		}
		
		return super.getIdentifierPattern(metaData);
	}

	/**
	 * @see net.sf.hajdbc.dialect.StandardDialect#sequencePattern()
	 */
	@Override
	protected String sequencePattern()
	{
		return "(?:CURR|NEXT)VAL\\s*\\(\\s*'([^']+)'\\s*\\)";
	}

	/**
	 * @see net.sf.hajdbc.dialect.StandardDialect#nextSequenceValueFormat()
	 */
	@Override
	protected String nextSequenceValueFormat()
	{
		return "NEXTVAL(''{0}'')";
	}

	/**
	 * @see net.sf.hajdbc.dialect.StandardDialect#alterIdentityColumnFormat()
	 */
	@Override
	protected String alterIdentityColumnFormat()
	{
		return "ALTER SEQUENCE {0}_{1}_seq RESTART WITH {2}";
	}

	/**
	 * @see net.sf.hajdbc.dialect.StandardDialect#currentTimestampPattern()
	 */
	@Override
	protected String currentTimestampPattern()
	{
		return super.currentTimestampPattern() + "|(?<=\\W)NOW\\s*\\(\\s*\\)|(?<=\\W)TRANSACTION_TIMESTAMP\\s*\\(\\s*\\)|(?<=\\W)STATEMENT_TIMESTAMP\\s*\\(\\s*\\)|(?<=\\W)CLOCK_TIMESTAMP\\s*\\(\\s*\\)";
	}

	/**
	 * @see net.sf.hajdbc.dialect.StandardDialect#randomPattern()
	 */
	@Override
	protected String randomPattern()
	{
		return "(?<=\\W)RANDOM\\s*\\(\\s*\\)";
	}

	/**
	 * Recognizes FOR SHARE and FOR UPDATE.
	 * @see net.sf.hajdbc.dialect.StandardDialect#selectForUpdatePattern()
	 */
	@Override
	protected String selectForUpdatePattern()
	{
		return "SELECT\\s+.+\\s+FOR\\s+(SHARE|UPDATE)";
	}
}
