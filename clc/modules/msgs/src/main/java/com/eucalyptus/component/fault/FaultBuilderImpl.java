/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.component.fault;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Faults.FaultBuilder;

public class FaultBuilderImpl implements FaultBuilder {
	private static final Logger LOG = Logger.getLogger(FaultBuilder.class);
	private class NameValuePair {
		private String name;
		private String value;
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
		public NameValuePair(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
		
	}
	private FaultSubsystemManager faultSubsystemManager;
	private Class<? extends ComponentId> componentIdClass;
	private ArrayList<NameValuePair> vars = new ArrayList<NameValuePair>();
	private int faultId;

	public FaultBuilderImpl(FaultSubsystemManager faultSubsystemManager, Class<? extends ComponentId> componentIdClass) {
		this.faultSubsystemManager = faultSubsystemManager;
		this.componentIdClass = componentIdClass;
	}
	@Override
	public FaultBuilder withVar(String name, String value) {
		vars.add(new NameValuePair(name, value));
		return this;
	}

	@Override
	public FaultBuilder havingId( int faultId ) {
		this.faultId = faultId;
		return this;
	}

  /**
   * GRZE: with displeasure I force the return of the fault string for use elsewhere in the name of consistency and dryness.
   */
	@Override
	public String log() {
    String faultMessage = "";
		try {
			FaultLogger faultLogger = faultSubsystemManager.getFaultLogger(componentIdClass);
			Fault fault = faultSubsystemManager.getFaultRegistry().lookupFault(faultId);
			if (fault == FaultRegistry.SUPPRESSED_FAULT) {
				LOG.debug( faultMessage = "Fault " + faultId + " detected, will not be logged because it has been configured to be suppressed.");
			} else if (fault == null) {
				LOG.error( faultMessage = "Fault " + faultId + " detected, could not find fault id in registry.");
			} else {
				for (NameValuePair nameValuePair: vars) {
					fault = fault.withVar(nameValuePair.getName(), nameValuePair.getValue());
				}
				faultLogger.log(fault);
        faultMessage = fault.toString();
			}
		} catch (Exception ex) {
			LOG.error( faultMessage = "Error writing fault with id " + faultId + "  for component " + componentIdClass.getName());
			ex.printStackTrace();
		}
    return faultMessage;
	}

  /**
   * GRZE: with displeasure I force the return of the fault string for use elsewhere in the name of consistency and dryness.
   */
  @Override
	public Callable<String> logOnFirstRun() {
		return new Callable<String>( ) {
			private final AtomicBoolean logged = new AtomicBoolean( false );
      private String faultMessage;

      @Override
			public String call( ) {
				if ( logged.compareAndSet( false, true ) ) {
					this.faultMessage = log( );
				}
        return this.faultMessage;
			}
		};
	}
}
