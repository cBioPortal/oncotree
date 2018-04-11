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

import javax.annotation.PostConstruct;

import org.mskcc.oncotree.error.InvalidVersionException;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.TopBraidException;
import org.mskcc.oncotree.utils.VersionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by Hongxin on 2/25/16.
 */
@Component
@EnableScheduling
public class CacheUtil {

    private static final Logger logger = LoggerFactory.getLogger(CacheUtil.class);

    private static Map<Version, Map<String, TumorType>> tumorTypes = null;
    private static Date lastSuccessfullRefresh = null;

    @PostConstruct // call when constructed
    @Scheduled(cron="0 0 11 * * *") // reset at 11am every day
    private void scheduleResetCache() {
        // TODO make sure we don't have two scheduled calls run simultaneously
        try {
            resetCache();
        } catch (TopBraidException exception) {
            // check to see if we should expire cache 
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime threeDaysAgo = now.plusDays(-3);
            if (lastSuccessfullRefresh != null && lastSuccessfullRefresh.toInstant().isBefore(threeDaysAgo.toInstant())) {
                logger.error("scheduleResetCache() -- failed to reset cache and cache is older than '" + threeDaysAgo + "' in cache");
                expireCache();
            } 
        }
    }

    public static Map<String, TumorType> getTumorTypesByVersion(Version version) throws InvalidVersionException, TopBraidException {
        if (tumorTypes == null) {
            logger.error("getTumorTypesByVersion() -- called on expired cache");
            throw new TopBraidException("Cache has expired, resets must have failed");
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
            logger.debug("getTumorTypesByVersion() -- did NOT find '" + version.getVersion() + "' in cache, getting now");
            tumorTypes.put(version, TumorTypesUtil.getTumorTypesByVersionFromRaw(version));
            return getUnmodifiableTumorTypesByVersion(tumorTypes.get(version));
        }
    }

    private static Map<String, TumorType> getUnmodifiableTumorTypesByVersion(Map<String, TumorType> tumorTypeMap) {
        // code is modifying the returned tumor types, make a copy of everything
        Map<String, TumorType> unmodifiableTumorTypeMap = new HashMap<String, TumorType>(tumorTypeMap.size());
        for (Map.Entry<String, TumorType> entry : tumorTypeMap.entrySet()) {
            unmodifiableTumorTypeMap.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return Collections.unmodifiableMap(unmodifiableTumorTypeMap);
    }

    public static void resetCache() throws TopBraidException {
        logger.info("resetCache() -- refilling tumor types cache");
        tumorTypes = new HashMap<>();
        Map<Version, Map<String, TumorType>> latestTumorTypes = new HashMap<>();
        try {
            List<Version> versions = VersionUtil.getVersions();
        } catch (TopBraidException exception) {
            logger.error("resetCache() -- failed to pull versions from repository");
            throw exception;
        }
        for (Version version : VersionUtil.getVersions()) {
            try {
                latestTumorTypes.put(version, TumorTypesUtil.getTumorTypesByVersionFromRaw(version));
            } catch (TopBraidException exception) {
                logger.error("resetCache() -- failed to pull tumor types for version '" + version.getVersion() + "' from repository");
                throw exception;
            }
        }
        logger.info("resetCache() -- successfully reset cache from repository");
        tumorTypes = latestTumorTypes;
        lastSuccessfullRefresh = new Date();
    }

    private static void expireCache() {
        logger.debug("expireCache() -- expiring cache");
        tumorTypes = null;
        lastSuccessfullRefresh = null;
    }

}
