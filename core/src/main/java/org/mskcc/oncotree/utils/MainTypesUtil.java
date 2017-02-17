package org.mskcc.oncotree.utils;

import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.Version;

import java.util.List;

/**
 * Created by Hongxin on 2/25/16.
 */
public class MainTypesUtil {
    /**
     * return MainType if not exist, create a new one.
     *
     * @param name The searched main type name
     * @return
     */
    public static MainType getOrCreateMainType(String name, Version version) {
        List<MainType> mainTypes = CacheUtil.getMainTypesByVersion(version);

        for (MainType mainType : mainTypes) {
            if (mainType.getName().equalsIgnoreCase(name)) {
                return mainType;
            }
        }

        MainType mainType = new MainType();
        mainType.setName(name);
        mainType.setId(mainTypes.size());

        CacheUtil.addMainTypeByVersion(version, mainType);
        return mainType;
    }
}
