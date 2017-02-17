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
        put("oncokb", new Version("oncokb", "ed012f404608679d3b4dfc84f952bb2dac506b91"));
        put("genie", new Version("oncokb", "a2560624a692ab99a610ab356e12b4e07cb1bd53"));
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
