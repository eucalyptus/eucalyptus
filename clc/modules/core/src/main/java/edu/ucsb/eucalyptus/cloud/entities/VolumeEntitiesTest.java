/*************************************************************************
 * Copyright 2013 Eucalyptus Systems, Inc.
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

package edu.ucsb.eucalyptus.cloud.entities;

import java.util.NoSuchElementException;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

/**
 * Tests for Volume-related entities
 */
public class VolumeEntitiesTest {
	private static final Logger LOG = Logger.getLogger(VolumeEntitiesTest.class);
	private static final String fakeSC = "testing_non_sc";
	public static VolumeInfo addFakeVolume(String volumeId) {
		EntityTransaction db = Entities.get(VolumeInfo.class);
		VolumeInfo vol = new VolumeInfo(volumeId);
		vol.setScName(fakeSC);
		vol.setSize(1);
		vol.setStatus("available");
		vol.updateTimeStamps();
		Entities.persist(vol);
		db.commit();
		return vol;
	}
	
	public static VolumeToken createFakeToken(VolumeInfo srcVol) {
		EntityTransaction db = Entities.get(VolumeInfo.class);
		try {
			LOG.info("Merging entity");
			VolumeInfo volEntity = Entities.merge(srcVol);
			LOG.info("Creating token");
			VolumeToken tok = volEntity.getOrCreateAttachmentToken();
			LOG.info("Got token: " + tok.getToken());
			return tok;
		} catch(Exception e) {
			LOG.error(e);
		} finally {
			if(db.isActive()) {
				db.commit();
			} else {
				db.rollback();
			}
		}
		return null;
	}
	
	public static void testAdd(String volumeName) {
		VolumeInfo volInfo = addFakeVolume(volumeName);
		VolumeToken tok = createFakeToken(volInfo);
		try {
			tok.addExport("1.1.1.1", "iqn-123", "fake-connect-string");
		} catch(Exception e) {
			LOG.error(e);
		}
	}
	
	public static void testRemove(String volumeName) {
		VolumeInfo vol = new VolumeInfo();
		vol.setVolumeId(volumeName);
		vol.setScName(fakeSC);
		EntityTransaction db = Entities.get(VolumeInfo.class);
		try {
			VolumeInfo volEntity = Entities.uniqueResult(vol);
			VolumeToken tok = volEntity.getCurrentValidToken();
			tok.invalidateExport("1.1.1.1", "iqn-123");
		} catch(Exception e) {
			LOG.error(e);
		} finally {
			db.commit();
		}
		
	}
	
	public static void printVolume(String vol) {
		EntityTransaction db = Entities.get(VolumeInfo.class);
		try {
			VolumeInfo example = new VolumeInfo();
			example.setVolumeId(vol);
			VolumeInfo volEntity = Entities.uniqueResult(example);
			LOG.info("Volume - " + volEntity.getVolumeId() + " - " + volEntity.getStatus());
			for(VolumeToken tok : volEntity.getAttachmentTokens()) {
				LOG.info("Volume - " + tok.getVolume().getVolumeId() + " -- " + tok.getToken() + " - " + tok.getIsValid());
				
				for(VolumeExportRecord rec : tok.getExportRecords()) {
					LOG.info("Token - " + tok.getToken() + rec.getHostIp() + " - " + rec.getHostIqn() + " - " + rec.getConnectionString() + " - " + rec.getIsActive());
				}
			}
			
			VolumeToken test = volEntity.getCurrentValidToken();
			if(test == null) {
				LOG.info("Null current valid token");				
			} else {
				LOG.info("Current valid token : " + test.getToken());
			}
		} catch(Exception e) {
			LOG.error(e);
		} finally {
			db.commit();
		}
	}
	
	//TODO: testing only!
	public static void TestExport(String volumeId) {
		final String ip = "1.1.1.1";
		final String iqn = "fake-iqn";
		final String connectionString = "fake-connect-string-12-1223123";
		Function<VolumeInfo, VolumeExportRecord> exportAndAttach = new Function<VolumeInfo, VolumeExportRecord>() {
			@Override
			public VolumeExportRecord apply(VolumeInfo volume) {
				VolumeToken tokenInfo = null;
				VolumeInfo volEntity = Entities.merge(volume);
				
				LOG.info("TESTING#: Set of tokens");
				for(VolumeToken tok : volEntity.getAttachmentTokens()) {
					LOG.info("TESTING#: " + tok.getToken() + " - " + tok.getIsValid());
					LOG.info("TESTING#: listing export records");
					for(VolumeExportRecord rec : tok.getExportRecords()) {
						LOG.info("TESTING#: " + rec.getHostIp() + " - " + rec.getHostIqn() + " - " + rec.getConnectionString() + " - " + rec.getIsActive());
					}
					LOG.info("TESTING#: done listing export records");
				}				
				LOG.info("TESTING#: Done listing tokens");
				
				try {
					tokenInfo = volEntity.getCurrentValidToken();
					if(tokenInfo == null) {
						LOG.info("TESTING#: No valid token found");
						LOG.info("TESTING#: Creating new token for testing");
						tokenInfo = volEntity.getOrCreateAttachmentToken();
						if(tokenInfo == null) {
							throw new Exception("Failed to create new attachment token");
						}
					} else {
						LOG.info("TESTING#: current valid token : " + tokenInfo.getToken());
					}
				} catch(Exception e) {
					LOG.error("TESTING#: Could not check for valid token", e);
					return null;
				}
				
				VolumeExportRecord export = null;
				//Normally do the actual export here...					
				try{
					//addExport must be idempotent, if one exists a new is not added with same data
					LOG.info("TESTING#: adding export");
					tokenInfo.addExport(ip, iqn, connectionString);
					LOG.info("TESTING#: done adding export");
				} catch(Exception e) {
						LOG.error("TESTING#: Could not export volume " + volEntity.getVolumeId() + " failed to add export record");
						return null;
				}			
				
				try {
					LOG.info("TESTING#: getting valid export");
					export = tokenInfo.getValidExport(ip, iqn);
					if(export != null) {
						LOG.info("TESTING#: got valid export " + export.getHostIp() + " - " + export.getHostIqn() + " - " + export.getConnectionString());
					} else {
						throw new EucalyptusCloudException("Null valid export returned");
					}
				} catch (EucalyptusCloudException e) {
					LOG.error("TESTING#: failed to get valid export", e);
				}
				return export;
			}
		};
				
		Function<String, VolumeInfo> createFakeVolume = new Function<String, VolumeInfo>() {
			@Override
			public VolumeInfo apply(String volumeId) {
				LOG.info("TESTING#: Creating fake volume record with id : " + volumeId);
				VolumeInfo fakeVolume = new VolumeInfo(volumeId);
				fakeVolume.setScName("fake_sc");
				fakeVolume.setSize(1);
				fakeVolume.setStatus("available");
				fakeVolume.updateTimeStamps();
				Entities.persist(fakeVolume);
				LOG.info("TESTING#: done persisting fake volume");
				return fakeVolume;
			}			
		};
		
		VolumeInfo searchVol = new VolumeInfo(volumeId);
		searchVol.setScName("fake_sc");
		EntityTransaction db = Entities.get(VolumeInfo.class);
		VolumeInfo vol = null;
		try {
			vol = Entities.uniqueResult(searchVol);
			db.commit();
		} catch(NoSuchElementException e) {
			LOG.info("TESTING#: No volume found, creating",e);			
			vol = Entities.asTransaction(VolumeInfo.class, createFakeVolume).apply(volumeId);
		} catch (TransactionException e) {			
			LOG.error("TESTING#: Failed to Export due to db error",e);
			return;
		} finally {
			if(db.isActive()) {
				db.rollback();
			}
		}
		
		try {
			Entities.asTransaction(VolumeInfo.class, exportAndAttach).apply(vol);
		} catch(Exception e) {
			LOG.error("TESTING#: Failed ExportVolume transaction due to: " + e.getMessage(), e);			
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String volName = "vol-fake";
		if(args.length > 1) {
			volName = args[1];
		}
		LOG.info("Testing add!");
		testAdd(volName);
		
		LOG.info("Testing remove!");
		testRemove(volName);

		LOG.info("Done with test!");
	}

}
