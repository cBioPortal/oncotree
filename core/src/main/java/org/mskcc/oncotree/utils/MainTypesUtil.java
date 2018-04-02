/** Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.oncotree.utils;

import com.google.common.collect.Lists;
import org.mskcc.oncotree.error.InvalidOncoTreeDataException;
import org.mskcc.oncotree.model.Version;

import java.util.*;
import org.mskcc.oncotree.model.TumorType;

/**
 * Created by Hongxin on 2/25/16.
 */
public class MainTypesUtil {

    public static List<String> getMainTypesByTumorTypes(Set<TumorType> tumorTypes) {
        Set<String> mainTypes = new HashSet<>();
        // skip the root node, "TISSUE". Just add it's children
        for (TumorType tumorType : tumorTypes) {
            if (tumorType.getMainType() != null && tumorType.getParent() != null) {
                mainTypes.add(tumorType.getMainType());
            }
        }
        return new ArrayList<>(mainTypes);
    }
}
