package org.mskcc.oncotree.utils;

import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.TumorType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hongxin on 2/25/16.
 */
public class MainTypesUtil {
    public static List<MainType> mainTypes = new ArrayList<>();

    public static List<MainType> getMainTypes() {
        return mainTypes;
    }

    public static MainType getMainType(String name) {
        for(MainType mainType : mainTypes) {
            if(mainType.getName().equalsIgnoreCase(name)) {
                return mainType;
            }
        }
        return null;
    }

    public static MainType getMainType(Integer id) {
        for(MainType mainType : mainTypes) {
            if(mainType.getId().equals(id)) {
                return mainType;
            }
        }
        return null;
    }

    /**
     * return MainType if not exist, create a new one.
     * @param name The searched main type name
     * @return
     */
    public static MainType getOrCreateMainType(String name) {
        for(MainType mainType : mainTypes) {
            if(mainType.getName().equalsIgnoreCase(name)) {
                return mainType;
            }
        }
        
        MainType mainType = new MainType();
        mainType.setName(name);
        mainType.setId(mainTypes.size());
        mainTypes.add(mainType);
        return mainType;
    }
}
