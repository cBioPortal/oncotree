package org.mskcc.oncotree.utils;

import org.mskcc.oncotree.model.Version;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Hongxin on 2/25/16.
 */
public class VersionUtil {
    private static final Map<String, Version> VERSIONS = new HashMap<String, Version>() {{
        put("1", new Version("1", "369b74c599ebefdb71bb25a85cb877be954a0928"));
        put("1.1", new Version("1.1", "52a743f6a3493d1cb46eca2a3e12e7f92225558d"));
        put("oncokb", new Version("oncokb", "a41b4b38aeabbcf0ae2a6414ee90add1fd72468d"));
        put("genie", new Version("genie", "15f41e75625f1e705470c4e4450cc63dcd2b2c8b"));
        put("realtime", new Version("realtime", "realtime"));
    }};

    public static Version getVersion(String version) {
        return VERSIONS.get(version);
    }

    public static Version getVersionOrRealtime(String version) {
        return VERSIONS.get(version) == null ? VERSIONS.get("realtime") : VERSIONS.get(version);
    }

    public static Set<Version> getVersions() {
        Set<Version> versions = new HashSet<>();
        for (Map.Entry<String, Version> entry : VERSIONS.entrySet()) {
            versions.add(entry.getValue());
        }
        return versions;
    }

    public static Boolean hasVersion(String version) {
        return VERSIONS.containsKey(version);
    }
}
