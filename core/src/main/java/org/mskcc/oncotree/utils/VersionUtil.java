package org.mskcc.oncotree.utils;

import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.error.InvalidVersionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Hongxin on 2/25/16.
 */
@Component
public class VersionUtil {

    public static final String DEFAULT_VERSION = "oncotree_latest_stable";

    @Autowired
    private CacheUtil cacheUtil;

    public List<Version> getVersions() {
        return cacheUtil.getCachedVersions();
    }

    public Version getVersion(String version) throws InvalidVersionException {
        if (version != null && version.trim() != "") {
            for (Version v : getVersions()) {
                if (v.getVersion().equals(version)) {
                    return v;
                }
            }
        } else {
            throw new InvalidVersionException("'' is not a valid version.");
        }
        throw new InvalidVersionException("'" + version + "' is not a valid version.");
    }

    public Version getDefaultVersion() throws InvalidVersionException {
        // note we will throw an InvalidVersionException if this is not found in Graphite
        return getVersion(DEFAULT_VERSION);
    }
}
