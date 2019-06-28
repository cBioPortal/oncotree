/*
 * Copyright (c) 2017 - 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.oncotree.crosswalk;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import javax.annotation.Generated;
import java.io.Serializable;
/**
 *
 * @author Manda Wilson
 **/
public class MSKConcept implements Serializable {
    // TODO this is a subset of the full model
    @JsonProperty("conceptId")
    private List<String> conceptIds;
    @JsonProperty("oncotreeCode")
    private List<String> oncotreeCodes;
    @JsonProperty("crosswalks")
    private HashMap<String, List<String>> crosswalks;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public MSKConcept() {
    }

    /**
    *
    * @return conceptIds
    */
    @JsonProperty("conceptId")
    public List<String> getConceptIds() {
        return conceptIds;
    }

    /**
    *
    * @param conceptIds
    */
    @JsonProperty("conceptIds")
    public void setConceptIds(List<String> conceptIds) {
        this.conceptIds = conceptIds;
    }

    /**
    *
    * @return oncotreeCodes
    */
    @JsonProperty("oncotreeCode")
    public List<String> getOncotreeCodes() {
        return oncotreeCodes;
    }

    /**
    *
    * @param oncotreeCodes
    */
    @JsonProperty("oncotreeCode")
    public void setOncotreeCodes(List<String> oncotreeCodes) {
        this.oncotreeCodes = oncotreeCodes;
    }

    /**
    *
    * @return crosswalks
    */
    @JsonProperty("crosswalks")
    public HashMap<String, List<String>> getCrosswalks() {
        return crosswalks;
    }

    /**
    *
    * @param crosswalks
    */
    @JsonProperty("crosswalks")
    public void setCrosswalks(HashMap<String, List<String>> crosswalks) {
        this.crosswalks = crosswalks;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
