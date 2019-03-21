/*
 * Copyright (c) 2016-2018 Memorial Sloan-Kettering Cancer Center.
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

import javax.annotation.PostConstruct;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;

import org.mskcc.oncotree.crosswalk.MSKConceptCache;
import org.mskcc.oncotree.crosswalk.MSKConcept;
import org.mskcc.oncotree.error.InvalidVersionException;
import org.mskcc.oncotree.error.InvalidOncoTreeDataException;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.TopBraidException;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;
import org.mskcc.oncotree.utils.FailedCacheRefreshException;
import org.mskcc.oncotree.utils.VersionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private static Map<Version, Map<String, TumorType>> tumorTypes = null;
    private Date dateOfLastCacheRefresh = null;
    public static final Integer MAXIMUM_CACHE_AGE_IN_DAYS = 3;

    @Autowired
    private OncoTreeVersionRepository oncoTreeVersionRepository;

    @Autowired
    private TumorTypesUtil tumorTypesUtil;

    @Value("${slack.url}")
    private String slackURL;

    @Value("${required.oncotree.version:oncotree_latest_stable}")
    private String requiredOncotreeVersion;
 
    @PostConstruct // call when constructed
    @Scheduled(cron="0 */10 * * * *") // call every 10 minutes
    private void scheduleResetCache() {
        // TODO make sure we don't have two scheduled calls run simultaneously
        if (tumorTypes == null || cacheIsStale()) {
            try {
                resetCache();
            } catch (FailedCacheRefreshException e) {
                sendSlackNotification("*URGENT: OncoTree Error* - an attempt to refresh an outdated or null cache failed.");
            }
        }
    }

    public Date getDateOfLastCacheRefresh() {
        return dateOfLastCacheRefresh;
    }

    public void setDateOfLastCacheRefresh(Date date) {
        dateOfLastCacheRefresh = date;
    }

    public Map<String, TumorType> getTumorTypesByVersion(Version version) throws InvalidVersionException, FailedCacheRefreshException {
        if (tumorTypes == null) {
            logger.error("getTumorTypesByVersion() -- called on expired cache");
            throw new FailedCacheRefreshException("Cache has expired, resets must have failed");
        }
        if (logger.isDebugEnabled()) {
            for (Version cachedVersion : tumorTypes.keySet()) {
                logger.debug("getTumorTypesByVersion() -- tumorTypes cache contains '" + cachedVersion.getVersion() + "'");
            }
        }
        if (tumorTypes.containsKey(version)) {
            logger.debug("getTumorTypesByVersion() -- found '" + version.getVersion() + "' in cache");
            return getUnmodifiableTumorTypesByVersion(tumorTypes.get(version));
        } else {
            // TODO how would we even get here if we have been given a Version object? all known Versions are cached
            logger.debug("getTumorTypesByVersion() -- did NOT find '" + version.getVersion() + "' in cache, throwing exception");
            throw new FailedCacheRefreshException("Unknown version '" + version.getVersion() + "'");
        }
    }

    public  List<Version> getCachedVersions() {
        if (tumorTypes == null) {
            resetCache();
        }
        List<Version> cachedVersions = new ArrayList<>(tumorTypes.keySet());
        return cachedVersions;
    }

    private Map<String, TumorType> getUnmodifiableTumorTypesByVersion(Map<String, TumorType> tumorTypeMap) {
        // code is modifying the returned tumor types, make a copy of everything
        Map<String, TumorType> unmodifiableTumorTypeMap = new HashMap<String, TumorType>(tumorTypeMap.size());
        for (Map.Entry<String, TumorType> entry : tumorTypeMap.entrySet()) {
            unmodifiableTumorTypeMap.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return Collections.unmodifiableMap(unmodifiableTumorTypeMap);
    }

    public void resetCache() throws FailedCacheRefreshException {
        logger.info("resetCache() -- refilling tumor types cache");
        Map<Version, Map<String, TumorType>> latestTumorTypes = new HashMap<>();
        List<String> failedVersions = new ArrayList<String>();
        try {
            List<Version> versions = oncoTreeVersionRepository.getOncoTreeVersions();
        } catch (TopBraidException exception) {
            logger.error("resetCache() -- failed to pull versions from repository");
            throw new FailedCacheRefreshException("Failed to refresh cache");
        }
        // use this to store and look up previous oncoTree codes
        HashMap<String, ArrayList<String>> topBraidURIsToOncotreeCodes = new HashMap<String, ArrayList<String>>();
        // versions are ascending by release date
        for (Version version : oncoTreeVersionRepository.getOncoTreeVersions()) {
            try {
                latestTumorTypes.put(version, tumorTypesUtil.getTumorTypesByVersionFromRaw(version, topBraidURIsToOncotreeCodes));
            } catch (TopBraidException | InvalidOncoTreeDataException exception) {
                logger.error("resetCache() -- failed to pull tumor types for version '" + version.getVersion() + "' from repository");
                failedVersions.add(version.getVersion());
                if (version.equals(requiredOncotreeVersion)) {
                    throw new FailedCacheRefreshException("Failed to refresh cache");
                }
            }
        }
        if (latestTumorTypes.keySet().size() == 0) {
            logger.error("resetCache() -- failed to pull a single valid OncoTree version");
            throw new FailedCacheRefreshException("Failed to refresh cache");
        }
        if (failedVersions.size() > 0) {
            sendSlackNotification("OncoTree successfully recached `" + requiredOncotreeVersion + "`, but ran into issues with the following versions: " + String.join(", ", failedVersions));
        }
        logger.info("resetCache() -- successfully reset cache from repository");
        tumorTypes = latestTumorTypes;
        dateOfLastCacheRefresh = new Date();
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
