package org.mskcc.oncotree.utils;

import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.error.InvalidVersionException;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Hongxin on 2/25/16.
 */
@Component
public class VersionUtil {

    public static final String DEFAULT_VERSION = "oncotree_latest_stable";
    private static OncoTreeVersionRepository oncoTreeVersionRepository;
    @Autowired
    public void setOncoTreeVersionRepository(OncoTreeVersionRepository property) { oncoTreeVersionRepository = property; }

    public static List<Version> getVersions() {
        return oncoTreeVersionRepository.getOncoTreeVersions();
    }

    public static Version getVersion(String version) throws InvalidVersionException {
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

    public static Version getDefaultVersion() throws InvalidVersionException {
        // note we will throw an InvalidVersionException if this is not found in TopBraid
        return getVersion(DEFAULT_VERSION);
    }
}
