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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */
package edu.ucsb.eucalyptus.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CaseInsensitiveMap {
	private ConcurrentHashMap<String, String> map;

	public CaseInsensitiveMap() {
		map = new ConcurrentHashMap<String, String> ();
	}

	public CaseInsensitiveMap(Map m) {
		map = new ConcurrentHashMap<String, String> ();
		Iterator iterator = m.keySet().iterator();
		while(iterator.hasNext()) {
			Object key = iterator.next();
			map.put(key.toString().toLowerCase(), (String) m.get(key));
		}
	}


	public String get(Object o) {
		return map.get(o.toString().toLowerCase());
	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean containsKey(Object o) {
		return map.containsKey(o.toString().toLowerCase());
	}

	public boolean containsValue(Object o) {
		return map.containsValue(o);
	}

	public String put(Object o, Object o1) {
		return map.put(o.toString().toLowerCase(), (String)o1);
	}

	public String remove(Object o) {
		return map.remove(o.toString().toLowerCase());
	}

	public void putAll(Map map) {
		map.putAll(map);
	}

	public void clear() {
		map.clear();
	}

	public Set keySet() {
		return map.keySet();
	}

	public Collection values() {
		return map.values();
	}

	public Set entrySet() {
		return map.entrySet();
	}

	public TreeMap removeSub(String subString) {
		TreeMap result = new TreeMap();
		Iterator iterator = map.keySet().iterator();
		while(iterator.hasNext()) {
			Object key = iterator.next();
			if(key.toString().startsWith(subString)) {
				result.put(key, map.get(key));
			}
		}
		return result;
	}
}
