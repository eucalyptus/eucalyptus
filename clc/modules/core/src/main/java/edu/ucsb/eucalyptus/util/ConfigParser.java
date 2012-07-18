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

package edu.ucsb.eucalyptus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ConfigParser extends Thread {
	private InputStream is;
	private File file;
	private Map<String, String> values;

	public ConfigParser(InputStream is) {
		this.is = is;
		values = new HashMap<String, String>();
	}

	public Map<String, String> getValues() {
		return values;
	}

	public void run() {
		try {			
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
            while((line = reader.readLine()) !=null) {
                if(!line.startsWith("#")) {
                    String[] parts = line.split("=");
                    if(parts.length > 1) {
                        values.put(parts[0], parts[1].replaceAll('\"' + "", ""));
                    }
                }
            }
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
