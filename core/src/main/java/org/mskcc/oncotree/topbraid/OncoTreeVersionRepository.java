/*
 * Copyright (c) 2017 - 2020 Memorial Sloan-Kettering Cancer Center.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public class OncoTreeVersionRepository extends TopBraidRepository<Version> {

    private static final Logger logger = LoggerFactory.getLogger(OncoTreeVersionRepository.class);

    @Value("${topbraid.oncotree_version_namespace_prefix:http://data.mskcc.org/ontologies/OncoTreeVersion#}")
    private String topBraidOncotreeVersionNamespacePrefix;

    @Value("${topbraid.oncotree_version_list_graph_id:urn:x-evn-master:oncotreeversionlist}")
    private String topBraidOncotreeVersionListGraphId;

    private String query = null;

    // NOTE we MUST order by release_date
    private String getQuery() {
        if (query == null) {
            query = "PREFIX oncotree-version:<" + topBraidOncotreeVersionNamespacePrefix + "> " +
                    "SELECT ?api_identifier ?graph_uri ?description ?release_date ?visible " +
                    "WHERE { " +
                    "   GRAPH <" + topBraidOncotreeVersionListGraphId + "> { " +
                    "       ?s oncotree-version:graphUri ?graph_uri. " +
                    "       ?s oncotree-version:apiIdentifier ?api_identifier. " +
                    "       ?s oncotree-version:releaseDate ?release_date. " +
                    "       OPTIONAL{?s oncotree-version:description ?description.} " +
                    "       ?s oncotree-version:visible ?visible. " +
                    "   } " +
                    "} ORDER BY ASC(?release_date)";
        }
        return query;
    }

    /**
     * @return all OncoTree versions ordered by ascending release date (development last)
     */
    public ArrayList<Version> getOncoTreeVersions() throws TopBraidException {
        return new ArrayList<Version>(super.query(getQuery(), new ParameterizedTypeReference<List<Version>>(){}));
    }
}
