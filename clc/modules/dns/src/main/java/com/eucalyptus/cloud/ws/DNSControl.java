/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud.ws;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Address;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.ResolverConfig;

import com.google.common.base.Strings;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.*;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.system.Capabilities;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.IO;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LockResource;
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.cloud.entities.ARecordAddressInfo;
import edu.ucsb.eucalyptus.cloud.entities.ARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ARecordNameInfo;
import edu.ucsb.eucalyptus.cloud.entities.CNAMERecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.NSRecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.SOARecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ZoneInfo;
import edu.ucsb.eucalyptus.msgs.AddMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.AddMultiARecordType;
import edu.ucsb.eucalyptus.msgs.AddZoneResponseType;
import edu.ucsb.eucalyptus.msgs.AddZoneType;
import edu.ucsb.eucalyptus.msgs.CreateMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.CreateMultiARecordType;
import edu.ucsb.eucalyptus.msgs.DeleteZoneResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteZoneType;
import edu.ucsb.eucalyptus.msgs.RemoveARecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveARecordType;
import edu.ucsb.eucalyptus.msgs.RemoveCNAMERecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveCNAMERecordType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordType;
import edu.ucsb.eucalyptus.msgs.UpdateARecordResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateARecordType;
import edu.ucsb.eucalyptus.msgs.UpdateCNAMERecordResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateCNAMERecordType;

@ConfigurableClass( root = "dns", description = "Controls dns listeners." )
public class DNSControl {
	private static Logger LOG = Logger.getLogger( DNSControl.class );

	private static final AtomicReference<Collection<Cidr>> addressMatchers =
			new AtomicReference<Collection<Cidr>>( Collections.<Cidr>emptySet( ) );

	private static final AtomicReference<Collection<UDPListener>> udpListenerRef =
			new AtomicReference<Collection<UDPListener>>( Collections.<UDPListener>emptySet( ) );

	private static final AtomicReference<Collection<TCPListener>> tcpListenerRef =
			new AtomicReference<Collection<TCPListener>>( Collections.<TCPListener>emptySet( ) );

	private static final Lock listenerLock = new ReentrantLock( );

	@ConfigurableField( displayName = "dns_listener_address_match",
			description = "Additional address patterns to listen on for DNS requests.",
			initial = "",
			readonly = false,
			changeListener = DnsAddressChangeListener.class)
	public static volatile String dns_listener_address_match = "";

	@ConfigurableField( displayName = "server_system_property",
			description = "Sets the value of 'dns.server' system property",
			initial = "",
			readonly = false,
			changeListener = DnsServerSystemProperty.class)
	public static volatile String server_system_property = "";

	@ConfigurableField( displayName = "search_system_property",
			description = "Sets the value of 'dns.search' system property",
			initial = "",
			readonly = false,
			changeListener = DnsSearchSystemProperty.class)
	public static volatile String search_system_property = "";

	public static class DnsServerSystemProperty implements PropertyChangeListener {
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			if (newValue == null || (newValue instanceof String)) {
				try {
					String newValueStr = (String) newValue;
					if (Strings.isNullOrEmpty(newValueStr)) {
						LOG.debug("Setting dns.server property to null (by removing it)");
						System.getProperties().remove("dns.server");
					} else {
						LOG.debug("Setting dns.server property to " + newValueStr);
						System.setProperty("dns.server", newValueStr);
					}
					ResolverConfig.refresh();
				} catch ( final Exception e ) {
					throw new ConfigurablePropertyException( e.getMessage( ) );
				}
			}
		}
	}

	public static class DnsSearchSystemProperty implements PropertyChangeListener {
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			if (newValue == null || (newValue instanceof String)) {
				try {
					String newValueStr = (String) newValue;
					if (Strings.isNullOrEmpty(newValueStr)) {
						LOG.debug("Setting dns.search property to null (by removing it)");
						System.getProperties().remove("dns.search");
					} else {
						LOG.debug("Setting dns.search property to " + newValueStr);
						System.setProperty("dns.search", newValueStr);
					}
					ResolverConfig.refresh();
				} catch ( final Exception e ) {
					throw new ConfigurablePropertyException( e.getMessage( ) );
				}
			}
		}
	}



	public static class DnsAddressChangeListener implements PropertyChangeListener {
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			if ( newValue instanceof String  ) {
				updateAddressMatchers( (String) newValue );
				try {
					restart( );
				} catch ( final Exception e ) {
					throw new ConfigurablePropertyException( e.getMessage( ) );
				}
			}
		}
	}

	private static void updateAddressMatchers( final String addressCidrs ) throws ConfigurablePropertyException {
		try {
			addressMatchers.set( ImmutableList.copyOf( Iterables.transform(
					Splitter.on( CharMatcher.anyOf(", ;:") ).trimResults( ).omitEmptyStrings( ).split( addressCidrs ),
					Cidr.parseUnsafe( )
			) ) );
		} catch ( IllegalArgumentException e ) {
			throw new ConfigurablePropertyException( e.getMessage( ) );
		}
	}

	private static void initializeUDP( ) {
		initializeListeners( udpListenerRef, "UDP", new ListenerBuilder<UDPListener>() {
			@Override
			public UDPListener build( final InetAddress address, final int port ) throws IOException {
				return new UDPListener( address, port );
			}
		});
	}

	private static void initializeTCP( ) {
		initializeListeners( tcpListenerRef, "TCP", new ListenerBuilder<TCPListener>() {
			@Override
			public TCPListener build( final InetAddress address, final int port ) throws IOException {
				return new TCPListener( address, port );
			}
		});
	}

	private static <T extends Thread> void initializeListeners(
			final AtomicReference<Collection<T>> listenerRef,
			final String description,
			final ListenerBuilder<T> builder
	) {
		try ( final LockResource lock = LockResource.lock( listenerLock ) ) {
			if ( listenerRef.get( ).isEmpty( ) ) {
				final int listenPort = DNSProperties.PORT;
				final Set<InetAddress> listenAddresses = Sets.newLinkedHashSet( );
				listenAddresses.add( Internets.localHostInetAddress( ) );
				Iterables.addAll(
						listenAddresses,
						Iterables.filter( Internets.getAllInetAddresses( ), Predicates.or( addressMatchers.get( ) ) ) );
				LOG.info( "Starting DNS " + description + " listeners on " + listenAddresses + ":" + listenPort );

				// Configured listeners
				final List<T> listeners = Lists.newArrayList( );
				for ( final InetAddress listenAddress : listenAddresses ) {
					try {
						final T listener = Capabilities.runWithCapabilities( new Callable<T>() {
							@Override
							public T call() throws Exception {
								final T listener = builder.build( listenAddress, listenPort );
								listener.start( );
								return listener;
							}
						} );
						listeners.add( listener );
					} catch( final Exception ex ) {
						LOG.error( "Error starting DNS "+description+" listener on "+listenAddress+":"+listenPort, ex );
					}
				}
				listenerRef.set( ImmutableList.copyOf( listeners ) );
			}
		}
	}

	private interface ListenerBuilder<T> {
		T build( InetAddress address, int port ) throws IOException;
	}

	public static class DnsPopulateTimer implements EventListener<ClockTick> {
	  public static void register( ) {
	      Listeners.register( ClockTick.class, new DnsPopulateTimer() );
	  }
	  private static int EventCounter = 0;
	  @Override
	  public void fireEvent(ClockTick event) {
	    if (!( Bootstrap.isFinished() &&
	         Topology.isEnabledLocally(Eucalyptus.class) && 
	             Topology.isEnabled(Dns.class) ) )
	       return;
	    
	    if(EventCounter++ >= 3){
	      try{
	        populateRecords();
	      }catch(final Exception ex){
	        LOG.error("Failed to populate DNS records");
	      }
	      EventCounter = 0;
	    }
	  }
	}

	public static void populateRecords() {
		DNSProperties.update();
		try ( final TransactionResource db = Entities.transactionFor( ZoneInfo.class ) ) {
			List<ZoneInfo> zoneInfos = Entities.query( new ZoneInfo() );
			for(ZoneInfo zoneInfo : zoneInfos) {
				String name = zoneInfo.getName();
				SOARecordInfo searchSOARecordInfo = new SOARecordInfo();
				searchSOARecordInfo.setName(name);
				SOARecordInfo soaRecordInfo = Entities.uniqueResult( searchSOARecordInfo );

				NSRecordInfo searchNSRecordInfo = new NSRecordInfo();
				searchNSRecordInfo.setName(name);
				NSRecordInfo nsRecordInfo = Entities.uniqueResult(searchNSRecordInfo);

				ZoneManager.addZone(zoneInfo, soaRecordInfo, nsRecordInfo);
			}

			ARecordInfo searchARecInfo = new ARecordInfo();
			List<ARecordInfo> aRecInfos = Entities.query(searchARecInfo);
			for(ARecordInfo aRecInfo : aRecInfos) {
				ZoneManager.addRecord(aRecInfo, false);
			}
			db.commit();
		} catch(NoSuchElementException | TransactionException ex) {
			LOG.error(ex);
    }

    final EntityTransaction db2 = Entities.get( ARecordNameInfo.class );
		try{
			int count = 0;
			List<ARecordNameInfo> multiARec = Entities.query(new ARecordNameInfo(), true);
			if(multiARec != null && multiARec.size()>0){
				for(ARecordNameInfo nameRec : multiARec){
					final Collection<ARecordAddressInfo> addresses = nameRec.getAddresses();
					if(addresses!=null && addresses.size()>0){
						for(final ARecordAddressInfo addrRec : addresses){
							ARecord record = new ARecord(new Name(nameRec.getName()), DClass.IN, nameRec.getTtl(), Address.getByAddress(addrRec.getAddress()));
							ZoneManager.addRecord(nameRec.getZone(), record, true);
							count++;
						}
					}
				}
			}
			LOG.debug(String.format("%d DNS records populated from database", count));
			db2.commit();
		}catch(Exception ex){
			db2.rollback();
			LOG.error("Failed to populate the existing DNS records", ex);
		}
	}

	public static void initialize() throws Exception {
		try {
			initializeUDP();
			initializeTCP();
			populateRecords();
		} catch(Exception ex) {
			LOG.error("DNS could not be initialized. Is some other service running on port 53?");
			throw ex;
		}
	}

	public static void stop() throws Exception {
		try ( final LockResource lock = LockResource.lock( listenerLock ) ) {
			IO.close( udpListenerRef.getAndSet( Collections.<UDPListener>emptySet( ) ) );
			IO.close( tcpListenerRef.getAndSet( Collections.<TCPListener>emptySet( ) ) );
		}
	}

	public static void restart()  throws Exception {
		stop();
		initialize();
	}
	
	public DNSControl() {}
	
	public CreateMultiARecordResponseType CreateMultiARecord(CreateMultiARecordType request) throws EucalyptusCloudException {
		CreateMultiARecordResponseType reply = (CreateMultiARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		long ttl = request.getTtl();

		// look for the records with the same name
		// create one if not found
		final EntityTransaction db = Entities.get( ARecordNameInfo.class );
		try{
			Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			db.commit();
			throw new EucalyptusCloudException("A record with the same name is found");
		}catch(NoSuchElementException ex){
			ARecordNameInfo newInfo = ARecordNameInfo.newInstance(name, zone,  DClass.IN, ttl);
			Entities.persist(newInfo);
			db.commit();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed due to database error");
		}
		return reply;
	}
	
	/// add a new name - address mapping
	public AddMultiARecordResponseType AddMultiARecord(AddMultiARecordType request) throws EucalyptusCloudException {
		AddMultiARecordResponseType reply = (AddMultiARecordResponseType) request.getReply();
		// find the exsting ARecordNameInfo; if not throw exception
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		long ttl = request.getTtl();
		String address = request.getAddress();
		
		EntityTransaction db = Entities.get( ARecordNameInfo.class );
		ARecordNameInfo nameInfo = null;
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			throw new EucalyptusCloudException("No dns record with name="+name+" is found");
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		// add new address
		// call ZoneManager.addRecord
		db = Entities.get( ARecordAddressInfo.class );
		try{
			List<ARecordAddressInfo> exist = Entities.query(ARecordAddressInfo.named(nameInfo, address));
			if(exist==null || exist.size()<=0){
				ARecord record = new ARecord(new Name(name), DClass.IN, ttl, Address.getByAddress(address));
				ZoneManager.addRecord(zone, record, true);
				ARecordAddressInfo addrInfo = ARecordAddressInfo.newInstance(nameInfo, address);
				Entities.persist(addrInfo);
			}
			db.commit();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to add the record", ex);
		}
		
		return reply;
	}
	
	/// remove an existing name - address mapping
	/// do nothing if no mapping is found
	public RemoveMultiARecordResponseType RemoveMultiARecord(RemoveMultiARecordType request) throws EucalyptusCloudException {
		RemoveMultiARecordResponseType reply = (RemoveMultiARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		String address = request.getAddress();
		EntityTransaction db = Entities.get( ARecordNameInfo.class );
		ARecordNameInfo nameInfo = null;
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			return reply;
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		db = Entities.get( ARecordAddressInfo.class );
		ARecordAddressInfo addrInfo = null;
		// find the existing record
		try{
			addrInfo = Entities.uniqueResult(ARecordAddressInfo.named(nameInfo, address));
			ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, nameInfo.getTtl(), Address.getByAddress(addrInfo.getAddress()));
		
			ZoneManager.deleteARecord(zone, arecord);
			Entities.delete(addrInfo);
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to delete the record");
		}
		
		return reply;
	}
	
	/// remove all name - {address1, address2, ...} mapping
	/// do nothing if no mapping is found
	public RemoveMultiANameResponseType RemoveMultiAName(RemoveMultiANameType request) throws EucalyptusCloudException {
		RemoveMultiANameResponseType reply = (RemoveMultiANameResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";

		EntityTransaction db = Entities.get( ARecordNameInfo.class );
		ARecordNameInfo nameInfo = null;
		List<ARecordAddressInfo> addresses = null;
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			addresses = Lists.newArrayList(nameInfo.getAddresses());
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			return reply;
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		// delete the records from zone
		for(ARecordAddressInfo addr : addresses){
			try{
				ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, nameInfo.getTtl(), Address.getByAddress(addr.getAddress()));
				ZoneManager.deleteARecord(zone, arecord);
			}catch(Exception ex){
				throw new EucalyptusCloudException("Failed to delete the record from zone", ex);
			}	
		}
		db = Entities.get( ARecordNameInfo.class );
		try{
			nameInfo = Entities.uniqueResult(ARecordNameInfo.named(name, zone));
			Entities.delete(nameInfo); // deletion will be cascaded to AddressInfo
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
		}catch(Exception ex){
			db.rollback();
			throw new EucalyptusCloudException("Failed to query dns name record", ex);
		}
		
		return reply;
	}	

	public UpdateARecordResponseType UpdateARecord(UpdateARecordType request)  throws EucalyptusCloudException {
		UpdateARecordResponseType reply = (UpdateARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		
		String address = request.getAddress();
		long ttl = request.getTtl();
		try ( final TransactionResource db = Entities.transactionFor( ARecordInfo.class ) ) {
			ARecordInfo aRecordInfo = new ARecordInfo();
			aRecordInfo.setName(name);
			aRecordInfo.setAddress(address);
			aRecordInfo.setZone(zone);
			List<ARecordInfo> arecords = Entities.query(aRecordInfo);
			if(arecords.size() > 0) {
				aRecordInfo = arecords.get(0);
				if(!zone.equals(aRecordInfo.getZone())) {
					db.rollback();
					throw new EucalyptusCloudException("Sorry, record already associated with a zone. Remove the record and try again.");
				}
				aRecordInfo.setAddress(address);
				aRecordInfo.setTtl(ttl);
				//update record
				try {
					ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, ttl, Address.getByAddress(address));
					ZoneManager.updateARecord(zone, arecord);
				} catch(Exception ex) {
					LOG.error(ex);
				}
			}  else {
				try {
					ARecordInfo searchARecInfo = new ARecordInfo();
					searchARecInfo.setZone(zone);
					ARecord record = new ARecord(new Name(name), DClass.IN, ttl, Address.getByAddress(address));
					ZoneManager.addRecord(zone, record, false);
					aRecordInfo = new ARecordInfo();
					aRecordInfo.setName(name);
					aRecordInfo.setAddress(address);
					aRecordInfo.setTtl(ttl);
					aRecordInfo.setZone(zone);
					aRecordInfo.setRecordclass(DClass.IN);
					Entities.persist(aRecordInfo);
				} catch(Exception ex) {
					LOG.error(ex);
				}
			}
			db.commit();
		}
		return reply;
	}

	public RemoveARecordResponseType RemoveARecord(RemoveARecordType request) throws EucalyptusCloudException {
		RemoveARecordResponseType reply = (RemoveARecordResponseType) request.getReply();
		String zone = request.getZone();
		if (zone.endsWith("."))
			zone += DNSProperties.DOMAIN + ".";
		else
			zone += "."+DNSProperties.DOMAIN + ".";
		String name = request.getName();
		if (name.endsWith("."))
			name+= DNSProperties.DOMAIN + ".";
		else
			name+= "."+DNSProperties.DOMAIN + ".";
		String address = request.getAddress();
		ARecordInfo aRecordInfo = new ARecordInfo();
		aRecordInfo.setName(name);
		aRecordInfo.setZone(zone);
		aRecordInfo.setAddress(address);
		try ( final TransactionResource db = Entities.transactionFor( ARecordInfo.class ) ) {
			ARecordInfo foundARecordInfo = Entities.uniqueResult(aRecordInfo);
			ARecord arecord = new ARecord(Name.fromString(name), DClass.IN, foundARecordInfo.getTtl(), Address.getByAddress(foundARecordInfo.getAddress()));
			ZoneManager.deleteRecord(zone, arecord);
			Entities.delete(foundARecordInfo);
			db.commit();
		} catch(NoSuchElementException e) {
			LOG.error(e);
		} catch(Exception ex) {
			LOG.error(ex, ex);
		}
		return reply;
	}

	public AddZoneResponseType AddZone(AddZoneType request) throws EucalyptusCloudException {
		AddZoneResponseType reply = (AddZoneResponseType) request.getReply();
		String name = request.getName();
		if(!Contexts.lookup().hasAdministrativePrivileges()) {
			throw new EucalyptusCloudException("Access Denied. Only administrator can add zone.");
			//zhill - removed this, should not use OSG/Walrus exceptions in the DNS stack.
			//throw new AccessDeniedException(name);
		}

		return reply;
	}

	public UpdateCNAMERecordResponseType UpdateCNAMERecord(UpdateCNAMERecordType request)  throws EucalyptusCloudException {
		UpdateCNAMERecordResponseType reply = (UpdateCNAMERecordResponseType) request.getReply();
		String zone = request.getZone()  + DNSProperties.DOMAIN + ".";
		String name = request.getName()  + DNSProperties.DOMAIN + ".";
		String alias = request.getAlias() + DNSProperties.DOMAIN + ".";
		long ttl = request.getTtl();
		CNAMERecordInfo cnameRecordInfo = new CNAMERecordInfo();
		cnameRecordInfo.setName(name);
		cnameRecordInfo.setAlias(alias);
		cnameRecordInfo.setZone(zone);
		try ( final TransactionResource db = Entities.transactionFor( CNAMERecordInfo.class ) ) {
			List<CNAMERecordInfo> cnamerecords = Entities.query(cnameRecordInfo);
			if(cnamerecords.size() > 0) {
				cnameRecordInfo = cnamerecords.get(0);
				if(!zone.equals(cnameRecordInfo.getZone())) {
					db.rollback();
					throw new EucalyptusCloudException("Sorry, record already associated with a zone. Remove the record and try again.");
				}
				cnameRecordInfo.setAlias(alias);
				cnameRecordInfo.setTtl(ttl);
				//update record
				try {
					CNAMERecord cnameRecord = new CNAMERecord(Name.fromString(name), DClass.IN, ttl, Name.fromString(alias));
					ZoneManager.updateCNAMERecord(zone, cnameRecord);
				} catch(Exception ex) {
					LOG.error(ex);
				}
			}  else {
				try {
					CNAMERecordInfo searchCNAMERecInfo = new CNAMERecordInfo();
					searchCNAMERecInfo.setZone(zone);
					CNAMERecord record = new CNAMERecord(new Name(name), DClass.IN, ttl, Name.fromString(alias));
					ZoneManager.addRecord(zone, record, false);
					cnameRecordInfo = new CNAMERecordInfo();
					cnameRecordInfo.setName(name);
					cnameRecordInfo.setAlias(alias);
					cnameRecordInfo.setTtl(ttl);
					cnameRecordInfo.setZone(zone);
					cnameRecordInfo.setRecordclass(DClass.IN);
					Entities.persist(cnameRecordInfo);
				} catch(Exception ex) {
					LOG.error(ex);
				}
			}
			db.commit();

		}
		return reply;
	}

	public RemoveCNAMERecordResponseType RemoveCNAMERecord(RemoveCNAMERecordType request) throws EucalyptusCloudException {
		RemoveCNAMERecordResponseType reply = (RemoveCNAMERecordResponseType) request.getReply();
		String zone = request.getZone()  + DNSProperties.DOMAIN + ".";
		String name = request.getName()  + DNSProperties.DOMAIN + ".";
		String alias = request.getAlias() + DNSProperties.DOMAIN + ".";
		CNAMERecordInfo cnameRecordInfo = new CNAMERecordInfo();
		cnameRecordInfo.setName(name);
		cnameRecordInfo.setZone(zone);
		cnameRecordInfo.setAlias(alias);
		try ( final TransactionResource db = Entities.transactionFor( CNAMERecordInfo.class ) ) {
			CNAMERecordInfo foundCNAMERecordInfo = Entities.uniqueResult(cnameRecordInfo);
			CNAMERecord cnameRecord = new CNAMERecord(Name.fromString(name), DClass.IN, foundCNAMERecordInfo.getTtl(), Name.fromString(foundCNAMERecordInfo.getAlias()));
			ZoneManager.deleteRecord(zone, cnameRecord);
			Entities.delete(foundCNAMERecordInfo);
			db.commit();
		} catch(Exception ex) {
			LOG.error(ex);
		}
		return reply;
	}

	public DeleteZoneResponseType DeleteZone(DeleteZoneType request) throws EucalyptusCloudException {
		DeleteZoneResponseType reply = (DeleteZoneResponseType) request.getReply();
		String name = request.getName();
		if(!Contexts.lookup().hasAdministrativePrivileges()) {
			throw new AccessDeniedException(name);
		}
		ZoneInfo zoneInfo = new ZoneInfo(name);
		try ( final TransactionResource db = Entities.transactionFor( ZoneInfo.class ) ) {
			ZoneInfo foundZoneInfo = Entities.uniqueResult(zoneInfo);
			Entities.delete(foundZoneInfo);
			db.commit();
		} catch(Exception ex) {
			LOG.error(ex);
		}
		ZoneManager.deleteZone(name);
		return reply;
	}

}
