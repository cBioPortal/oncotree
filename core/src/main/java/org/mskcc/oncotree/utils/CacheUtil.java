/*
 * Copyright (c) 2016 - 2020, 2024 Memorial Sloan-Kettering Cancer Center.
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
import org.mskcc.oncotree.graphite.OncoTreeNode;
import org.mskcc.oncotree.graphite.GraphiteException;

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
    private SlackUtil slackUtil;

    @Autowired
    private TumorTypesUtil tumorTypesUtil;

    @Autowired
    private OncoTreePersistentCache oncoTreePersistentCache;

    @Value("${required.oncotree.version:oncotree_latest_stable}")
    private String requiredOncotreeVersion;

    // Starts immediately, calls 10 minutes after previous execution ends (prevents any overlapping attempts to reset cache)
    @Scheduled(fixedDelay = 600000, initialDelay = 0)
    private void scheduleResetCache() {
        if (tumorTypesCache == null || cacheIsStale()) {
            try {
                resetCache();
            } catch (FailedCacheRefreshException e) {
                slackUtil.sendSlackNotification("*URGENT: OncoTree Error* - an attempt to refresh an outdated or null cache failed." + e.getMessage());
            }
        }
    }

    public void resetCache() throws FailedCacheRefreshException {
        logger.info("resetCache() -- refilling tumor types cache");
        Map<Version, Map<String, TumorType>> latestTumorTypesCache = new HashMap<>();
        ArrayList<Version> oncoTreeVersions = new ArrayList<Version>();
        ArrayList<String> failedVersions = new ArrayList<String>();
        // use this to store and look up previous oncoTree codes
        HashMap<String, ArrayList<String>> internalIdsToOncotreeCodes = new HashMap<String, ArrayList<String>>();

        boolean failedOncoTreeVersionsCacheRefresh = false;
        boolean failedVersionedOncoTreeNodesCacheRefresh = false;
        // update EHCache with newest versions available
        try {
            oncoTreePersistentCache.updateOncoTreeVersionsInPersistentCache();
        } catch (GraphiteException exception) {
            logger.error("resetCache() -- failed to pull versions from repository");
            failedOncoTreeVersionsCacheRefresh = true;
        }
        // attmpt to get versions from EHCache -- RuntimeException thrown when ALL options are exhausted (ehcache, graphite, backup)
        try {
            oncoTreeVersions = oncoTreePersistentCache.getOncoTreeVersionsFromPersistentCache();
        } catch (RuntimeException e) {
            throw new FailedCacheRefreshException("No data found in specified backup cache location...");
        }
        if (!failedOncoTreeVersionsCacheRefresh) {
            try {
                oncoTreePersistentCache.backupOncoTreeVersionsPersistentCache(oncoTreeVersions);
            } catch (Exception e) {
                logger.error("Unable to backup versions EHCache");
                slackUtil.sendSlackNotification("*OncoTree Error* - OncoTreeVersionsCache backup failed." + e.getMessage());
            }
        }

        // versions are ascending by release date
        for (Version version : oncoTreeVersions) {
            Map<String, TumorType> latestTumorTypes = new HashMap<String, TumorType>();
            ArrayList<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>();
            failedVersionedOncoTreeNodesCacheRefresh = false;
            if (version != null) {
                try {
                    // this returns something but we don't need it -- it should be in the cache now, but to debug I will save it
                    ArrayList<OncoTreeNode> tmpOncoTreeNodes = oncoTreePersistentCache.updateOncoTreeNodesInPersistentCache(version);
                } catch (GraphiteException e) {
                    logger.error("resetCache() -- failed to pull tumor types for version '" + version.getVersion() + "' from repository : " + e.toString());
                    failedVersionedOncoTreeNodesCacheRefresh = true;
                }
                // attempt to get nodes from EHCache -- RuntimeException thrown when ALL options are exhausted (ehcache, graphite, backup)
                // store versions for which nodes cannot be successfully loaded (either due to inaccessible data or invalid data)
                try {
                    oncoTreeNodes = oncoTreePersistentCache.getOncoTreeNodesFromPersistentCache(version);
                } catch (RuntimeException e) {
                    failedVersions.add(version.getVersion());
                    logger.warn("resetCache() -- failed to retrieve version '" + version.getVersion() + "'");
                    continue;
                }
                if (!failedVersionedOncoTreeNodesCacheRefresh) {
                    try {
                        oncoTreePersistentCache.backupOncoTreeNodesPersistentCache(oncoTreeNodes, version);
                    } catch (Exception e) {
                        logger.error("Unable to backup oncotree nodes EHCache");
                        slackUtil.sendSlackNotification("*OncoTree Error* - OncoTreeNodesCache backup failed." + e.getMessage());
                    }
                }
                try {
                    latestTumorTypes = tumorTypesUtil.getAllTumorTypesFromOncoTreeNodes(oncoTreeNodes, version, internalIdsToOncotreeCodes);
                } catch (InvalidOncoTreeDataException exception) {
                    logger.error("Unable to get tumor types from oncotree nodes");
                    failedVersions.add(version.getVersion());
                    logger.warn("resetCache() -- failed to retrieve version : " + version.getVersion() + " : " + exception.toString());
                    continue;
                }
            }
            latestTumorTypesCache.put(version, latestTumorTypes);
        }
        // Fail the cache refresh if required oncotree version cannot be constructed or if no versions can be constructed
        if (latestTumorTypesCache.keySet().size() == 0) {
            logger.error("resetCache() -- failed to pull a single valid OncoTree version");
            throw new FailedCacheRefreshException("Failed to refresh cache - no versions constructed");
        }
        if (failedVersions.contains(requiredOncotreeVersion)) {
            logger.error("resetCache() -- failed to pull required oncotree version: " + requiredOncotreeVersion);
            throw new FailedCacheRefreshException("Failed to refresh cache - required version not constructed");
        }
        if (failedVersions.size() > 0) {
            slackUtil.sendSlackNotification("OncoTree successfully recached `" + requiredOncotreeVersion + "`, but ran into issues with the following versions: " + String.join(", ", failedVersions));
        }

        tumorTypesCache = latestTumorTypesCache;
        // cache is filled, but indicate to endpoint that we did not successfully pull updated data from Graphite
        if (failedOncoTreeVersionsCacheRefresh || failedVersionedOncoTreeNodesCacheRefresh) {
            throw new FailedCacheRefreshException("Failed to refresh cache - composite error");
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

    public List<Version> getCachedVersions() {
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
}
