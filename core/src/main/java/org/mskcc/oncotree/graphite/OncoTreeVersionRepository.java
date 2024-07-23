/*
 * Copyright (c) 2017 - 2020, 2024 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.oncotree.graphite;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mskcc.oncotree.model.Version;
import org.mskcc.oncotree.graphite.jsonmodeling.version.Binding;
import org.mskcc.oncotree.graphite.jsonmodeling.version.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public class OncoTreeVersionRepository extends GraphiteRepository<Response> {

    private static final Logger logger = LoggerFactory.getLogger(OncoTreeVersionRepository.class);

    @Value("${graphite.oncotree_version_namespace_prefix:http://data.mskcc.org/ontologies/oncotree-version#}")
    private String oncotreeVersionNamespacePrefix;

    @Value("${graphite.oncotree_version_list_graph_id}")
    private String oncotreeVersionListGraphId;

    private String query = null;

    // NOTE we MUST order by release_date
    private String getQuery() {
        logger.debug("query() -- oncotree version list graph id: " + oncotreeVersionListGraphId);
        if (query == null) {
            query = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                    "PREFIX otvl:<" + oncotreeVersionNamespacePrefix + "> " +
                    "PREFIX g:<http://schema.synaptica.com/oasis#> " +
                    "SELECT ?api_identifier ?graph_uri ?description ?release_date ?visible WHERE { " +
                        "?s skos:inScheme <" + oncotreeVersionListGraphId + "> . " +
                        "?s otvl:retrievalidentifier ?graph_uri. " +
                        "?s otvl:apiidentifier ?api_identifier. " +
                        "?s otvl:releasedate ?release_date. " +
                        "OPTIONAL{?s otvl:description ?description.} " +
                        "?s otvl:visible ?visible. " +
                        "OPTIONAL{?s g:conceptStatus ?concept_status.} " +
                        "FILTER (?concept_status = 'Published') " +
                        "} ORDER BY ASC(?release_date)";
        }
        return query;
    }

    /**
     * @return all OncoTree versions ordered by ascending release date (development last)
     */
    public ArrayList<Version> getOncoTreeVersions() throws GraphiteException {
        return map(super.query(getQuery(), new ParameterizedTypeReference<Response>(){}));
    }

    private ArrayList<Version> map(Response versionResponse) {
        List<Binding> bindings = versionResponse.getResults().getBindings();
        ArrayList<Version> versions = new ArrayList<Version>(bindings.size());
        for (Binding binding : bindings) {
            Version version = new Version();
            if (binding.getApiIdentifier() != null) {
                version.setVersion(binding.getApiIdentifier().getValue());
            }
            if (binding.getGraphUri() != null) {
                version.setGraphURI(binding.getGraphUri().getValue());
            }
            if (binding.getDescription() != null) {
                version.setDescription(binding.getDescription().getValue());
            }
            if (binding.getVisible() != null) {
                version.setVisible(Boolean.parseBoolean(binding.getVisible().getValue()));
            }
            if (binding.getReleaseDate() != null) {
                version.setReleaseDate(binding.getReleaseDate().getValue());
            }
            versions.add(version);
        }
        return versions;
    }
}
