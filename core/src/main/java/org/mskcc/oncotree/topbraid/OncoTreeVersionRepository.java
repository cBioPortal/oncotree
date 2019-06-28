/*
 * Copyright (c) 2017-2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.oncotree.topbraid;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mskcc.oncotree.model.Version;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public class OncoTreeVersionRepository extends TopBraidRepository<Version> {

    private static final Logger logger = LoggerFactory.getLogger(OncoTreeVersionRepository.class);

    // NOTE we MUST order by release_date
    private String query = "PREFIX oncotree-version:<http://data.mskcc.org/ontologies/oncotree_version/> " +
        "SELECT ?api_identifier ?graph_uri ?description ?release_date ?visible " +
        "WHERE { " +
        "   GRAPH <urn:x-evn-master:oncotree_version> { " +
        "       ?s oncotree-version:graph_uri ?graph_uri. " +
        "       ?s oncotree-version:api_identifier ?api_identifier. " +
        "       ?s oncotree-version:release_date ?release_date. " +
        "       OPTIONAL{?s oncotree-version:description ?description.} " +
        "       OPTIONAL{?s oncotree-version:visible ?visible.} " +
        "   } " +
        "} ORDER BY ASC(?release_date)";

    /**
     * @return all OncoTree versions ordered by ascending release date (development last)
     */
    public ArrayList<Version> getOncoTreeVersions() throws TopBraidException {
        ArrayList<Version> list = new ArrayList<Version>(super.query(query, new ParameterizedTypeReference<List<Version>>(){}));
        return list;
    }
}
