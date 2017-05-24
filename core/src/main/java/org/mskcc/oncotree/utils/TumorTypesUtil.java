package org.mskcc.oncotree.utils;

import org.apache.log4j.Logger;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.commons.lang3.StringUtils;
import org.mskcc.oncotree.error.InvalidTreeException;
import org.mskcc.oncotree.model.Level;
import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hongxin on 2/25/16.
 */
@Component
public class TumorTypesUtil {

    private final static Logger logger = Logger.getLogger(TumorTypesUtil.class);

    private static OncoTreeRepository oncoTreeRepository;
    @Autowired
    public void setOncoTreeRepository(OncoTreeRepository property) { oncoTreeRepository = property; }

    private static final String PROPERTY_FILE = "classpath:application.properties";
    private static List<String> TumorTypeKeys = Arrays.asList("code", "name", "nci", "level", "umls", "maintype", "color");

    public static Map<String, TumorType> getTumorTypesByVersionFromRaw(Version version) {
        Map<String, TumorType> tumorTypes = new HashMap<>();
        if (version != null) {
            tumorTypes = loadFromRepository(version);
        }
        return tumorTypes;
    }

    public static Properties getProperties() {
        Properties properties = new Properties();
        InputStream inputStream = getInputStream(PROPERTY_FILE);

        try {
            if (inputStream != null) {
                properties.load(inputStream);
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }

    public static List<TumorType> findTumorTypesByVersion(String key, String keyword, Boolean exactMatch, Version version, Boolean includeParent) {
        logger.debug("Searching for key '" + key + "' and keyword '" + keyword + "'");
        List<TumorType> tumorTypes = new ArrayList<>();
        key = normalizeTumorTypeKey(key);
        if (TumorTypeKeys.contains(key)) {
            tumorTypes = findTumorType(CacheUtil.getTumorTypesByVersion(version).get("TISSUE"),
                CacheUtil.getTumorTypesByVersion(version).get("TISSUE"),
                tumorTypes, key, keyword, exactMatch, includeParent);
        }
        logger.debug("Returning " + tumorTypes.size() + " tumor types");
        return tumorTypes;
    }

    public static List<TumorType> filterTumorTypesByLevel(List<TumorType> tumorTypes, List<Level> levels) {
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

    public static InputStream getTumorTypeInputStream() {
        Properties properties = getProperties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(properties.getProperty("tumor_type_file_path"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    public static Set<TumorType> flattenTumorTypes(Map<String, TumorType> nestedTumorTypes, String parent) {
        Set<TumorType> tumorTypes = new HashSet<>();

        Iterator it = nestedTumorTypes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TumorType> pair = (Map.Entry) it.next();
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

    private static Map<String, TumorType> loadFromRepository(Version version) {
        List<OncoTreeNode> oncoTreeNodes = oncoTreeRepository.getOncoTree(version);
        Map<String, TumorType> tree = new HashMap<>();

        TumorType root = new TumorType();
        root.setCode("TISSUE");
        root.setName("Tissue");
        // tree seems to only contain root node
        tree.put(root.getCode(), root);

        Map<String, TumorType> allNodes = new HashMap<>();

        for (OncoTreeNode node : oncoTreeNodes) {
            logger.debug("loadFromRepository() -- OncoTreeNode: code='" + node.getCode() + "', name='" + node.getName() + "'");
            TumorType tumorType = initTumorType(node, version);
            allNodes.put(tumorType.getCode(), tumorType);
        }

        // now we have all nodes, fill in children
        for (TumorType tumorType : allNodes.values()) {
            // TISSUE is root and has no parent, skip
            if (!tumorType.getCode().equals("TISSUE")) {
                if (tumorType.getParent() == null) {
                    logger.debug("loadFromRepository() -- Parent is null for tumor type code '" +
                        tumorType.getCode() + "'.  Adding to 'TISSUE' node.");
                    tumorType.setParent(root.getCode());
                    root.addChild(tumorType);
                } else if (allNodes.containsKey(tumorType.getParent())) {
                    TumorType parent = allNodes.get(tumorType.getParent());
                    parent.addChild(tumorType);
                } else {
                    throw new InvalidTreeException("Could not find parent '" +
                        tumorType.getParent() + "' for tumor type code '" +
                        tumorType.getCode() + "'.");
                }
            }
        }

        for (TumorType tumorType : root.getChildren().values()) {
            setDepthAndTissue(tumorType, 1, tumorType.getName());
        }

        return tree;
    }

    private static void setDepthAndTissue(TumorType tumorType, int depth, String tissue) {
        if (tumorType != null) {
            tumorType.setLevel(Level.getByLevel(Integer.toString(depth)));
            tumorType.setTissue(tissue);
            for (TumorType childTumorType: tumorType.getChildren().values()) {
                setDepthAndTissue(childTumorType, depth + 1, tissue);
            }
        }
    }

    private static TumorType initTumorType(OncoTreeNode oncoTreeNode, Version version) {
        // we do not have level or tissue
        TumorType tumorType = new TumorType();
        if (oncoTreeNode.getMainType() != null) {
            tumorType.setMainType(MainTypesUtil.getOrCreateMainType(oncoTreeNode.getMainType(), version));
        }
        tumorType.setCode(oncoTreeNode.getCode());
        tumorType.setName(oncoTreeNode.getName());
        tumorType.setColor(oncoTreeNode.getColor());
        tumorType.setNCI(oncoTreeNode.getNci());
        tumorType.setUMLS(oncoTreeNode.getUmls());
        tumorType.setParent(oncoTreeNode.getParentCode());
        return tumorType;
    }

    public static List<TumorType> findTumorType(TumorType allTumorTypes, TumorType currentTumorType, List<TumorType> matchedTumorTypes,
                                                String key, String keyword, Boolean exactMatch, Boolean includeParent) {
        Map<String, TumorType> childrenTumorTypes = currentTumorType.getChildren();
        Boolean match = false;

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
                    match = currentTumorType.getNCI() == null ? false : currentTumorType.getNCI().equalsIgnoreCase(keyword);
                } else {
                    match = currentTumorType.getNCI() == null ?
                        false :
                        StringUtils.containsIgnoreCase(currentTumorType.getNCI(), keyword);
                }
                break;
            case "umls":
                if (exactMatch) {
                    match = currentTumorType.getUMLS() == null ? false : currentTumorType.getUMLS().equalsIgnoreCase(keyword);
                } else {
                    match = currentTumorType.getUMLS() == null ?
                        false :
                        StringUtils.containsIgnoreCase(currentTumorType.getUMLS(), keyword);
                }
                break;
            case "maintype":
                if (exactMatch) {
                    match = currentTumorType == null ? false :
                        (currentTumorType.getMainType() == null ? false :
                            (currentTumorType.getMainType().getName() == null ? false :
                                currentTumorType.getMainType().getName().equals(keyword)));
                } else {
                    match = currentTumorType == null ? false :
                        (currentTumorType.getMainType() == null ? false :
                            (currentTumorType.getMainType().getName() == null ? false :
                                StringUtils.containsIgnoreCase(currentTumorType.getMainType().getName(), keyword)));
                }
                break;
            case "level":
                match = currentTumorType == null ? false :
                    (currentTumorType.getLevel() == null ? false :
                        (currentTumorType.getLevel() == null ? false :
                            currentTumorType.getLevel().equals(keyword)));
                break;
            default:
                if (exactMatch) {
                    match = currentTumorType.getCode() == null ? false : currentTumorType.getCode().equalsIgnoreCase(keyword);
                } else {
                    match = currentTumorType.getCode() == null ?
                        false :
                        StringUtils.containsIgnoreCase(currentTumorType.getCode(), keyword);
                }
        }

        if (match) {
            TumorType tumorType = new TumorType();
            tumorType.setTissue(currentTumorType.getTissue());
            tumorType.setCode(currentTumorType.getCode());
            tumorType.setName(currentTumorType.getName());
            tumorType.setUMLS(currentTumorType.getUMLS());
            tumorType.setNCI(currentTumorType.getNCI());
            tumorType.setMainType(currentTumorType.getMainType());
            tumorType.setColor(currentTumorType.getColor());
            tumorType.setLevel(currentTumorType.getLevel());
            tumorType.setParent(currentTumorType.getParent());

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

    private static String normalizeTumorTypeKey(String key) {
        key = key.toLowerCase();
        key.replaceAll("[^a-z]+", "");
        return key;
    }

    /**
     * Parsing cell content into tumor type code and tumor type name.
     *
     * @param content One cell of each row.
     * @return The map of current tumor type. It includes 'code' and 'name'.
     */
    private static HashMap<String, String> parseCodeName(String content) {
        HashMap<String, String> result = new HashMap<>();

        Pattern pattern = Pattern.compile("([^\\(]+)\\(([^\\)]+)\\)");

        Matcher matcher = pattern.matcher(content);
        if (matcher.matches()) {
            result.put("name", matcher.group(1).trim());
            result.put("code", matcher.group(2).trim());
        }
        return result;
    }

    private static InputStream getInputStream(String relativePath) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        Resource resource = applicationContext.getResource(relativePath);
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
