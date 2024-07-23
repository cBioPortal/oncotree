/** Copyright (c) 2017 - 2018, 2024 Memorial Sloan-Kettering Cancer Center.
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

import java.util.*;
import org.mockito.Mockito;
import org.mskcc.oncotree.crosswalk.MSKConcept;
import org.mskcc.oncotree.crosswalk.MSKConceptCache;
import org.mskcc.oncotree.crosswalk.CrosswalkRepository;
import org.mskcc.oncotree.model.TumorType;
import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.utils.CacheUtil;
import org.mskcc.oncotree.config.OncoTreeAppConfig;
import org.mskcc.oncotree.utils.OncoTreePersistentCache;
import org.mskcc.oncotree.utils.VersionUtil;
import org.mskcc.oncotree.utils.SlackUtil;
import org.mskcc.oncotree.graphite.OncoTreeNode;
import org.mskcc.oncotree.graphite.OncoTreeRepository;
import org.mskcc.oncotree.graphite.OncoTreeVersionRepository;
import org.mskcc.oncotree.graphite.GraphiteException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.*;

/**
 *
 * @author heinsz
 */
@Configuration
@Import(OncoTreeAppConfig.class)
public class OncotreeTestConfig {

    private ArrayList<OncoTreeNode> oncoTreeRepositoryMockResponse = setupOncotreeRepositoryMockResponse();
    private ArrayList<OncoTreeNode> oncoTreeRepositoryMockResponseHistoryChange = setupOncotreeRepositoryMockResponseHistoryChange();

    @Bean
    public OncoTreeVersionRepository oncoTreeVersionRepository() {
        OncoTreeVersionRepository repository = Mockito.mock(OncoTreeVersionRepository.class);
        Mockito.when(repository.getOncoTreeVersions()).thenReturn(oncoTreeVersionRepositoryMockResponse());
        return repository;
    }

    public void resetVersionRepository(OncoTreeVersionRepository mockRepository) {
        Mockito.reset(mockRepository);
        Mockito.when(mockRepository.getOncoTreeVersions()).thenReturn(oncoTreeVersionRepositoryMockResponse());
    }

    public void resetAdditionalVersionRepository(OncoTreeVersionRepository mockRepository) {
        Mockito.reset(mockRepository);
        Mockito.when(mockRepository.getOncoTreeVersions()).thenReturn(oncoTreeAdditionalVersionRepositoryMockResponse());
    }

    public void resetNotWorkingVersionRepository(OncoTreeVersionRepository mockRepository) {
        Mockito.reset(mockRepository);
        Mockito.when(mockRepository.getOncoTreeVersions()).thenThrow(new GraphiteException("faking a problem getting the graphite data"));
    }

    @Bean
    public Version legacyVersion() {
        Version legacyVersion = new Version();
        legacyVersion.setVersion("oncotree_legacy_1.1");
        legacyVersion.setGraphURI("urn:x-evn-master:oncotree_legacy_1_1");
        legacyVersion.setDescription("This is the closest match in Graphite for the TumorTypes_txt file associated with release 1.1 of OncoTree (approved by committee)");
        return legacyVersion;
    }

    @Bean
    public ArrayList<Version> oncoTreeVersionRepositoryMockResponse() {
        ArrayList<Version> oncoTreeVersionRepositoryMockResponse = new ArrayList<Version>();
        // versions are always supposed to be in ascending order

        oncoTreeVersionRepositoryMockResponse.add(legacyVersion());

        Version nextVersion = new Version();
        nextVersion.setVersion("oncotree_2017_06_21");
        nextVersion.setGraphURI("urn:x-evn-master:oncotree_2017_06_21");
        nextVersion.setDescription("Stable OncoTree released on date 2017-06-21");
        oncoTreeVersionRepositoryMockResponse.add(nextVersion);

        oncoTreeVersionRepositoryMockResponse.add(latestVersion());

        nextVersion = new Version();
        nextVersion.setVersion("oncotree_development");
        nextVersion.setGraphURI("urn:x-evn-master:oncotree_current");
        nextVersion.setDescription("Latest OncoTree under development (subject to change without notice)");
        oncoTreeVersionRepositoryMockResponse.add(nextVersion);
        return oncoTreeVersionRepositoryMockResponse;
    }

    @Bean
    public ArrayList<Version> oncoTreeAdditionalVersionRepositoryMockResponse() {
        ArrayList<Version> oncoTreeAdditionalVersionRepositoryMockResponse = new ArrayList<Version>(oncoTreeVersionRepositoryMockResponse());
        Version nextVersion = new Version();
        nextVersion.setVersion("test_version");
        nextVersion.setGraphURI("urn:x-evn-master:test_version");
        nextVersion.setDescription("This is just another test OncoTree version for testing cache updates");
        oncoTreeAdditionalVersionRepositoryMockResponse.add(nextVersion);
        return oncoTreeAdditionalVersionRepositoryMockResponse;
    }

    @Bean
    public MSKConceptCache mskConceptCache() {
        return new MSKConceptCache();
    }

    @Bean
    public CrosswalkRepository crosswalkRepository() {
        CrosswalkRepository repository = Mockito.mock(CrosswalkRepository.class);
        MSKConcept mskConcept = new MSKConcept();
        mskConcept.setConceptIds(Arrays.asList("MSK00001", "MSK00002"));
        Mockito.when(repository.getByOncotreeCode(any(String.class))).thenReturn(mskConcept);
        return repository;
    }

    @Bean
    public SlackUtil slackUtil() {
        return Mockito.mock(SlackUtil.class);
    }
    
    @Bean
    public VersionUtil versionUtil() {
        return new VersionUtil();
    }

    @Bean
    public CacheUtil cacheUtil() {
        return new CacheUtil();
    }

    @Bean
    public OncoTreePersistentCache oncoTreePersistentCache() {
        return new OncoTreePersistentCache();
    }

    @Bean
    public Map<String, TumorType> expectedTumorTypeMap() throws Exception {
        return setupExpectedTumorTypeMap();
    }

    @Bean
    public List<String> expectedMainTypeList() throws Exception {
        return setupExpectedMainTypeList();
    }

    @Bean
    public Version latestVersion() {
        return setupLatestVersion();
    }

    @Bean
    public OncoTreeRepository oncoTreeRepository() {
        OncoTreeRepository mockRepository = Mockito.mock(OncoTreeRepository.class);
        Mockito.when(mockRepository.getOncoTree(eq(legacyVersion()))).thenReturn(oncoTreeRepositoryMockResponse);
        Mockito.when(mockRepository.getOncoTree(not(eq(legacyVersion())))).thenReturn(oncoTreeRepositoryMockResponseHistoryChange);
        return mockRepository;
    }

    public void resetWorkingRepository(OncoTreeRepository mockRepository) {
        Mockito.reset(mockRepository);
        Mockito.when(mockRepository.getOncoTree(eq(legacyVersion()))).thenReturn(oncoTreeRepositoryMockResponse);
        Mockito.when(mockRepository.getOncoTree(not(eq(legacyVersion())))).thenReturn(oncoTreeRepositoryMockResponseHistoryChange);
    }

    public void resetNotWorkingRepository(OncoTreeRepository mockRepository) {
        Mockito.reset(mockRepository);
        Mockito.when(mockRepository.getOncoTree(any(Version.class))).thenThrow(new GraphiteException("faking a problem getting the graphite data"));
    }

    @Bean
    public TumorTypesUtil tumorTypesUtil() {
        return new TumorTypesUtil();
    }

    @Bean
    public MainTypesUtil mainTypesUtil() {
        return new MainTypesUtil();
    }

    @Bean
    public ApiUtil ApiUtil() {
        return new ApiUtil();
    }

    private Version setupLatestVersion() {
        Version latestVersion = new Version();
        latestVersion = new Version();
        latestVersion.setVersion("oncotree_latest_stable");
        latestVersion.setGraphURI("urn:x-evn-master:oncotree_2017_06_21");
        latestVersion.setDescription("This is an alias for whatever OncoTree version is the latest stable (timestamped) release.");
        return latestVersion;
    }

    private ArrayList<OncoTreeNode> setupOncotreeRepositoryMockResponse() {
        String[] rawTestValueSource = getRawTestValueSource();
        final int valuesPerCase = 6;
        if (rawTestValueSource.length % valuesPerCase != 0) {
            throw new RuntimeException("Error : malformed rawTestValueSource " + rawTestValueSource.length);
        }
        final int caseCount = rawTestValueSource.length / valuesPerCase;
        if (caseCount < 1) {
            throw new RuntimeException("Error : no test cases defined in rawTestValueSource");
        }
        ArrayList<OncoTreeNode> tmpOncoTreeRepositoryMockResponse = new ArrayList<>();
        for (int pos = 0; pos < rawTestValueSource.length; pos = pos + valuesPerCase) {
            OncoTreeNode nextNode = new OncoTreeNode();
            nextNode.setCode(rawTestValueSource[pos]);
            nextNode.setName(rawTestValueSource[pos + 1]);
            nextNode.setMainType(rawTestValueSource[pos + 2]);
            nextNode.setColor(rawTestValueSource[pos + 3]);
            nextNode.setParentCode(rawTestValueSource[pos + 4]);
            nextNode.setClinicalCasesSubset(rawTestValueSource[pos + 5]);
            tmpOncoTreeRepositoryMockResponse.add(nextNode);
        }
        return tmpOncoTreeRepositoryMockResponse;
    }

    private ArrayList<OncoTreeNode> setupOncotreeRepositoryMockResponseHistoryChange() {
        ArrayList<OncoTreeNode> tmpOncoTreeRepositoryMockResponseHistoryChange = new ArrayList<>();
        for (OncoTreeNode oncotreeNode : oncoTreeRepositoryMockResponse) {
            OncoTreeNode newOncotreeNode = new OncoTreeNode(oncotreeNode);

            // SEZS will be in the history of SS
            if ("SEZS".equals(newOncotreeNode.getCode())) {
                newOncotreeNode.setCode("SS");
            }

            // add revocations
            if ("URMM".equals(newOncotreeNode.getCode())) {
                newOncotreeNode.setRevocations("ONC000503");
            }

            tmpOncoTreeRepositoryMockResponseHistoryChange.add(newOncotreeNode);
        }

        OncoTreeNode newOncotreeNode = new OncoTreeNode();
        newOncotreeNode.setURI("http://data.mskcc.org/ontologies/oncotree/ONC000932");
        newOncotreeNode.setCode("CLLSLL");
        newOncotreeNode.setName("DLBCL Associated with Chronic Inflammation");
        newOncotreeNode.setMainType("Mature B-Cell Neoplasms");
        newOncotreeNode.setColor("LimeGreen");
        newOncotreeNode.setParentCode("BCL");
        newOncotreeNode.setPrecursors("ONC000413 ONC000820");
        tmpOncoTreeRepositoryMockResponseHistoryChange.add(newOncotreeNode);

        return tmpOncoTreeRepositoryMockResponseHistoryChange;
    }

    private Map<String, TumorType> setupExpectedTumorTypeMap() throws Exception {
        String[] rawTestValueSource = getRawTestValueSource();
        final int valuesPerCase = 6;
        if (rawTestValueSource.length % valuesPerCase != 0) {
            throw new RuntimeException("Error : malformed rawTestValueSource " + rawTestValueSource.length);
        }
        final int caseCount = rawTestValueSource.length / valuesPerCase;
        if (caseCount < 1) {
            throw new Exception("Error : no test cases defined in rawTestValueSource");
        }
        Map<String, TumorType> expectedTumorTypeMap = new HashMap<>(caseCount + 1);
        for (int pos = 0; pos < rawTestValueSource.length; pos = pos + valuesPerCase) {
            TumorType nextType = new TumorType();
            String code = rawTestValueSource[pos];
            if (code != null && code.trim().length() > 0) {
                nextType.setCode(code.trim());
            } else {
                fail("Error : list of expected oncotree codes contains a node with no code");
            }
            String name = rawTestValueSource[pos + 1];
            if (name != null && name.trim().length() > 0) {
                nextType.setName(name.trim());
            }
            String mainType = rawTestValueSource[pos + 2];
            if (mainType != null && mainType.trim().length() > 0) {
                nextType.setMainType(mainType.trim());
            }
            String color = rawTestValueSource[pos + 3];
            if (color != null && color.trim().length() > 0) {
                nextType.setColor(color.trim());
            }
            String parentCode = rawTestValueSource[pos + 4];
            if (parentCode != null && parentCode.trim().length() > 0) {
                nextType.setParent(parentCode.trim());
            }
            String internalId = rawTestValueSource[pos + 5];
            if (internalId != null && internalId.trim().length() > 0) {
                nextType.setClinicalCasesSubset(internalId.trim());
            }
            expectedTumorTypeMap.put(code, nextType);
        }
        return expectedTumorTypeMap;
    }

    private List<String> setupExpectedMainTypeList() throws Exception {
        Set<String> expectedMainTypeSet = new HashSet<>();
        List<String> expectedMainTypeList = new ArrayList<>();
        String[] rawTestValueSource = getRawTestValueSource();
        final int valuesPerCase = 6;
        if (rawTestValueSource.length % valuesPerCase != 0) {
            throw new RuntimeException("Error : malformed rawTestValueSource " + rawTestValueSource.length);
        }
        final int caseCount = rawTestValueSource.length / valuesPerCase;
        if (caseCount < 1) {
            throw new Exception("Error : no test cases defined in rawTestValueSource");
        }
        for (int pos = 0; pos < rawTestValueSource.length; pos = pos + valuesPerCase) {
            String mainType = rawTestValueSource[pos + 2];
            String parentCode = rawTestValueSource[pos + 4];
            if (mainType != null && mainType.trim().length() > 0 && parentCode != null && parentCode.trim().length() > 0) {
                expectedMainTypeSet.add(mainType);
            }
        }
        expectedMainTypeList.addAll(expectedMainTypeSet);
        return expectedMainTypeList;
    }

    private static String[] getRawTestValueSource() {
        String[] rawTestValueSource = {
                "AA", "Aggressive Angiomyxoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000288",
                "AASTR", "Anaplastic Astrocytoma", "Glioma", "Gray", "DIFG", "ONC000289",
                "ACA", "Adrenocortical Adenoma", "Adrenocortical Carcinoma", "Purple", "ADRENAL_GLAND", "ONC000290",
                "ACBC", "Adenoid Cystic Breast Cancer", "Breast Cancer", "HotPink", "BRCA", "ONC000291",
                "ACC", "Adrenocortical Carcinoma", "Adrenocortical Carcinoma", "Purple", "ADRENAL_GLAND", "ONC000292",
                "ACCC", "Acinic Cell Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000293",
                "ACN", "Acinar Cell Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP", "ONC000294",
                "ACPG", "Craniopharyngioma, Adamantinomatous Type", "Sellar Tumor", "Gray", "SELT", "ONC000295",
                "ACPP", "Atypical Choroid Plexus Papilloma", "Choroid Plexus Tumor", "Gray", "CPT", "ONC000296",
                "ACRM", "Acral Melanoma", "Melanoma", "Black", "MEL", "ONC000297",
                "ACYC", "Adenoid Cystic Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000298",
                "ADNOS", "Adenocarcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP", "ONC000299",
                "ADPA", "Aggressive Digital Papillary Adenocarcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000300",
                "ADRENAL_GLAND", "Adrenal Gland", null, "Purple", "TISSUE", "ONC000301",
                "AECA", "Sweat Gland Carcinoma/Apocrine Eccrine Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000302",
                "AFX", "Atypical Fibroxanthoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000303",
                "AGA", "Anal Gland Adenocarcinoma", "Anal Cancer", "SaddleBrown", "BOWEL", "ONC000304",
                "AGNG", "Anaplastic Ganglioglioma", "Glioma", "Gray", "ENCG", "ONC000305",
                "AIS", "Adenocarcinoma In Situ", "Adenocarcinoma In Situ", "Black", "OTHER", "ONC000306",
                "AITL", "Angioimmunoblastic T-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "PTCL", "ONC000307",
                "ALCL", "Anaplastic Large Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "PTCL", "ONC000308",
                "ALL", "Acute Lymphoid Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000309",
                "ALUCA", "Atypical Lung Carcinoid", "Non-Small Cell Lung Cancer", "Gainsboro", "LNET", "ONC000310",
                "AMBL", "Large Cell/Anaplastic Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000311",
                "AML", "Acute Myeloid Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000312",
                "AMOL", "Acute Monocytic Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000313",
                "AMPCA", "Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPULLA_OF_VATER", "ONC000314",
                "AMPULLA_OF_VATER", "Ampulla of Vater", "Ampullary Carcinoma", "Purple", "TISSUE", "ONC000315",
                "ANGL", "Angiocentric Glioma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000316",
                "ANGS", "Angiosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000317",
                "ANM", "Anaplastic Meningioma", "CNS Cancer", "Gray", "MNGT", "ONC000318",
                "ANSC", "Anal Squamous Cell Carcinoma", "Anal Cancer", "SaddleBrown", "BOWEL", "ONC000319",
                "AOAST", "Anaplastic Oligoastrocytoma", "Glioma", "Gray", "DIFG", "ONC000320",
                "AODG", "Anaplastic Oligodendroglioma", "Glioma", "Gray", "DIFG", "ONC000321",
                "APAD", "Appendiceal Adenocarcinoma", "Appendiceal Cancer", "SaddleBrown", "BOWEL", "ONC000322",
                "APE", "Anaplastic Ependymoma", "CNS Cancer", "Gray", "EPMT", "ONC000323",
                "APTAD", "Atypical Pituitary Adenoma", "Sellar Tumor", "Gray", "SELT", "ONC000324",
                "APXA", "Anaplastic Pleomorphic Xanthoastrocytoma", "Glioma", "Gray", "ENCG", "ONC000325",
                "ARMM", "Anorectal Mucosal Melanoma", "Melanoma", "SaddleBrown", "BOWEL", "ONC000326",
                "ARMS", "Alveolar Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS", "ONC000327",
                "ASPS", "Alveolar Soft Part Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000328",
                "ASTB", "Astroblastoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000329",
                "ASTR", "Astrocytoma", "Glioma", "Gray", "DIFG", "ONC000330",
                "ATM", "Atypical Meningioma", "CNS Cancer", "Gray", "MNGT", "ONC000331",
                "ATRT", "Atypical Teratoid/Rhabdoid Tumor", "Embryonal Tumor", "Gray", "EMBT", "ONC000332",
                "AWDNET", "Well-Differentiated Neuroendocrine Tumor of the Appendix", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET", "ONC000333",
                "BA", "Breast Angiosarcoma", "Breast Sarcoma", "HotPink", "PBS", "ONC000334",
                "BALL", "B-Cell Acute Lymphoid Leukemia", "Leukemia", "LightSalmon", "ALL", "ONC000335",
                "BCC", "Basal Cell Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000336",
                "BCCA", "Choriocarcinoma", "Germ Cell Tumor", "Gray", "BGCT", "ONC000337",
                "BCL", "B-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "NHL", "ONC000338",
                "BEC", "Embryonal Carcinoma", "Germ Cell Tumor", "Gray", "BGCT", "ONC000339",
                "BFN", "Breast Fibroepithelial Neoplasms", "Breast Sarcoma", "HotPink", "BREAST", "ONC000340",
                "BGCT", "Germ Cell Tumor, Brain", "Germ Cell Tumor", "Gray", "BRAIN", "ONC000341",
                "BILIARY_TRACT", "Biliary Tract", "Hepatobiliary Cancer", "Green", "TISSUE", "ONC000342",
                "BIMT", "Immature Teratoma", "Germ Cell Tumor", "Gray", "BGCT", "ONC000343",
                "BL", "Burkitt Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000344",
                "BLAD", "Bladder Adenocarcinoma", "Bladder Cancer", "Yellow", "BLADDER", "ONC000345",
                "BLADDER", "Bladder/Urinary Tract", "Bladder Cancer", "Yellow", "TISSUE", "ONC000346",
                "BLCA", "Bladder Urothelial Carcinoma", "Bladder Cancer", "Yellow", "BLADDER", "ONC000347",
                "BLCLC", "Basaloid Large Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC", "ONC000348",
                "BLOOD", "Blood", null, "LightSalmon", "TISSUE", "ONC000349",
                "BLPT", "Borderline Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "PT", "ONC000350",
                "BLSC", "Bladder Squamous Cell Carcinoma", "Bladder Cancer", "Yellow", "BLADDER", "ONC000351",
                "BMGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "Gray", "BGCT", "ONC000352",
                "BMGT", "Malignant Teratoma", "Germ Cell Tumor", "Gray", "BGCT", "ONC000353",
                "BMT", "Mature Teratoma", "Germ Cell Tumor", "Gray", "BGCT", "ONC000354",
                "BNNOS", "Breast Neoplasm, NOS", "Breast Cancer", "HotPink", "BREAST", "ONC000355",
                "BONE", "Bone", "Bone Cancer", "White", "TISSUE", "ONC000356",
                "BOWEL", "Bowel", null, "SaddleBrown", "TISSUE", "ONC000357",
                "BPDCN", "Blastic Plasmacytoid Dendritic Cell Neoplasm", "Blastic Plasmacytoid Dendritic Cell Neoplasm", "LightSalmon", "BLOOD", "ONC000358",
                "BPSCC", "Basaloid Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PSCC", "ONC000359",
                "BPT", "Benign Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "PT", "ONC000360",
                "BRAIN", "CNS/Brain", null, "Gray", "TISSUE", "ONC000361",
                "BRAME", "Adenomyoepithelioma of the Breast", "Breast Cancer", "HotPink", "BREAST", "ONC000362",
                "BRCA", "Invasive Breast Carcinoma", "Breast Cancer", "HotPink", "BREAST", "ONC000363",
                "BRCANOS", "Breast Invasive Cancer, NOS", "Breast Cancer", "HotPink", "BRCA", "ONC000364",
                "BRCNOS", "Breast Invasive Carcinoma, NOS", "Breast Cancer", "HotPink", "BRCA", "ONC000365",
                "BREAST", "Breast", null, "HotPink", "TISSUE", "ONC000366",
                "BRSRCC", "Breast Carcinoma with Signet Ring", "Breast Cancer", "HotPink", "BRCA", "ONC000367",
                "BTBEOV", "Brenner Tumor, Benign", "Ovarian Cancer", "LightBlue", "BTOV", "ONC000368",
                "BTBOV", "Brenner Tumor, Borderline", "Ovarian Cancer", "LightBlue", "BTOV", "ONC000369",
                "BTMOV", "Brenner Tumor, Malignant", "Ovarian Cancer", "LightBlue", "BTOV", "ONC000370",
                "BTOV", "Brenner Tumor", "Ovarian Cancer", "LightBlue", "OVT", "ONC000371",
                "BYST", "Yolk Sac Tumor", "Germ Cell Tumor", "Gray", "BGCT", "ONC000372",
                "CABC", "Cervical Adenoid Basal Carcinoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000373",
                "CACC", "Cervical Adenoid Cystic Carcinoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000374",
                "CAIS", "Colon Adenocarcinoma In Situ", "Colorectal Cancer", "SaddleBrown", "COADREAD", "ONC000375",
                "CCBOV", "Clear Cell Borderline Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT", "ONC000376",
                "CCE", "Clear Cell Ependymoma", "CNS Cancer", "Gray", "EPMT", "ONC000377",
                "CCHDM", "Conventional Type Chordoma", "Bone Cancer", "White", "CHDM", "ONC000378",
                "CCHM", "Carcinoma with Chondroid Metaplasia", "Breast Cancer", "HotPink", "MMBC", "ONC000379",
                "CCLC", "Clear Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC", "ONC000380",
                "CCM", "Clear cell Meningioma", "CNS Cancer", "Gray", "MNGT", "ONC000381",
                "CCOC", "Clear Cell Odontogenic Carcinoma", "Head and Neck Cancer", "DarkRed", "ODGC", "ONC000382",
                "CCOV", "Clear Cell Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT", "ONC000383",
                "CCPRC", "Clear Cell Papillary Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000384",
                "CCRCC", "Renal Clear Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "RCC", "ONC000385",
                "CCS", "Clear Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000386",
                "CDRCC", "Collecting Duct Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000387",
                "CEAD", "Cervical Adenocarcinoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000388",
                "CEAIS", "Cervical Adenocarcinoma In Situ", "Cervical Cancer", "Teal", "CERVIX", "ONC000389",
                "CEAS", "Cervical Adenosquamous Carcinoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000390",
                "CECC", "Cervical Clear Cell Carcinoma", "Cervical Cancer", "Teal", "CEAD", "ONC000391",
                "CEEN", "Cervical Endometrioid Carcinoma", "Cervical Cancer", "Teal", "CEAD", "ONC000392",
                "CEGCC", "Glassy Cell Carcinoma of the Cervix", "Cervical Cancer", "Teal", "CERVIX", "ONC000393",
                "CELI", "Cervical Leiomyosarcoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000394",
                "CEMN", "Mesonephric Carcinoma", "Cervical Cancer", "Teal", "CEAD", "ONC000395",
                "CEMU", "Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEAD", "ONC000396",
                "CENE", "Cervical Neuroendocrine Tumor", "Cervical Cancer", "Teal", "CERVIX", "ONC000397",
                "CERMS", "Cervical Rhabdomyosarcoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000398",
                "CERVIX", "Cervix", "Cervical Cancer", "Teal", "TISSUE", "ONC000399",
                "CESC", "Cervical Squamous Cell Carcinoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000400",
                "CESE", "Cervical Serous Carcinoma", "Cervical Cancer", "Teal", "CEAD", "ONC000401",
                "CEVG", "Villoglandular Carcinoma", "Cervical Cancer", "Teal", "CEAD", "ONC000402",
                "CHBL", "Chondroblastoma", "Bone Cancer", "White", "BONE", "ONC000403",
                "CHDM", "Chordoma", "Bone Cancer", "White", "BONE", "ONC000404",
                "CHGL", "Chordoid Glioma of the Third Ventricle", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000405",
                "CHL", "Classical Hodgkin Lymphoma", "Hodgkin Lymphoma", "LimeGreen", "HL", "ONC000406",
                "CHM", "Complete Hydatidiform Mole", "Gestational Trophoblastic Disease", "PeachPuff", "MP", "ONC000407",
                "CHOL", "Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "BILIARY_TRACT", "ONC000408",
                "CHOM", "Chordoid Meningioma", "CNS Cancer", "Gray", "MNGT", "ONC000409",
                "CHOS", "Chondroblastic Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000410",
                "CHRCC", "Chromophobe Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000411",
                "CHS", "Chondrosarcoma", "Bone Cancer", "White", "BONE", "ONC000412",
                "CLL", "Chronic Lymphocytic Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000413",
                "CLNC", "Cerebellar Liponeurocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000414",
                "CM", "Conjunctival Melanoma", "Melanoma", "Green", "OM", "ONC000415",
                "CMC", "Medullary Carcinoma of the Colon", "Colorectal Cancer", "SaddleBrown", "BOWEL", "ONC000416",
                "CML", "Chronic Myelogenous Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000417",
                "CMML", "Chronic Myelomonocytic Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000418",
                "CMPT", "Ciliated Muconodular Papillary Tumor of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000419",
                "CNC", "Central Neurocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000420",
                "COAD", "Colon Adenocarcinoma", "Colorectal Cancer", "SaddleBrown", "COADREAD", "ONC000421",
                "COADREAD", "Colorectal Adenocarcinoma", "Colorectal Cancer", "SaddleBrown", "BOWEL", "ONC000422",
                "COM", "Carcinoma with Osseous Metaplasia", "Breast Cancer", "HotPink", "MMBC", "ONC000423",
                "CPC", "Choroid Plexus Carcinoma", "Choroid Plexus Tumor", "Gray", "CPT", "ONC000424",
                "CPP", "Choroid Plexus Papilloma", "Choroid Plexus Tumor", "Gray", "CPT", "ONC000425",
                "CPT", "Choroid Plexus Tumor", "Choroid Plexus Tumor", "Gray", "BRAIN", "ONC000426",
                "CSCC", "Cutaneous Squamous Cell Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000427",
                "CSCHW", "Cellular Schwannoma", "Nerve Sheath Tumor", "Gray", "SCHW", "ONC000428",
                "CSCLC", "Combined Small Cell Lung Carcinoma", "Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000429",
                "CSNOS", "Breast Invasive Carcinosarcoma, NOS", "Breast Cancer", "HotPink", "BRCA", "ONC000430",
                "CTAAP", "Colonic Type Adenocarcinoma of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD", "ONC000431",
                "CTCL", "Cutaneous T-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "TNKL", "ONC000432",
                "CUP", "Cancer of Unknown Primary", "Cancer of Unknown Primary", "Black", "OTHER", "ONC000433",
                "CUPNOS", "Cancer of Unknown Primary, NOS", "Cancer of Unknown Primary", "Black", "CUP", "ONC000434",
                "DA", "Duodenal Adenocarcinoma", "Small Bowel Cancer", "SaddleBrown", "SBC", "ONC000435",
                "DCIS", "Breast Ductal Carcinoma In Situ", "Breast Cancer", "HotPink", "BREAST", "ONC000436",
                "DCS", "Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000437",
                "DDCHDM", "Dedifferentiated Chordoma", "Bone Cancer", "White", "CHDM", "ONC000438",
                "DDCHS", "Dedifferentiated Chondrosarcoma", "Bone Cancer", "White", "CHS", "ONC000439",
                "DDLS", "Dedifferentiated Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO", "ONC000440",
                "DES", "Desmoid/Aggressive Fibromatosis", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000441",
                "DESM", "Desmoplastic Melanoma", "Melanoma", "Black", "MEL", "ONC000442",
                "DF", "Dermatofibroma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000443",
                "DFSP", "Dermatofibrosarcoma Protuberans", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000444",
                "DIA", "Desmoplastic Infantile Astrocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000445",
                "DIFG", "Diffuse Glioma", "Glioma", "Gray", "BRAIN", "ONC000446",
                "DIG", "Desmoplastic Infantile Ganglioglioma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000447",
                "DIPG", "Diffuse Intrinsic Pontine Glioma", "Glioma", "Gray", "DIFG", "ONC000448",
                "DLBCL", "Diffuse Large B-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000449",
                "DMBL", "Desmoplastic/Nodular Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000450",
                "DNT", "Dysembryoplastic Neuroepithelial Tumor", "Glioma", "Gray", "ENCG", "ONC000451",
                "DSRCT", "Desmoplastic Small-Round-Cell Tumor", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000452",
                "DSTAD", "Diffuse Type Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD", "ONC000453",
                "DTE", "Desmoplastic Trichoepithelioma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000454",
                "EBOV", "Endometrioid Borderlin Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT", "ONC000455",
                "ECAD", "Endocervical Adenocarcinoma", "Cervical Cancer", "Teal", "CEAD", "ONC000456",
                "ECD", "Non-Langerhans Cell Histiocytosis/Erdheim-Chester Disease", "Histiocytosis", "LightSalmon", "HIST", "ONC000457",
                "EGC", "Esophagogastric Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH", "ONC000458",
                "EHAE", "Epithelioid Hemangioendothelioma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000459",
                "EHCH", "Extrahepatic Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "CHOL", "ONC000460",
                "EMBC", "Epithelial Type Metaplastic Breast Cancer", "Breast Cancer", "HotPink", "MBC", "ONC000461",
                "EMBCA", "Embryonal Carcinoma", "Germ Cell Tumor", "Red", "NSGCT", "ONC000462",
                "EMBT", "Embryonal Tumor", "Embryonal Tumor", "Gray", "BRAIN", "ONC000463",
                "EMCHS", "Extraskeletal Myxoid Chondrosarcoma", "Bone Cancer", "White", "CHS", "ONC000464",
                "EMPD", "Extramammary Paget Disease", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000465",
                "EMPSGC", "Endocrine Mucin Producing Sweat Gland Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000466",
                "EMYOCA", "Epithelial-Myoepithelial Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA", "ONC000467",
                "ENCG", "Encapsulated Glioma", "Glioma", "Gray", "BRAIN", "ONC000468",
                "EOV", "Endometrioid Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT", "ONC000469",
                "EPDCA", "Esophageal Poorly Differentiated Carcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH", "ONC000470",
                "EPIS", "Epithelioid Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000471",
                "EPM", "Ependymoma", "CNS Cancer", "Gray", "EPMT", "ONC000472",
                "EPMT", "Ependymomal Tumor", "CNS Cancer", "Gray", "BRAIN", "ONC000473",
                "ERMS", "Embryonal Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS", "ONC000474",
                "ES", "Ewing Sarcoma", "Bone Cancer", "White", "BONE", "ONC000475",
                "ESCA", "Esophageal Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC", "ONC000476",
                "ESCC", "Esophageal Squamous Cell Carcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH", "ONC000477",
                "ESMM", "Mucosal Melanoma of the Esophagus", "Melanoma", "LightSkyBlue", "STOMACH", "ONC000478",
                "ESS", "Endometrial Stromal Sarcoma", "Uterine Sarcoma", "PeachPuff", "USARC", "ONC000479",
                "ETANTR", "Embryonal Tumor with Abundant Neuropil and True Rosettes", "Embryonal Tumor", "Gray", "EMBT", "ONC000480",
                "ETC", "Essential Thrombocythaemia", "Myeloproliferative Neoplasm", "LightSalmon", "MPN", "ONC000481",
                "ETT", "Epithelioid Trophoblastic Tumor", "Gestational Trophoblastic Disease", "PeachPuff", "GTD", "ONC000482",
                "EVN", "Extraventricular Neurocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000483",
                "EYE", "Eye", null, "Green", "TISSUE", "ONC000484",
                "FA", "Fibroadenoma", "Breast Sarcoma", "HotPink", "BFN", "ONC000485",
                "FDCS", "Follicular Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "DCS", "ONC000486",
                "FIBS", "Fibrosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000487",
                "FIOS", "Fibroblastic Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000488",
                "FL", "Follicular Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000489",
                "FT", "Fibrothecoma", "Sex Cord Stromal Tumor", "LightBlue", "SCST", "ONC000490",
                "GB", "Glioblastoma", "Glioma", "Gray", "DIFG", "ONC000491",
                "GBC", "Gallbladder Cancer", "Hepatobiliary Cancer", "Green", "BILIARY_TRACT", "ONC000492",
                "GBM", "Glioblastoma Multiforme", "Glioma", "Gray", "GB", "ONC000493",
                "GCCAP", "Goblet Cell Carcinoid of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD", "ONC000494",
                "GCEMU", "Gastric Type Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEMU", "ONC000495",
                "GCLC", "Giant Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC", "ONC000496",
                "GCT", "Granular Cell Tumor", "Sellar Tumor", "Gray", "SELT", "ONC000497",
                "GCTB", "Giant Cell Tumor of Bone", "Bone Cancer", "White", "BONE", "ONC000498",
                "GEJ", "Adenocarcinoma of the Gastroesophageal Junction", "Esophagogastric Cancer", "LightSkyBlue", "EGC", "ONC000499",
                "GINET", "Gastrointestinal Neuroendocrine Tumors", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "BOWEL", "ONC000500",
                "GIST", "Gastrointestinal Stromal Tumor", "Gastrointestinal Stromal Tumor", "LightYellow", "SOFT_TISSUE", "ONC000501",
                "GMN", "Germinoma", "Germ Cell Tumor", "Gray", "BGCT", "ONC000502",
                "GMUCM", "Genitourinary Mucosal Melanoma", "Melanoma", "Black", "MEL", "ONC000503",
                "GNBL", "Ganglioneuroblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000504",
                "GNC", "Gangliocytoma", "Glioma", "Gray", "ENCG", "ONC000505",
                "GNG", "Ganglioglioma", "Glioma", "Gray", "ENCG", "ONC000506",
                "GRC", "Gastric Remnant Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC", "ONC000507",
                "GRCT", "Granulosa Cell Tumor", "Sex Cord Stromal Tumor", "LightBlue", "SCST", "ONC000508",
                "GS", "Glomangiosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000509",
                "GSARC", "Gliosarcoma", "Glioma", "Gray", "GB", "ONC000510",
                "GTD", "Gestational Trophoblastic Disease", "Gestational Trophoblastic Disease", "PeachPuff", "UTERUS", "ONC000511",
                "HCC", "Hepatocellular Carcinoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER", "ONC000512",
                "HCCIHCH", "Hepatocellular Carcinoma plus Intrahepatic Cholangiocarcinoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER", "ONC000513",
                "HCL", "Hairy Cell Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000514",
                "HDCS", "Histiocytic Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "DCS", "ONC000515",
                "HEAD_NECK", "Head and Neck", "Head and Neck Cancer", "DarkRed", "TISSUE", "ONC000516",
                "HEMA", "Hemangioma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000517",
                "HGESS", "High-Grade Endometrial Stromal Sarcoma", "Uterine Sarcoma", "PeachPuff", "ESS", "ONC000518",
                "HGGNOS", "High-Grade Glioma, NOS", "Glioma", "Gray", "DIFG", "ONC000519",
                "HGNEC", "High-Grade Neuroendocrine Carcinoma of the Colon and Rectum", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET", "ONC000520",
                "HGNET", "High-Grade Neuroepithelial Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000521",
                "HGONEC", "High-Grade Neuroendocrine Carcinoma of the Ovary", "Ovarian Cancer", "LightBlue", "OOVC", "ONC000522",
                "HGSOC", "High-Grade Serous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "SOC", "ONC000523",
                "HGSOS", "High-Grade Surface Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000524",
                "HIST", "Histiocytosis", "Histiocytosis", "LightSalmon", "BLOOD", "ONC000525",
                "HL", "Hodgkin Lymphoma", "Hodgkin Lymphoma", "LimeGreen", "LYMPH", "ONC000526",
                "HMBL", "Hemangioblastoma", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000527",
                "HNMASC", "Mammary Analogue Secretory Carcinoma of Salivary Gland Origin", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000528",
                "HNMUCM", "Head and Neck Mucosal Melanoma", "Melanoma", "DarkRed", "HEAD_NECK", "ONC000529",
                "HNNE", "Head and Neck Neuroendocrine Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA", "ONC000530",
                "HNSC", "Head and Neck Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HEAD_NECK", "ONC000531",
                "HNSCUP", "Head and Neck Squamous Cell Carcinoma of Unknown Primary", "Head and Neck Cancer", "DarkRed", "HNSC", "ONC000532",
                "HPCCNS", "Hemangiopericytoma of the Central Nervous System", "CNS Cancer", "Gray", "MNGT", "ONC000533",
                "HPHSC", "Hypopharynx Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC", "ONC000534",
                "HTAT", "Hyalinizing Trabecular Adenoma of the Thyroid", "Thyroid Cancer", "Teal", "THYROID", "ONC000535",
                "IAMPCA", "Intestinal Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPCA", "ONC000536",
                "IBC", "Inflammatory Breast Cancer", "Breast Cancer", "HotPink", "BREAST", "ONC000537",
                "ICEMU", "Intestinal Type Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEMU", "ONC000538",
                "IDC", "Breast Invasive Ductal Carcinoma", "Breast Cancer", "HotPink", "BRCA", "ONC000539",
                "IDCS", "Interdigitating Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "DCS", "ONC000540",
                "IHCH", "Intrahepatic Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "CHOL", "ONC000541",
                "IHM", "Invasive Hydatidiform Mole", "Gestational Trophoblastic Disease", "PeachPuff", "MP", "ONC000542",
                "ILC", "Breast Invasive Lobular Carcinoma", "Breast Cancer", "HotPink", "BRCA", "ONC000543",
                "IMMC", "Breast Invasive Mixed Mucinous Carcinoma", "Breast Cancer", "HotPink", "BRCA", "ONC000544",
                "IMT", "Inflammatory Myofibroblastic Tumor", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000545",
                "IMTB", "Inflammatory Myofibroblastic Bladder Tumor", "Bladder Cancer", "Yellow", "BLADDER", "ONC000546",
                "IMTL", "Inflammatory Myofibroblastic Lung Tumor", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000547",
                "INTS", "Intimal Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000548",
                "IPMN", "Intraductal Papillary Mucinous Neoplasm", "Pancreatic Cancer", "Purple", "PACT", "ONC000549",
                "ISTAD", "Intestinal Type Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD", "ONC000550",
                "KIDNEY", "Kidney", null, "Orange", "TISSUE", "ONC000551",
                "LAIS", "Lung Adenocarcinoma In Situ", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000552",
                "LAM", "Pulmonary Lymphangiomyomatosis", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000553",
                "LCH", "Langerhans Cell Histiocytosis", "Histiocytosis", "LightSalmon", "HIST", "ONC000554",
                "LCIS", "Breast Lobular Carcinoma In Situ", "Breast Cancer", "HotPink", "BREAST", "ONC000555",
                "LCLC", "Large Cell Lung Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000556",
                "LDD", "Dysplastic Gangliocytoma of the Cerebellum/Lhermitte-Duclos Disease", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000557",
                "LECLC", "Lymphoepithelioma-like Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC", "ONC000558",
                "LEUK", "Leukemia", "Leukemia", "LightSalmon", "BLOOD", "ONC000559",
                "LGCOS", "Low-Grade Central Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000560",
                "LGESS", "Low-Grade Endometrial Stromal Sarcoma", "Uterine Sarcoma", "PeachPuff", "ESS", "ONC000561",
                "LGFMS", "Low-Grade Fibromyxoid Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000562",
                "LGGNOS", "Low-Grade Glioma, NOS", "Glioma", "Gray", "ENCG", "ONC000563",
                "LGLL", "Large Granular Lymphocytic Leukemia", "Leukemia", "LightSalmon", "LEUK", "ONC000564",
                "LGNET", "Low-Grade Neuroepithelial Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000565",
                "LGSOC", "Low-Grade Serous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "SOC", "ONC000566",
                "LIAD", "Hepatocellular Adenoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER", "ONC000567",
                "LIAS", "Liver Angiosarcoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER", "ONC000568",
                "LIHB", "Hepatoblastoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER", "ONC000569",
                "LIMNET", "Malignant Nonepithelial Tumor of the Liver", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER", "ONC000570",
                "LIPO", "Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000571",
                "LIVER", "Liver", "Hepatobiliary Cancer", "MediumSeaGreen", "TISSUE", "ONC000572",
                "LMS", "Leiomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000573",
                "LNET", "Lung Neuroendocrine Tumor", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000574",
                "LUACC", "Adenoid Cystic Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "SGTTL", "ONC000575",
                "LUAD", "Lung Adenocarcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000576",
                "LUAS", "Lung Adenosquamous Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000577",
                "LUCA", "Lung Carcinoid", "Non-Small Cell Lung Cancer", "Gainsboro", "LNET", "ONC000578",
                "LUMEC", "Mucoepidermoid Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "SGTTL", "ONC000579",
                "LUNE", "Large Cell Neuroendocrine Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "LNET", "ONC000580",
                "LUNG", "Lung", null, "Gainsboro", "TISSUE", "ONC000581",
                "LUPC", "Pleomorphic Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000582",
                "LUSC", "Lung Squamous Cell Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000583",
                "LXSC", "Larynx Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC", "ONC000584",
                "LYMPH", "Lymph", null, "LimeGreen", "TISSUE", "ONC000585",
                "MAAP", "Mucinous Adenocarcinoma of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD", "ONC000586",
                "MAC", "Microcystic Adnexal Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000587",
                "MACR", "Mucinous Adenocarcinoma of the Colon and Rectum", "Colorectal Cancer", "SaddleBrown", "COADREAD", "ONC000588",
                "MALTL", "Extranodal Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000589",
                "MAMPCA", "Mixed Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPCA", "ONC000590",
                "MASC", "Metaplastic Adenosquamous Carcinoma", "Breast Cancer", "HotPink", "EMBC", "ONC000591",
                "MASCC", "Metaplastic Adenocarcinoma with Spindle Cell Differentiation", "Breast Cancer", "HotPink", "EMBC", "ONC000592",
                "MBC", "Metaplastic Breast Cancer", "Breast Cancer", "HotPink", "BREAST", "ONC000593",
                "MBCL", "Mediastinal Large B-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000594",
                "MBEN", "Medulloblastoma with Extensive Nodularity", "Embryonal Tumor", "Gray", "EMBT", "ONC000595",
                "MBL", "Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000596",
                "MBOV", "Mucinous Borderline Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT", "ONC000597",
                "MBT", "Miscellaneous Brain Tumor", "Miscellaneous Brain Tumor", "Gray", "BRAIN", "ONC000598",
                "MCC", "Merkel Cell Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000599",
                "MCCE", "Mixed Cervical Carcinoma", "Cervical Cancer", "Teal", "CERVIX", "ONC000600",
                "MCHS", "Mesenchymal Chondrosarcoma", "Bone Cancer", "White", "CHS", "ONC000601",
                "MCHSCNS", "Mesenchymal Chondrosarcoma of the CNS", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000602",
                "MCL", "Mantle Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000603",
                "MCN", "Mucinous Cystic Neoplasm", "Pancreatic Cancer", "Purple", "PACT", "ONC000604",
                "MCS", "Metaplastic Carcinosarcoma", "Breast Cancer", "HotPink", "MMBC", "ONC000605",
                "MDEP", "Medulloepithelioma", "Embryonal Tumor", "Gray", "EMBT", "ONC000606",
                "MDLC", "Breast Mixed Ductal and Lobular Carcinoma", "Breast Cancer", "HotPink", "BRCA", "ONC000607",
                "MDS", "Myelodysplasia", "Myelodysplasia", "LightSalmon", "BLOOD", "ONC000608",
                "MEL", "Melanoma", "Melanoma", "Black", "SKIN", "ONC000609",
                "MF", "Myofibroma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000610",
                "MFH", "Undifferentiated Pleomorphic Sarcoma/Malignant Fibrous Histiocytoma/High-Grade Spindle Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000611",
                "MFS", "Myxofibrosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000612",
                "MGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "Red", "NSGCT", "ONC000613",
                "MIXED", "Mixed Cancer Types", "Cancer of Unknown Primary", "Black", "CUP", "ONC000614",
                "MLYM", "Malignant Lymphoma", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000615",
                "MM", "Multiple Myeloma", "Multiple Myeloma", "LightSalmon", "BLOOD", "ONC000616",
                "MMB", "Medullomyoblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000617",
                "MMBC", "Mixed Type Metaplastic Breast Cancer", "Breast Cancer", "HotPink", "MBC", "ONC000618",
                "MMBL", "Melanotic Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000619",
                "MNET", "Miscellaneous Neuroepithelial Tumor", "Miscellaneous Neuroepithelial Tumor", "Gray", "BRAIN", "ONC000620",
                "MNG", "Meningioma", "CNS Cancer", "Gray", "MNGT", "ONC000621",
                "MNGT", "Meningothelial Tumor", "CNS Cancer", "Gray", "BRAIN", "ONC000622",
                "MOV", "Mucinous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT", "ONC000623",
                "MP", "Molar Pregnancy", "Gestational Trophoblastic Disease", "PeachPuff", "GTD", "ONC000624",
                "MPC", "Myopericytoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000625",
                "MPE", "Myxopapillary Ependymoma", "CNS Cancer", "Gray", "EPMT", "ONC000626",
                "MPN", "Myeloproliferative Neoplasm", "Myeloproliferative Neoplasm", "LightSalmon", "BLOOD", "ONC000627",
                "MPNST", "Malignant Peripheral Nerve Sheath Tumor", "Nerve Sheath Tumor", "Gray", "NST", "ONC000628",
                "MPT", "Malignant Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "PT", "ONC000629",
                "MRC", "Renal Medullary Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000630",
                "MRLS", "Myxoid/Round-Cell Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO", "ONC000631",
                "MRT", "Rhabdoid Cancer", "Wilms Tumor", "Orange", "WT", "ONC000632",
                "MSCC", "Metaplastic Squamous Cell Carcinoma", "Breast Cancer", "HotPink", "EMBC", "ONC000633",
                "MSCHW", "Melanotic Schwannoma", "Nerve Sheath Tumor", "Gray", "SCHW", "ONC000634",
                "MSTAD", "Mucinous Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD", "ONC000635",
                "MT", "Malignant Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000636",
                "MTSCC", "Renal Mucinous Tubular Spindle Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000637",
                "MUCC", "Mucoepidermoid Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000638",
                "MUP", "Melanoma of Unknown Primary", "Melanoma", "Black", "MEL", "ONC000639",
                "MXOV", "Mixed Ovarian Carcinoma", "Ovarian Cancer", "LightBlue", "OVT", "ONC000640",
                "MYCF", "Mycosis Fungoides", "Non-Hodgkin Lymphoma", "LimeGreen", "CTCL", "ONC000641",
                "MYCHS", "Myxoid Chondrosarcoma", "Bone Cancer", "White", "CHS", "ONC000642",
                "MYEC", "Myoepithelial Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000643",
                "MYF", "Myelofibrosis/Osteomyelofibrosis", "Myeloproliferative Neoplasm", "LightSalmon", "MPN", "ONC000644",
                "MYXO", "Myxoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000645",
                "MZL", "Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000646",
                "NBL", "Neuroblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000647",
                "NCCRCC", "Renal Non-Clear Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "RCC", "ONC000648",
                "NECNOS", "Neuroendocrine Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP", "ONC000649",
                "NETNOS", "Neuroendocrine Tumor, NOS", "Cancer of Unknown Primary", "Black", "CUP", "ONC000650",
                "NFIB", "Neurofibroma", "Nerve Sheath Tumor", "Gray", "NST", "ONC000651",
                "NHL", "Non-Hodgkin Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "LYMPH", "ONC000652",
                "NLPHL", "Nodular Lymphocyte-Predominant Hodgkin Lymphoma", "Hodgkin Lymphoma", "LimeGreen", "HL", "ONC000653",
                "NMCHN", "NUT Midline Carcinoma of the Head and Neck", "Head and Neck Cancer", "DarkRed", "OHNCA", "ONC000654",
                "NMZL", "Nodal Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000655",
                "NPC", "Nasopharyngeal Carcinoma", "Head and Neck Cancer", "DarkRed", "HEAD_NECK", "ONC000656",
                "NSCLC", "Non-Small Cell Lung Cancer", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000657",
                "NSCLCPD", "Poorly Differentiated Non-Small Cell Lung Cancer", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000658",
                "NSGCT", "Non-Seminomatous Germ Cell Tumor", "Germ Cell Tumor", "Red", "TESTIS", "ONC000659",
                "NST", "Nerve Sheath Tumor", "Nerve Sheath Tumor", "Gray", "PNS", "ONC000660",
                "OAST", "Oligoastrocytoma", "Glioma", "Gray", "DIFG", "ONC000661",
                "OCS", "Ovarian Carcinosarcoma/Malignant Mixed Mesodermal Tumor", "Ovarian Cancer", "LightBlue", "OVT", "ONC000662",
                "OCSC", "Oral Cavity Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC", "ONC000663",
                "ODG", "Oligodendroglioma", "Glioma", "Gray", "DIFG", "ONC000664",
                "ODGC", "Odontogenic Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA", "ONC000665",
                "ODYS", "Dysgerminoma", "Germ Cell Tumor", "LightBlue", "OGCT", "ONC000666",
                "OEC", "Embryonal Carcinoma", "Germ Cell Tumor", "LightBlue", "OGCT", "ONC000667",
                "OFMT", "Ossifying Fibromyxoid Tumor", "Soft Tissue Sarcoma", "LightYellow", "MYXO", "ONC000668",
                "OGBL", "Gonadoblastoma", "Sex Cord Stromal Tumor", "LightBlue", "SCST", "ONC000669",
                "OGCT", "Ovarian Germ Cell Tumor", "Germ Cell Tumor", "LightBlue", "OVARY", "ONC000670",
                "OHNCA", "Head and Neck Carcinoma, Other", "Head and Neck Cancer", "DarkRed", "HEAD_NECK", "ONC000671",
                "OIMT", "Immature Teratoma", "Germ Cell Tumor", "LightBlue", "OGCT", "ONC000672",
                "OM", "Ocular Melanoma", "Melanoma", "Green", "EYE", "ONC000673",
                "OMGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "LightBlue", "OGCT", "ONC000674",
                "OMT", "Mature Teratoma", "Germ Cell Tumor", "LightBlue", "OGCT", "ONC000675",
                "ONBL", "Olfactory Neuroblastoma", "Embryonal Tumor", "Gray", "EMBT", "ONC000676",
                "OOVC", "Ovarian Cancer, Other", "Ovarian Cancer", "LightBlue", "OVARY", "ONC000677",
                "OPE", "Polyembryoma", "Germ Cell Tumor", "LightBlue", "OGCT", "ONC000678",
                "OPHSC", "Oropharynx Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC", "ONC000679",
                "OS", "Osteosarcoma", "Bone Cancer", "White", "BONE", "ONC000680",
                "OSACA", "Salivary Carcinoma, Other", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000681",
                "OSMAD", "Ovarian Seromucinous Adenoma", "Ovarian Cancer", "LightBlue", "OVT", "ONC000682",
                "OSMBT", "Ovarian Seromucinous Borderline Tumor", "Ovarian Cancer", "LightBlue", "OVT", "ONC000683",
                "OSMCA", "Ovarian Seromucinous Carcinoma", "Ovarian Cancer", "LightBlue", "OVT", "ONC000684",
                "OSOS", "Osteoblastic Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000685",
                "OTHER", "Other", null, "Black", "TISSUE", "ONC000686",
                "OUSARC", "Uterine Sarcoma, Other", "Uterine Sarcoma", "PeachPuff", "USARC", "ONC000687",
                "OUTT", "Other Uterine Tumor", "Endometrial Cancer", "PeachPuff", "UTERUS", "ONC000688",
                "OVARY", "Ovary/Fallopian Tube", "Ovarian Cancer", "LightBlue", "TISSUE", "ONC000689",
                "OVT", "Ovarian Epithelial Tumor", "Ovarian Cancer", "LightBlue", "OVARY", "ONC000690",
                "OYST", "Yolk Sac Tumor", "Germ Cell Tumor", "LightBlue", "OGCT", "ONC000691",
                "PAAC", "Acinar Cell Carcinoma of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000692",
                "PAAD", "Pancreatic Adenocarcinoma", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000693",
                "PAASC", "Adenosquamous Carcinoma of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000694",
                "PACT", "Cystic Tumor of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000695",
                "PAMPCA", "Pancreatobiliary Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPCA", "ONC000696",
                "PANCREAS", "Pancreas", "Pancreatic Cancer", "Purple", "TISSUE", "ONC000697",
                "PANET", "Pancreatic Neuroendocrine Tumor", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000698",
                "PAOS", "Parosteal Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000699",
                "PAST", "Pilocytic Astrocytoma", "Glioma", "Gray", "ENCG", "ONC000700",
                "PB", "Pancreatoblastoma", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000701",
                "PBL", "Pineoblastoma", "Pineal Tumor", "Gray", "PINT", "ONC000702",
                "PBS", "Breast Sarcoma", "Breast Sarcoma", "HotPink", "BREAST", "ONC000703",
                "PBT", "Primary Brain Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000704",
                "PCGP", "Craniopharyngioma, Papillary Type", "Sellar Tumor", "Gray", "SELT", "ONC000705",
                "PCNSL", "Primary CNS Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000706",
                "PCNSM", "Primary CNS Melanoma", "Melanoma", "LightSkyBlue", "BRAIN", "ONC000707",
                "PCT", "Porphyria Cutania Tarda", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000708",
                "PCV", "Polycythemia Vera", "Myeloproliferative Neoplasm", "LightSalmon", "MPN", "ONC000709",
                "PD", "Paget Disease of the Nipple", "Breast Cancer", "HotPink", "DCIS", "ONC000710",
                "PDC", "Poorly Differentiated Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP", "ONC000711",
                "PECOMA", "Perivascular Epithelioid Cell Tumor", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000712",
                "PEL", "Primary Effusion Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000713",
                "PEMESO", "Peritoneal Mesothelioma", "Mesothelioma", "Green", "PERITONEUM", "ONC000714",
                "PENIS", "Penis", "Penile Cancer", "Blue", "TISSUE", "ONC000715",
                "PEOS", "Periosteal Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000716",
                "PERITONEUM", "Peritoneum", "Mesothelioma", "Green", "TISSUE", "ONC000717",
                "PGNG", "Paraganglioma", "Miscellaneous Neuroepithelial Tumor", "Gray", "SOFT_TISSUE", "ONC000718",
                "PGNT", "Papillary Glioneuronal Tumor", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000719",
                "PHC", "Pheochromocytoma", "Pheochromocytoma", "Purple", "ADRENAL_GLAND", "ONC000720",
                "PHCH", "Perihilar Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "CHOL", "ONC000721",
                "PHM", "Partial Hydatidiform Mole", "Gestational Trophoblastic Disease", "PeachPuff", "MP", "ONC000722",
                "PINC", "Pineocytoma", "Pineal Tumor", "Gray", "PINT", "ONC000723",
                "PINT", "Pineal Tumor", "Pineal Tumor", "Gray", "BRAIN", "ONC000724",
                "PLBMESO", "Pleural Mesothelioma, Biphasic Type", "Mesothelioma", "Blue", "PLMESO", "ONC000725",
                "PLEMESO", "Pleural Mesothelioma, Epithelioid Type", "Mesothelioma", "Blue", "PLMESO", "ONC000726",
                "PLEURA", "Pleura", "Mesothelioma", "Blue", "TISSUE", "ONC000727",
                "PLLS", "Pleomorphic Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO", "ONC000728",
                "PLMESO", "Pleural Mesothelioma", "Mesothelioma", "Blue", "PLEURA", "ONC000729",
                "PLRMS", "Pleomorphic Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS", "ONC000730",
                "PLSMESO", "Pleural Mesothelioma, Sarcomatoid Type", "Mesothelioma", "Blue", "PLMESO", "ONC000731",
                "PMA", "Pilomyxoid Astrocytoma", "Glioma", "Gray", "ENCG", "ONC000732",
                "PMHE", "Pseudomyogenic Hemangioendothelioma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000733",
                "PNET", "Primitive Neuroectodermal Tumor", "Embryonal Tumor", "Gray", "EMBT", "ONC000734",
                "PNS", "Peripheral Nervous System", "Nerve Sheath Tumor", "Gray", "TISSUE", "ONC000735",
                "POCA", "Porocarcinoma/Spiroadenocarcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000736",
                "PORO", "Poroma/Acrospiroma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000737",
                "PPB", "Pleuropulmonary Blastoma", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000738",
                "PPCT", "Proliferating Pilar Cystic Tumor", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000739",
                "PPM", "Papillary Meningioma", "CNS Cancer", "Gray", "MNGT", "ONC000740",
                "PPTID", "Pineal Parenchymal Tumor of Intermediate Differentiation", "Pineal Tumor", "Gray", "PINT", "ONC000741",
                "PRAD", "Prostate Adenocarcinoma", "Prostate Cancer", "Cyan", "PROSTATE", "ONC000742",
                "PRCC", "Papillary Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000743",
                "PRNE", "Prostate Neuroendocrine Carcinoma", "Prostate Cancer", "Cyan", "PROSTATE", "ONC000744",
                "PRNET", "Primary Neuroepithelial Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT", "ONC000745",
                "PROSTATE", "Prostate", "Prostate Cancer", "Cyan", "TISSUE", "ONC000746",
                "PRSC", "Prostate Sqamous Cell Carcinoma", "Prostate Cancer", "Cyan", "PROSTATE", "ONC000747",
                "PRSCC", "Prostate Small Cell Carcinoma", "Prostate Cancer", "Cyan", "PROSTATE", "ONC000748",
                "PSC", "Serous Cystadenoma of the Pancreas", "Pancreatic Cancer", "Purple", "PACT", "ONC000749",
                "PSCC", "Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PENIS", "ONC000750",
                "PSTAD", "Papillary Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD", "ONC000751",
                "PSTT", "Placental Site Trophoblastic Tumor", "Gestational Trophoblastic Disease", "PeachPuff", "GTD", "ONC000752",
                "PT", "Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "BFN", "ONC000753",
                "PTAD", "Pituitary Adenoma", "Sellar Tumor", "Gray", "SELT", "ONC000754",
                "PTCA", "Pituitary Carcinoma", "Sellar Tumor", "Gray", "SELT", "ONC000755",
                "PTCL", "Peripheral T-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "TNKL", "ONC000756",
                "PTCLNOS", "Peripheral T-Cell Lymphoma, NOS", "Non-Hodgkin Lymphoma", "LimeGreen", "PTCL", "ONC000757",
                "PTCY", "Pituicytoma", "Sellar Tumor", "Gray", "SELT", "ONC000758",
                "PTES", "Proximal-Type Epithelioid Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "EPIS", "ONC000759",
                "PTPR", "Papillary Tumor of the Pineal Region", "Pineal Tumor", "Gray", "PINT", "ONC000760",
                "PXA", "Pleomorphic Xanthoastrocytoma", "Glioma", "Gray", "ENCG", "ONC000761",
                "RAML", "Renal Angiomyolipoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000762",
                "RAS", "Radiation-Associated Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000763",
                "RBL", "Retinoblastoma", "Retinoblastoma", "Green", "EYE", "ONC000764",
                "RCC", "Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "KIDNEY", "ONC000765",
                "RCSNOS", "Round Cell Sarcoma, NOS", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000766",
                "RD", "Rosai-Dorfman Disease", "Histiocytic Disorder", "LimeGreen", "LYMPH", "ONC000767",
                "READ", "Rectal Adenocarcinoma", "Colorectal Cancer", "SaddleBrown", "COADREAD", "ONC000768",
                "RGNT", "Rosette-forming Glioneuronal Tumor of the Fourth Ventricle", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET", "ONC000769",
                "RHM", "Rhabdoid Meningioma", "CNS Cancer", "Gray", "MNGT", "ONC000770",
                "RLCLC", "Large Cell Lung Carcinoma With Rhabdoid Phenotype", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC", "ONC000771",
                "RMS", "Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000772",
                "ROCY", "Renal Oncocytoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000773",
                "RSCC", "Renal Small Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000774",
                "RWDNET", "Well-Differentiated Neuroendocrine Tumor of the Rectum", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET", "ONC000775",
                "SAAD", "Salivary Adenocarcinoma", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000776",
                "SACA", "Salivary Carcinoma", "Salivary Gland Cancer", "DarkRed", "HEAD_NECK", "ONC000777",
                "SARCL", "Sarcomatoid Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG", "ONC000778",
                "SARCNOS", "Sarcoma, NOS", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000779",
                "SBC", "Small Bowel Cancer", "Small Bowel Cancer", "SaddleBrown", "BOWEL", "ONC000780",
                "SBMOV", "Serous Borderline Ovarian Tumor, Micropapillary", "Ovarian Cancer", "LightBlue", "OVT", "ONC000781",
                "SBOV", "Serous Borderline Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT", "ONC000782",
                "SBWDNET", "Small Bowel Well-Differentiated Neuroendocrine Tumor", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET", "ONC000783",
                "SCB", "Sarcomatoid Carcinoma of the Urinary Bladder", "Bladder Cancer", "Yellow", "BLADDER", "ONC000784",
                "SCBC", "Small Cell Bladder Cancer", "Bladder Cancer", "Yellow", "BLADDER", "ONC000785",
                "SCCE", "Small Cell Carcinoma of the Cervix", "Cervical Cancer", "Teal", "CERVIX", "ONC000786",
                "SCCNOS", "Squamous Cell Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP", "ONC000787",
                "SCCO", "Small Cell Carcinoma of the Ovary", "Ovarian Cancer", "LightBlue", "OVT", "ONC000788",
                "SCCRCC", "Renal Clear Cell Carcinoma with Sarcomatoid Features", "Renal Cell Carcinoma", "Orange", "CCRCC", "ONC000789",
                "SCEMU", "Signet Ring Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEMU", "ONC000790",
                "SCGBM", "Small Cell Glioblastoma", "Glioma", "Gray", "GB", "ONC000791",
                "SCHW", "Schwannoma", "Nerve Sheath Tumor", "Gray", "NST", "ONC000792",
                "SCLC", "Small Cell Lung Cancer", "Small Cell Lung Cancer", "Gainsboro", "LNET", "ONC000793",
                "SCOAH", "Spindle Cell Oncocytoma of the Adenohypophysis", "Sellar Tumor", "Gray", "SELT", "ONC000794",
                "SCOS", "Small Cell Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000795",
                "SCRMS", "Spindle Cell Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS", "ONC000796",
                "SCSRMS", "Spindle Cell/Sclerosing Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS", "ONC000797",
                "SCST", "Sex Cord Stromal Tumor", "Sex Cord Stromal Tumor", "LightBlue", "OVARY", "ONC000798",
                "SCT", "Steroid Cell Tumor, NOS", "Sex Cord Stromal Tumor", "LightBlue", "SCST", "ONC000799",
                "SCUP", "Small Cell Carcinoma of Unknown Primary", "Cancer of Unknown Primary", "Black", "CUP", "ONC000800",
                "SDCA", "Salivary Duct Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000801",
                "SEBA", "Sebaceous Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000802",
                "SECOS", "Secondary Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000803",
                "SEF", "Sclerosing Epithelioid Fibrosarcoma", "Soft Tissue Sarcoma", "LightYellow", "FIBS", "ONC000804",
                "SELT", "Sellar Tumor", "Sellar Tumor", "Gray", "BRAIN", "ONC000805",
                "SEM", "Seminoma", "Germ Cell Tumor", "Red", "TESTIS", "ONC000806",
                "SEZS", "Sezary Syndrome", "Non-Hodgkin Lymphoma", "LimeGreen", "CTCL", "ONC000807",
                "SFT", "Solitary Fibrous Tumor/Hemangiopericytoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000808",
                "SFTCNS", "Solitary Fibrous Tumor of the Central Nervous System", "CNS Cancer", "Gray", "MNGT", "ONC000809",
                "SGAD", "Sweat Gland Adenocarcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000810",
                "SGO", "Salivary Gland Oncocytoma", "Salivary Gland Cancer", "DarkRed", "SACA", "ONC000811",
                "SGTTL", "Salivary Gland–Type Tumor of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000812",
                "SIC", "Small Intestinal Carcinoma", "Small Bowel Cancer", "SaddleBrown", "BOWEL", "ONC000813",
                "SKAC", "Skin Adnexal Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000814",
                "SKCM", "Cutaneous Melanoma", "Melanoma", "Black", "MEL", "ONC000815",
                "SKCN", "Congenital Nevus", "Melanoma", "Black", "MEL", "ONC000816",
                "SKIN", "Skin", null, "Black", "TISSUE", "ONC000817",
                "SKLMM", "Lentigo Maligna Melanoma", "Melanoma", "Black", "MEL", "ONC000818",
                "SLCT", "Sertoli-Leydig Cell Tumor", "Sex Cord Stromal Tumor", "LightBlue", "SCST", "ONC000819",
                "SLL", "Small Lymphocytic Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000820",
                "SM", "Systemic Mastocytosis", "Mastocytosis", "LightSalmon", "BLOOD", "ONC000821",
                "SMN", "Smooth Muscle Neoplasm, NOS", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH", "ONC000822",
                "SMZL", "Splenic Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000823",
                "SNA", "Sinonasal Adenocarcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA", "ONC000824",
                "SNSC", "Sinonasal Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC", "ONC000825",
                "SNUC", "Sinonasal Undifferentiated Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA", "ONC000826",
                "SOC", "Serous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT", "ONC000827",
                "SOFT_TISSUE", "Soft Tissue", null, "LightYellow", "TISSUE", "ONC000828",
                "SPC", "Solid Papillary Carcinoma of the Breast", "Breast Cancer", "HotPink", "BRCA", "ONC000829",
                "SPDAC", "Poorly Differentiated Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "USTAD", "ONC000830",
                "SPIR", "Spiroma/Spiradenoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN", "ONC000831",
                "SPN", "Solid Pseudopapillary Neoplasm of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000832",
                "SRAP", "Signet Ring Cell Type of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD", "ONC000833",
                "SRCBC", "Plasmacytoid/Signet Ring Cell Bladder Carcinoma", "Bladder Cancer", "Yellow", "BLADDER", "ONC000834",
                "SRCC", "Sarcomatoid Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000835",
                "SRCCR", "Signet Ring Cell Adenocarcinoma of the Colon and Rectum", "Colorectal Cancer", "SaddleBrown", "COADREAD", "ONC000836",
                "SSRCC", "Signet Ring Cell Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "USTAD", "ONC000837",
                "STAD", "Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC", "ONC000838",
                "STAS", "Adenosquamous Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "EGC", "ONC000839",
                "STMYEC", "Soft Tissue Myoepithelial Carcinoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000840",
                "STOMACH", "Esophagus/Stomach", "Esophagogastric Cancer", "LightSkyBlue", "TISSUE", "ONC000841",
                "STSC", "Small Cell Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "EGC", "ONC000842",
                "SUBE", "Subependymoma", "CNS Cancer", "Gray", "EPMT", "ONC000843",
                "SWDNET", "Well-Differentiated Neuroendocrine Tumors of the Stomach", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET", "ONC000844",
                "SYNS", "Synovial Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000845",
                "SpCC", "Spindle Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC", "ONC000846",
                "TALL", "T-Cell Acute Lymphoid Leukemia", "Leukemia", "LightSalmon", "ALL", "ONC000847",
                "TCCA", "Choriocarcinoma", "Germ Cell Tumor", "Red", "NSGCT", "ONC000848",
                "TEOS", "Telangiectatic Osteosarcoma", "Bone Cancer", "White", "OS", "ONC000849",
                "TESTIS", "Testis", null, "Red", "TISSUE", "ONC000850",
                "TET", "Thymic Epithelial Tumor", "Thymic Tumor", "Purple", "THYMUS", "ONC000851",
                "TGCT", "Tenosynovial Giant Cell Tumor Diffuse Type", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE", "ONC000852",
                "THAP", "Anaplastic Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID", "ONC000853",
                "THFO", "Follicular Thyroid Cancer", "Thyroid Cancer", "Teal", "WDTC", "ONC000854",
                "THHC", "Hurthle Cell Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID", "ONC000855",
                "THME", "Medullary Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID", "ONC000856",
                "THPA", "Papillary Thyroid Cancer", "Thyroid Cancer", "Teal", "WDTC", "ONC000857",
                "THPD", "Poorly Differentiated Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID", "ONC000858",
                "THYC", "Thymic Carcinoma", "Thymic Tumor", "Purple", "TET", "ONC000859",
                "THYM", "Thymoma", "Thymic Tumor", "Purple", "TET", "ONC000860",
                "THYMUS", "Thymus", "Thymic Tumor", "Purple", "TISSUE", "ONC000861",
                "THYROID", "Thyroid", "Thyroid Cancer", "Teal", "TISSUE", "ONC000862",
                "TISSUE", "Tissue", null, null, null, "ONC000863",
                "TLYM", "Testicular Lymphoma", "Non-Hodgkin Lymphoma", "Red", "TESTIS", "ONC000864",
                "TMESO", "Testicular Mesothelioma", "Mesothelioma", "Red", "TESTIS", "ONC000865",
                "TMT", "Teratoma with Malignant Transformation", "Germ Cell Tumor", "Red", "NSGCT", "ONC000866",
                "TNET", "Thymic Neuroendocrine Tumor", "Thymic Tumor", "Purple", "THYMUS", "ONC000867",
                "TNKL", "T-Cell and Natural Killer Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "NHL", "ONC000868",
                "TRCC", "Translocation-Associated Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000869",
                "TSCST", "Sex Cord Stromal Tumor", "Sex Cord Stromal Tumor", "Red", "TESTIS", "ONC000870",
                "TSTAD", "Tubular Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD", "ONC000871",
                "TT", "Teratoma", "Germ Cell Tumor", "Red", "NSGCT", "ONC000872",
                "TYST", "Yolk Sac Tumor", "Germ Cell Tumor", "Red", "NSGCT", "ONC000873",
                "UA", "Urachal Adenocarcinoma", "Bladder Cancer", "Yellow", "URCA", "ONC000874",
                "UAD", "Urethral Adenocarcinoma", "Bladder Cancer", "Yellow", "UCA", "ONC000875",
                "UAS", "Uterine Adenosarcoma", "Uterine Sarcoma", "PeachPuff", "USARC", "ONC000876",
                "UASC", "Uterine Adenosquamous Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000877",
                "UCA", "Urethral Cancer", "Bladder Cancer", "Yellow", "BLADDER", "ONC000878",
                "UCCA", "Choriocarcinoma", "Gestational Trophoblastic Disease", "PeachPuff", "GTD", "ONC000879",
                "UCCC", "Uterine Clear Cell Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000880",
                "UCEC", "Endometrial Carcinoma", "Endometrial Cancer", "PeachPuff", "UTERUS", "ONC000881",
                "UCP", "Undifferentiated Carcinoma of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS", "ONC000882",
                "UCS", "Uterine Carcinosarcoma/Uterine Malignant Mixed Mullerian Tumor", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000883",
                "UCU", "Urethral Urothelial Carcinoma", "Bladder Cancer", "Yellow", "UCA", "ONC000884",
                "UDDC", "Uterine Dedifferentiated Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000885",
                "UDMN", "Undifferentiated Malignant Neoplasm", "Cancer of Unknown Primary", "Black", "CUP", "ONC000886",
                "UEC", "Uterine Endometrioid Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000887",
                "UELMS", "Uterine Epithelioid Leiomyosarcoma", "Uterine Sarcoma", "PeachPuff", "USMT", "ONC000888",
                "ULM", "Uterine Leiomyoma", "Uterine Sarcoma", "PeachPuff", "USMT", "ONC000889",
                "ULMS", "Uterine Leiomyosarcoma", "Uterine Sarcoma", "PeachPuff", "USMT", "ONC000890",
                "UM", "Uveal Melanoma", "Melanoma", "Green", "OM", "ONC000891",
                "UMC", "Uterine Mucinous Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000892",
                "UMEC", "Uterine Mixed Endometrial Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000893",
                "UMLMS", "Uterine Myxoid Leiomyosarcoma", "Uterine Sarcoma", "PeachPuff", "USMT", "ONC000894",
                "UMNC", "Uterine Mesonephric Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000895",
                "UNEC", "Uterine Neuroendocrine Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000896",
                "UPDC", "Poorly Differentiated Carcinoma of the Uterus", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000897",
                "UPECOMA", "Uterine Perivascular Epithelioid Cell Tumor", "Uterine Sarcoma", "PeachPuff", "USARC", "ONC000898",
                "URCA", "Urachal Carcinoma", "Bladder Cancer", "Yellow", "BLADDER", "ONC000899",
                "URCC", "Unclassified Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC", "ONC000900",
                "URMM", "Mucosal Melanoma of the Urethra", "Melanoma", "Yellow", "BLADDER", "ONC000901",
                "USARC", "Uterine Sarcoma/Mesenchymal", "Uterine Sarcoma", "PeachPuff", "UTERUS", "ONC000902",
                "USC", "Uterine Serous Carcinoma/Uterine Papillary Serous Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000903",
                "USCC", "Urethral Squamous Cell Carcinoma", "Bladder Cancer", "Yellow", "UCA", "ONC000904",
                "USMT", "Uterine Smooth Muscle Tumor", "Uterine Sarcoma", "PeachPuff", "USARC", "ONC000905",
                "USTAD", "Undifferentiated Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC", "ONC000906",
                "USTUMP", "Uterine Smooth Muscle Tumor of Uncertain Malignant Potential", "Uterine Sarcoma", "PeachPuff", "USMT", "ONC000907",
                "UTERUS", "Uterus", null, "PeachPuff", "TISSUE", "ONC000908",
                "UTUC", "Upper Tract Urothelial Carcinoma", "Bladder Cancer", "Yellow", "BLADDER", "ONC000909",
                "UUC", "Uterine Undifferentiated Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC", "ONC000910",
                "VA", "Vaginal Adenocarcinoma", "Vaginal Cancer", "Purple", "VULVA", "ONC000911",
                "VDYS", "Dysgerminoma", "Germ Cell Tumor", "Purple", "VGCT", "ONC000912",
                "VGCE", "Villoglandular Adenocarcinoma of the Cervix", "Cervical Cancer", "Teal", "CERVIX", "ONC000913",
                "VGCT", "Germ Cell Tumor of the Vulva", "Germ Cell Tumor", "Purple", "VULVA", "ONC000914",
                "VIMT", "Immature Teratoma", "Germ Cell Tumor", "Purple", "VGCT", "ONC000915",
                "VMA", "Mucinous Adenocarcinoma of the Vulva/Vagina", "Vulvar Carcinoma", "Purple", "VULVA", "ONC000916",
                "VMGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "Purple", "VGCT", "ONC000917",
                "VMM", "Mucosal Melanoma of the Vulva/Vagina", "Melanoma", "Purple", "VULVA", "ONC000918",
                "VMT", "Mature Teratoma", "Germ Cell Tumor", "Purple", "VGCT", "ONC000919",
                "VOEC", "Embryonal Carcinoma", "Germ Cell Tumor", "Purple", "VGCT", "ONC000920",
                "VPDC", "Poorly Differentiated Vaginal Carcinoma", "Vaginal Cancer", "Purple", "VULVA", "ONC000921",
                "VPE", "Polyembryoma", "Germ Cell Tumor", "Purple", "VGCT", "ONC000922",
                "VPSCC", "Verrucous Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PSCC", "ONC000923",
                "VSC", "Squamous Cell Carcinoma of the Vulva/Vagina", "Vaginal Cancer", "Purple", "VULVA", "ONC000924",
                "VULVA", "Vulva/Vagina", null, "Purple", "TISSUE", "ONC000925",
                "VYST", "Yolk Sac Tumor", "Germ Cell Tumor", "Purple", "VGCT", "ONC000926",
                "WDLS", "Well-Differentiated Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO", "ONC000927",
                "WDTC", "Well-Differentiated Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID", "ONC000928",
                "WM", "Waldenstrom Macroglobulinemia", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL", "ONC000929",
                "WPSCC", "Warty Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PSCC", "ONC000930",
                "WT", "Wilms' Tumor", "Wilms Tumor", "Orange", "KIDNEY", "ONC000931"};
        return rawTestValueSource;
    }
}
