/*
 * Copyright (c) 2019 Memorial Sloan-Kettering Cancer Center.
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
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OncoTreePersistentCache {

    // TODO should this be a templated class somehow because we are doing the same stuff for 3 kinds of objects
    // TODO for users of this class getting from the cache and then from backup is complicated
    // make a method that is just a getBlah() which attempts to get from backup too

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
        return oncoTreeRepository.getOncoTree(version);
    }

    @Cacheable(value = "oncoTreeVersionsEHCache", key = "#root.target.ONCOTREE_VERSIONS_CACHE_KEY", unless = "#result==null")
    public ArrayList<Version> getOncoTreeVersionsFromPersistentCache() {
        return oncoTreeVersionRepository.getOncoTreeVersions();
    }

    @Cacheable(value = "mskConceptEHCache", key = "#oncoTreeCode", unless = "#result==null")
    public MSKConcept getMSKConceptFromPersistentCache(String oncoTreeCode) {
        // if crosswalk can't find it, then return emtpy MSKConcept
        // else if crosswalk had other error, get from backup
        return crosswalkRepository.getByOncotreeCode(oncoTreeCode);
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

    @CachePut(value = "mskConceptEHCache", key = "#oncoTreeCode", unless = "#result==null")
    public MSKConcept updateMSKConceptInPersistentCache(String oncoTreeCode) {
        logger.info("updating EHCache with updated MSKConcept from Crosswalk");
        return crosswalkRepository.getByOncotreeCode(oncoTreeCode);
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
    public synchronized ArrayList<OncoTreeNode> getOncoTreeNodesFromPersistentCacheBackup(Version version) throws Exception {
        CacheManager cacheManager = null;
        Cache cache = null;
        ArrayList<OncoTreeNode> oncoTreeNodes = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(ONCOTREE_NODES_CACHE);
            oncoTreeNodes = (ArrayList<OncoTreeNode>) cache.get(version.getVersion());
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
        return oncoTreeNodes;
    }

    @Cacheable(value = "oncoTreeVersionsEHCache", key = "#root.target.ONCOTREE_VERSIONS_CACHE_KEY", unless = "#result==null")
    public synchronized ArrayList<Version> getOncoTreeVersionsFromPersistentCacheBackup() throws Exception {
        CacheManager cacheManager = null;
        Cache cache = null;
        ArrayList<Version> versions = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(ONCOTREE_VERSIONS_CACHE);
            versions = (ArrayList<Version>) cache.get(ONCOTREE_VERSIONS_CACHE_KEY);
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
        return versions;
    }

    @Cacheable(value = "mskConceptEHCache", key = "#oncoTreeCode", unless = "#result==null")
    public synchronized MSKConcept getMSKConceptFromPersistentCacheBackup(String oncoTreeCode) throws Exception {
        CacheManager cacheManager = null;
        Cache cache = null;
        MSKConcept mskConcept = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(MSKCONCEPT_CACHE);
            mskConcept = (MSKConcept) cache.get(oncoTreeCode);
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
        Cache cache = null;
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
        Cache cache = null;
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
        Cache cache = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(MSKCONCEPT_CACHE);
            cache.put(oncoTreeCode, mskConcept);
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
    }

    public synchronized void backupMSKConceptPersistentCache(Map<String, MSKConcept> oncoTreeCodesToMSKConcepts) throws Exception {
        CacheManager cacheManager = null;
        Cache cache = null;
        try {
            cacheManager = getCacheManager(BACKUP_CACHE_CONFIG_FILENAME);
            cache = cacheManager.getCache(MSKCONCEPT_CACHE);
            cache.putAll(oncoTreeCodesToMSKConcepts);
        } finally {
            if (cacheManager != null) {
                cacheManager.close(); // closes all caches it knows about
            }
        }
    }
}
