/** Copyright (c) 2017-2019 Memorial Sloan-Kettering Cancer Center.
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

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import org.mskcc.oncotree.crosswalk.MSKConceptCache;
import org.mskcc.oncotree.crosswalk.MSKConcept;
import org.mskcc.oncotree.error.InvalidOncoTreeDataException;
import org.mskcc.oncotree.error.InvalidQueryException;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Created by Hongxin on 2/25/16.
 */
@Component
public class TumorTypesUtil {

    private static final Logger logger = LoggerFactory.getLogger(TumorTypesUtil.class);
    public final static String TSV_HEADER = "level_1\tlevel_2\tlevel_3\tlevel_4\tlevel_5\tlevel_6\tlevel_7\tmetamaintype\tmetacolor\tmetanci\tmetaumls\thistory";
    private static final String TOPBRAID_BASE_URI = "http://data.mskcc.org/ontologies/oncotree/";

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private MSKConceptCache mskConceptCache;

    public static List<String> TumorTypeKeys = Arrays.asList("code", "name", "nci", "level", "umls", "maintype", "color");

    public List<TumorType> findTumorTypesByVersion(String key, String keyword, Boolean exactMatch, Version version, Boolean includeParent) throws InvalidOncoTreeDataException, InvalidQueryException {
        logger.debug("Searching for key '" + key + "' and keyword '" + keyword + "'");
        List<TumorType> tumorTypes = new ArrayList<>();
        key = normalizeTumorTypeKey(key);
        if (TumorTypeKeys.contains(key)) {
            tumorTypes = findTumorType(cacheUtil.getTumorTypesByVersion(version).get("TISSUE"),
                cacheUtil.getTumorTypesByVersion(version).get("TISSUE"),
                tumorTypes, key, keyword, exactMatch, includeParent);
        } else {
            throw new InvalidQueryException(buildInvalidQueryTypeError(key));
        }
        logger.debug("Returning " + tumorTypes.size() + " tumor types");
        return tumorTypes;
    }

    public List<TumorType> filterTumorTypesByLevel(List<TumorType> tumorTypes, List<Integer> levels) {
        List<TumorType> filtered = new ArrayList<>();
        if (tumorTypes != null && levels != null) {
            for (TumorType tumorType : tumorTypes) {
                if (levels.contains(tumorType.getLevel())) {
                    filtered.add(tumorType);
                }
            }
        }
        return filtered;
    }

    public InputStream getTumorTypeInputStream(Map<String, TumorType> tumorTypes) {
        String tsvAsString = getTsvFromTumorTypes(tumorTypes);
        try {
            return new ByteArrayInputStream(tsvAsString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
           e.printStackTrace();
        }
        return null;
    }

    private String getTsvFromTumorTypes(Map<String, TumorType> tumorTypes) {
        List<String> rows = new ArrayList<>();
        for (String code : tumorTypes.keySet()) {
            TumorType tumorType = tumorTypes.get(code);
            // skip the root node, "TISSUE". Just add it's children
            Map<String, TumorType> children = tumorType.getChildren();
            for (String childCode : children.keySet()) {
                addTumorTypeToRows(children.get(childCode), rows, new ArrayList<String>());
            }
        }
        Collections.sort(rows);
        rows.add(0, TSV_HEADER);
        return StringUtils.join(rows, "\n") + "\n";
    }

    private void addTumorTypeToRows(TumorType tumorType, List<String> rows, List<String> parents) {
        List<String> row = new ArrayList<>();
        String oncotreeCode = StringUtils.defaultString(tumorType.getCode()).trim();

        // if parents.size() > 6 at this point, this means that the OncoTree cannot be represented in our expected format as there are
        // only 7 levels of headers. Abort the attemp to render the spreadsheet and throw an exception.
        if (parents.size() > 7) {
            throw new RuntimeException("OncoTree depth for code " + oncotreeCode + " exceeds max representation. Depth cannot be > 7");
        }

        String displayName = StringUtils.defaultString(tumorType.getName()).trim() + " (" + oncotreeCode + ")";
        row.addAll(parents);
        row.add(displayName);

        // Need to pad for the primary - quinternary columns if all parents are not present
        for (int i = 6; i > parents.size(); i--) {
            row.add("");
        }

        row.add(StringUtils.defaultString(tumorType.getMainType()));
        row.add(StringUtils.defaultString(tumorType.getColor()));
        // TODO enum or something
        if (tumorType.getExternalReferences().containsKey("NCI")) {
            row.add(StringUtils.defaultString(StringUtils.join(tumorType.getExternalReferences().get("NCI"), ",")));
        } else {
            row.add("");
        }
        // TODO enum or something
        if (tumorType.getExternalReferences().containsKey("UMLS")) {
            row.add(StringUtils.defaultString(StringUtils.join(tumorType.getExternalReferences().get("UMLS"), ",")));
        } else {
            row.add("");
        }
        // add revocations and precursors to history column
        List<String> combinedHistory = new ArrayList<String>();
        combinedHistory.addAll(tumorType.getHistory());
        combinedHistory.addAll(tumorType.getRevocations());
        combinedHistory.addAll(tumorType.getPrecursors());
        combinedHistory.removeAll(Collections.singletonList(tumorType.getCode())); // do not display a code in its own history (e.g. PTCL)
        row.add(StringUtils.defaultString(StringUtils.join(combinedHistory, ",")));

        rows.add(StringUtils.join(row, "\t"));

        // Prepare for next recursive call
        Map<String, TumorType> children = tumorType.getChildren();
        if (children.size() > 0) {
            parents.add(displayName);
            for (String code : children.keySet()) {
                TumorType child = children.get(code);
                addTumorTypeToRows(child, rows, parents);
            }
            parents.remove(parents.size() - 1);
        }
    }

    public Set<TumorType> flattenTumorTypes(Map<String, TumorType> nestedTumorTypes, String parent) {
        Set<TumorType> tumorTypes = new HashSet<>();

        Iterator<Map.Entry<String, TumorType>> it = nestedTumorTypes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TumorType> pair = it.next();
            TumorType tumorType = pair.getValue();

            if (tumorType.getChildren() != null && tumorType.getChildren().size() > 0) {
                tumorTypes.addAll(flattenTumorTypes(tumorType.getChildren(), pair.getKey()));
            }
            tumorType.setParent(parent);
            tumorType.setChildren(null);
            tumorTypes.add(tumorType);
        }

        return tumorTypes;
    }

    private boolean hasNoParent(TumorType node) {
        String parentCode = node.getParent();
        if (parentCode == null) {
            return true;
        }
        if (parentCode.trim().length() == 0) {
            return true;
        }
        return false;
    }

    private void validateOncoTreeOrThrowException(Set<String> rootNodeCodeSet, Set<String> duplicateCodeSet, Map<String, TumorType> allNodes) throws InvalidOncoTreeDataException {
        StringBuilder errorMessageBuilder = new StringBuilder();
        //check for one root node
        if (rootNodeCodeSet.size() == 0) {
            errorMessageBuilder.append("\toncotree has no root node (a node where parent is empty)\n");
        } else if (rootNodeCodeSet.size() > 1) {
            errorMessageBuilder.append("\toncotree has more than one root node (nodes where parent is empty):\n");
            for (String code : rootNodeCodeSet) {
                errorMessageBuilder.append("\t\t" + code + "\n");
            }
        }
        //check for no duplicated OncoTree codes
        if (duplicateCodeSet.size() > 0) {
            errorMessageBuilder.append("\tduplication : OncoTree has more than one node containing each of the following OncoTree codes:\n");
            for (String code : duplicateCodeSet) {
                errorMessageBuilder.append("\t\t" + code + "\n");
            }
        }
        //check that non-root nodes have a parent in the set of all nodes
        Set<String> allCodeSet = allNodes.keySet();
        for (TumorType tumorType : allNodes.values()) {
            String thisNodeCode = tumorType.getCode();
            String parentCode = tumorType.getParent();
            if (rootNodeCodeSet.contains(thisNodeCode)) {
                continue; //by definition, root nodes have no parent
            } else {
                if (!allCodeSet.contains(parentCode)) {
                    errorMessageBuilder.append("\tnode " + thisNodeCode + " has parent code '" + parentCode + "', which is not a code for any node in the tree\n");
                }
            }
        }
        if (errorMessageBuilder.length() > 0) {
            throw new InvalidOncoTreeDataException("Invalid OncoTree received:\n" + errorMessageBuilder.toString());
        }
    }

    private void setDepthAndTissue(TumorType tumorType, int depth, String tissue) {
        if (tumorType != null) {
            tumorType.setLevel(new Integer(depth));
            if (depth == 1) {
                tissue = tumorType.getName();
            }
            tumorType.setTissue(tissue);
            for (TumorType childTumorType: tumorType.getChildren().values()) {
                setDepthAndTissue(childTumorType, depth + 1, tissue);
            }
        }
    }

    public Map<String, TumorType> getAllTumorTypesFromOncoTreeNodes(List<OncoTreeNode> oncoTreeNodes, Version version, HashMap<String, ArrayList<String>> topBraidURIsToOncotreeCodes) throws InvalidOncoTreeDataException {
        Map<String, TumorType> allNodes = new HashMap<>();
        HashSet<String> rootNodeCodeSet = new HashSet<>();
        HashSet<String> duplicateCodeSet = new HashSet<>();
        // construct basic nodes
        for (OncoTreeNode thisNode : oncoTreeNodes) {
            logger.debug("OncoTreeNode: code='" + thisNode.getCode() + "', name='" + thisNode.getName() + "'");
            TumorType tumorType = initTumorType(thisNode, version);
            String thisNodeCode = tumorType.getCode();
            if (allNodes.containsKey(thisNodeCode)) {
                duplicateCodeSet.add(thisNodeCode);
            }
            allNodes.put(thisNodeCode, tumorType);
            if (hasNoParent(tumorType)) {
                rootNodeCodeSet.add(thisNodeCode);
            }

            // get all codes defined so far for this topbraid uri and save in history
            if (topBraidURIsToOncotreeCodes.containsKey(thisNode.getURI())) {
                // do not add this code to the history, but add any others
                HashSet<String> allButThisNode = new HashSet<String>(topBraidURIsToOncotreeCodes.get(thisNode.getURI()));
                allButThisNode.remove(thisNode.getCode());
                tumorType.setHistory(new ArrayList<String>(allButThisNode));
            } else {
                topBraidURIsToOncotreeCodes.put(thisNode.getURI(), new ArrayList<String>());
            }
            for (String topBraidURI : thisNode.getRevocations()) {
                String fullTopBraidURI = TOPBRAID_BASE_URI + topBraidURI;
                if (topBraidURIsToOncotreeCodes.containsKey(fullTopBraidURI)) {
                    ArrayList<String> nodeHistory = topBraidURIsToOncotreeCodes.get(fullTopBraidURI);
                    // last node is most recent for this URI
                    tumorType.addRevocations(nodeHistory.get(nodeHistory.size() - 1));
                }
                else {
                    logger.error("loadFromRepository() -- unknown topBraidURI " + fullTopBraidURI + " in revocations field for topBraidURI " + thisNode.getURI());
                    throw new InvalidOncoTreeDataException("Unknown topBraidURI " + fullTopBraidURI + " in revocations field for topBraidURI " + thisNode.getURI());
                }
            }
            for (String topBraidURI : thisNode.getPrecursors()) {
                String fullTopBraidURI = TOPBRAID_BASE_URI + topBraidURI;
                if (topBraidURIsToOncotreeCodes.containsKey(fullTopBraidURI)) {
                    ArrayList<String> nodeHistory = topBraidURIsToOncotreeCodes.get(fullTopBraidURI);
                    // last node is most recent for this URI
                    tumorType.addPrecursors(nodeHistory.get(nodeHistory.size() - 1));
                }
                else {
                    logger.error("loadFromRepository() -- unknown topBraidURI " + fullTopBraidURI + " in precursors field for topBraidURI " + thisNode.getURI());
                    throw new InvalidOncoTreeDataException("Unknown topBraidURI " + fullTopBraidURI + " in precursors field for topBraidURI " + thisNode.getURI());
                }
            }
            // now save this as onoctree code history for this topbraid uri
            topBraidURIsToOncotreeCodes.get(thisNode.getURI()).add(thisNode.getCode());
        }
        validateOncoTreeOrThrowException(rootNodeCodeSet, duplicateCodeSet, allNodes);
        // fill in children property, based on parent
        // also set NCI and UMLS codes
        for (TumorType tumorType : allNodes.values()) {
            String thisNodeCode = tumorType.getCode();
            MSKConcept mskConcept = mskConceptCache.get(thisNodeCode);
            if (mskConcept != null) {
                HashMap<String, List<String>> crosswalks = mskConcept.getCrosswalks();
                if (crosswalks != null && crosswalks.containsKey("NCI")) {
                    tumorType.setExternalReference("NCI", crosswalks.get("NCI"));
                }
                if (mskConcept.getConceptIds() != null) {
                    for (String mskConceptId : mskConcept.getConceptIds()) {
                        tumorType.addExternalReference("UMLS", mskConceptId.replace("MSK", "C"));
                    }
                }
            }
            if (rootNodeCodeSet.contains(thisNodeCode)) {
                continue; //root node has no parent
            }
            TumorType parent = allNodes.get(tumorType.getParent());
            parent.addChild(tumorType);
        }
        // set depth and tissue properties (root has tissue = null)
        String rootCode = rootNodeCodeSet.iterator().next();
        TumorType rootNode = allNodes.get(rootCode);
        setDepthAndTissue(rootNode, 0, null);
        // now that all children have a path of references to them from the root, return only the root node.
        allNodes.clear();
        allNodes.put(rootCode, rootNode);
        return allNodes;
    }

    private TumorType initTumorType(OncoTreeNode oncoTreeNode, Version version) throws InvalidOncoTreeDataException {
        // we do not have level or tissue
        TumorType tumorType = new TumorType();
        tumorType.setMainType(oncoTreeNode.getMainType());
        tumorType.setCode(oncoTreeNode.getCode());
        tumorType.setName(oncoTreeNode.getName());
        tumorType.setColor(oncoTreeNode.getColor());
        tumorType.setParent(oncoTreeNode.getParentCode());
        // do not copy history, revocations, or precursors
        return tumorType;
    }

    private List<TumorType> findTumorType(TumorType allTumorTypes, TumorType currentTumorType, List<TumorType> matchedTumorTypes,
            String key, String keyword, Boolean exactMatch, Boolean includeParent) throws InvalidQueryException {
        Map<String, TumorType> childrenTumorTypes = currentTumorType.getChildren();
        Boolean match = false;
        Map<String, List<String>> externalReferences = currentTumorType.getExternalReferences();

        if (includeParent == null) {
            includeParent = false;
        }

        if (exactMatch == null) {
            exactMatch = true;
        }

        switch (key) {
            case "code":
                if (exactMatch) {
                    match = currentTumorType.getCode() == null ? false : currentTumorType.getCode().equalsIgnoreCase(keyword);
                } else {
                    match = currentTumorType.getCode() == null ?
                        false :
                        StringUtils.containsIgnoreCase(currentTumorType.getCode(), keyword);
                }
                break;
            case "color":
                if (exactMatch) {
                    match = currentTumorType.getColor() == null ? false : currentTumorType.getColor().equalsIgnoreCase(keyword);
                } else {
                    match = currentTumorType.getColor() == null ?
                        false :
                        StringUtils.containsIgnoreCase(currentTumorType.getColor(), keyword);
                }
                break;
            case "name":
                if (exactMatch) {
                    match = currentTumorType.getName() == null ? false : currentTumorType.getName().equalsIgnoreCase(keyword);
                } else {
                    match = currentTumorType.getName() == null ?
                        false :
                        StringUtils.containsIgnoreCase(currentTumorType.getName(), keyword);
                }
                break;
            case "nci":
                if (exactMatch) {
                    match = externalReferences.containsKey("NCI") ? ListUtil.hasMatchingElementIgnoreCase(externalReferences.get("NCI"), keyword) : false;
                } else {
                    match = externalReferences.containsKey("NCI") ?
                        ListUtil.hasElementWhichContainsStringIgnoreCase(externalReferences.get("NCI"), keyword) : false;
                }
                break;
            case "umls":
                if (exactMatch) {
                    match = externalReferences.containsKey("UMLS") ? ListUtil.hasMatchingElementIgnoreCase(externalReferences.get("UMLS"), keyword) : false;
                } else {
                    match = externalReferences.containsKey("UMLS") ?
                        ListUtil.hasElementWhichContainsStringIgnoreCase(externalReferences.get("UMLS"), keyword) : false;
                }
                break;
            case "maintype":
                if (exactMatch) {
                    match = currentTumorType == null ? false :
                        (currentTumorType.getMainType() == null ? false :
                                currentTumorType.getMainType().equals(keyword));
                } else {
                    match = currentTumorType == null ? false :
                        (currentTumorType.getMainType() == null ? false :
                                StringUtils.containsIgnoreCase(currentTumorType.getMainType(), keyword));
                }
                break;
            case "level":
                try {
                    Integer keywordAsInteger = Integer.parseInt(keyword);
                    match = currentTumorType == null ? false :
                        (currentTumorType.getLevel() == null ? false :
                            (currentTumorType.getLevel() == null ? false :
                                currentTumorType.getLevel().equals(keywordAsInteger)));
                } catch (NumberFormatException e) {
                    throw new InvalidQueryException("'" + keyword + "' is not a valid level.  Level must be an integer.");
                }
                break;
            default:
                // we should never get here because we have already checked if this is a valid key
                throw new InvalidQueryException(buildInvalidQueryTypeError(key));
        }

        if (match) {
            TumorType tumorType = new TumorType();
            tumorType.setCode(currentTumorType.getCode());
            tumorType.setColor(currentTumorType.getColor());
            tumorType.setName(currentTumorType.getName());
            tumorType.setMainType(currentTumorType.getMainType());
            tumorType.setExternalReferences(currentTumorType.getExternalReferences());
            tumorType.setTissue(currentTumorType.getTissue());
            // the results of search operations are flat lists of TumorTypes .. no nested children so do not setChildren()
            tumorType.setParent(currentTumorType.getParent());
            tumorType.setHistory(currentTumorType.getHistory());
            tumorType.setRevocations(currentTumorType.getRevocations());
            tumorType.setPrecursors(currentTumorType.getPrecursors());
            tumorType.setLevel(currentTumorType.getLevel());

            matchedTumorTypes.add(tumorType);

            if (includeParent) {
                String code = currentTumorType.getParent();
                List<TumorType> parentTumorTypes = findTumorType(allTumorTypes, allTumorTypes, new ArrayList<TumorType>(), "code", code, true, true);
                if (parentTumorTypes != null && parentTumorTypes.size() > 0) {
                    TumorType parentNode = parentTumorTypes.get(0);
                    matchedTumorTypes.add(parentNode);
                    if(parentNode.getParent() != null) {
                        matchedTumorTypes = findTumorType(allTumorTypes, allTumorTypes, matchedTumorTypes, "code", parentNode.getParent(), true, true);
                    }
                }
            }
        }

        if (childrenTumorTypes.size() > 0) {
            Iterator it = childrenTumorTypes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                matchedTumorTypes = findTumorType(allTumorTypes, (TumorType) pair.getValue(), matchedTumorTypes, key, keyword, exactMatch, includeParent);
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(matchedTumorTypes));
    }

    private String normalizeTumorTypeKey(String key) {
        key = key.toLowerCase();
        key.replaceAll("[^a-z]+", "");
        return key;
    }

    private InputStream getInputStream(String relativePath) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        Resource resource = applicationContext.getResource(relativePath);
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String buildInvalidQueryTypeError(String queryType) {
        StringBuilder errorMessageBuilder = new StringBuilder("'");
        errorMessageBuilder.append(queryType);
        errorMessageBuilder.append("' is not a valid query type.  Valid query types are: ");
        errorMessageBuilder.append(StringUtils.join(TumorTypeKeys, ", "));
        return errorMessageBuilder.toString();
    }

}
