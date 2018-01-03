package org.mskcc.oncotree.api;

import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.mskcc.oncotree.crosswalk.CrosswalkRepository;
import org.mskcc.oncotree.crosswalk.MSKConcept;
import org.mskcc.oncotree.error.InvalidOncotreeMappingsParameters;
import org.mskcc.oncotree.model.OncotreeMappingsResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OncotreeMappingsApi {
    
    @Autowired
    private CrosswalkRepository crosswalkRepository;
    
    private final static Logger logger = Logger.getLogger(OncotreeMappingsApi.class);
    
    @RequestMapping(value = "api/crosswalk", method = RequestMethod.GET)
    public OncotreeMappingsResp getMappings(
                                        @RequestParam(value="vocabularyId", required=false) String vocabularyId,
                                        @RequestParam(value="conceptId", required=false) String conceptId,
                                        @RequestParam(value="histologyCode", required=false) String histologyCode,
                                        @RequestParam(value="siteCode", required=false) String siteCode
                                        ) {
        
        if( !validateMappingParameters(vocabularyId,
                                       conceptId,
                                       histologyCode,
                                       siteCode)
                                      ){
            throw new InvalidOncotreeMappingsParameters("Your query parameters, vocabularyId: " + vocabularyId +
                    ", conceptId: " + conceptId + ", histologyCode: " + histologyCode +
                    ", siteCode: " + siteCode + " are not valid. Please refer to the documentation");
        }
        
        MSKConcept mskConcept = crosswalkRepository.queryCVS(vocabularyId, conceptId, histologyCode, siteCode);
        
        return extractOncotreeMappings( mskConcept );
    }
    
    private static boolean validateMappingParameters(
                                                      String vocabularyId,
                                                      String conceptId,
                                                      String histologyCode,
                                                      String siteCode
                                                     ){
        // vocabularyId can't be null
        if(StringUtils.isEmpty(vocabularyId)){
            return false;
        }

        // if there's a conceptId, both histology and site must be null
        if(!StringUtils.isEmpty(conceptId)){
            if(!StringUtils.isEmpty(histologyCode) || !StringUtils.isEmpty(siteCode)){
                return false;
            }
        }

        // if there's no conceptId, both histology and site need to appear
        if(StringUtils.isEmpty(conceptId)){
            if(StringUtils.isEmpty(histologyCode) || StringUtils.isEmpty(siteCode)){
                return false;
            }
        }

        // otherwise validates
        return true;
    }    
    
    private OncotreeMappingsResp extractOncotreeMappings( MSKConcept mskConcept ){
        OncotreeMappingsResp rsp = new OncotreeMappingsResp();
        
        if( mskConcept == null ){
            return rsp;
        }
        
        if( mskConcept.getCrosswalks() != null && mskConcept.getCrosswalks().size() > 0 ){
            // TODO make constant for "ONCOTREE"
            if( mskConcept.getCrosswalks().containsKey("ONCOTREE")){
                logger.info("Oncotree mskConcept found for concept id " + mskConcept.getConceptIds().get(0));
                rsp.setOncotreeCode( mskConcept.getCrosswalks().get( "ONCOTREE" ) );
            }
        }
        
        else if(mskConcept.getOncotreeCodes() != null && mskConcept.getOncotreeCodes().size() > 0 ){
            logger.info("Oncotree mskConcept found for concept id " + mskConcept.getOncotreeCodes().toString());
            rsp.setOncotreeCode(mskConcept.getOncotreeCodes());
        }
        return rsp;
    }
    
}
