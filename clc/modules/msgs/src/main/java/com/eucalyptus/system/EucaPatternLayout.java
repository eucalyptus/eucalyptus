package com.eucalyptus.system;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

public class EucaPatternLayout extends PatternLayout {

	public EucaPatternLayout() {
	}

	public EucaPatternLayout(String pattern) {
		super(pattern);
	}

	@Override
	protected PatternParser createPatternParser(String pattern) {
		return new EucaPatternParser(pattern);
	}

}
