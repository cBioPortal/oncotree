/*
 * Copyright (c) 2017, 2024 Memorial Sloan-Kettering Cancer Center.
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
import org.mskcc.oncotree.graphite.jsonmodeling.oncotree.Binding;
import org.mskcc.oncotree.graphite.jsonmodeling.oncotree.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public class OncoTreeRepository extends GraphiteRepository<Response> {

    private static final Logger logger = LoggerFactory.getLogger(OncoTreeRepository.class);

    @Value("${graphite.oncotree_namespace_prefix:http://data.mskcc.org/ontologies/oncotree#}")
    private String oncotreeNamespacePrefix;

    private String query = null;

    private String getQuery() {
        if (query == null) {
            query = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                    "PREFIX ottt:<" + oncotreeNamespacePrefix + "> " +
                    "PREFIX g:<http://schema.synaptica.com/oasis#> " +
                    "SELECT DISTINCT (?s AS ?uri) ?code ?name ?mainType ?color ?parentCode ?revocations ?precursors ?clinicalCasesSubset WHERE { " +
                    "   ?s skos:inScheme <%s> { " +
                    "       ?s skos:prefLabel ?name;" +
                    "       skos:notation ?code." +
                    "       OPTIONAL{?s skos:broader ?broader." +
                    "           ?broader skos:notation ?parentCode}." +
                    "       OPTIONAL{?s ottt:maintype ?mainType}." +
                    "       OPTIONAL{?s ottt:color ?color}." +
                    "       OPTIONAL{?s ottt:revocations ?revocations}." +
                    "       OPTIONAL{?s ottt:precursors ?precursors}." +
                    "       ?s ottt:clinicalcasessubset ?clinicalCasesSubset." +
                    "       OPTIONAL{?s g:conceptStatus ?concept_status.} " +
                    "       FILTER (?concept_status = 'Published') " +
                    "}}";
        }
        return query;
    }

    public ArrayList<OncoTreeNode> getOncoTree(Version version) throws GraphiteException {
        logger.debug("getOncoTree() -- version: " + version.getVersion() + ", graphURI: " + version.getGraphURI());
        return map(super.query(String.format(getQuery(), version.getGraphURI()), new ParameterizedTypeReference<Response>(){}));
    }

    private ArrayList<OncoTreeNode> map(Response oncoTreeResponse) {
        List<Binding> bindings = oncoTreeResponse.getResults().getBindings();
        ArrayList<OncoTreeNode> oncoTreeNodes = new ArrayList<OncoTreeNode>(bindings.size());
        for (Binding binding : bindings) {
            OncoTreeNode oncoTreeNode = new OncoTreeNode();
            if (binding.getUri() != null) {
                oncoTreeNode.setURI(binding.getUri().getValue());
            }
            if (binding.getCode() != null) {
                oncoTreeNode.setCode(binding.getCode().getValue());
            }
            if (binding.getName() != null) {
                oncoTreeNode.setName(binding.getName().getValue());
            }
            if (binding.getMainType() != null) {
                oncoTreeNode.setMainType(binding.getMainType().getValue());
            }
            if (binding.getClinicalCasesSubset() != null) {
                oncoTreeNode.setClinicalCasesSubset(binding.getClinicalCasesSubset().getValue());
            }
            if (binding.getColor() != null) {
                oncoTreeNode.setColor(binding.getColor().getValue());
            }
            if (binding.getParentCode() != null) {
                oncoTreeNode.setParentCode(binding.getParentCode().getValue());
            }
            if (binding.getRevocations() != null) {
                oncoTreeNode.setRevocations(binding.getRevocations().getValue());
            }
            if (binding.getPrecursors() != null) {
                oncoTreeNode.setPrecursors(binding.getPrecursors().getValue());
            }
            oncoTreeNodes.add(oncoTreeNode);
        }
        return oncoTreeNodes;
    }
}
