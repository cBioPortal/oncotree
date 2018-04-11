/** Copyright (c) 2017 - 2018 Memorial Sloan-Kettering Cancer Center.
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
import org.mskcc.oncotree.utils.VersionUtil;
import org.mskcc.oncotree.topbraid.OncoTreeNode;
import org.mskcc.oncotree.topbraid.OncoTreeRepository;
import org.mskcc.oncotree.topbraid.OncoTreeVersionRepository;
import org.mskcc.oncotree.topbraid.TopBraidSessionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;

/**
 *
 * @author heinsz
 */
@Configuration
public class OncotreeTestConfig {

    @Bean
    public OncoTreeVersionRepository oncoTreeVersionRepository() {
        OncoTreeVersionRepository repository = Mockito.mock(OncoTreeVersionRepository.class);
        Mockito.when(repository.getOncoTreeVersions()).thenReturn(oncoTreeVersionRepositoryMockResponse());
        return repository;
    }

    @Bean
    public List<Version> oncoTreeVersionRepositoryMockResponse() {
        List<Version> oncoTreeVersionRepositoryMockResponse = new ArrayList<Version>();
        Version nextVersion = new Version();
        nextVersion.setVersion("oncotree_latest_stable");
        nextVersion.setGraphURI("urn:x-evn-master:oncotree_2017_06_21");
        nextVersion.setDescription("This is an alias for whatever OncoTree version is the latest stable (timestamped) release.");
        oncoTreeVersionRepositoryMockResponse.add(nextVersion);
        nextVersion = new Version();
        nextVersion.setVersion("oncotree_development");
        nextVersion.setGraphURI("urn:x-evn-master:oncotree_current");
        nextVersion.setDescription("Latest OncoTree under development (subject to change without notice)");
        oncoTreeVersionRepositoryMockResponse.add(nextVersion);
        nextVersion = new Version();
        nextVersion.setVersion("oncotree_2017_06_21");
        nextVersion.setGraphURI("urn:x-evn-master:oncotree_2017_06_21");
        nextVersion.setDescription("Stable OncoTree released on date 2017-06-21");
        oncoTreeVersionRepositoryMockResponse.add(nextVersion);
        nextVersion = new Version();
        nextVersion.setVersion("oncotree_legacy_1.1");
        nextVersion.setGraphURI("urn:x-evn-master:oncotree_legacy_1_1");
        nextVersion.setDescription("This is the closest match in TopBraid for the TumorTypes_txt file associated with release 1.1 of OncoTree (approved by committee)");
        oncoTreeVersionRepositoryMockResponse.add(nextVersion);
        return oncoTreeVersionRepositoryMockResponse;
    }

    @Bean
    public TopBraidSessionConfiguration topBraidSessionConfiguration() {
        return new TopBraidSessionConfiguration();
    }

    @Bean
    public MSKConceptCache mskConceptCache() {
        MSKConceptCache mskConceptCache = Mockito.mock(MSKConceptCache.class);
        MSKConcept mskConcept = new MSKConcept();
        mskConcept.setConceptIds(Arrays.asList("MSK00001", "MSK00002"));
        Mockito.when(mskConceptCache.get(any(String.class))).thenReturn(mskConcept); 
        return mskConceptCache;
    }

    @Bean
    public CrosswalkRepository crosswalkRepository() {
        return Mockito.mock(CrosswalkRepository.class);
    }

    @Bean
    public VersionUtil versionUtil() {
        return new VersionUtil();
    }

    @Bean
    public List<OncoTreeNode> oncoTreeRepositoryMockResponse() throws Exception {
        return setupOncotreeRepositoryMockResponse();
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
    public Version mockVersion() {
        return setupMockVersion();
    }

    @Bean
    public OncoTreeRepository oncoTreeRepository() {
        return Mockito.mock(OncoTreeRepository.class);
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

    private Version setupMockVersion() {
        Version mockVersion = new Version();
        mockVersion = new Version();
        mockVersion.setVersion("mockversion");
        return mockVersion;
    }

    private List<OncoTreeNode> setupOncotreeRepositoryMockResponse() throws Exception {
        String[] rawTestValueSource = getRawTestValueSource();
        final int valuesPerCase = 5;
        if (rawTestValueSource.length % valuesPerCase != 0) {
            throw new Exception("Error : malformed rawTestValueSource");
        }
        final int caseCount = rawTestValueSource.length / valuesPerCase;
        if (caseCount < 1) {
            throw new Exception("Error : no test cases defined in rawTestValueSource");
        }
        List<OncoTreeNode> oncoTreeRepositoryMockResponse = new ArrayList<>();
        for (int pos = 0; pos < rawTestValueSource.length; pos = pos + valuesPerCase) {
            OncoTreeNode nextNode = new OncoTreeNode();
            nextNode.setCode(rawTestValueSource[pos]);
            nextNode.setName(rawTestValueSource[pos + 1]);
            nextNode.setMainType(rawTestValueSource[pos + 2]);
            nextNode.setColor(rawTestValueSource[pos + 3]);
            nextNode.setParentCode(rawTestValueSource[pos + 4]);
            oncoTreeRepositoryMockResponse.add(nextNode);
        }
        return oncoTreeRepositoryMockResponse;
    }

    private Map<String, TumorType> setupExpectedTumorTypeMap() throws Exception {
        String[] rawTestValueSource = getRawTestValueSource();
        final int valuesPerCase = 5;
        if (rawTestValueSource.length % valuesPerCase != 0) {
            throw new Exception("Error : malformed rawTestValueSource");
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
            expectedTumorTypeMap.put(code, nextType);
        }
        return expectedTumorTypeMap;
    }

    private List<String> setupExpectedMainTypeList() throws Exception {
        Set<String> expectedMainTypeSet = new HashSet<>();
        List<String> expectedMainTypeList = new ArrayList<>();
        String[] rawTestValueSource = getRawTestValueSource();
        final int valuesPerCase = 5;
        if (rawTestValueSource.length % valuesPerCase != 0) {
            throw new Exception("Error : malformed rawTestValueSource");
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
                "AA", "Aggressive Angiomyxoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "AASTR", "Anaplastic Astrocytoma", "Glioma", "Gray", "DIFG",
                "ACA", "Adrenocortical Adenoma", "Adrenocortical Carcinoma", "Purple", "ADRENAL_GLAND",
                "ACBC", "Adenoid Cystic Breast Cancer", "Breast Cancer", "HotPink", "BRCA",
                "ACC", "Adrenocortical Carcinoma", "Adrenocortical Carcinoma", "Purple", "ADRENAL_GLAND",
                "ACCC", "Acinic Cell Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA",
                "ACN", "Acinar Cell Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP",
                "ACPG", "Craniopharyngioma, Adamantinomatous Type", "Sellar Tumor", "Gray", "SELT",
                "ACPP", "Atypical Choroid Plexus Papilloma", "Choroid Plexus Tumor", "Gray", "CPT",
                "ACRM", "Acral Melanoma", "Melanoma", "Black", "MEL",
                "ACYC", "Adenoid Cystic Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA",
                "ADNOS", "Adenocarcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP",
                "ADPA", "Aggressive Digital Papillary Adenocarcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "ADRENAL_GLAND", "Adrenal Gland", null, "Purple", "TISSUE",
                "AECA", "Sweat Gland Carcinoma/Apocrine Eccrine Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "AFX", "Atypical Fibroxanthoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "AGA", "Anal Gland Adenocarcinoma", "Anal Cancer", "SaddleBrown", "BOWEL",
                "AGNG", "Anaplastic Ganglioglioma", "Glioma", "Gray", "ENCG",
                "AIS", "Adenocarcinoma In Situ", "Adenocarcinoma In Situ", "Black", "OTHER",
                "AITL", "Angioimmunoblastic T-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "PTCL",
                "ALCL", "Anaplastic Large Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "PTCL",
                "ALL", "Acute Lymphoid Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "ALUCA", "Atypical Lung Carcinoid", "Non-Small Cell Lung Cancer", "Gainsboro", "LNET",
                "AMBL", "Large Cell/Anaplastic Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "AML", "Acute Myeloid Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "AMOL", "Acute Monocytic Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "AMPCA", "Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPULLA_OF_VATER",
                "AMPULLA_OF_VATER", "Ampulla of Vater", "Ampullary Carcinoma", "Purple", "TISSUE",
                "ANGL", "Angiocentric Glioma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "ANGS", "Angiosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "ANM", "Anaplastic Meningioma", "CNS Cancer", "Gray", "MNGT",
                "ANSC", "Anal Squamous Cell Carcinoma", "Anal Cancer", "SaddleBrown", "BOWEL",
                "AOAST", "Anaplastic Oligoastrocytoma", "Glioma", "Gray", "DIFG",
                "AODG", "Anaplastic Oligodendroglioma", "Glioma", "Gray", "DIFG",
                "APAD", "Appendiceal Adenocarcinoma", "Appendiceal Cancer", "SaddleBrown", "BOWEL",
                "APE", "Anaplastic Ependymoma", "CNS Cancer", "Gray", "EPMT",
                "APTAD", "Atypical Pituitary Adenoma", "Sellar Tumor", "Gray", "SELT",
                "APXA", "Anaplastic Pleomorphic Xanthoastrocytoma", "Glioma", "Gray", "ENCG",
                "ARMM", "Anorectal Mucosal Melanoma", "Melanoma", "SaddleBrown", "BOWEL",
                "ARMS", "Alveolar Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS",
                "ASPS", "Alveolar Soft Part Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "ASTB", "Astroblastoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "ASTR", "Astrocytoma", "Glioma", "Gray", "DIFG",
                "ATM", "Atypical Meningioma", "CNS Cancer", "Gray", "MNGT",
                "ATRT", "Atypical Teratoid/Rhabdoid Tumor", "Embryonal Tumor", "Gray", "EMBT",
                "AWDNET", "Well-Differentiated Neuroendocrine Tumor of the Appendix", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET",
                "BA", "Breast Angiosarcoma", "Breast Sarcoma", "HotPink", "PBS",
                "BALL", "B-Cell Acute Lymphoid Leukemia", "Leukemia", "LightSalmon", "ALL",
                "BCC", "Basal Cell Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "BCCA", "Choriocarcinoma", "Germ Cell Tumor", "Gray", "BGCT",
                "BCL", "B-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "NHL",
                "BEC", "Embryonal Carcinoma", "Germ Cell Tumor", "Gray", "BGCT",
                "BFN", "Breast Fibroepithelial Neoplasms", "Breast Sarcoma", "HotPink", "BREAST",
                "BGCT", "Germ Cell Tumor, Brain", "Germ Cell Tumor", "Gray", "BRAIN",
                "BILIARY_TRACT", "Biliary Tract", "Hepatobiliary Cancer", "Green", "TISSUE",
                "BIMT", "Immature Teratoma", "Germ Cell Tumor", "Gray", "BGCT",
                "BL", "Burkitt Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "BLAD", "Bladder Adenocarcinoma", "Bladder Cancer", "Yellow", "BLADDER",
                "BLADDER", "Bladder/Urinary Tract", "Bladder Cancer", "Yellow", "TISSUE",
                "BLCA", "Bladder Urothelial Carcinoma", "Bladder Cancer", "Yellow", "BLADDER",
                "BLCLC", "Basaloid Large Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC",
                "BLOOD", "Blood", null, "LightSalmon", "TISSUE",
                "BLPT", "Borderline Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "PT",
                "BLSC", "Bladder Squamous Cell Carcinoma", "Bladder Cancer", "Yellow", "BLADDER",
                "BMGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "Gray", "BGCT",
                "BMGT", "Malignant Teratoma", "Germ Cell Tumor", "Gray", "BGCT",
                "BMT", "Mature Teratoma", "Germ Cell Tumor", "Gray", "BGCT",
                "BNNOS", "Breast Neoplasm, NOS", "Breast Cancer", "HotPink", "BREAST",
                "BONE", "Bone", "Bone Cancer", "White", "TISSUE",
                "BOWEL", "Bowel", null, "SaddleBrown", "TISSUE",
                "BPDCN", "Blastic Plasmacytoid Dendritic Cell Neoplasm", "Blastic Plasmacytoid Dendritic Cell Neoplasm", "LightSalmon", "BLOOD",
                "BPSCC", "Basaloid Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PSCC",
                "BPT", "Benign Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "PT",
                "BRAIN", "CNS/Brain", null, "Gray", "TISSUE",
                "BRAME", "Adenomyoepithelioma of the Breast", "Breast Cancer", "HotPink", "BREAST",
                "BRCA", "Invasive Breast Carcinoma", "Breast Cancer", "HotPink", "BREAST",
                "BRCANOS", "Breast Invasive Cancer, NOS", "Breast Cancer", "HotPink", "BRCA",
                "BRCNOS", "Breast Invasive Carcinoma, NOS", "Breast Cancer", "HotPink", "BRCA",
                "BREAST", "Breast", null, "HotPink", "TISSUE",
                "BRSRCC", "Breast Carcinoma with Signet Ring", "Breast Cancer", "HotPink", "BRCA",
                "BTBEOV", "Brenner Tumor, Benign", "Ovarian Cancer", "LightBlue", "BTOV",
                "BTBOV", "Brenner Tumor, Borderline", "Ovarian Cancer", "LightBlue", "BTOV",
                "BTMOV", "Brenner Tumor, Malignant", "Ovarian Cancer", "LightBlue", "BTOV",
                "BTOV", "Brenner Tumor", "Ovarian Cancer", "LightBlue", "OVT",
                "BYST", "Yolk Sac Tumor", "Germ Cell Tumor", "Gray", "BGCT",
                "CABC", "Cervical Adenoid Basal Carcinoma", "Cervical Cancer", "Teal", "CERVIX",
                "CACC", "Cervical Adenoid Cystic Carcinoma", "Cervical Cancer", "Teal", "CERVIX",
                "CAIS", "Colon Adenocarcinoma In Situ", "Colorectal Cancer", "SaddleBrown", "COADREAD",
                "CCBOV", "Clear Cell Borderline Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT",
                "CCE", "Clear Cell Ependymoma", "CNS Cancer", "Gray", "EPMT",
                "CCHDM", "Conventional Type Chordoma", "Bone Cancer", "White", "CHDM",
                "CCHM", "Carcinoma with Chondroid Metaplasia", "Breast Cancer", "HotPink", "MMBC",
                "CCLC", "Clear Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC",
                "CCM", "Clear cell Meningioma", "CNS Cancer", "Gray", "MNGT",
                "CCOC", "Clear Cell Odontogenic Carcinoma", "Head and Neck Cancer", "DarkRed", "ODGC",
                "CCOV", "Clear Cell Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT",
                "CCPRC", "Clear Cell Papillary Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "CCRCC", "Renal Clear Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "RCC",
                "CCS", "Clear Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "CDRCC", "Collecting Duct Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "CEAD", "Cervical Adenocarcinoma", "Cervical Cancer", "Teal", "CERVIX",
                "CEAIS", "Cervical Adenocarcinoma In Situ", "Cervical Cancer", "Teal", "CERVIX",
                "CEAS", "Cervical Adenosquamous Carcinoma", "Cervical Cancer", "Teal", "CERVIX",
                "CECC", "Cervical Clear Cell Carcinoma", "Cervical Cancer", "Teal", "CEAD",
                "CEEN", "Cervical Endometrioid Carcinoma", "Cervical Cancer", "Teal", "CEAD",
                "CEGCC", "Glassy Cell Carcinoma of the Cervix", "Cervical Cancer", "Teal", "CERVIX",
                "CELI", "Cervical Leiomyosarcoma", "Cervical Cancer", "Teal", "CERVIX",
                "CEMN", "Mesonephric Carcinoma", "Cervical Cancer", "Teal", "CEAD",
                "CEMU", "Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEAD",
                "CENE", "Cervical Neuroendocrine Tumor", "Cervical Cancer", "Teal", "CERVIX",
                "CERMS", "Cervical Rhabdomyosarcoma", "Cervical Cancer", "Teal", "CERVIX",
                "CERVIX", "Cervix", "Cervical Cancer", "Teal", "TISSUE",
                "CESC", "Cervical Squamous Cell Carcinoma", "Cervical Cancer", "Teal", "CERVIX",
                "CESE", "Cervical Serous Carcinoma", "Cervical Cancer", "Teal", "CEAD",
                "CEVG", "Villoglandular Carcinoma", "Cervical Cancer", "Teal", "CEAD",
                "CHBL", "Chondroblastoma", "Bone Cancer", "White", "BONE",
                "CHDM", "Chordoma", "Bone Cancer", "White", "BONE",
                "CHGL", "Chordoid Glioma of the Third Ventricle", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "CHL", "Classical Hodgkin Lymphoma", "Hodgkin Lymphoma", "LimeGreen", "HL",
                "CHM", "Complete Hydatidiform Mole", "Gestational Trophoblastic Disease", "PeachPuff", "MP",
                "CHOL", "Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "BILIARY_TRACT",
                "CHOM", "Chordoid Meningioma", "CNS Cancer", "Gray", "MNGT",
                "CHOS", "Chondroblastic Osteosarcoma", "Bone Cancer", "White", "OS",
                "CHRCC", "Chromophobe Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "CHS", "Chondrosarcoma", "Bone Cancer", "White", "BONE",
                "CLL", "Chronic Lymphocytic Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "CLNC", "Cerebellar Liponeurocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "CM", "Conjunctival Melanoma", "Melanoma", "Green", "OM",
                "CMC", "Medullary Carcinoma of the Colon", "Colorectal Cancer", "SaddleBrown", "BOWEL",
                "CML", "Chronic Myelogenous Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "CMML", "Chronic Myelomonocytic Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "CMPT", "Ciliated Muconodular Papillary Tumor of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "CNC", "Central Neurocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "COAD", "Colon Adenocarcinoma", "Colorectal Cancer", "SaddleBrown", "COADREAD",
                "COADREAD", "Colorectal Adenocarcinoma", "Colorectal Cancer", "SaddleBrown", "BOWEL",
                "COM", "Carcinoma with Osseous Metaplasia", "Breast Cancer", "HotPink", "MMBC",
                "CPC", "Choroid Plexus Carcinoma", "Choroid Plexus Tumor", "Gray", "CPT",
                "CPP", "Choroid Plexus Papilloma", "Choroid Plexus Tumor", "Gray", "CPT",
                "CPT", "Choroid Plexus Tumor", "Choroid Plexus Tumor", "Gray", "BRAIN",
                "CSCC", "Cutaneous Squamous Cell Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "CSCHW", "Cellular Schwannoma", "Nerve Sheath Tumor", "Gray", "SCHW",
                "CSCLC", "Combined Small Cell Lung Carcinoma", "Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "CSNOS", "Breast Invasive Carcinosarcoma, NOS", "Breast Cancer", "HotPink", "BRCA",
                "CTAAP", "Colonic Type Adenocarcinoma of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD",
                "CTCL", "Cutaneous T-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "TNKL",
                "CUP", "Cancer of Unknown Primary", "Cancer of Unknown Primary", "Black", "OTHER",
                "CUPNOS", "Cancer of Unknown Primary, NOS", "Cancer of Unknown Primary", "Black", "CUP",
                "DA", "Duodenal Adenocarcinoma", "Small Bowel Cancer", "SaddleBrown", "SBC",
                "DCIS", "Breast Ductal Carcinoma In Situ", "Breast Cancer", "HotPink", "BREAST",
                "DCS", "Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "DDCHDM", "Dedifferentiated Chordoma", "Bone Cancer", "White", "CHDM",
                "DDCHS", "Dedifferentiated Chondrosarcoma", "Bone Cancer", "White", "CHS",
                "DDLS", "Dedifferentiated Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO",
                "DES", "Desmoid/Aggressive Fibromatosis", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "DESM", "Desmoplastic Melanoma", "Melanoma", "Black", "MEL",
                "DF", "Dermatofibroma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "DFSP", "Dermatofibrosarcoma Protuberans", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "DIA", "Desmoplastic Infantile Astrocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "DIFG", "Diffuse Glioma", "Glioma", "Gray", "BRAIN",
                "DIG", "Desmoplastic Infantile Ganglioglioma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "DIPG", "Diffuse Intrinsic Pontine Glioma", "Glioma", "Gray", "DIFG",
                "DLBCL", "Diffuse Large B-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "DMBL", "Desmoplastic/Nodular Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "DNT", "Dysembryoplastic Neuroepithelial Tumor", "Glioma", "Gray", "ENCG",
                "DSRCT", "Desmoplastic Small-Round-Cell Tumor", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "DSTAD", "Diffuse Type Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD",
                "DTE", "Desmoplastic Trichoepithelioma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "EBOV", "Endometrioid Borderlin Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT",
                "ECAD", "Endocervical Adenocarcinoma", "Cervical Cancer", "Teal", "CEAD",
                "ECD", "Non-Langerhans Cell Histiocytosis/Erdheim-Chester Disease", "Histiocytosis", "LightSalmon", "HIST",
                "EGC", "Esophagogastric Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH",
                "EHAE", "Epithelioid Hemangioendothelioma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "EHCH", "Extrahepatic Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "CHOL",
                "EMBC", "Epithelial Type Metaplastic Breast Cancer", "Breast Cancer", "HotPink", "MBC",
                "EMBCA", "Embryonal Carcinoma", "Germ Cell Tumor", "Red", "NSGCT",
                "EMBT", "Embryonal Tumor", "Embryonal Tumor", "Gray", "BRAIN",
                "EMCHS", "Extraskeletal Myxoid Chondrosarcoma", "Bone Cancer", "White", "CHS",
                "EMPD", "Extramammary Paget Disease", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "EMPSGC", "Endocrine Mucin Producing Sweat Gland Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "EMYOCA", "Epithelial-Myoepithelial Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA",
                "ENCG", "Encapsulated Glioma", "Glioma", "Gray", "BRAIN",
                "EOV", "Endometrioid Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT",
                "EPDCA", "Esophageal Poorly Differentiated Carcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH",
                "EPIS", "Epithelioid Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "EPM", "Ependymoma", "CNS Cancer", "Gray", "EPMT",
                "EPMT", "Ependymomal Tumor", "CNS Cancer", "Gray", "BRAIN",
                "ERMS", "Embryonal Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS",
                "ES", "Ewing Sarcoma", "Bone Cancer", "White", "BONE",
                "ESCA", "Esophageal Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC",
                "ESCC", "Esophageal Squamous Cell Carcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH",
                "ESMM", "Mucosal Melanoma of the Esophagus", "Melanoma", "LightSkyBlue", "STOMACH",
                "ESS", "Endometrial Stromal Sarcoma", "Uterine Sarcoma", "PeachPuff", "USARC",
                "ETANTR", "Embryonal Tumor with Abundant Neuropil and True Rosettes", "Embryonal Tumor", "Gray", "EMBT",
                "ETC", "Essential Thrombocythaemia", "Myeloproliferative Neoplasm", "LightSalmon", "MPN",
                "ETT", "Epithelioid Trophoblastic Tumor", "Gestational Trophoblastic Disease", "PeachPuff", "GTD",
                "EVN", "Extraventricular Neurocytoma", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "EYE", "Eye", null, "Green", "TISSUE",
                "FA", "Fibroadenoma", "Breast Sarcoma", "HotPink", "BFN",
                "FDCS", "Follicular Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "DCS",
                "FIBS", "Fibrosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "FIOS", "Fibroblastic Osteosarcoma", "Bone Cancer", "White", "OS",
                "FL", "Follicular Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "FT", "Fibrothecoma", "Sex Cord Stromal Tumor", "LightBlue", "SCST",
                "GB", "Glioblastoma", "Glioma", "Gray", "DIFG",
                "GBC", "Gallbladder Cancer", "Hepatobiliary Cancer", "Green", "BILIARY_TRACT",
                "GBM", "Glioblastoma Multiforme", "Glioma", "Gray", "GB",
                "GCCAP", "Goblet Cell Carcinoid of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD",
                "GCEMU", "Gastric Type Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEMU",
                "GCLC", "Giant Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC",
                "GCT", "Granular Cell Tumor", "Sellar Tumor", "Gray", "SELT",
                "GCTB", "Giant Cell Tumor of Bone", "Bone Cancer", "White", "BONE",
                "GEJ", "Adenocarcinoma of the Gastroesophageal Junction", "Esophagogastric Cancer", "LightSkyBlue", "EGC",
                "GINET", "Gastrointestinal Neuroendocrine Tumors", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "BOWEL",
                "GIST", "Gastrointestinal Stromal Tumor", "Gastrointestinal Stromal Tumor", "LightYellow", "SOFT_TISSUE",
                "GMN", "Germinoma", "Germ Cell Tumor", "Gray", "BGCT",
                "GMUCM", "Genitourinary Mucosal Melanoma", "Melanoma", "Black", "MEL",
                "GNBL", "Ganglioneuroblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "GNC", "Gangliocytoma", "Glioma", "Gray", "ENCG",
                "GNG", "Ganglioglioma", "Glioma", "Gray", "ENCG",
                "GRC", "Gastric Remnant Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC",
                "GRCT", "Granulosa Cell Tumor", "Sex Cord Stromal Tumor", "LightBlue", "SCST",
                "GS", "Glomangiosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "GSARC", "Gliosarcoma", "Glioma", "Gray", "GB",
                "GTD", "Gestational Trophoblastic Disease", "Gestational Trophoblastic Disease", "PeachPuff", "UTERUS",
                "HCC", "Hepatocellular Carcinoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER",
                "HCCIHCH", "Hepatocellular Carcinoma plus Intrahepatic Cholangiocarcinoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER",
                "HCL", "Hairy Cell Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "HDCS", "Histiocytic Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "DCS",
                "HEAD_NECK", "Head and Neck", "Head and Neck Cancer", "DarkRed", "TISSUE",
                "HEMA", "Hemangioma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "HGESS", "High-Grade Endometrial Stromal Sarcoma", "Uterine Sarcoma", "PeachPuff", "ESS",
                "HGGNOS", "High-Grade Glioma, NOS", "Glioma", "Gray", "DIFG",
                "HGNEC", "High-Grade Neuroendocrine Carcinoma of the Colon and Rectum", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET",
                "HGNET", "High-Grade Neuroepithelial Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "HGONEC", "High-Grade Neuroendocrine Carcinoma of the Ovary", "Ovarian Cancer", "LightBlue", "OOVC",
                "HGSOC", "High-Grade Serous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "SOC",
                "HGSOS", "High-Grade Surface Osteosarcoma", "Bone Cancer", "White", "OS",
                "HIST", "Histiocytosis", "Histiocytosis", "LightSalmon", "BLOOD",
                "HL", "Hodgkin Lymphoma", "Hodgkin Lymphoma", "LimeGreen", "LYMPH",
                "HMBL", "Hemangioblastoma", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "HNMASC", "Mammary Analogue Secretory Carcinoma of Salivary Gland Origin", "Salivary Gland Cancer", "DarkRed", "SACA",
                "HNMUCM", "Head and Neck Mucosal Melanoma", "Melanoma", "DarkRed", "HEAD_NECK",
                "HNNE", "Head and Neck Neuroendocrine Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA",
                "HNSC", "Head and Neck Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HEAD_NECK",
                "HNSCUP", "Head and Neck Squamous Cell Carcinoma of Unknown Primary", "Head and Neck Cancer", "DarkRed", "HNSC",
                "HPCCNS", "Hemangiopericytoma of the Central Nervous System", "CNS Cancer", "Gray", "MNGT",
                "HPHSC", "Hypopharynx Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC",
                "HTAT", "Hyalinizing Trabecular Adenoma of the Thyroid", "Thyroid Cancer", "Teal", "THYROID",
                "IAMPCA", "Intestinal Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPCA",
                "IBC", "Inflammatory Breast Cancer", "Breast Cancer", "HotPink", "BREAST",
                "ICEMU", "Intestinal Type Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEMU",
                "IDC", "Breast Invasive Ductal Carcinoma", "Breast Cancer", "HotPink", "BRCA",
                "IDCS", "Interdigitating Dendritic Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "DCS",
                "IHCH", "Intrahepatic Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "CHOL",
                "IHM", "Invasive Hydatidiform Mole", "Gestational Trophoblastic Disease", "PeachPuff", "MP",
                "ILC", "Breast Invasive Lobular Carcinoma", "Breast Cancer", "HotPink", "BRCA",
                "IMMC", "Breast Invasive Mixed Mucinous Carcinoma", "Breast Cancer", "HotPink", "BRCA",
                "IMT", "Inflammatory Myofibroblastic Tumor", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "IMTB", "Inflammatory Myofibroblastic Bladder Tumor", "Bladder Cancer", "Yellow", "BLADDER",
                "IMTL", "Inflammatory Myofibroblastic Lung Tumor", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "INTS", "Intimal Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "IPMN", "Intraductal Papillary Mucinous Neoplasm", "Pancreatic Cancer", "Purple", "PACT",
                "ISTAD", "Intestinal Type Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD",
                "KIDNEY", "Kidney", null, "Orange", "TISSUE",
                "LAIS", "Lung Adenocarcinoma In Situ", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "LAM", "Pulmonary Lymphangiomyomatosis", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "LCH", "Langerhans Cell Histiocytosis", "Histiocytosis", "LightSalmon", "HIST",
                "LCIS", "Breast Lobular Carcinoma In Situ", "Breast Cancer", "HotPink", "BREAST",
                "LCLC", "Large Cell Lung Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "LDD", "Dysplastic Gangliocytoma of the Cerebellum/Lhermitte-Duclos Disease", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "LECLC", "Lymphoepithelioma-like Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC",
                "LEUK", "Leukemia", "Leukemia", "LightSalmon", "BLOOD",
                "LGCOS", "Low-Grade Central Osteosarcoma", "Bone Cancer", "White", "OS",
                "LGESS", "Low-Grade Endometrial Stromal Sarcoma", "Uterine Sarcoma", "PeachPuff", "ESS",
                "LGFMS", "Low-Grade Fibromyxoid Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "LGGNOS", "Low-Grade Glioma, NOS", "Glioma", "Gray", "ENCG",
                "LGLL", "Large Granular Lymphocytic Leukemia", "Leukemia", "LightSalmon", "LEUK",
                "LGNET", "Low-Grade Neuroepithelial Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "LGSOC", "Low-Grade Serous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "SOC",
                "LIAD", "Hepatocellular Adenoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER",
                "LIAS", "Liver Angiosarcoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER",
                "LIHB", "Hepatoblastoma", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER",
                "LIMNET", "Malignant Nonepithelial Tumor of the Liver", "Hepatobiliary Cancer", "MediumSeaGreen", "LIVER",
                "LIPO", "Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "LIVER", "Liver", "Hepatobiliary Cancer", "MediumSeaGreen", "TISSUE",
                "LMS", "Leiomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "LNET", "Lung Neuroendocrine Tumor", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "LUACC", "Adenoid Cystic Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "SGTTL",
                "LUAD", "Lung Adenocarcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "LUAS", "Lung Adenosquamous Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "LUCA", "Lung Carcinoid", "Non-Small Cell Lung Cancer", "Gainsboro", "LNET",
                "LUMEC", "Mucoepidermoid Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "SGTTL",
                "LUNE", "Large Cell Neuroendocrine Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "LNET",
                "LUNG", "Lung", null, "Gainsboro", "TISSUE",
                "LUPC", "Pleomorphic Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "LUSC", "Lung Squamous Cell Carcinoma", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "LXSC", "Larynx Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC",
                "LYMPH", "Lymph", null, "LimeGreen", "TISSUE",
                "MAAP", "Mucinous Adenocarcinoma of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD",
                "MAC", "Microcystic Adnexal Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "MACR", "Mucinous Adenocarcinoma of the Colon and Rectum", "Colorectal Cancer", "SaddleBrown", "COADREAD",
                "MALTL", "Extranodal Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "MAMPCA", "Mixed Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPCA",
                "MASC", "Metaplastic Adenosquamous Carcinoma", "Breast Cancer", "HotPink", "EMBC",
                "MASCC", "Metaplastic Adenocarcinoma with Spindle Cell Differentiation", "Breast Cancer", "HotPink", "EMBC",
                "MBC", "Metaplastic Breast Cancer", "Breast Cancer", "HotPink", "BREAST",
                "MBCL", "Mediastinal Large B-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "MBEN", "Medulloblastoma with Extensive Nodularity", "Embryonal Tumor", "Gray", "EMBT",
                "MBL", "Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "MBOV", "Mucinous Borderline Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT",
                "MBT", "Miscellaneous Brain Tumor", "Miscellaneous Brain Tumor", "Gray", "BRAIN",
                "MCC", "Merkel Cell Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "MCCE", "Mixed Cervical Carcinoma", "Cervical Cancer", "Teal", "CERVIX",
                "MCHS", "Mesenchymal Chondrosarcoma", "Bone Cancer", "White", "CHS",
                "MCHSCNS", "Mesenchymal Chondrosarcoma of the CNS", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "MCL", "Mantle Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "MCN", "Mucinous Cystic Neoplasm", "Pancreatic Cancer", "Purple", "PACT",
                "MCS", "Metaplastic Carcinosarcoma", "Breast Cancer", "HotPink", "MMBC",
                "MDEP", "Medulloepithelioma", "Embryonal Tumor", "Gray", "EMBT",
                "MDLC", "Breast Mixed Ductal and Lobular Carcinoma", "Breast Cancer", "HotPink", "BRCA",
                "MDS", "Myelodysplasia", "Myelodysplasia", "LightSalmon", "BLOOD",
                "MEL", "Melanoma", "Melanoma", "Black", "SKIN",
                "MF", "Myofibroma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "MFH", "Undifferentiated Pleomorphic Sarcoma/Malignant Fibrous Histiocytoma/High-Grade Spindle Cell Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "MFS", "Myxofibrosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "MGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "Red", "NSGCT",
                "MIXED", "Mixed Cancer Types", "Cancer of Unknown Primary", "Black", "CUP",
                "MLYM", "Malignant Lymphoma", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "MM", "Multiple Myeloma", "Multiple Myeloma", "LightSalmon", "BLOOD",
                "MMB", "Medullomyoblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "MMBC", "Mixed Type Metaplastic Breast Cancer", "Breast Cancer", "HotPink", "MBC",
                "MMBL", "Melanotic Medulloblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "MNET", "Miscellaneous Neuroepithelial Tumor", "Miscellaneous Neuroepithelial Tumor", "Gray", "BRAIN",
                "MNG", "Meningioma", "CNS Cancer", "Gray", "MNGT",
                "MNGT", "Meningothelial Tumor", "CNS Cancer", "Gray", "BRAIN",
                "MOV", "Mucinous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT",
                "MP", "Molar Pregnancy", "Gestational Trophoblastic Disease", "PeachPuff", "GTD",
                "MPC", "Myopericytoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "MPE", "Myxopapillary Ependymoma", "CNS Cancer", "Gray", "EPMT",
                "MPN", "Myeloproliferative Neoplasm", "Myeloproliferative Neoplasm", "LightSalmon", "BLOOD",
                "MPNST", "Malignant Peripheral Nerve Sheath Tumor", "Nerve Sheath Tumor", "Gray", "NST",
                "MPT", "Malignant Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "PT",
                "MRC", "Renal Medullary Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "MRLS", "Myxoid/Round-Cell Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO",
                "MRT", "Rhabdoid Cancer", "Wilms Tumor", "Orange", "WT",
                "MSCC", "Metaplastic Squamous Cell Carcinoma", "Breast Cancer", "HotPink", "EMBC",
                "MSCHW", "Melanotic Schwannoma", "Nerve Sheath Tumor", "Gray", "SCHW",
                "MSTAD", "Mucinous Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD",
                "MT", "Malignant Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "MTSCC", "Renal Mucinous Tubular Spindle Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "MUCC", "Mucoepidermoid Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA",
                "MUP", "Melanoma of Unknown Primary", "Melanoma", "Black", "MEL",
                "MXOV", "Mixed Ovarian Carcinoma", "Ovarian Cancer", "LightBlue", "OVT",
                "MYCF", "Mycosis Fungoides", "Non-Hodgkin Lymphoma", "LimeGreen", "CTCL",
                "MYCHS", "Myxoid Chondrosarcoma", "Bone Cancer", "White", "CHS",
                "MYEC", "Myoepithelial Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA",
                "MYF", "Myelofibrosis/Osteomyelofibrosis", "Myeloproliferative Neoplasm", "LightSalmon", "MPN",
                "MYXO", "Myxoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "MZL", "Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "NBL", "Neuroblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "NCCRCC", "Renal Non-Clear Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "RCC",
                "NECNOS", "Neuroendocrine Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP",
                "NETNOS", "Neuroendocrine Tumor, NOS", "Cancer of Unknown Primary", "Black", "CUP",
                "NFIB", "Neurofibroma", "Nerve Sheath Tumor", "Gray", "NST",
                "NHL", "Non-Hodgkin Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "LYMPH",
                "NLPHL", "Nodular Lymphocyte-Predominant Hodgkin Lymphoma", "Hodgkin Lymphoma", "LimeGreen", "HL",
                "NMCHN", "NUT Midline Carcinoma of the Head and Neck", "Head and Neck Cancer", "DarkRed", "OHNCA",
                "NMZL", "Nodal Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "NPC", "Nasopharyngeal Carcinoma", "Head and Neck Cancer", "DarkRed", "HEAD_NECK",
                "NSCLC", "Non-Small Cell Lung Cancer", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "NSCLCPD", "Poorly Differentiated Non-Small Cell Lung Cancer", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "NSGCT", "Non-Seminomatous Germ Cell Tumor", "Germ Cell Tumor", "Red", "TESTIS",
                "NST", "Nerve Sheath Tumor", "Nerve Sheath Tumor", "Gray", "PNS",
                "OAST", "Oligoastrocytoma", "Glioma", "Gray", "DIFG",
                "OCS", "Ovarian Carcinosarcoma/Malignant Mixed Mesodermal Tumor", "Ovarian Cancer", "LightBlue", "OVT",
                "OCSC", "Oral Cavity Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC",
                "ODG", "Oligodendroglioma", "Glioma", "Gray", "DIFG",
                "ODGC", "Odontogenic Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA",
                "ODYS", "Dysgerminoma", "Germ Cell Tumor", "LightBlue", "OGCT",
                "OEC", "Embryonal Carcinoma", "Germ Cell Tumor", "LightBlue", "OGCT",
                "OFMT", "Ossifying Fibromyxoid Tumor", "Soft Tissue Sarcoma", "LightYellow", "MYXO",
                "OGBL", "Gonadoblastoma", "Sex Cord Stromal Tumor", "LightBlue", "SCST",
                "OGCT", "Ovarian Germ Cell Tumor", "Germ Cell Tumor", "LightBlue", "OVARY",
                "OHNCA", "Head and Neck Carcinoma, Other", "Head and Neck Cancer", "DarkRed", "HEAD_NECK",
                "OIMT", "Immature Teratoma", "Germ Cell Tumor", "LightBlue", "OGCT",
                "OM", "Ocular Melanoma", "Melanoma", "Green", "EYE",
                "OMGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "LightBlue", "OGCT",
                "OMT", "Mature Teratoma", "Germ Cell Tumor", "LightBlue", "OGCT",
                "ONBL", "Olfactory Neuroblastoma", "Embryonal Tumor", "Gray", "EMBT",
                "OOVC", "Ovarian Cancer, Other", "Ovarian Cancer", "LightBlue", "OVARY",
                "OPE", "Polyembryoma", "Germ Cell Tumor", "LightBlue", "OGCT",
                "OPHSC", "Oropharynx Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC",
                "OS", "Osteosarcoma", "Bone Cancer", "White", "BONE",
                "OSACA", "Salivary Carcinoma, Other", "Salivary Gland Cancer", "DarkRed", "SACA",
                "OSMAD", "Ovarian Seromucinous Adenoma", "Ovarian Cancer", "LightBlue", "OVT",
                "OSMBT", "Ovarian Seromucinous Borderline Tumor", "Ovarian Cancer", "LightBlue", "OVT",
                "OSMCA", "Ovarian Seromucinous Carcinoma", "Ovarian Cancer", "LightBlue", "OVT",
                "OSOS", "Osteoblastic Osteosarcoma", "Bone Cancer", "White", "OS",
                "OTHER", "Other", null, "Black", "TISSUE",
                "OUSARC", "Uterine Sarcoma, Other", "Uterine Sarcoma", "PeachPuff", "USARC",
                "OUTT", "Other Uterine Tumor", "Endometrial Cancer", "PeachPuff", "UTERUS",
                "OVARY", "Ovary/Fallopian Tube", "Ovarian Cancer", "LightBlue", "TISSUE",
                "OVT", "Ovarian Epithelial Tumor", "Ovarian Cancer", "LightBlue", "OVARY",
                "OYST", "Yolk Sac Tumor", "Germ Cell Tumor", "LightBlue", "OGCT",
                "PAAC", "Acinar Cell Carcinoma of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS",
                "PAAD", "Pancreatic Adenocarcinoma", "Pancreatic Cancer", "Purple", "PANCREAS",
                "PAASC", "Adenosquamous Carcinoma of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS",
                "PACT", "Cystic Tumor of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS",
                "PAMPCA", "Pancreatobiliary Ampullary Carcinoma", "Ampullary Carcinoma", "Purple", "AMPCA",
                "PANCREAS", "Pancreas", "Pancreatic Cancer", "Purple", "TISSUE",
                "PANET", "Pancreatic Neuroendocrine Tumor", "Pancreatic Cancer", "Purple", "PANCREAS",
                "PAOS", "Parosteal Osteosarcoma", "Bone Cancer", "White", "OS",
                "PAST", "Pilocytic Astrocytoma", "Glioma", "Gray", "ENCG",
                "PB", "Pancreatoblastoma", "Pancreatic Cancer", "Purple", "PANCREAS",
                "PBL", "Pineoblastoma", "Pineal Tumor", "Gray", "PINT",
                "PBS", "Breast Sarcoma", "Breast Sarcoma", "HotPink", "BREAST",
                "PBT", "Primary Brain Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "PCGP", "Craniopharyngioma, Papillary Type", "Sellar Tumor", "Gray", "SELT",
                "PCNSL", "Primary CNS Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "PCNSM", "Primary CNS Melanoma", "Melanoma", "LightSkyBlue", "BRAIN",
                "PCT", "Porphyria Cutania Tarda", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "PCV", "Polycythemia Vera", "Myeloproliferative Neoplasm", "LightSalmon", "MPN",
                "PD", "Paget Disease of the Nipple", "Breast Cancer", "HotPink", "DCIS",
                "PDC", "Poorly Differentiated Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP",
                "PECOMA", "Perivascular Epithelioid Cell Tumor", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "PEL", "Primary Effusion Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "PEMESO", "Peritoneal Mesothelioma", "Mesothelioma", "Green", "PERITONEUM",
                "PENIS", "Penis", "Penile Cancer", "Blue", "TISSUE",
                "PEOS", "Periosteal Osteosarcoma", "Bone Cancer", "White", "OS",
                "PERITONEUM", "Peritoneum", "Mesothelioma", "Green", "TISSUE",
                "PGNG", "Paraganglioma", "Miscellaneous Neuroepithelial Tumor", "Gray", "SOFT_TISSUE",
                "PGNT", "Papillary Glioneuronal Tumor", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "PHC", "Pheochromocytoma", "Pheochromocytoma", "Purple", "ADRENAL_GLAND",
                "PHCH", "Perihilar Cholangiocarcinoma", "Hepatobiliary Cancer", "Green", "CHOL",
                "PHM", "Partial Hydatidiform Mole", "Gestational Trophoblastic Disease", "PeachPuff", "MP",
                "PINC", "Pineocytoma", "Pineal Tumor", "Gray", "PINT",
                "PINT", "Pineal Tumor", "Pineal Tumor", "Gray", "BRAIN",
                "PLBMESO", "Pleural Mesothelioma, Biphasic Type", "Mesothelioma", "Blue", "PLMESO",
                "PLEMESO", "Pleural Mesothelioma, Epithelioid Type", "Mesothelioma", "Blue", "PLMESO",
                "PLEURA", "Pleura", "Mesothelioma", "Blue", "TISSUE",
                "PLLS", "Pleomorphic Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO",
                "PLMESO", "Pleural Mesothelioma", "Mesothelioma", "Blue", "PLEURA",
                "PLRMS", "Pleomorphic Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS",
                "PLSMESO", "Pleural Mesothelioma, Sarcomatoid Type", "Mesothelioma", "Blue", "PLMESO",
                "PMA", "Pilomyxoid Astrocytoma", "Glioma", "Gray", "ENCG",
                "PMHE", "Pseudomyogenic Hemangioendothelioma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "PNET", "Primitive Neuroectodermal Tumor", "Embryonal Tumor", "Gray", "EMBT",
                "PNS", "Peripheral Nervous System", "Nerve Sheath Tumor", "Gray", "TISSUE",
                "POCA", "Porocarcinoma/Spiroadenocarcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "PORO", "Poroma/Acrospiroma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "PPB", "Pleuropulmonary Blastoma", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "PPCT", "Proliferating Pilar Cystic Tumor", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "PPM", "Papillary Meningioma", "CNS Cancer", "Gray", "MNGT",
                "PPTID", "Pineal Parenchymal Tumor of Intermediate Differentiation", "Pineal Tumor", "Gray", "PINT",
                "PRAD", "Prostate Adenocarcinoma", "Prostate Cancer", "Cyan", "PROSTATE",
                "PRCC", "Papillary Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "PRNE", "Prostate Neuroendocrine Carcinoma", "Prostate Cancer", "Cyan", "PROSTATE",
                "PRNET", "Primary Neuroepithelial Tumor", "Miscellaneous Brain Tumor", "Gray", "MBT",
                "PROSTATE", "Prostate", "Prostate Cancer", "Cyan", "TISSUE",
                "PRSC", "Prostate Sqamous Cell Carcinoma", "Prostate Cancer", "Cyan", "PROSTATE",
                "PRSCC", "Prostate Small Cell Carcinoma", "Prostate Cancer", "Cyan", "PROSTATE",
                "PSC", "Serous Cystadenoma of the Pancreas", "Pancreatic Cancer", "Purple", "PACT",
                "PSCC", "Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PENIS",
                "PSTAD", "Papillary Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD",
                "PSTT", "Placental Site Trophoblastic Tumor", "Gestational Trophoblastic Disease", "PeachPuff", "GTD",
                "PT", "Phyllodes Tumor of the Breast", "Breast Sarcoma", "HotPink", "BFN",
                "PTAD", "Pituitary Adenoma", "Sellar Tumor", "Gray", "SELT",
                "PTCA", "Pituitary Carcinoma", "Sellar Tumor", "Gray", "SELT",
                "PTCL", "Peripheral T-Cell Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "TNKL",
                "PTCLNOS", "Peripheral T-Cell Lymphoma, NOS", "Non-Hodgkin Lymphoma", "LimeGreen", "PTCL",
                "PTCY", "Pituicytoma", "Sellar Tumor", "Gray", "SELT",
                "PTES", "Proximal-Type Epithelioid Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "EPIS",
                "PTPR", "Papillary Tumor of the Pineal Region", "Pineal Tumor", "Gray", "PINT",
                "PXA", "Pleomorphic Xanthoastrocytoma", "Glioma", "Gray", "ENCG",
                "RAML", "Renal Angiomyolipoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "RAS", "Radiation-Associated Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "RBL", "Retinoblastoma", "Retinoblastoma", "Green", "EYE",
                "RCC", "Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "KIDNEY",
                "RCSNOS", "Round Cell Sarcoma, NOS", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "RD", "Rosai-Dorfman Disease", "Histiocytic Disorder", "LimeGreen", "LYMPH",
                "READ", "Rectal Adenocarcinoma", "Colorectal Cancer", "SaddleBrown", "COADREAD",
                "RGNT", "Rosette-forming Glioneuronal Tumor of the Fourth Ventricle", "Miscellaneous Neuroepithelial Tumor", "Gray", "MNET",
                "RHM", "Rhabdoid Meningioma", "CNS Cancer", "Gray", "MNGT",
                "RLCLC", "Large Cell Lung Carcinoma With Rhabdoid Phenotype", "Non-Small Cell Lung Cancer", "Gainsboro", "LCLC",
                "RMS", "Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "ROCY", "Renal Oncocytoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "RSCC", "Renal Small Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "RWDNET", "Well-Differentiated Neuroendocrine Tumor of the Rectum", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET",
                "SAAD", "Salivary Adenocarcinoma", "Salivary Gland Cancer", "DarkRed", "SACA",
                "SACA", "Salivary Carcinoma", "Salivary Gland Cancer", "DarkRed", "HEAD_NECK",
                "SARCL", "Sarcomatoid Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "LUNG",
                "SARCNOS", "Sarcoma, NOS", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "SBC", "Small Bowel Cancer", "Small Bowel Cancer", "SaddleBrown", "BOWEL",
                "SBMOV", "Serous Borderline Ovarian Tumor, Micropapillary", "Ovarian Cancer", "LightBlue", "OVT",
                "SBOV", "Serous Borderline Ovarian Tumor", "Ovarian Cancer", "LightBlue", "OVT",
                "SBWDNET", "Small Bowel Well-Differentiated Neuroendocrine Tumor", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET",
                "SCB", "Sarcomatoid Carcinoma of the Urinary Bladder", "Bladder Cancer", "Yellow", "BLADDER",
                "SCBC", "Small Cell Bladder Cancer", "Bladder Cancer", "Yellow", "BLADDER",
                "SCCE", "Small Cell Carcinoma of the Cervix", "Cervical Cancer", "Teal", "CERVIX",
                "SCCNOS", "Squamous Cell Carcinoma, NOS", "Cancer of Unknown Primary", "Black", "CUP",
                "SCCO", "Small Cell Carcinoma of the Ovary", "Ovarian Cancer", "LightBlue", "OVT",
                "SCCRCC", "Renal Clear Cell Carcinoma with Sarcomatoid Features", "Renal Cell Carcinoma", "Orange", "CCRCC",
                "SCEMU", "Signet Ring Mucinous Carcinoma", "Cervical Cancer", "Teal", "CEMU",
                "SCGBM", "Small Cell Glioblastoma", "Glioma", "Gray", "GB",
                "SCHW", "Schwannoma", "Nerve Sheath Tumor", "Gray", "NST",
                "SCLC", "Small Cell Lung Cancer", "Small Cell Lung Cancer", "Gainsboro", "LNET",
                "SCOAH", "Spindle Cell Oncocytoma of the Adenohypophysis", "Sellar Tumor", "Gray", "SELT",
                "SCOS", "Small Cell Osteosarcoma", "Bone Cancer", "White", "OS",
                "SCRMS", "Spindle Cell Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS",
                "SCSRMS", "Spindle Cell/Sclerosing Rhabdomyosarcoma", "Soft Tissue Sarcoma", "LightYellow", "RMS",
                "SCST", "Sex Cord Stromal Tumor", "Sex Cord Stromal Tumor", "LightBlue", "OVARY",
                "SCT", "Steroid Cell Tumor, NOS", "Sex Cord Stromal Tumor", "LightBlue", "SCST",
                "SCUP", "Small Cell Carcinoma of Unknown Primary", "Cancer of Unknown Primary", "Black", "CUP",
                "SDCA", "Salivary Duct Carcinoma", "Salivary Gland Cancer", "DarkRed", "SACA",
                "SEBA", "Sebaceous Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "SECOS", "Secondary Osteosarcoma", "Bone Cancer", "White", "OS",
                "SEF", "Sclerosing Epithelioid Fibrosarcoma", "Soft Tissue Sarcoma", "LightYellow", "FIBS",
                "SELT", "Sellar Tumor", "Sellar Tumor", "Gray", "BRAIN",
                "SEM", "Seminoma", "Germ Cell Tumor", "Red", "TESTIS",
                "SEZS", "Sezary Syndrome", "Non-Hodgkin Lymphoma", "LimeGreen", "CTCL",
                "SFT", "Solitary Fibrous Tumor/Hemangiopericytoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "SFTCNS", "Solitary Fibrous Tumor of the Central Nervous System", "CNS Cancer", "Gray", "MNGT",
                "SGAD", "Sweat Gland Adenocarcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "SGO", "Salivary Gland Oncocytoma", "Salivary Gland Cancer", "DarkRed", "SACA",
                "SGTTL", "Salivary Gland–Type Tumor of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "SIC", "Small Intestinal Carcinoma", "Small Bowel Cancer", "SaddleBrown", "BOWEL",
                "SKAC", "Skin Adnexal Carcinoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "SKCM", "Cutaneous Melanoma", "Melanoma", "Black", "MEL",
                "SKCN", "Congenital Nevus", "Melanoma", "Black", "MEL",
                "SKIN", "Skin", null, "Black", "TISSUE",
                "SKLMM", "Lentigo Maligna Melanoma", "Melanoma", "Black", "MEL",
                "SLCT", "Sertoli-Leydig Cell Tumor", "Sex Cord Stromal Tumor", "LightBlue", "SCST",
                "SLL", "Small Lymphocytic Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "SM", "Systemic Mastocytosis", "Mastocytosis", "LightSalmon", "BLOOD",
                "SMN", "Smooth Muscle Neoplasm, NOS", "Esophagogastric Cancer", "LightSkyBlue", "STOMACH",
                "SMZL", "Splenic Marginal Zone Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "SNA", "Sinonasal Adenocarcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA",
                "SNSC", "Sinonasal Squamous Cell Carcinoma", "Head and Neck Cancer", "DarkRed", "HNSC",
                "SNUC", "Sinonasal Undifferentiated Carcinoma", "Head and Neck Cancer", "DarkRed", "OHNCA",
                "SOC", "Serous Ovarian Cancer", "Ovarian Cancer", "LightBlue", "OVT",
                "SOFT_TISSUE", "Soft Tissue", null, "LightYellow", "TISSUE",
                "SPC", "Solid Papillary Carcinoma of the Breast", "Breast Cancer", "HotPink", "BRCA",
                "SPDAC", "Poorly Differentiated Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "USTAD",
                "SPIR", "Spiroma/Spiradenoma", "Skin Cancer, Non-Melanoma", "Black", "SKIN",
                "SPN", "Solid Pseudopapillary Neoplasm of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS",
                "SRAP", "Signet Ring Cell Type of the Appendix", "Appendiceal Cancer", "SaddleBrown", "APAD",
                "SRCBC", "Plasmacytoid/Signet Ring Cell Bladder Carcinoma", "Bladder Cancer", "Yellow", "BLADDER",
                "SRCC", "Sarcomatoid Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "SRCCR", "Signet Ring Cell Adenocarcinoma of the Colon and Rectum", "Colorectal Cancer", "SaddleBrown", "COADREAD",
                "SSRCC", "Signet Ring Cell Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "USTAD",
                "STAD", "Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC",
                "STAS", "Adenosquamous Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "EGC",
                "STMYEC", "Soft Tissue Myoepithelial Carcinoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "STOMACH", "Esophagus/Stomach", "Esophagogastric Cancer", "LightSkyBlue", "TISSUE",
                "STSC", "Small Cell Carcinoma of the Stomach", "Esophagogastric Cancer", "LightSkyBlue", "EGC",
                "SUBE", "Subependymoma", "CNS Cancer", "Gray", "EPMT",
                "SWDNET", "Well-Differentiated Neuroendocrine Tumors of the Stomach", "Gastrointestinal Neuroendocrine Tumor", "SaddleBrown", "GINET",
                "SYNS", "Synovial Sarcoma", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "SpCC", "Spindle Cell Carcinoma of the Lung", "Non-Small Cell Lung Cancer", "Gainsboro", "NSCLC",
                "TALL", "T-Cell Acute Lymphoid Leukemia", "Leukemia", "LightSalmon", "ALL",
                "TCCA", "Choriocarcinoma", "Germ Cell Tumor", "Red", "NSGCT",
                "TEOS", "Telangiectatic Osteosarcoma", "Bone Cancer", "White", "OS",
                "TESTIS", "Testis", null, "Red", "TISSUE",
                "TET", "Thymic Epithelial Tumor", "Thymic Tumor", "Purple", "THYMUS",
                "TGCT", "Tenosynovial Giant Cell Tumor Diffuse Type", "Soft Tissue Sarcoma", "LightYellow", "SOFT_TISSUE",
                "THAP", "Anaplastic Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID",
                "THFO", "Follicular Thyroid Cancer", "Thyroid Cancer", "Teal", "WDTC",
                "THHC", "Hurthle Cell Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID",
                "THME", "Medullary Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID",
                "THPA", "Papillary Thyroid Cancer", "Thyroid Cancer", "Teal", "WDTC",
                "THPD", "Poorly Differentiated Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID",
                "THYC", "Thymic Carcinoma", "Thymic Tumor", "Purple", "TET",
                "THYM", "Thymoma", "Thymic Tumor", "Purple", "TET",
                "THYMUS", "Thymus", "Thymic Tumor", "Purple", "TISSUE",
                "THYROID", "Thyroid", "Thyroid Cancer", "Teal", "TISSUE",
                "TISSUE", "Tissue", null, null, null,
                "TLYM", "Testicular Lymphoma", "Non-Hodgkin Lymphoma", "Red", "TESTIS",
                "TMESO", "Testicular Mesothelioma", "Mesothelioma", "Red", "TESTIS",
                "TMT", "Teratoma with Malignant Transformation", "Germ Cell Tumor", "Red", "NSGCT",
                "TNET", "Thymic Neuroendocrine Tumor", "Thymic Tumor", "Purple", "THYMUS",
                "TNKL", "T-Cell and Natural Killer Lymphoma", "Non-Hodgkin Lymphoma", "LimeGreen", "NHL",
                "TRCC", "Translocation-Associated Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "TSCST", "Sex Cord Stromal Tumor", "Sex Cord Stromal Tumor", "Red", "TESTIS",
                "TSTAD", "Tubular Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "STAD",
                "TT", "Teratoma", "Germ Cell Tumor", "Red", "NSGCT",
                "TYST", "Yolk Sac Tumor", "Germ Cell Tumor", "Red", "NSGCT",
                "UA", "Urachal Adenocarcinoma", "Bladder Cancer", "Yellow", "URCA",
                "UAD", "Urethral Adenocarcinoma", "Bladder Cancer", "Yellow", "UCA",
                "UAS", "Uterine Adenosarcoma", "Uterine Sarcoma", "PeachPuff", "USARC",
                "UASC", "Uterine Adenosquamous Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UCA", "Urethral Cancer", "Bladder Cancer", "Yellow", "BLADDER",
                "UCCA", "Choriocarcinoma", "Gestational Trophoblastic Disease", "PeachPuff", "GTD",
                "UCCC", "Uterine Clear Cell Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UCEC", "Endometrial Carcinoma", "Endometrial Cancer", "PeachPuff", "UTERUS",
                "UCP", "Undifferentiated Carcinoma of the Pancreas", "Pancreatic Cancer", "Purple", "PANCREAS",
                "UCS", "Uterine Carcinosarcoma/Uterine Malignant Mixed Mullerian Tumor", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UCU", "Urethral Urothelial Carcinoma", "Bladder Cancer", "Yellow", "UCA",
                "UDDC", "Uterine Dedifferentiated Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UDMN", "Undifferentiated Malignant Neoplasm", "Cancer of Unknown Primary", "Black", "CUP",
                "UEC", "Uterine Endometrioid Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UELMS", "Uterine Epithelioid Leiomyosarcoma", "Uterine Sarcoma", "PeachPuff", "USMT",
                "ULM", "Uterine Leiomyoma", "Uterine Sarcoma", "PeachPuff", "USMT",
                "ULMS", "Uterine Leiomyosarcoma", "Uterine Sarcoma", "PeachPuff", "USMT",
                "UM", "Uveal Melanoma", "Melanoma", "Green", "OM",
                "UMC", "Uterine Mucinous Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UMEC", "Uterine Mixed Endometrial Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UMLMS", "Uterine Myxoid Leiomyosarcoma", "Uterine Sarcoma", "PeachPuff", "USMT",
                "UMNC", "Uterine Mesonephric Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UNEC", "Uterine Neuroendocrine Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UPDC", "Poorly Differentiated Carcinoma of the Uterus", "Endometrial Cancer", "PeachPuff", "UCEC",
                "UPECOMA", "Uterine Perivascular Epithelioid Cell Tumor", "Uterine Sarcoma", "PeachPuff", "USARC",
                "URCA", "Urachal Carcinoma", "Bladder Cancer", "Yellow", "BLADDER",
                "URCC", "Unclassified Renal Cell Carcinoma", "Renal Cell Carcinoma", "Orange", "NCCRCC",
                "URMM", "Mucosal Melanoma of the Urethra", "Melanoma", "Yellow", "BLADDER",
                "USARC", "Uterine Sarcoma/Mesenchymal", "Uterine Sarcoma", "PeachPuff", "UTERUS",
                "USC", "Uterine Serous Carcinoma/Uterine Papillary Serous Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "USCC", "Urethral Squamous Cell Carcinoma", "Bladder Cancer", "Yellow", "UCA",
                "USMT", "Uterine Smooth Muscle Tumor", "Uterine Sarcoma", "PeachPuff", "USARC",
                "USTAD", "Undifferentiated Stomach Adenocarcinoma", "Esophagogastric Cancer", "LightSkyBlue", "EGC",
                "USTUMP", "Uterine Smooth Muscle Tumor of Uncertain Malignant Potential", "Uterine Sarcoma", "PeachPuff", "USMT",
                "UTERUS", "Uterus", null, "PeachPuff", "TISSUE",
                "UTUC", "Upper Tract Urothelial Carcinoma", "Bladder Cancer", "Yellow", "BLADDER",
                "UUC", "Uterine Undifferentiated Carcinoma", "Endometrial Cancer", "PeachPuff", "UCEC",
                "VA", "Vaginal Adenocarcinoma", "Vaginal Cancer", "Purple", "VULVA",
                "VDYS", "Dysgerminoma", "Germ Cell Tumor", "Purple", "VGCT",
                "VGCE", "Villoglandular Adenocarcinoma of the Cervix", "Cervical Cancer", "Teal", "CERVIX",
                "VGCT", "Germ Cell Tumor of the Vulva", "Germ Cell Tumor", "Purple", "VULVA",
                "VIMT", "Immature Teratoma", "Germ Cell Tumor", "Purple", "VGCT",
                "VMA", "Mucinous Adenocarcinoma of the Vulva/Vagina", "Vulvar Carcinoma", "Purple", "VULVA",
                "VMGCT", "Mixed Germ Cell Tumor", "Germ Cell Tumor", "Purple", "VGCT",
                "VMM", "Mucosal Melanoma of the Vulva/Vagina", "Melanoma", "Purple", "VULVA",
                "VMT", "Mature Teratoma", "Germ Cell Tumor", "Purple", "VGCT",
                "VOEC", "Embryonal Carcinoma", "Germ Cell Tumor", "Purple", "VGCT",
                "VPDC", "Poorly Differentiated Vaginal Carcinoma", "Vaginal Cancer", "Purple", "VULVA",
                "VPE", "Polyembryoma", "Germ Cell Tumor", "Purple", "VGCT",
                "VPSCC", "Verrucous Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PSCC",
                "VSC", "Squamous Cell Carcinoma of the Vulva/Vagina", "Vaginal Cancer", "Purple", "VULVA",
                "VULVA", "Vulva/Vagina", null, "Purple", "TISSUE",
                "VYST", "Yolk Sac Tumor", "Germ Cell Tumor", "Purple", "VGCT",
                "WDLS", "Well-Differentiated Liposarcoma", "Soft Tissue Sarcoma", "LightYellow", "LIPO",
                "WDTC", "Well-Differentiated Thyroid Cancer", "Thyroid Cancer", "Teal", "THYROID",
                "WM", "Waldenstrom Macroglobulinemia", "Non-Hodgkin Lymphoma", "LimeGreen", "BCL",
                "WPSCC", "Warty Penile Squamous Cell Carcinoma", "Penile Cancer", "Blue", "PSCC",
                "WT", "Wilms' Tumor", "Wilms Tumor", "Orange", "KIDNEY"};
        return rawTestValueSource;
    }
}
