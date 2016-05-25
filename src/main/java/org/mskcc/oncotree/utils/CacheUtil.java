package org.mskcc.oncotree.utils;

import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hongxin on 2/25/16.
 */
public class CacheUtil {
    public static Map<Version, List<MainType>> mainTypes = new HashMap<>();
    public static Map<Version, Map<String, TumorType>> tumorTypes = new HashMap<>();

    public static void resetTumorTypesByVersion(Version version) {
        if (tumorTypes.containsKey(version)) {
            tumorTypes.get(version).clear();
        } else {
            tumorTypes.put(version, new HashMap<>());
        }
    }

    public static Map<String, TumorType> getOrResetTumorTypesByVersion(Version version) {
        if(version != null) {
            if(version.getVersion() == "realtime") {
                tumorTypes.remove(version);
            }
            return getTumorTypesByVersion(version);
        }else {
            return new HashMap<>();
        }
    }

    public static Map<String, TumorType> getTumorTypesByVersion(Version version) {
        if (tumorTypes.containsKey(version)) {
            return tumorTypes.get(version);
        } else {
            tumorTypes.put(version, TumorTypesUtil.getTumorTypesByVersionFromRaw(version));
            return tumorTypes.get(version);
        }
    }

    public static List<MainType> getMainTypesByVersion(Version version) {
        if (mainTypes.containsKey(version)) {
            return mainTypes.get(version);
        } else {
            mainTypes.put(version, new ArrayList<>());
            Map<String, TumorType> tumorTypeMap = TumorTypesUtil.getTumorTypesByVersionFromRaw(version);
            return mainTypes.get(version);
        }
    }

    public static void addMainTypeByVersion(Version version, MainType mainType) {
        if (!mainTypes.containsKey(version)) {
            mainTypes.put(version, new ArrayList<>());
        }
        mainTypes.get(version).add(mainType);
    }

    public static void resetMainTypesByVersion(Version version) {
        if (mainTypes.containsKey(version)) {
            mainTypes.get(version).clear();
        } else {
            mainTypes.put(version, new ArrayList<>());
        }
    }

    public static MainType getMainTypeByVersion(Version version, Integer id) {
        List<MainType> selectedMainTypes = mainTypes.get(version);
        if (selectedMainTypes != null) {
            for (MainType mainType : selectedMainTypes) {
                if (mainType.getId().equals(id)) {
                    return mainType;
                }
            }
        }
        return null;
    }

}
