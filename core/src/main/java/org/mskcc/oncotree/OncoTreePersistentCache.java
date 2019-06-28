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

import java.util.List;
import java.util.ArrayList;

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
        return crosswalkRepository.getByOncotreeCode(oncoTreeCode);
    }

    // retrieve cache TopBraid responses from backup EHCache location (and re-populate default EHCache locatin)
    @Cacheable(value = "oncoTreeNodesEHCache", key = "#version.version", unless = "#result==null")
    public ArrayList<OncoTreeNode> getOncoTreeNodesFromPersistentCacheBackup(Version version) throws Exception {
        CacheManager backupCacheManager = getCacheManager("ehcache_backup.xml");
        ArrayList<OncoTreeNode> oncoTreeNodes = (ArrayList<OncoTreeNode>)backupCacheManager.getCache(ONCOTREE_NODES_CACHE).get(version.getVersion());
        backupCacheManager.close();
        return oncoTreeNodes;
    }

    @Cacheable(value = "oncoTreeVersionsEHCache", key = "#root.target.ONCOTREE_VERSIONS_CACHE_KEY", unless = "#result==null")
    public ArrayList<Version> getOncoTreeVersionsFromPersistentCacheBackup() throws Exception {
        CacheManager backupCacheManager = getCacheManager("ehcache_backup.xml");
        ArrayList<Version> versions = (ArrayList<Version>)backupCacheManager.getCache(ONCOTREE_VERSIONS_CACHE).get(ONCOTREE_VERSIONS_CACHE_KEY);
        backupCacheManager.close();
        return versions;
    }

    @Cacheable(value = "mskConceptEHCache", key = "#oncoTreeCode", unless = "#result==null")
    public MSKConcept getMSKConceptFromPersistentCacheBackup(String oncoTreeCode) throws Exception {
        CacheManager backupCacheManager = getCacheManager("ehcache_backup.xml");
        MSKConcept mskConcept = (MSKConcept)backupCacheManager.getCache(MSKCONCEPT_CACHE).get(oncoTreeCode);
        backupCacheManager.close();
        return mskConcept;
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

    // update backup EHCache location with modeled-object cache values
    public void backupOncoTreeNodesPersistentCache(ArrayList<OncoTreeNode> oncoTreeNodes, Version version) throws Exception {
        CacheManager cacheManager = getCacheManager("ehcache_backup.xml");
        cacheManager.getCache(ONCOTREE_NODES_CACHE).put(version.getVersion(), oncoTreeNodes);
        cacheManager.close();
    }

    public void backupOncoTreeVersionsPersistentCache(ArrayList<Version> versions) throws Exception {
        CacheManager cacheManager = getCacheManager("ehcache_backup.xml");
        cacheManager.getCache(ONCOTREE_VERSIONS_CACHE).put(ONCOTREE_VERSIONS_CACHE_KEY, versions);
        cacheManager.close();
    }

    public void backupMSKConceptPersistentCache(MSKConcept mskConcept, String oncoTreeCode) throws Exception {
        CacheManager cacheManager = getCacheManager("ehcache_backup.xml");
        cacheManager.getCache(MSKCONCEPT_CACHE).put(oncoTreeCode, mskConcept);
        cacheManager.close();
    }
}

