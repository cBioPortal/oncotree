/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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
public class OncoTreeRepository extends TopBraidRepository<OncoTreeNode> {

    private static final Logger logger = LoggerFactory.getLogger(OncoTreeRepository.class);

    private String query = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
        "PREFIX onc:<http://data.mskcc.org/ontologies/oncotree#> " +
        "SELECT DISTINCT (?s AS ?uri) ?code ?name ?mainType ?color ?parentCode ?revocations ?precursors " +
        "WHERE { " +
        "   GRAPH <%s> { " +
        "       ?s skos:prefLabel ?name;" +
        "       skos:notation ?code." +
        "       OPTIONAL{?s skos:broader ?broader." +
        "           ?broader skos:notation ?parentCode}." +
        "       OPTIONAL{?s onc:mainType ?mainType}." +
        "       OPTIONAL{?s onc:color ?color}." +
        "       OPTIONAL{?s onc:revocations ?revocations}." +
        "       OPTIONAL{?s onc:precursors ?precursors}." +
        "   }" +
        "}";

    public ArrayList<OncoTreeNode> getOncoTree(Version version) throws TopBraidException {
        ArrayList<OncoTreeNode> list = new ArrayList<OncoTreeNode>(super.query(String.format(query, version.getGraphURI()), new ParameterizedTypeReference<List<OncoTreeNode>>(){}));
        return list;
    }

}
