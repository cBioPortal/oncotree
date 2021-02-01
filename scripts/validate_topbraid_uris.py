# Compares concept URIs in docs/resource_uri_to_oncocode_mapping.txt to the current concept URIs defined in TopBraid.
#
# ./validate_topbraid_uris.py --curated-file resource_uri_to_oncotree_mapping_file --properties-file application.properties
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
import urllib.request
from collections import defaultdict

TOPBRAID_REFERENCE_VERSION_ID='urn:x-evn-master:oncotree_candidate_release'
TOPBRAID_VERSION_QUERY = """
PREFIX oncotree-version:<http://data.mskcc.org/ontologies/oncotree_version/>
        SELECT ?api_identifier ?graph_uri ?description ?release_date ?visible
        WHERE {
           GRAPH <urn:x-evn-master:oncotree_version> {
               ?s oncotree-version:graph_uri ?graph_uri.
               ?s oncotree-version:api_identifier ?api_identifier.
               ?s oncotree-version:release_date ?release_date.
               OPTIONAL{?s oncotree-version:description ?description.}
               OPTIONAL{?s oncotree-version:visible ?visible.}
           }
        } ORDER BY ASC(?release_date)
"""

TOPBRAID_DATA_QUERY = """
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
        PREFIX onc:<http://data.mskcc.org/ontologies/oncotree#>
SELECT DISTINCT (?s AS ?uri) ?code ?name ?mainType ?color ?parentCode ?revocations ?precursors
        WHERE {
           GRAPH <%s> {
               ?s skos:prefLabel ?name;
               skos:notation ?code.
               OPTIONAL{?s skos:broader ?broader.
                   ?broader skos:notation ?parentCode}.
               OPTIONAL{?s onc:mainType ?mainType}.
               OPTIONAL{?s onc:color ?color}.
               OPTIONAL{?s onc:revocations ?revocations}.
               OPTIONAL{?s onc:precursors ?precursors}.
           }
        }
"""
NUM_FIELDS_IN_CURATED_FILE = 3
URI_PATTERN_STR = "^ONC[0-9]{6}$" # e.g. ONC000873
URI_PATTERN = re.compile(URI_PATTERN_STR)
EXPECTED_VERBS = ["hadCode", "hasCode", "hasPrecursor", "hasRevocation"]
TOPBRAID_URL_PROPERTY_NAME = "topbraid.url"
TOPBRAID_USERNAME_PROPERTY_NAME = "topbraid.username"
TOPBRAID_PASSWORD_PROPERTY_NAME = "topbraid.password"
DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE = "DEFAULT"
JSESSION_ID_COOKIE_NAME = "JSESSIONID"
errors = []
warnings = []
information = []

# from https://stackoverflow.com/questions/2819696/parsing-properties-file-in-python/2819788#2819788
class DefaultSectionHeadOnPropertiesFile:

    def __init__(self, fp):
        self.fp = fp
        self.section_head = "[%s]\n" % (DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE)

    def readline(self):
        if self.section_head:
            try:
                return self.section_head
            finally:
                self.section_head = None
        else:
            return self.fp.readline()

def get_logged_in_session_id(topbraid_url, topbraid_username, topbraid_password):
    # first we just hit the page and get a session id
    session = urllib.request.Session()
    response = session.get(topbraid_url)
    if response.status_code != 200:
        sys.stderr.write("ERROR: Initial connection to '%s' failed, response status code is '%d', body is '%s'\n" % (topbraid_url, response.status_code, response.text))
        sys.exit(2)
    initial_jsession_id = session.cookies.get_dict()[JSESSION_ID_COOKIE_NAME]
    # now we login using that session id
    response = session.get(topbraid_url + "/j_security_check?j_username=" + topbraid_username + "&j_password=" + topbraid_password, cookies={ JSESSION_ID_COOKIE_NAME : initial_jsession_id })
    if response.status_code != 200:
        sys.stderr.write("ERROR: Failed to log into '%s', response status code is '%d', body is '%s'\n" % (topbraid_url, response.status_code, response.text))
        sys.exit(2)
    logged_in_session_id = session.cookies.get_dict()[JSESSION_ID_COOKIE_NAME]
    return logged_in_session_id

def query_topbraid(query, topbraid_url, logged_in_session_id):
    session = urllib.request.Session()
    data = {"format" : "json-simple", "query" : query}
    response = session.post(topbraid_url, cookies={ JSESSION_ID_COOKIE_NAME : logged_in_session_id}, data=data)
    if response.status_code != 200:
        sys.stderr.write("ERROR: Failed to query '%s', response status code is '%d', body is '%s'\n" % (topbraid_url, response.status_code, response.text))
        sys.exit(2)
    return response.json()

def validate_uri(uri, source):
    """Adds error to errors array if uri does not match expected pattern"""
    if not URI_PATTERN.match(uri):
        errors.append("'%s' does not match expected pattern '%s' in '%s'" % (uri, URI_PATTERN_STR, source))

def validate_verb(verb, source):
    """Adds error to errors array if verb does not match one of expected verbs"""
    if verb not in EXPECTED_VERBS:
        errors.append("'%s' does not match one of expected verbs '%s' in '%s'" % (verb, ", ".join(EXPECTED_VERBS), source))

def read_curated_uris(curated_filename):
    """Validate that file is tab delimited, format is subject verb object"""
    uris_to_verbs_to_objects = defaultdict(lambda: defaultdict(set))
    # concept for storing what comes out of the mapping file
    #   {"URI000001" : {
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
                uris_to_verbs_to_objects[fields[0]][fields[1]].add(fields[2])
                validate_uri(fields[0], curated_filename)
                validate_verb(fields[1], curated_filename)
                # we don't really need to validate the subject because that will be done when comparing to TopBraid
    #TODO : check that all references to URIs that appear in the objects (like precursors, revocations) match some URI subject that was parsed
    return uris_to_verbs_to_objects

def read_topbraid_versions(topbraid_results):
    versions = []
    for version in topbraid_results:
        versions.append(version["graph_uri"])
    return versions

def read_topbraid_uris(topbraid_results):
    """Read the results from a query of a single TopBraid version"""
    # concept for storing what comes out of the parsing of SPARQL output for a version
    #   {"URI000001" : {
    #       "code" : set(string), ##set of one item for convenience of initialization
    #       "precursors" : set(string),
    #       "revocations" : set(string)
    #   }}

    uris_to_properties_to_values = defaultdict(lambda: defaultdict(set))
    for node in topbraid_results:
        # URI looks like http://data.mskcc.org/ontologies/oncotree/ONC000371
        uri = node['uri'].split("/")[-1]
        uris_to_properties_to_values[uri]['code'].add(node['code'])
        if node['precursors']:
            uris_to_properties_to_values[uri]['precursors'] |= set(node['precursors'].split())
        if node['revocations']:
            uris_to_properties_to_values[uri]['revocations'] |= set(node['revocations'].split())
        validate_uri(uri, "TopBraid")
    return uris_to_properties_to_values

def validate_curated_statements(topbraid_uris_to_prior_code_set, topbraid_uris_to_properties_to_values, curated_uris_to_verbs_to_objects):
    """Compare all code attributes in TopBraid to the curated 'hasCode' values -- all should match"""
    """Compare all precursors attributes in TopBraid to the curated 'hasPrecursor' values -- all should match"""
    """Compare all revocations attributes in TopBraid to the curated 'hasRevocation' values -- all should match"""
    """compare all prior code attributes in TopBraid which differ from current to curated 'hadCode' values -- all should match"""
    curated_key_set = set(curated_uris_to_verbs_to_objects.keys())
    topbraid_key_set = set(topbraid_uris_to_properties_to_values.keys())

    in_topbraid_only = topbraid_key_set - curated_key_set
    if in_topbraid_only:
        # this should not happen
        errors.append("TopBraid URIs not found in curated: '%s'" % (", ".join(sorted(in_topbraid_only))))

    # things that are in curated only are not errors ... these would be nodes which were deleted before the release of the current version

    keys_in_both = curated_key_set & topbraid_key_set
    for key in sorted(keys_in_both):
        topbraid_current_code_set = topbraid_uris_to_properties_to_values[key]["code"]
        if curated_uris_to_verbs_to_objects[key]["hasCode"] != topbraid_current_code_set:
            errors.append("Code for URI '%s' does not match between curated '%s' and TopBraid '%s'" % (key, ",".join(curated_uris_to_verbs_to_objects[key]["hasCode"]), ",".join(topbraid_uris_to_properties_to_values[key]["code"])))
        topbraid_had_code_set = topbraid_uris_to_prior_code_set[key] - topbraid_current_code_set
        if topbraid_had_code_set - curated_uris_to_verbs_to_objects[key]["hadCode"]: # allow hadCode references which are not in topbraid, but not vice-vera
            errors.append("HadCode set for URI '%s' does not match between curated '%s' and TopBraid '%s'" % (key, ",".join(curated_uris_to_verbs_to_objects[key]["hadCode"]), ",".join(topbraid_had_code_set)))
        if curated_uris_to_verbs_to_objects[key]["hasPrecursor"] != topbraid_uris_to_properties_to_values[key]["precursors"]:
            errors.append("Precursor set for URI '%s' does not match between curated '%s' and TopBraid '%s'" % (key, ",".join(curated_uris_to_verbs_to_objects[key]["hasPrecursor"]), ",".join(topbraid_uris_to_properties_to_values[key]["precursors"])))
        if curated_uris_to_verbs_to_objects[key]["hasRevocation"] != topbraid_uris_to_properties_to_values[key]["revocations"]:
            errors.append("Revocation set for URI '%s' does not match between curated '%s' and TopBraid '%s'" % (key, ",".join(curated_uris_to_verbs_to_objects[key]["hasRevocation"]), ",".join(topbraid_uris_to_properties_to_values[key]["revocations"])))

def accumulate_codes_from_version(topbraid_uris_to_prior_code_set, topbraid_uris_to_properties_to_values):
    """add the current code for each URI into the appropriate set in the accumulator topbraid_uris_to_prior_code_set"""
    for uri in topbraid_uris_to_properties_to_values:
        topbraid_uris_to_prior_code_set[uri] |= topbraid_uris_to_properties_to_values[uri]['code']

def usage():
    sys.stdout.write('python3 validate_topbraid_uris.py --curated-file [path/to/curated/file] --properties-file [path/to/properties/file]\n')

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
    config.readfp(DefaultSectionHeadOnPropertiesFile(open(properties_filename)))
    try:
        topbraid_url = config.get(DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE, TOPBRAID_URL_PROPERTY_NAME)
        topbraid_username = config.get(DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE, TOPBRAID_USERNAME_PROPERTY_NAME)
        topbraid_password = config.get(DEFAULT_SECTION_HEAD_FOR_PROPERTIES_FILE, TOPBRAID_PASSWORD_PROPERTY_NAME)
    except configparser.NoOptionError as noe:
        sys.stderr.write("ERROR: %s in properties file\n" % (noe))
        sys.exit(2)

    jsession_id = get_logged_in_session_id(topbraid_url, topbraid_username, topbraid_password)
    topbraid_version_results = query_topbraid(TOPBRAID_VERSION_QUERY, topbraid_url, jsession_id)

    curated_uris_to_verbs_to_objects = read_curated_uris(curated_filename)
    topbraid_versions = read_topbraid_versions(topbraid_version_results)

    topbraid_uris_to_prior_code_set = defaultdict(set)
    for version in topbraid_versions:
        sys.stdout.write("Looking at version: %s\n" % (version))
        topbraid_results = query_topbraid(TOPBRAID_DATA_QUERY % (version), topbraid_url, jsession_id)
        topbraid_uris_to_properties_to_values = read_topbraid_uris(topbraid_results)
        if version == TOPBRAID_REFERENCE_VERSION_ID:
            validate_curated_statements(topbraid_uris_to_prior_code_set, topbraid_uris_to_properties_to_values, curated_uris_to_verbs_to_objects)
            break
        accumulate_codes_from_version(topbraid_uris_to_prior_code_set, topbraid_uris_to_properties_to_values)

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
