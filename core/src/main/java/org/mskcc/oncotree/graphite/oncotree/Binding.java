/*
 * Copyright (c) 2024 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.oncotree.graphite.oncotree;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Binding {
    @JsonProperty("uri")
    private Field uri;

    @JsonProperty("code")
    private Field code;

    @JsonProperty("name")
    private Field name;

    @JsonProperty("clinicalCasesSubset")
    private Field clinicalCasesSubset;

    @JsonProperty("mainType")
    private Field mainType;

    @JsonProperty("color")
    private Field color;

    @JsonProperty("parentCode")
    private Field parentCode;

    @JsonProperty("precursors")
    private Field precursors;

    @JsonProperty("revocations")
    private Field revocations;

    public Field getUri() {
        return uri;
    }

    public void setUri(Field uri) {
        this.uri = uri;
    }

    public Field getCode() {
        return code;
    }

    public void setCode(Field code) {
        this.code = code;
    }

    public Field getName() {
        return name;
    }

    public void setName(Field name) {
        this.name = name;
    }

    public Field getClinicalCasesSubset() {
        return clinicalCasesSubset;
    }

    public void setClinicalCasesSubset(Field clinicalCasesSubset) {
        this.clinicalCasesSubset = clinicalCasesSubset;
    }

    public Field getMainType() {
        return mainType;
    }

    public void setMainType(Field mainType) {
        this.mainType = mainType;
    }

    public Field getColor() {
        return color;
    }

    public void setColor(Field color) {
        this.color = color;
    }

    public Field getParentCode() {
        return parentCode;
    }

    public void setParentCode(Field parentCode) {
        this.parentCode = parentCode;
    }

    public Field getPrecursors() {
        return precursors;
    }

    public void setPrecursors(Field precursors) {
        this.precursors = precursors;
    }

    public Field getRevocations() {
        return revocations;
    }

    public void setRevocations(Field revocations) {
        this.revocations = revocations;
    }
}
