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
