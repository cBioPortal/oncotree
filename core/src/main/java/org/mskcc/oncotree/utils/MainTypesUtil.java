package org.mskcc.oncotree.utils;

import com.google.common.collect.Lists;
import org.mskcc.oncotree.error.InvalidOncoTreeDataException;
import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.Version;

import java.util.*;
import org.mskcc.oncotree.model.TumorType;

/**
 * Created by Hongxin on 2/25/16.
 */
public class MainTypesUtil {

    public static List<MainType> getMainTypesByTumorTypes(Map<String, TumorType> tumorTypes) {
        Set<MainType> mainTypes = new HashSet<>();
        // skip the root node, "TISSUE". Just add it's children
        for (String tumorTypeName : tumorTypes.keySet()) {
            TumorType tumorType = tumorTypes.get(tumorTypeName);
            Map<String, TumorType> children = tumorType.getChildren();
            for (String code : children.keySet()) {
                addMainTypesToSet(children.get(code), mainTypes);
            }
        }
        List<MainType> toReturn = new ArrayList<>();
        toReturn.addAll(mainTypes);
        return toReturn;
    }

    private static void addMainTypesToSet(TumorType tumorType, Set<MainType> mainTypes) {
        if (tumorType.getMainType() != null) {
            mainTypes.add(tumorType.getMainType());
        }
        Map<String, TumorType> children = tumorType.getChildren();
        if (children.size() > 0) {
            for (String code : children.keySet()) {
                TumorType child = children.get(code);
                addMainTypesToSet(child, mainTypes);
            }
        }
    }
}
