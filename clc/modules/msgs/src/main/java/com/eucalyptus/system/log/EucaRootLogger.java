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

package com.eucalyptus.system.log;

import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;

public final class EucaRootLogger extends EucaLogger {

	// same code as RootLogger in log4j-17
	public EucaRootLogger(Level level) {
		super("root");
		setLevel(level);
	}

	public final Level getChainedLevel() {
		return level;
	}

	public final void setLevel(Level level) {
		if (level == null) {
			LogLog.error(
					"You have tried to set a null level to root.", new Throwable());
		} else {
			this.level = level;
		}
	}
}
