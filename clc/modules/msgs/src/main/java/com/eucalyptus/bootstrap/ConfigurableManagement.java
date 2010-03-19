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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: Neil Soman <neil@eucalyptus.com>
 */
package com.eucalyptus.bootstrap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class ConfigurableManagement {
	private static Logger LOG = Logger.getLogger( ConfigurableManagement.class );
	private ConcurrentHashMap<String, List<FieldType>> configurableKlasses;

	private static ConfigurableManagement singleton = ConfigurableManagement.getInstance( );

	private ConfigurableManagement() {
		configurableKlasses = new ConcurrentHashMap<String, List<FieldType>>();
	}

	public static ConfigurableManagement getInstance( ) {
		synchronized ( ConfigurableManagement.class ) {
			if ( singleton == null ) singleton = new ConfigurableManagement( );
		}
		return singleton;
	}

	public void add(Class klass) {
		List<FieldType> fields = new ArrayList<FieldType>();
		for(Field field : klass.getFields()) {
			if(field.isAnnotationPresent(ConfigurableField.class)) {
				ConfigurableField configurableField = field.getAnnotation(ConfigurableField.class);
				fields.add(new FieldType(configurableField.displayName(), field.getName()));
			}
		}
		configurableKlasses.put(klass.getName(), fields);
	}

	public ArrayList<ComponentProperty> getProperties(Class klass) {
		ArrayList<ComponentProperty> properties = new ArrayList<ComponentProperty>();
		List<FieldType> fields = configurableKlasses.get(klass.getName());
		if(fields != null) {			
			for (FieldType field : fields) {
				try {
					Field f = klass.getField(field.getKey());
					ConfigurableField configurableField = f.getAnnotation(ConfigurableField.class);
					try {
						String value = f.get(klass).toString();						
						if(String.class.equals(f.getType()) && configurableField.type().equals(ConfigurableFieldType.KEYVALUE)) {
							properties.add(new ComponentProperty("KEYVALUE", field.getDisplayName(), value));
						} else if(Boolean.class.equals(f.getType())) {
							properties.add(new ComponentProperty("BOOLEAN", field.getDisplayName(), value));
						} else if(String.class.equals(f.getType()) && configurableField.type().equals(ConfigurableFieldType.KEYVALUEHIDDEN)) {
							properties.add(new ComponentProperty("KEYVALUEHIDDEN", field.getDisplayName(), value));
						}
					} catch (IllegalArgumentException e) {
						LOG.error(e);
					} catch (IllegalAccessException e) {
						LOG.error(e);
					}					
				} catch (SecurityException e) {
					LOG.error(e);
				} catch (NoSuchFieldException e) {
					LOG.error(e);
				}				
			}
		}
		return properties;
	}

	public void setProperties(Class klass,
			ArrayList<ComponentProperty> storageParams) {
		List<FieldType> fields = configurableKlasses.get(klass.getName());
		if(fields != null) {
			for(ComponentProperty property : storageParams) {
				for(FieldType field : fields) {
					if(field.getDisplayName().equals(property.getKey())) {
						try {
							Field f = klass.getField(field.getKey());
							try {
								if("KEYVALUE".equals(property.getType()))
									f.set(klass, property.getValue());
								else if("BOOLEAN".equals(property.getType()))
									f.set(klass, Boolean.parseBoolean(property.getValue()));
								else if("KEYVALUEHIDDEN".equals(property.getType()))
									f.set(klass, property.getValue());
							} catch (IllegalArgumentException e) {
								LOG.error(e);
							} catch (IllegalAccessException e) {
								LOG.error(e);
							}
						} catch (SecurityException e) {
							LOG.error(e);
						} catch (NoSuchFieldException e) {
							LOG.error(e);
						}						
					}
				}
			}
		}
	}

	private class FieldType {
		private String displayName;
		private String key;

		public FieldType(String displayName, String key) {
			this.displayName = displayName;
			this.key = key;
		}

		public String getDisplayName() {
			return displayName;
		}
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}				
	}
}