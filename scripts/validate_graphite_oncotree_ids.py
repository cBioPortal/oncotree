# Compares concept URIs in docs/resource_uri_to_oncocode_mapping.txt to the current concept IDs defined in Graphite.
#
# ./validate_graphite_oncotree_ids.py --curated-file resource_uri_to_oncotree_mapping_file --properties-file application.properties
#
# Author: Manda Wilson and Robert Sheridan
#
# -*- coding: utf-8 -*-

import optparse
import os.path
import sys
import csv
import re
import configparser
import requests
from requests.auth import HTTPBasicAuth
from collections import defaultdict

# TODO URI has to point to production system (2 places)
GRAPHITE_REFERENCE_VERSION_ID = 'https://preprod3.msk.synaptica.net/concept_scheme/2df299e4-596b-cbd6-45f8-48cbf287bb95'
GRAPHITE_VERSION_QUERY = """
PREFIX otvl:<http://data.mskcc.org/ontologies/oncotree-version#>
        SELECT ?api_identifier ?graph_uri ?description ?release_date ?visible
        WHERE {
            GRAPH <https://preprod3.msk.synaptica.net/concept_scheme/3bb2d189-54c5-7cf7-e44b-e44a727611e6> {
                ?s otvl:retrievalidentifier ?graph_uri. 
                ?s otvl:apiidentifier ?api_identifier. 
                ?s otvl:releasedate ?release_date. 
                OPTIONAL{?s otvl:description ?description.} 
                ?s otvl:visible ?visible. 
            }
        } ORDER BY ASC(?release_date)
"""

GRAPHITE_DATA_QUERY = """
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
        PREFIX ottt:<http://data.mskcc.org/ontologies/oncotree#>
SELECT DISTINCT (?s AS ?uri) ?code ?name ?mainType ?color ?parentCode ?revocations ?precursors ?clinicalCasesSubset
        WHERE {
           GRAPH <%s> {
               ?s skos:prefLabel ?name;
               skos:notation ?code.
               OPTIONAL{?s skos:broader ?broader.
                   ?broader skos:notation ?parentCode}.
               OPTIONAL{?s ottt:maintype ?mainType}.
               OPTIONAL{?s ottt:color ?color}.
               OPTIONAL{?s ottt:revocations ?revocations}.
               OPTIONAL{?s ottt:precursors ?precursors}.
               ?s ottt:clinicalcasessubset ?clinicalCasesSubset.
           }
        }
"""
NUM_FIELDS_IN_CURATED_FILE = 3
URI_PATTERN_STR = "^ONC[0-9]{6}$" # e.g. ONC000873
URI_PATTERN = re.compile(URI_PATTERN_STR)
EXPECTED_VERBS = ["hadCode", "hasCode", "hasPrecursor", "hasRevocation"]
GRAPHITE_URL_PROPERTY_NAME = "graphite.url"
GRAPHITE_USERNAME_PROPERTY_NAME = "graphite.username"
GRAPHITE_PASSWORD_PROPERTY_NAME = "graphite.password"
DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE = "DEFAULT"
errors = []
warnings = []
information = []

# from https://stackoverflow.com/questions/2819696/parsing-properties-file-in-python/2819788#2819788
def add_section_header(fp):
    yield "[%s]\n" % (DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE)
    yield from fp

def query_graphite(query, graphite_url, graphite_username, graphite_password):
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Accept': 'application/sparql-results+json',
    }

    data = {
        'query': query
    }

    auth = HTTPBasicAuth(graphite_username, graphite_password)

    response = requests.post(graphite_url, headers=headers, data=data, auth=auth)
    if response.status_code != 200:
        sys.stderr.write("ERROR: Failed to query '%s', response status code is '%d', body is '%s'\n" % (graphite_url, response.status_code, response.text))
        sys.exit(2)
    return response.json()

def validate_oncotree_id(oncotree_id, source):
    """Adds error to errors array if oncotree_id does not match expected pattern"""
    if not URI_PATTERN.match(oncotree_id):
        errors.append("'%s' does not match expected pattern '%s' in '%s'" % (oncotree_id, URI_PATTERN_STR, source))

def validate_verb(verb, source):
    """Adds error to errors array if verb does not match one of expected verbs"""
    if verb not in EXPECTED_VERBS:
        errors.append("'%s' does not match one of expected verbs '%s' in '%s'" % (verb, ", ".join(EXPECTED_VERBS), source))

def read_curated_oncotree_ids(curated_filename):
    """Validate that file is tab delimited, format is subject verb object"""
    oncotree_ids_to_verbs_to_objects = defaultdict(lambda: defaultdict(set))
    # concept for storing what comes out of the mapping file
    #   {"ONC000001" : {
    #       "hasCode" : set(string), ##set of one item for convenience of initialization
    #       "hadCode" : set(string),
    #       "hasPrecursor" : set(string),
    #       "hasRevocation" : set(string)
    #   }}

    with open(curated_filename) as curated_file:
        for line in curated_file:
            line = line.strip()
            fields = line.split("\t")
            if "\t" not in line:
                errors.append("Line '%s' does not have a '\\t' as a delimiter in file '%s'" % (line, curated_filename))
            elif len(fields) != NUM_FIELDS_IN_CURATED_FILE:
                errors.append("Line '%s' has %d field(s) in file '%s', when %d are expected.  Fields are: %s" % (line, len(fields), curated_filename, NUM_FIELDS_IN_CURATED_FILE, ",".join("'" + field + "'" for field in fields)))
            else:
                oncotree_ids_to_verbs_to_objects[fields[0]][fields[1]].add(fields[2])
                validate_oncotree_id(fields[0], curated_filename)
                validate_verb(fields[1], curated_filename)
                # we don't really need to validate the subject because that will be done when comparing to Graphite
    #TODO : check that all references to URIs that appear in the objects (like precursors, revocations) match some URI subject that was parsed
    return oncotree_ids_to_verbs_to_objects

def read_graphite_versions(graphite_results):
    versions = []
    for version in graphite_results['results']['bindings']:
        versions.append(version['graph_uri']['value'])
    return versions

def read_oncotree_ids(graphite_results):
    """Read the results from a query of a single Grahpite version"""
    # concept for storing what comes out of the parsing of SPARQL output for a version
    #   {"ONC000001" : {
    #       "code" : set(string), ##set of one item for convenience of initialization
    #       "precursors" : set(string),
    #       "revocations" : set(string)
    #   }}
    oncotree_ids_to_properties_to_values = defaultdict(lambda: defaultdict(set))
    for node in graphite_results['results']['bindings']:
        oncotree_id = node['clinicalCasesSubset']['value']
        oncotree_ids_to_properties_to_values[oncotree_id]['code'].add(node['code']['value'])
        if 'precursors' in node and node['precursors']:
            oncotree_ids_to_properties_to_values[oncotree_id]['precursors'] |= set(node['precursors']['value'].split())
        if 'revocations' in node and node['revocations']:
            oncotree_ids_to_properties_to_values[oncotree_id]['revocations'] |= set(node['revocations']['value'].split())
        validate_oncotree_id(oncotree_id, "Graphite")
    return oncotree_ids_to_properties_to_values

def validate_curated_statements(oncotree_ids_to_prior_code_set, oncotree_ids_to_properties_to_values, curated_oncotree_ids_to_verbs_to_objects):
    """Compare all code attributes in Graphite to the curated 'hasCode' values -- all should match"""
    """Compare all precursors attributes in Graphite to the curated 'hasPrecursor' values -- all should match"""
    """Compare all revocations attributes in Graphite to the curated 'hasRevocation' values -- all should match"""
    """compare all prior code attributes in Graphite which differ from current to curated 'hadCode' values -- all should match"""
    curated_key_set = set(curated_oncotree_ids_to_verbs_to_objects.keys())
    graphite_key_set = set(oncotree_ids_to_properties_to_values.keys())

    in_graphite_only = graphite_key_set - curated_key_set
    if in_graphite_only:
        # this should not happen
        errors.append("Graphite URIs not found in curated: '%s'" % (", ".join(sorted(in_graphite_only))))

    # things that are in curated only are not errors ... these would be nodes which were deleted before the release of the current version

    keys_in_both = curated_key_set & graphite_key_set
    for key in sorted(keys_in_both):
        print("Key in both:", key)
        graphite_current_code_set = oncotree_ids_to_properties_to_values[key]["code"]
        if curated_oncotree_ids_to_verbs_to_objects[key]["hasCode"] != graphite_current_code_set:
            errors.append("Code for URI '%s' does not match between curated '%s' and Graphite '%s'" % (key, ",".join(curated_oncotree_ids_to_verbs_to_objects[key]["hasCode"]), ",".join(oncotree_ids_to_properties_to_values[key]["code"])))
        graphite_had_code_set = oncotree_ids_to_prior_code_set[key] - graphite_current_code_set
        if graphite_had_code_set - curated_oncotree_ids_to_verbs_to_objects[key]["hadCode"]: # allow hadCode references which are not in graphite, but not vice-vera
            errors.append("HadCode set for URI '%s' does not match between curated '%s' and Graphite '%s'" % (key, ",".join(curated_oncotree_ids_to_verbs_to_objects[key]["hadCode"]), ",".join(graphite_had_code_set)))
        if curated_oncotree_ids_to_verbs_to_objects[key]["hasPrecursor"] != oncotree_ids_to_properties_to_values[key]["precursors"]:
            errors.append("Precursor set for URI '%s' does not match between curated '%s' and Graphite '%s'" % (key, ",".join(curated_oncotree_ids_to_verbs_to_objects[key]["hasPrecursor"]), ",".join(oncotree_ids_to_properties_to_values[key]["precursors"])))
        if curated_oncotree_ids_to_verbs_to_objects[key]["hasRevocation"] != oncotree_ids_to_properties_to_values[key]["revocations"]:
            errors.append("Revocation set for URI '%s' does not match between curated '%s' and Graphite '%s'" % (key, ",".join(curated_oncotree_ids_to_verbs_to_objects[key]["hasRevocation"]), ",".join(oncotree_ids_to_properties_to_values[key]["revocations"])))

def accumulate_codes_from_version(oncotree_ids_to_prior_code_set, oncotree_ids_to_properties_to_values):
    """add the current code for each oncotree ID (ONC00XXXX) into the appropriate set in the accumulator oncotree_ids_to_prior_code_set"""
    for oncotree_id in oncotree_ids_to_properties_to_values:
        oncotree_ids_to_prior_code_set[oncotree_id] |= oncotree_ids_to_properties_to_values[oncotree_id]['code']

def usage():
    sys.stdout.write('python3 validate_graphite_oncotree_ids.py --curated-file [path/to/curated/file] --properties-file [path/to/properties/file]\n')

def main():
    # get command line stuff
    parser = optparse.OptionParser()
    parser.add_option('-c', '--curated-file', action = 'store', dest = 'curated_filename')
    parser.add_option('-p', '--properties-file', action = 'store', dest = 'properties_filename')

    (options, args) = parser.parse_args()
    curated_filename = options.curated_filename
    properties_filename = options.properties_filename

    if not curated_filename:
        sys.stderr.write('Curated file is required\n')
        usage()
        sys.exit(2)
    if not properties_filename:
        sys.stderr.write('Properties file is required\n')
        usage()
        sys.exit(2)
    if not os.path.exists(curated_filename):
        sys.stderr.write('No such file: %s\n' % (curated_filename))
        usage()
        sys.exit(2)
    if not os.path.exists(properties_filename):
        sys.stderr.write('No such file: %s\n' % (properties_filename))
        usage()
        sys.exit(2)

    config = configparser.RawConfigParser()
    config.read_file(add_section_header(open(properties_filename)))
    try:
        graphite_url = config.get(DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE, GRAPHITE_URL_PROPERTY_NAME)
        graphite_username = config.get(DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE, GRAPHITE_USERNAME_PROPERTY_NAME)
        graphite_password = config.get(DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE, GRAPHITE_PASSWORD_PROPERTY_NAME)
    except configparser.NoOptionError as noe:
        sys.stderr.write("ERROR: %s in properties file\n" % (noe))
        sys.exit(2)

    graphite_version_results = query_graphite(GRAPHITE_VERSION_QUERY, graphite_url, graphite_username, graphite_password)

    curated_oncotree_ids_to_verbs_to_objects = read_curated_oncotree_ids(curated_filename)
    graphite_versions = read_graphite_versions(graphite_version_results)

    oncotree_ids_to_prior_code_set = defaultdict(set)
    for version in graphite_versions:
        sys.stdout.write("Looking at version: %s\n" % (version))
        graphite_results = query_graphite(GRAPHITE_DATA_QUERY % (version), graphite_url, graphite_username, graphite_password)
        oncotree_ids_to_properties_to_values = read_oncotree_ids(graphite_results)
        if version == GRAPHITE_REFERENCE_VERSION_ID:
            validate_curated_statements(oncotree_ids_to_prior_code_set, oncotree_ids_to_properties_to_values, curated_oncotree_ids_to_verbs_to_objects)
            break
        accumulate_codes_from_version(oncotree_ids_to_prior_code_set, oncotree_ids_to_properties_to_values)

    # print information to stdout and do not exit with failure error code
    if information:
        for info in information:
            sys.stdout.write("INFO: %s\n" % (info))

    # print warnings to stdout and do not exit with failure error code
    if warnings:
        for warning in warnings:
            sys.stdout.write("WARNING: %s\n" % (warning))

    # print warnings to stderr and exit with failure error code
    if errors:
        for error in errors:
            sys.stderr.write("ERROR: %s\n" % (error))
        sys.exit(1)

if __name__ == '__main__':
    main()
