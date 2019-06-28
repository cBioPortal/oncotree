/*
 * Copyright (c) 2016-2019 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.
*/
package org.mskcc.oncotree.utils;

import java.time.ZonedDateTime;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import org.mskcc.oncotree.error.InvalidOncoTreeDataException;
import org.mskcc.oncotree.error.InvalidVersionException;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.TopBraidException;
import org.mskcc.oncotree.utils.FailedCacheRefreshException;
import org.mskcc.oncotree.utils.OncoTreePersistentCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by Hongxin on 2/25/16.
 */
@Component
@EnableScheduling
public class CacheUtil {

    private static final Logger logger = LoggerFactory.getLogger(CacheUtil.class);

    private static Map<Version, Map<String, TumorType>> tumorTypesCache = null;
    private Date dateOfLastCacheRefresh = null;
    public static final Integer MAXIMUM_CACHE_AGE_IN_DAYS = 3;

    @Autowired
    private OncoTreePersistentCache oncoTreePersistentCache;

    @Autowired
    private TumorTypesUtil tumorTypesUtil;

    @Value("${slack.url}")
    private String slackURL;

    @Value("${required.oncotree.version:oncotree_latest_stable}")
    private String requiredOncotreeVersion;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron="0 */10 * * * *") // call every 10 minutes
    private void scheduleResetCache() {
        // TODO make sure we don't have two scheduled calls run simultaneously
        if (tumorTypesCache == null || cacheIsStale()) {
            try {
                resetCache();
            } catch (FailedCacheRefreshException e) {
                sendSlackNotification("*URGENT: OncoTree Error* - an attempt to refresh an outdated or null cache failed." + e.getMessage());
            }
        }
    }

    public void resetCache() throws FailedCacheRefreshException {
        logger.info("resetCache() -- refilling tumor types cache");
        Map<Version, Map<String, TumorType>> latestTumorTypesCache = new HashMap<>();
        ArrayList<Version> oncoTreeVersions = new ArrayList<Version>();
        ArrayList<String> failedVersions = new ArrayList<String>();
        // use this to store and look up previous oncoTree codes
        HashMap<String, ArrayList<String>> topBraidURIsToOncotreeCodes = new HashMap<String, ArrayList<String>>();

        boolean failedOncoTreeVersionsCacheRefresh = false;
        boolean failedVersionedOncoTreeNodesCacheRefresh = false;       
        // update EHCache with newest versions available
        try {
            oncoTreePersistentCache.updateOncoTreeVersionsInPersistentCache();
        } catch (TopBraidException exception) {
            logger.error("resetCache() -- failed to pull versions from repository");
            failedOncoTreeVersionsCacheRefresh = true;
        }

        try {
            oncoTreeVersions = oncoTreePersistentCache.getOncoTreeVersionsFromPersistentCache();
        } catch (TopBraidException e) {
            try {
                logger.error("Unable to load versions from default EHCache... attempting to read from backup cache.");
                oncoTreeVersions = oncoTreePersistentCache.getOncoTreeVersionsFromPersistentCacheBackup();
                if (oncoTreeVersions == null) {
                    throw new FailedCacheRefreshException("No data found in specified backup cache location...");
                }
            } catch (Exception e2) {
                logger.error("Unable to load versions from backup EHCache...");
                logger.error(e2.getMessage());
                throw new FailedCacheRefreshException("Unable to load versions from backup cache...");
            }
        }
        if (!failedOncoTreeVersionsCacheRefresh) {
            try {
                oncoTreePersistentCache.backupOncoTreeVersionsPersistentCache(oncoTreeVersions);
            } catch (Exception e) {
                logger.error("Unable to backup versions EHCache");
            }
        }

        // versions are ascending by release date
        for (Version version : oncoTreeVersions) {
            Map<String, TumorType> latestTumorTypes = new HashMap<String, TumorType>();
            ArrayList<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>();
            failedVersionedOncoTreeNodesCacheRefresh = false;       
            if (version != null) { 
                try {
                    oncoTreePersistentCache.updateOncoTreeNodesInPersistentCache(version);
                } catch (TopBraidException e) {
                    logger.error("resetCache() -- failed to pull tumor types for version '" + version.getVersion() + "' from repository");
                    failedVersionedOncoTreeNodesCacheRefresh = true;
                }
                try {
                    oncoTreeNodes = oncoTreePersistentCache.getOncoTreeNodesFromPersistentCache(version);
                } catch (TopBraidException e) {
                    try {
                        logger.error("Unable to load oncotree nodes from default EHCache... attempting to read from backup.");
                        oncoTreeNodes = oncoTreePersistentCache.getOncoTreeNodesFromPersistentCacheBackup(version);
                        if (oncoTreeNodes == null) {
                            logger.error("No data found for version " + version.getVersion() + " in backup EHCache");
                            failedVersions.add(version.getVersion());
                            continue;
                        }
                    } catch (Exception e2) {
                        logger.error("Unable to load oncotree nodes for version " + version.getVersion() + " from backup cache...");
                        failedVersions.add(version.getVersion());
                        continue;
                    } 
                }
                if (!failedVersionedOncoTreeNodesCacheRefresh) {
                    try {
                        oncoTreePersistentCache.backupOncoTreeNodesPersistentCache(oncoTreeNodes, version);
                    } catch (Exception e) {
                        logger.error("Unale to backup oncotree nodes EHCche");
                    }
                }
                try {
                    latestTumorTypes = tumorTypesUtil.getAllTumorTypesFromOncoTreeNodes(oncoTreeNodes, version, topBraidURIsToOncotreeCodes);
                } catch (InvalidOncoTreeDataException exception) {
                    logger.error("Unable to get tumor types from oncotree nodes");
                    failedVersions.add(version.getVersion());
                    continue;
                }   
            }
            latestTumorTypesCache.put(version, latestTumorTypes);
        }
        if (failedVersions.contains(requiredOncotreeVersion)) {
            logger.error("resetCache() -- failed to pull required oncotree version: " + requiredOncotreeVersion);
            throw new FailedCacheRefreshException("Failed to refresh cache");
        }
        if (latestTumorTypesCache.keySet().size() == 0) {
            logger.error("resetCache() -- failed to pull a single valid OncoTree version");
            throw new FailedCacheRefreshException("Failed to refresh cache");
        }
        if (failedVersions.size() > 0) {
            sendSlackNotification("OncoTree successfully recached `" + requiredOncotreeVersion + "`, but ran into issues with the following versions: " + String.join(", ", failedVersions));
        }

        tumorTypesCache = latestTumorTypesCache;
        if (failedOncoTreeVersionsCacheRefresh || failedVersionedOncoTreeNodesCacheRefresh) {
            throw new FailedCacheRefreshException("Failed to refresh cache");
        } else {
            dateOfLastCacheRefresh = new Date();
            logger.info("resetCache() -- successfully reset cache from repository");
        }
    }

    public Date getDateOfLastCacheRefresh() {
        return dateOfLastCacheRefresh;
    }

    public void setDateOfLastCacheRefresh(Date date) {
        dateOfLastCacheRefresh = date;
    }

    public  List<Version> getCachedVersions() {
        if (tumorTypesCache == null) {
            resetCache();
        }
        List<Version> cachedVersions = new ArrayList<>(tumorTypesCache.keySet());
        return cachedVersions;
    }

    public Map<String, TumorType> getTumorTypesByVersion(Version version) throws InvalidVersionException, FailedCacheRefreshException {
        if (tumorTypesCache == null) {
            logger.error("getTumorTypesByVersion() -- called on expired cache");
            throw new FailedCacheRefreshException("Cache has expired, resets must have failed");
        }
        if (logger.isDebugEnabled()) {
            for (Version cachedVersion : tumorTypesCache.keySet()) {
                logger.debug("getTumorTypesByVersion() -- tumorTypes cache contains '" + cachedVersion.getVersion() + "'");
            }
        }
        if (tumorTypesCache.containsKey(version)) {
            logger.debug("getTumorTypesByVersion() -- found '" + version.getVersion() + "' in cache");
            return getUnmodifiableTumorTypesByVersion(tumorTypesCache.get(version));
        } else {
            // TODO how would we even get here if we have been given a Version object? all known Versions are cached
            logger.debug("getTumorTypesByVersion() -- did NOT find '" + version.getVersion() + "' in cache, throwing exception");
            throw new FailedCacheRefreshException("Unknown version '" + version.getVersion() + "'");
        }
    }

    private Map<String, TumorType> getUnmodifiableTumorTypesByVersion(Map<String, TumorType> tumorTypeMap) {
        // code is modifying the returned tumor types, make a copy of everything
        Map<String, TumorType> unmodifiableTumorTypeMap = new HashMap<String, TumorType>(tumorTypeMap.size());
        for (Map.Entry<String, TumorType> entry : tumorTypeMap.entrySet()) {
            unmodifiableTumorTypeMap.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return Collections.unmodifiableMap(unmodifiableTumorTypeMap);
    }

    public boolean cacheIsStale() {
        ZonedDateTime currentDate = ZonedDateTime.now();
        ZonedDateTime dateOfCacheExpiration = currentDate.plusDays(- MAXIMUM_CACHE_AGE_IN_DAYS);
        if (dateOfLastCacheRefresh == null || dateOfLastCacheRefresh.toInstant().isBefore(dateOfCacheExpiration.toInstant())) {
            return true;
        } else {
            return false;
        }
    }

    private void sendSlackNotification(String message) {
        String payload = "payload={\"channel\": \"#msk-pipeline-logs\", \"username\": \"cbioportal_importer\", \"text\": \"" + message + "\", \"icon_emoji\": \":rotating_light:\"}";
        StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_FORM_URLENCODED);
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(slackURL);
        request.setEntity(entity);
        HttpResponse response = null;
        try {
            response = httpClient.execute(request);
        } catch (Exception e) {
            logger.error("failed to send slack notification -- cache is outdated and failed to refresh");
        }
    }

}
