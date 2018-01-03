package org.mskcc.oncotree.model;

import java.util.ArrayList;
import java.util.List;

public class OncotreeMappingsResp {
    
    private List<String> oncotreeCode;
    
    public OncotreeMappingsResp() {
        oncotreeCode = new ArrayList<String>();
    }

    public List<String> getOncotreeCode() {
        return oncotreeCode;
    }

    public void setOncotreeCode(List<String> oncotreeCode) {
        this.oncotreeCode = oncotreeCode;
    }
}
