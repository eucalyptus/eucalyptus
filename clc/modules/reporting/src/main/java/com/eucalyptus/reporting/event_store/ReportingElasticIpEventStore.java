package com.eucalyptus.reporting.event_store;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import com.eucalyptus.entities.EntityWrapper;

/**
 * @author tom.werges
 */
public class ReportingElasticIpEventStore
{
	private static Logger LOG = Logger.getLogger( ReportingElasticIpEventStore.class );

	private static ReportingElasticIpEventStore elasticIpCrud = null;
	
	public static synchronized ReportingElasticIpEventStore getElasticIp()
	{
		if (elasticIpCrud == null) {
			elasticIpCrud = new ReportingElasticIpEventStore();
		}
		return elasticIpCrud;
	}
	
	private ReportingElasticIpEventStore()
	{
		
	}

	public void insertCreateEvent(String uuid, long timestampMs, String userId, String ip)
	{
		
		EntityWrapper<ReportingElasticIpCreateEvent> entityWrapper =
			EntityWrapper.get(ReportingElasticIpCreateEvent.class);

		try {
			ReportingElasticIpCreateEvent event = new ReportingElasticIpCreateEvent(uuid,
					timestampMs, userId, ip);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertDeleteEvent(String uuid, long timestampMs)
	{
		
		EntityWrapper<ReportingElasticIpDeleteEvent> entityWrapper =
			EntityWrapper.get(ReportingElasticIpDeleteEvent.class);

		try {
			ReportingElasticIpDeleteEvent event = new ReportingElasticIpDeleteEvent(uuid, timestampMs);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertAttachEvent(String ipUuid, String instanceUuid, long timestampMs)
	{
		
		EntityWrapper<ReportingElasticIpAttachEvent> entityWrapper =
			EntityWrapper.get(ReportingElasticIpAttachEvent.class);

		try {
			ReportingElasticIpAttachEvent event = new ReportingElasticIpAttachEvent(ipUuid, instanceUuid, timestampMs);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

	public void insertDetachEvent(String ipUuid, String instanceUuid, long timestampMs)
	{
		
		EntityWrapper<ReportingElasticIpDetachEvent> entityWrapper =
			EntityWrapper.get(ReportingElasticIpDetachEvent.class);

		try {
			ReportingElasticIpDetachEvent event = new ReportingElasticIpDetachEvent(ipUuid, instanceUuid, timestampMs);
			entityWrapper.add(event);
			entityWrapper.commit();
			LOG.debug("Added event to db:" + event);
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

}

