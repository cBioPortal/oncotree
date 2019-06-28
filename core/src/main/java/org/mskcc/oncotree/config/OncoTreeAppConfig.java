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

package org.mskcc.oncotree.config;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import org.ehcache.jsr107.EhcacheCachingProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author averyniceday
 */
@Configuration
@EnableCaching
public class OncoTreeAppConfig {

    @Bean
    public CachingProvider cachingProvider() throws Exception {
        CachingProvider cachingProvider = new EhcacheCachingProvider();
        return cachingProvider;
    }

    @Bean(destroyMethod = "close")
    public CacheManager oncoTreeCacheManager() throws Exception {
        return cachingProvider().getCacheManager(getClass().getClassLoader().getResource("ehcache.xml").toURI(),
            getClass().getClassLoader());
    }

    @Bean
    public JCacheCacheManager jCacheCacheManager() throws Exception {
        return new JCacheCacheManager(oncoTreeCacheManager());
    }

}

