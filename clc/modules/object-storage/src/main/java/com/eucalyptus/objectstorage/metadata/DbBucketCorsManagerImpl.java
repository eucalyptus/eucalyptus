/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.BucketCorsManagers;
import com.eucalyptus.objectstorage.entities.CorsRule;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;

import net.sf.json.JSONArray;

/*
 *
 */
public class DbBucketCorsManagerImpl implements BucketCorsManager {

  private static Logger LOG = Logger.getLogger(DbBucketCorsManagerImpl.class);

  @Override
  public void start() throws Exception {
    // no-op
  }

  @Override
  public void stop() throws Exception {
    // no-op
  }

  @Override
  public void deleteCorsRules(@Nonnull String bucketUuid, TransactionResource tran) {

    if (tran == null || !tran.isActive()) {
      throw new RuntimeException(new ObjectStorageException("in DbBucketCorsManagerImpl.deleteCorsRules, "
          + "but was not given an active transaction"));
    }
    CorsRule example = new CorsRule();
    example.setBucketUuid(bucketUuid);
    List<CorsRule> existing = Entities.query(example);
    if (existing != null && existing.size() > 0) {
      // delete them
      Map<String, String> criteria = new HashMap<>();
      criteria.put("bucketUuid", bucketUuid);
      Entities.deleteAllMatching(CorsRule.class, "WHERE bucketUuid = :bucketUuid", criteria);
    }
  }

  @Override
  public void deleteCorsRules(@Nonnull String bucketUuid) throws ObjectStorageException {
    try (final TransactionResource tran = Entities.transactionFor(CorsRule.class)) {
      BucketCorsManagers.getInstance().deleteCorsRules(bucketUuid, tran);
      tran.commit();
    } catch (Exception ex) {
      LOG.error("Exception caught while deleting CORS rules for bucket " + bucketUuid, ex);
      throw new ObjectStorageException("InternalServerError", "Exception caught while deleting CORS rules for bucket "
          + bucketUuid, "Bucket", bucketUuid, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void addCorsRules(@Nonnull List<com.eucalyptus.storage.msgs.s3.CorsRule> rules, @Nonnull String bucketUuid)
      throws ObjectStorageException {

    try (TransactionResource tran = Entities.transactionFor(CorsRule.class)) {
      // first get rid of existing rules
      BucketCorsManagers.getInstance().deleteCorsRules(bucketUuid, tran);
      // now add the rules from the messages
      if (rules != null && rules.size() > 0) {
        for (com.eucalyptus.storage.msgs.s3.CorsRule ruleInfo : rules) {
          CorsRule converted = convertCorsRule(ruleInfo, bucketUuid);
          Entities.merge(converted);
        }
      }
      tran.commit();
    } catch (Exception ex) {
      LOG.error("Exception caught while adding CORS rules for bucket " + bucketUuid, ex);
      throw new ObjectStorageException("InternalServerError", "An exception was caught while adding CORS rules for bucket "
          + bucketUuid, "Bucket", bucketUuid, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public List<com.eucalyptus.storage.msgs.s3.CorsRule> getCorsRules(@Nonnull String bucketUuid) {

    List<CorsRule> rulesFromDb = null;

    CorsRule exampleRule = new CorsRule();
    exampleRule.setBucketUuid(bucketUuid);
    try (final TransactionResource tran = Entities.transactionFor(CorsRule.class)) {
      rulesFromDb = Entities.query(exampleRule);
      tran.commit();
    } catch (NoSuchElementException e) {
      // No CORS configuration exists. An empty list will be returned.
    } catch (Exception ex) {
      LOG.error("Exception caught while retrieving CORS rules for bucket " + bucketUuid, ex);
    }

    List<com.eucalyptus.storage.msgs.s3.CorsRule> responseRules = null;

    if (rulesFromDb != null) {
      responseRules = new ArrayList<com.eucalyptus.storage.msgs.s3.CorsRule>(rulesFromDb.size());
      for (CorsRule fromDb : rulesFromDb) {
        int sequence = fromDb.getSequence();
        if (responseRules.size() <= sequence) {
          com.eucalyptus.storage.msgs.s3.CorsRule dummyRule = null;
          for (int idx = responseRules.size(); idx < sequence+1; idx++) {
            responseRules.add(dummyRule);
          }
        }
        responseRules.set(sequence, convertCorsRule(fromDb));
      }
    }

    return responseRules;
  }

  private CorsRule convertCorsRule(com.eucalyptus.storage.msgs.s3.CorsRule rule, String bucketUuid) {

    CorsRule entity = new CorsRule();
    entity.setBucketUuid(bucketUuid);
    entity.setRuleId(rule.getId());
    entity.setSequence(rule.getSequence());
    entity.setMaxAgeSeconds(rule.getMaxAgeSeconds());

    entity.setAllowedMethodsJSON(convertCorsListToJSON(rule.getAllowedMethods()));
    entity.setAllowedOriginsJSON(convertCorsListToJSON(rule.getAllowedOrigins()));
    entity.setAllowedHeadersJSON(convertCorsListToJSON(rule.getAllowedHeaders()));
    entity.setExposeHeadersJSON(convertCorsListToJSON(rule.getExposeHeaders()));

    return entity;
  }

  private String convertCorsListToJSON(List<String> corsArray) {
    JSONArray corsJSON = new JSONArray();
    corsJSON.addAll(corsArray);
    return corsJSON.toString();
  }

  private com.eucalyptus.storage.msgs.s3.CorsRule convertCorsRule(CorsRule entity) {

    com.eucalyptus.storage.msgs.s3.CorsRule ruleResponse = new com.eucalyptus.storage.msgs.s3.CorsRule();
    ruleResponse.setId(entity.getRuleId());
    ruleResponse.setSequence(entity.getSequence());
    ruleResponse.setMaxAgeSeconds(entity.getMaxAgeSeconds());

    ruleResponse.setAllowedMethods(convertCorsJSONToList(entity.getAllowedMethodsJSON()));
    ruleResponse.setAllowedOrigins(convertCorsJSONToList(entity.getAllowedOriginsJSON()));
    ruleResponse.setAllowedHeaders(convertCorsJSONToList(entity.getAllowedHeadersJSON()));
    ruleResponse.setExposeHeaders(convertCorsJSONToList(entity.getExposeHeadersJSON()));

    return ruleResponse;
  }

  @SuppressWarnings( "unchecked" )
  private ArrayList<String> convertCorsJSONToList( String corsJSONString) {
    JSONArray corsJSON = JSONArray.fromObject(corsJSONString);
    ArrayList<String> corsList = new ArrayList<String>();
    corsList.addAll( JSONArray.toCollection(corsJSON, String.class) );
    return corsList;
  }

}