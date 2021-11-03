/*
 * Copyright (c) 2019 - 2020 Memorial Sloan-Kettering Cancer Center.
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

import java.util.ArrayList;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;

import org.mskcc.oncotree.crosswalk.CrosswalkRepository;
import org.mskcc.oncotree.crosswalk.MSKConcept;

import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.TopBraidException;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;
import org.mskcc.oncotree.error.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OncoTreePersistentCache {

    // TODO should this be a templated class somehow because we are doing the same stuff for 3 kinds of objects
    private final static Logger logger = LoggerFactory.getLogger(OncoTreePersistentCache.class);

    @Autowired
    private CachingProvider cachingProvider;

    @Autowired
    private OncoTreeRepository oncoTreeRepository;

    @Autowired
    private OncoTreeVersionRepository oncoTreeVersionRepository;

    @Autowired
    private CrosswalkRepository crosswalkRepository;

    private static final String ONCOTREE_NODES_CACHE = "oncoTreeNodesEHCache";
    private static final String ONCOTREE_VERSIONS_CACHE = "oncoTreeVersionsEHCache";
    private static final String MSKCONCEPT_CACHE = "mskConceptEHCache";
    public static final String ONCOTREE_VERSIONS_CACHE_KEY = "ONCOTREE_VERSIONS_CACHE_KEY";
    private static final String BACKUP_CACHE_CONFIG_FILENAME = "ehcache_backup.xml";

    public CacheManager getCacheManager(String ehcacheXMLFilename) throws Exception {
        CacheManager cacheManager = cachingProvider.getCacheManager(getClass().getClassLoader().getResource(ehcacheXMLFilename).toURI(), getClass().getClassLoader());
        return cacheManager;
    }

    // retrieve cached TopBraid responses from default EHCache location
    @Cacheable(value = "oncoTreeNodesEHCache", key = "#version.version", unless = "#result==null")
    public ArrayList<OncoTreeNode> getOncoTreeNodesFromPersistentCache(Version version) {
        ArrayList<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>();
        try {
            oncoTreeNodes = oncoTreeRepository.getOncoTree(version);
        } catch (TopBraidException e) {
            logger.error("Unable to get oncotree nodes from TopBraid... attempting to read from backup.");
            try {
                oncoTreeNodes = getOncoTreeNodesFromPersistentCacheBackup(version);
                if (oncoTreeNodes == null) {
                    logger.error("No oncotree nodes were found in backup.");
                    throw new RuntimeException("No oncotree nodes were found in backup.");
                }
            } catch (Exception e2) {
                logger.error("Unable to read oncotree nodes from backup.");
                throw new RuntimeException(e2);
            }
        }
        return oncoTreeNodes;
    }

    @Cacheable(value = "oncoTreeVersionsEHCache", key = "#root.target.ONCOTREE_VERSIONS_CACHE_KEY", unless = "#result==null")
    public ArrayList<Version> getOncoTreeVersionsFromPersistentCache() {
        ArrayList<Version> versions = new ArrayList<Version>();
        try {
            versions = oncoTreeVersionRepository.getOncoTreeVersions();
        } catch (TopBraidException e) {
            logger.error("Unable to get versions from TopBraid... attempting to read from backup.");
            try {
                versions = getOncoTreeVersionsFromPersistentCacheBackup();
                if (versions == null) {
                    logger.error("No versions were found in backup.");
                    throw new RuntimeException("No versions were found in backup.");
                }
            } catch (Exception e2) {
                logger.error("Unable to read versions from backup.");
                throw new RuntimeException(e2);
            }
        }
        return versions;
    }

    /* this function will ALWAYS return an MSKConcept object (may be empty)
     * is not required for application startup
     */
    @Cacheable(value = "mskConceptEHCache", key = "#oncoTreeCode", unless = "#result==null")
    public MSKConcept getMSKConceptFromPersistentCache(String oncoTreeCode) {
        // commented exception handling logic kept here for future crosswalk implementation
        // try {
        MSKConcept mskConcept = crosswalkRepository.getByOncotreeCode(oncoTreeCode);
        // } catch (CrosswalkExeption e) {
        //     logger.error("Unable to get MSKConcept from Crosswalk... attempting to read from backup.");
        //     try {
        //         mskConcept = getMSKConceptFromPersistentCacheBackup(oncoTreeCode);
        //         if (mskConcept == null) {
        //             mskConcept = new MSKConcept();
        //         }
        //     } catch (Exception e2) {}
        // }
        if (mskConcept != null) {
            return mskConcept;
        }
        return new MSKConcept();
    }

    @CachePut(value = "oncoTreeNodesEHCache", key = "#version.version", unless = "#result==null")
    public ArrayList<OncoTreeNode> updateOncoTreeNodesInPersistentCache(Version version) {
        logger.info("updating EHCache with updated oncotree nodes from TopBraid for version " + version.getVersion());
        return oncoTreeRepository.getOncoTree(version);
    }

    @CachePut(value = "oncoTreeVersionsEHCache", key = "#root.target.ONCOTREE_VERSIONS_CACHE_KEY", unless = "#result==null")
    public ArrayList<Version> updateOncoTreeVersionsInPersistentCache() {
        logger.info("updating EHCache with updated versions from TopBraid");
        return oncoTreeVersionRepository.getOncoTreeVersions();
    }

    // Updating MSKConcepts
    @CachePut(value = "mskConceptEHCache", key = "#oncoTreeCode", unless = "#result==null")
    public MSKConcept updateMSKConceptInPersistentCache(String oncoTreeCode) {
        logger.info("updating EHCache with updated MSKConcept from Crosswalk");
        MSKConcept mskConcept = crosswalkRepository.getByOncotreeCode(oncoTreeCode);
        if (mskConcept != null) {
            return mskConcept;
        }
        return new MSKConcept();
    }

    /*
        All methods using the backup cache manager should be synchronized on the same object.
        In this case, we synchronize on the instance of this class.  This is so only one
        thread is modifying the caches defined in BACKUP_CACHE_CONFIG_FILENAME at a time.
        These methods close all caches known to the cacheManager.  You wouldn't want to
        be using a cache that another method (or the same method) is closing.
    */

    // retrieve cache TopBraid responses from backup EHCache location (and re-populate default EHCache locatin)
    @Cacheable(value = "oncoTreeNodesEHCache", key = "#version.version", unless = "#result==null")
    private synchronized ArrayList<OncoTreeNode> getOncoTreeNodesFromPersistentCacheBackup(Version version) throws Exception {
        CacheManager cacheManager = null;
        Cache<String, ArrayList<OncoTreeNode>> cache = null;
        ArrayList<OncoTreeNode> oncoTreeNodes = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(ONCOTREE_NODES_CACHE);
            oncoTreeNodes = cache.get(version.getVersion());
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
        return oncoTreeNodes;
    }

    @Cacheable(value = "oncoTreeVersionsEHCache", key = "#root.target.ONCOTREE_VERSIONS_CACHE_KEY", unless = "#result==null")
    private synchronized ArrayList<Version> getOncoTreeVersionsFromPersistentCacheBackup() throws Exception {
        CacheManager cacheManager = null;
        Cache<String, ArrayList<Version>>  cache = null;
        ArrayList<Version> versions = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(ONCOTREE_VERSIONS_CACHE);
            versions = cache.get(ONCOTREE_VERSIONS_CACHE_KEY);
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
        return versions;
    }

    @Cacheable(value = "mskConceptEHCache", key = "#oncoTreeCode", unless = "#result==null")
    private synchronized MSKConcept getMSKConceptFromPersistentCacheBackup(String oncoTreeCode) throws Exception {
        CacheManager cacheManager = null;
        Cache<String, MSKConcept> cache = null;
        MSKConcept mskConcept = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(MSKCONCEPT_CACHE);
            mskConcept = cache.get(oncoTreeCode);
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
        return mskConcept;
    }

    // update backup EHCache location with modeled-object cache values
    public synchronized void backupOncoTreeNodesPersistentCache(ArrayList<OncoTreeNode> oncoTreeNodes, Version version) throws Exception {
        CacheManager cacheManager = null;
        Cache<String, ArrayList<OncoTreeNode>> cache = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(ONCOTREE_NODES_CACHE);
            cache.put(version.getVersion(), oncoTreeNodes);
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
    }

    public synchronized void backupOncoTreeVersionsPersistentCache(ArrayList<Version> versions) throws Exception {
        CacheManager cacheManager = null;
        Cache<String, ArrayList<Version>> cache = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(ONCOTREE_VERSIONS_CACHE);
            cache.put(ONCOTREE_VERSIONS_CACHE_KEY, versions);
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
    }

    public synchronized void backupMSKConceptPersistentCache(MSKConcept mskConcept, String oncoTreeCode) throws Exception {
        CacheManager cacheManager = null;
        Cache<String, MSKConcept> cache = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(MSKCONCEPT_CACHE);
            cache.put(oncoTreeCode, mskConcept);
        } catch (Exception e) {
            logger.warn("exception in mskConcept backup " + mskConcept + " exception: " + e);
            throw e;
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
    }

    public synchronized void backupMSKConceptPersistentCache(Map<String, MSKConcept> oncoTreeCodesToMSKConcepts) throws Exception {
        CacheManager cacheManager = null;
        Cache<String, MSKConcept> cache = null;
        try {
            if (oncoTreeCodesToMSKConcepts == null) {
                System.out.println("null argument passed to backupMSKConceptPersistentCache");
                return;
            }
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(MSKCONCEPT_CACHE);
            cache.putAll(oncoTreeCodesToMSKConcepts);
        } catch (Exception e) {
            logger.warn("exception in oncotreeCocdesToMSKConcepts -- exception: " + e);
            throw e;
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
    }
}
