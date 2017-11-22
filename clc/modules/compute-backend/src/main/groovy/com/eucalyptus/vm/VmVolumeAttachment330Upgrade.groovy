/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.vm

import groovy.sql.Sql

import java.util.concurrent.Callable

import org.apache.log4j.Logger
import com.eucalyptus.upgrade.Upgrades.DatabaseFilters
import com.eucalyptus.upgrade.Upgrades.PostUpgrade
import com.eucalyptus.upgrade.Upgrades.Version

//@EntityUpgrade(entities = [VmVolumeAttachment.class], since = Version.v3_3_0, value = com.eucalyptus.component.id.Eucalyptus.class)
//public class EmcVnxInfo330EntityUpgrade implements Predicate<Class> {
//	private static Logger LOG = Logger.getLogger(EmcVnxInfo330EntityUpgrade.class);
//
//	@Override
//	public boolean apply(Class arg0) {
//		LOG.info("Entity upgrade for transient VmVolumeAttachment");
//		Sql sql = null;
//		try {
//			sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_cloud");
//			def attachments = sql.rows("select * from metadata_instances_volume_attachments");
//			if (attachments != null && attachments.size() > 0) {
//				LOG.info("Found ${attachments.size()} row(s) in metadata_instances_volume_attachments table");
//				attachments.each() {
//					LOG.info("Processing VmVolumeAttachment entity: ${it}");
//					if (it.containsKey('metadata_vm_volume_remove_device') && it.metadata_vm_volume_remove_device != null) {
//						LOG.info("Copying metadata_vm_volume_remove_device to metadata_vm_volume_remote_device")
//						int result = sql.executeUpdate('update metadata_instances_volume_attachments set metadata_vm_volume_remote_device=? where metadata_vm_volume_id=?', [it.metadata_vm_volume_remove_device, it.metadata_vm_volume_id]);
//						LOG.info("Updated ${result} row");
//						sql.eachRow('select * from metadata_instances_volume_attachments where metadata_vm_volume_id=?', [it.metadata_vm_volume_id]) {
//							postrow -> LOG.info("Post update VmVolumeAttachment entity: ${postrow}");
//						}
//					} else {
//						LOG.info("VmVolumeAttachment entity does not contain a populated metadata_vm_volume_remove_device column. Skipping the upgrade")
//					}
//				}
//			} else {
//				LOG.info("No transient VmVolumeAttachment entities to upgrade");
//			}
//			return true;
//		} catch (Exception ex) {
//			LOG.error("Failed to upgrade transient VmVolumeAttachment entity", ex);
//			return false;
//		} finally {
//			if (sql != null) {
//				sql.close();
//			}
//		}
//	}
//}

/**
 * <p>Renamed metadata_vm_volume_remove_device column to metadata_vm_volume_remote_device in 
 * {@link com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment}. Dropping the incorrectly named column.</p>
 * 
 * @author Swathi Gangisetty
 */
@PostUpgrade(value = com.eucalyptus.component.id.Eucalyptus.class, since = Version.v3_3_0)
public class VmVolumeAttachment330PostUpgrade implements Callable<Boolean> {
	private static Logger LOG = Logger.getLogger(VmVolumeAttachment330PostUpgrade.class);

	@Override
	public Boolean call() throws Exception {
		LOG.info("Post upgrade for EmcVnxInfo");
		Sql sql = null;
		try {
			sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_cloud");
			LOG.info("Dropping column metadata_vm_volume_remove_device in metadata_instances_persistent_volumes table");
			sql.execute( "ALTER TABLE metadata_instances_persistent_volumes DROP COLUMN IF EXISTS metadata_vm_volume_remove_device" );
			LOG.info("Dropping column metadata_vm_volume_remove_device in metadata_instances_volume_attachments table");
			sql.execute( "ALTER TABLE metadata_instances_volume_attachments DROP COLUMN IF EXISTS metadata_vm_volume_remove_device" );
		}catch (Exception ex) {
			LOG.error("Failed to drop columns in EmcVnxInfo table", ex);
			return false;
		} finally {
			if (sql != null) {
				sql.close();
			}
		}
	}
}
