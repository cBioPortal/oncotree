package org.mskcc.oncotree.utils;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.commons.lang3.StringUtils;
import org.mskcc.oncotree.model.Level;
import org.mskcc.oncotree.model.MainType;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hongxin on 2/25/16.
 */
public class TumorTypesUtil {
    private static final String PROPERTY_FILE = "classpath:application.properties";
    private static List<String> TumorTypeKeys = Arrays.asList("code", "name", "nci", "level", "umls", "maintype", "color");

    public static Map<String, TumorType> getTumorTypes() {
        Map<String, TumorType> tumorTypes = new HashMap<>();
        Properties properties = getProperties();
        try {
            Version version = VersionUtil.getVersion("realtime");
            CacheUtil.resetMainTypesByVersion(version);

            tumorTypes = parseFromRaw(new FileInputStream(properties.getProperty("tumor_type_file_path")), version);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return tumorTypes;
    }

    public static Map<String, TumorType> getTumorTypesByVersionFromRaw(Version version) {
        Map<String, TumorType> tumorTypes = new HashMap<>();
        if (version != null && version.getCommitId() != null) {
            if (version.getVersion() == "realtime") {
                tumorTypes = getTumorTypes();
            } else {
                tumorTypes = parseFromRaw(getTumorTypeInputStreamFromGitHub(version), version);
            }
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

    public static List<TumorType> findTumorTypesByVersion(String key, String keyword, Boolean exactMatch, Version version) {
        List<TumorType> tumorTypes = new ArrayList<>();
        key = normalizeTumorTypeKey(key);
        if (TumorTypeKeys.contains(key)) {
            tumorTypes = findTumorType(CacheUtil.getTumorTypesByVersion(version).get("TISSUE"), tumorTypes, key, keyword, exactMatch);
        }
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

    public static InputStream getTumorTypeInputStreamByVersion(Version version) {
        if (version.getVersion() == "realtime") {
            return getTumorTypeInputStream();
        } else {
            return getTumorTypeInputStreamFromGitHub(version);
        }
    }

    public static InputStream getTumorTypeInputStreamFromGitHub(Version version) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/cBioPortal/oncotree/" + version.getCommitId() + "/tumor_tree.txt");
            return url.openStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, TumorType> parseFromRaw(InputStream inputStream, Version version) {
        TsvParserSettings settings = new TsvParserSettings();

        //the line separator sequence is defined here to ensure systems such as MacOS and Windows
        //are able to process this file correctly (MacOS uses '\r'; and Windows uses '\r\n').
        settings.getFormat().setLineSeparator("\n");

        // creates a TSV parser
        TsvParser parser = new TsvParser(settings);

        Map<String, TumorType> tumorTypes = new HashMap<>();

        List<String[]> allRows = parser.parseAll(inputStream);

        TumorType tumorType = new TumorType();
        tumorType.setCode("TISSUE");
        tumorType.setName("Tissue");

        //Iterate each row and assign tumor type to parent following the order of appearing
        for (String[] row : allRows.subList(1, allRows.size())) {
            tumorType.setChildren(attachTumorType(tumorType.getChildren(), row, 0, version));
        }

        //Attach a root node in the JSON file
        tumorTypes.put("TISSUE", tumorType);

        return tumorTypes;
    }

    private static List<TumorType> findTumorType(TumorType allTumorTypes, List<TumorType> matchedTumorTypes,
                                                 String key, String keyword, Boolean exactMatch) {
        Map<String, TumorType> childrenTumorTypes = allTumorTypes.getChildren();
        Boolean match = false;

        if (exactMatch == null) {
            exactMatch = true;
        }

        switch (key) {
            case "code":
                if (exactMatch) {
                    match = allTumorTypes.getCode() == null ? false : allTumorTypes.getCode().equalsIgnoreCase(keyword);
                } else {
                    match = allTumorTypes.getCode() == null ?
                        false :
                        StringUtils.containsIgnoreCase(allTumorTypes.getCode(), keyword);
                }
                break;
            case "color":
                if (exactMatch) {
                    match = allTumorTypes.getColor() == null ? false : allTumorTypes.getColor().equalsIgnoreCase(keyword);
                } else {
                    match = allTumorTypes.getColor() == null ?
                        false :
                        StringUtils.containsIgnoreCase(allTumorTypes.getColor(), keyword);
                }
                break;
            case "name":
                if (exactMatch) {
                    match = allTumorTypes.getName() == null ? false : allTumorTypes.getName().equalsIgnoreCase(keyword);
                } else {
                    match = allTumorTypes.getName() == null ?
                        false :
                        StringUtils.containsIgnoreCase(allTumorTypes.getName(), keyword);
                }
                break;
            case "nci":
                if (exactMatch) {
                    match = allTumorTypes.getNCI() == null ? false : allTumorTypes.getNCI().equalsIgnoreCase(keyword);
                } else {
                    match = allTumorTypes.getNCI() == null ?
                        false :
                        StringUtils.containsIgnoreCase(allTumorTypes.getNCI(), keyword);
                }
                break;
            case "umls":
                if (exactMatch) {
                    match = allTumorTypes.getUMLS() == null ? false : allTumorTypes.getUMLS().equalsIgnoreCase(keyword);
                } else {
                    match = allTumorTypes.getUMLS() == null ?
                        false :
                        StringUtils.containsIgnoreCase(allTumorTypes.getUMLS(), keyword);
                }
                break;
            case "maintype":
                if (exactMatch) {
                    match = allTumorTypes == null ? false :
                        (allTumorTypes.getMainType() == null ? false :
                            (allTumorTypes.getMainType().getName() == null ? false :
                                allTumorTypes.getMainType().getName().equals(keyword)));
                }else {
                    match = allTumorTypes == null ? false :
                        (allTumorTypes.getMainType() == null ? false :
                            (allTumorTypes.getMainType().getName() == null ? false :
                                StringUtils.containsIgnoreCase(allTumorTypes.getMainType().getName(), keyword)));
                }
                break;
            case "level":
                match = allTumorTypes == null ? false :
                    (allTumorTypes.getLevel() == null ? false :
                        (allTumorTypes.getLevel() == null ? false :
                            allTumorTypes.getLevel().equals(keyword)));
                break;
            default:
                if (exactMatch) {
                    match = allTumorTypes.getCode() == null ? false : allTumorTypes.getCode().equalsIgnoreCase(keyword);
                } else {
                    match = allTumorTypes.getCode() == null ?
                        false :
                        StringUtils.containsIgnoreCase(allTumorTypes.getCode(), keyword);
                }
        }

        if (match) {
            TumorType tumorType = new TumorType();
            tumorType.setTissue(allTumorTypes.getTissue());
            tumorType.setCode(allTumorTypes.getCode());
            tumorType.setName(allTumorTypes.getName());
            tumorType.setUMLS(allTumorTypes.getUMLS());
            tumorType.setNCI(allTumorTypes.getNCI());
            tumorType.setMainType(allTumorTypes.getMainType());
            tumorType.setColor(allTumorTypes.getColor());
            tumorType.setLevel(allTumorTypes.getLevel());

            matchedTumorTypes.add(tumorType);
        }

        if (childrenTumorTypes.size() > 0) {
            Iterator it = childrenTumorTypes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                matchedTumorTypes = findTumorType((TumorType) pair.getValue(), matchedTumorTypes, key, keyword, exactMatch);
            }
        }
        return matchedTumorTypes;
    }

    private static String normalizeTumorTypeKey(String key) {
        key = key.toLowerCase();
        key.replaceAll("[^a-z]+", "");
        return key;
    }

    /**
     * Attach children to parent node.
     *
     * @param tumorTypes
     * @param row        One row of text file
     * @param index      Current index of row. It will be increased everytime this function has been called.
     * @return parent node.
     */
    private static Map<String, TumorType> attachTumorType(Map<String, TumorType> tumorTypes, String[] row, int index, Version version) {
        if (index < 5 && row.length > index && row[index] != null && !row[index].isEmpty()) {
            Map<String, String> result = parseCodeName(row[index]);
            Map<String, String> tissue = parseCodeName(row[0]);
            if (result.containsKey("code")) {
                String code = result.get("code");
                TumorType tumorType = new TumorType();
                if (!tumorTypes.containsKey(code)) {
                    MainType mainType = null;
                    if (row.length > 5 && !row[5].isEmpty()) {
                        mainType = MainTypesUtil.getOrCreateMainType(row[5], version);
                    }
                    tumorType.setTissue(tissue.get("name"));
                    tumorType.setLevel(Level.getByLevel(Integer.toString(index + 1)));
                    tumorType.setCode(code);
                    tumorType.setName(result.get("name"));
                    tumorType.setMainType(mainType);
                    tumorType.setColor(row.length > 6 ? row[6] : "");
                    tumorType.setNCI(row.length > 7 ? row[7] : "");
                    tumorType.setUMLS(row.length > 8 ? row[8] : "");
                } else {
                    tumorType = tumorTypes.get(code);
                }
                tumorType.setChildren(attachTumorType(tumorType.getChildren(), row, ++index, version));
                tumorTypes.put(code, tumorType);
            }
        }
        return tumorTypes;
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

    /**
     * Get tumor type text file input stream.
     *
     * @param relativePath Tumor type text file path.
     * @return Input stream
     */
    private static InputStreamReader getReader(String relativePath) {
        try {
            return new InputStreamReader(getInputStream(relativePath), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
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
