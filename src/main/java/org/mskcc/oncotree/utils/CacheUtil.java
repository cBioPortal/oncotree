package org.mskcc.oncotree.utils;

import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hongxin on 2/25/16.
 */
public class CacheUtil {
    public static Map<Version, Map<String, TumorType>> tumorTypes = new HashMap<>();

    /**
     * Get most up to date tumor types
     *
     * @return the tumor types
     */
    public static Map<String, TumorType> getTumorTypes() {
        return TumorTypesUtil.getTumorTypes();
    }


    public static Map<String, TumorType> getTumorTypesByVersion(String v) {
        if (VersionUtil.hasVersion(v)) {
            Version version = VersionUtil.getVersion(v);
            if (tumorTypes.containsKey(v)) {
                return tumorTypes.get(v);
            } else {
                tumorTypes.put(version, TumorTypesUtil.getTumorTypesByVersion(version));
                return tumorTypes.get(version);
            }
        } else {
            return null;
        }
    }
}
