import argparse
import csv
import os
import requests
import sys

#ONCOTREE_API_URL_BASE = "http://oncotree.mskcc.org/api/"
ONCOTREE_API_URL_BASE = "http://dashi-dev.cbio.mskcc.org:8080/manda-oncotree/api/"
ONCOTREE_VERSION_ENDPOINT = ONCOTREE_API_URL_BASE + "versions"
ONCOTREE_TUMORTYPES_ENDPOINT = ONCOTREE_API_URL_BASE + "tumorTypes"
VERSION_API_IDENTIFIER_FIELD = "api_identifier"
METADATA_HEADER_PREFIX = "#"

GLOBAL_LOG_MAP = {}
#--------------------------------------------------------------
def get_oncotree_version_indexes(source_oncotree_version_name, target_oncotree_version_name):
    response = requests.get(ONCOTREE_VERSION_ENDPOINT)
    if response.status_code != 200:
        print >> sys.stderr, "ERROR (HttpStatusCode %d): Unable to retrieve oncotree versions." % (response.status_code)
        sys.exit(1)
    source_oncotree_version_index, target_oncotree_version_index = -1, -1
    for index, version in enumerate(response.json()):
        if version[VERSION_API_IDENTIFIER_FIELD] == source_oncotree_version_name:
            source_oncotree_version_index = index
        if version[VERSION_API_IDENTIFIER_FIELD] == target_oncotree_version_name:
            target_oncotree_version_index = index

    return source_oncotree_version_index, target_oncotree_version_index

#--------------------------------------------------------------
def load_oncotree_version(oncotree_version_name):
    oncotree_nodes = {}
    response = requests.get(ONCOTREE_TUMORTYPES_ENDPOINT + "?version=" + oncotree_version_name)
    if response.status_code != 200:
        print >> sys.stderr, "ERROR (HttpStatusCode %d): Unable to retrieve oncotree version %s." % (response.status_code, oncotree_version_name)
        sys.exit(1)
    for json_oncotree_node in response.json():
        new_node = {}
        new_node["parent"] = json_oncotree_node["parent"]
        new_node["precursors"] = json_oncotree_node["precursors"]
        new_node["revocations"] = json_oncotree_node["revocations"]
        new_node["history"] = json_oncotree_node["history"]
        new_node["code"] = json_oncotree_node["code"]
        new_node["children"] = []
        oncotree_nodes[json_oncotree_node["code"]] = new_node
    # second pass, add in children
    for oncotree_node in oncotree_nodes.values():
        try:
            oncotree_nodes[oncotree_node["parent"]]["children"].append(oncotree_node["code"])
        except:
            continue
    return oncotree_nodes

#--------------------------------------------------------------
def get_header(file):
    header = []
    with open(file, "r") as header_source:
        for line in header_source:
            if not line.startswith("#"):
                header = line.rstrip().split('\t')
                break
    return header
#--------------------------------------------------------------
def load_input_file(input_file):
    header = get_header(input_file)
    headers_processed = False
    input_file_mapped_list = []
    header_and_comment_lines = {}

    with open(input_file) as data_file:
        for line_number, line in enumerate(data_file):
            if line.startswith(METADATA_HEADER_PREFIX) or len(line.rstrip()) == 0:
                header_and_comment_lines[line_number] = line
                continue
            if not headers_processed:
                headers_processed = True
                header_and_comment_lines[line_number] = line
                continue
            data = dict(zip(header, map(str.strip, line.split('\t'))))
            input_file_mapped_list.append(data)
    return input_file_mapped_list, header, header_and_comment_lines
#--------------------------------------------------------------
# given list of dictionaries (each index being a record) - replace the ONCOTREE_CODE value with translated code
def translate_oncotree_codes(input_file_mapped_list, source_oncotree, target_oncotree, auto_mapping_enabled, is_backwards_mapping):
    for record in input_file_mapped_list:
        source_oncotree_code = record["ONCOTREE_CODE"]
        record["ONCOTREE_CODE"] = convert_to_target_oncotree_code(source_oncotree_code, source_oncotree, target_oncotree, auto_mapping_enabled, is_backwards_mapping)
    return input_file_mapped_list

#--------------------------------------------------------------
# convert a signle oncotree code to its "target" version equivalent
def convert_to_target_oncotree_code(source_oncotree_code, source_oncotree, target_oncotree, auto_mapping_enabled, is_backwards_mapping):
    if source_oncotree_code in ["N/A", "", "NA"]:
        return source_oncotree_code
    if source_oncotree_code not in source_oncotree:
        #print >> sys.stderr, "ERROR: Oncotree code (%s) can not be found in source oncotree. Please verify source version." % (source_oncotree_code)
        return source_oncotree_code
        #sys.exit(1)
    source_oncotree_node = source_oncotree[source_oncotree_code]
    # get a set of possible codes that source code has been mapped to
    possible_target_oncotree_codes = get_possible_target_oncotree_codes(source_oncotree_node, target_oncotree, is_backwards_mapping)
    # resolve set of codes (cannot use possible_target_oncotree_nodes anymore)
    target_oncotree_code = resolve_possible_target_oncotree_codes(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, auto_mapping_enabled, is_backwards_mapping)
    return target_oncotree_code

#--------------------------------------------------------------
# given a single oncotree node and mapping direction - return a set with oncotree codes which mapped succesfully (from target version)
def get_possible_target_oncotree_codes(source_oncotree_node, target_oncotree, is_backwards_mapping):
    possible_target_oncotree_codes = set()
    source_oncotree_code = source_oncotree_node["code"]
    if is_backwards_mapping:        
        # Backwards mapping
        # codes in history is in the target version (Same URI - different name)
        possible_target_oncotree_codes.update(get_past_oncotree_codes_for_related_codes(source_oncotree_node, target_oncotree, "history"))
        if not possible_target_oncotree_codes: # history overrides current code when history is present (e.g PTCLNOS)
            if source_oncotree_code in target_oncotree:
                possible_target_oncotree_codes.add(source_oncotree_code)
        # codes in precusors is in the target version
        possible_target_oncotree_codes.update(get_past_oncotree_codes_for_related_codes(source_oncotree_node, target_oncotree, "precursors"))
        # skip checking codes in revocations - invalid codes which should not be considered 
        return possible_target_oncotree_codes
    else:
        # Forwards mapping
        # codes where source code is in history (this should at most be 1 node - because its the same URI)
        future_codes = get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, "history")
        if len(future_codes) > 1:
            print >> sys.stderr, "ERROR: Future oncotree has multiple codes with code %s in history" % (source_oncotree_code)
            sys.exit(1)
        if len(future_codes) == 1:
            possible_target_oncotree_codes.update(future_codes)
            return possible_target_oncotree_codes
        # codes where source code is in precursor (this can be more than 1, but can not intersect with revocations)
        possible_target_oncotree_codes.update(get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, "precursor"))
        if len(possible_target_oncotree_codes) > 0:
            return possible_target_oncotree_codes
        # codes where source code is in revocations (this can be more than 1) 
        possible_target_oncotree_codes.update(get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, "revocations"))
        if len(possible_target_oncotree_codes) > 0:
            return possible_target_oncotree_codes
        # at this point, no matches - check if source code exists in future oncotree
        if source_oncotree_code in target_oncotree:
            possible_target_oncotree_codes.add(source_oncotree_code)
        return possible_target_oncotree_codes

#--------------------------------------------------------------
# exclusively for mapping in backward direction
# looking through 'related nodes (history, precursors)', return if found in target (past) oncotree
# i.e SLLCLL (SLL precusor, CLL precursor) -> SLL, CLL
def get_past_oncotree_codes_for_related_codes(source_oncotree_node, target_oncotree, field):
    return [past_oncotree_code for past_oncotree_code in source_oncotree_node[field] if past_oncotree_code in target_oncotree]

#--------------------------------------------------------------
# exclusively for mapping in forward direction
# returns codes where source code is related in target (future) oncotree
# i.e ALL -> BLL (ALL revocation), TLL (ALL revocation)
def get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, field):
    return [future_oncotree_code for future_oncotree_code, future_oncotree_node in target_oncotree.items() if source_oncotree_code in future_oncotree_node[field]]

#--------------------------------------------------------------
# given a set of oncotree code(s), return a string which formatting/contents are determined by whether auto mapping is enabled
# auto mapping means set will resolve to a single oncotree code
# no auto mapping means users will be presented with list of options to resolve
def resolve_possible_target_oncotree_codes(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, auto_mapping_enabled, is_backwards_mapping):
    if source_oncotree_code not in GLOBAL_LOG_MAP:
        GLOBAL_LOG_MAP[source_oncotree_code] = {
            "target_code": "", 
            "valid_related_oncotree_codes" : [], 
            "mappable_target_oncotree_codes" : [], 
            "closest_common_parent" : ""
        }
    source_oncotree_code_log = GLOBAL_LOG_MAP[source_oncotree_code]

    if len(possible_target_oncotree_codes) == 1:
        target_code = possible_target_oncotree_codes.pop()
        if source_oncotree_code != target_code:
            if not source_oncotree_code_log["target_code"]:
                source_oncotree_code_log["target_code"] = target_code
        return target_code
    if not auto_mapping_enabled:
        # 0 possible target codes
        if len(possible_target_oncotree_codes) == 0:
            target_code = find_closest_common_parent_code(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, is_backwards_mapping)
            return "Closest parent: %s" % target_code
        # more than once possible target code
        return "Candidates: " + ", ".join(possible_target_oncotree_codes)
    else:
        # no distinct choice, have to find closest common parent
        target_code = find_closest_common_parent_code(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, is_backwards_mapping)
        return target_code

#--------------------------------------------------------------
# decides between cases where there are multiple possible mappings or no possible mappings
def find_closest_common_parent_code(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, is_backwards_mapping):
    # case where there are 0 possible target oncotree nodes
    source_oncotree_code_log = GLOBAL_LOG_MAP[source_oncotree_code]
    if not possible_target_oncotree_codes:
        # find valid related oncotree nodes (i.e parents/children which map successfully to something in the target version)
        possible_related_target_oncotree_codes = get_valid_related_oncotree_codes([source_oncotree_code], source_oncotree, target_oncotree, True, is_backwards_mapping)
        if not source_oncotree_code_log["valid_related_oncotree_codes"]:
            source_oncotree_code_log["valid_related_oncotree_codes"] = possible_related_target_oncotree_codes
        # log: source_oncotree_code had no direct mapping (relatives used to find parent: possible_related_target_oncotree_codes -> get_closest_common_parent(....)
        closest_common_parent = get_closest_common_parent(possible_related_target_oncotree_codes, target_oncotree)
    else: # case where there are already a defined set of possible target oncotree nodes but we want to return one - find closest common parent of provided set
        if not source_oncotree_code_log["mappable_target_oncotree_codes"]:
            source_oncotree_code_log["mappable_target_oncotree_codes"] = possible_target_oncotree_codes
        closest_common_parent = get_closest_common_parent(possible_target_oncotree_codes, target_oncotree)
        # log: source_oncotree_code had multiple possible mappings (possible_target_oncotree_codes - resolved to one parent - get_closest-common_parent)
    if not source_oncotree_code_log["closest_common_parent"]:
        source_oncotree_code_log["closest_common_parent"] = closest_common_parent
    return closest_common_parent

#--------------------------------------------------------------
# returns codes (in target version) which succesfully mapped from relatives
def get_valid_related_oncotree_codes(source_oncotree_codes, source_oncotree, target_oncotree, include_children, is_backwards_mapping):
    immediate_relatives = set()
    immediate_relatives_in_target_oncotree = set()
    # collect codes which are directly related (parents + children) in first iteration, just parents in second+ iteration
    for source_oncotree_code in source_oncotree_codes:
        if source_oncotree[source_oncotree_code]["parent"]:
           immediate_relatives.add(source_oncotree[source_oncotree_code]["parent"])
        if source_oncotree[source_oncotree_code]["children"] and include_children:
            immediate_relatives.update(source_oncotree[source_oncotree_code]["children"]) 
   
    for source_oncotree_code in source_oncotree_codes: 
        immediate_relatives_in_target_oncotree.update(get_possible_target_oncotree_codes(source_oncotree[source_oncotree_code], target_oncotree, is_backwards_mapping))
    
    # no immediate relative could be mapped backwards - try again with expanded search
    if not immediate_relatives_in_target_oncotree:
       return get_valid_related_oncotree_codes(immediate_relatives, source_oncotree, target_oncotree, False, is_backwards_mapping) 
    else: # at least one code was mapped successfully backwards
       return immediate_relatives_in_target_oncotree  

#--------------------------------------------------------------
# get the common parent (furthest down the tree) for a set of oncotree_codes
def get_closest_common_parent(possible_target_oncotree_codes, target_oncotree):
    oncotree_code_to_ancestors_mapping = {}
    # for every possible target oncotree code - construct and ordered list of ancestors
    for oncotree_code in possible_target_oncotree_codes:
        oncotree_code_ancestors = [oncotree_code] 
        oncotree_code_to_ancestors_mapping[oncotree_code] = get_ancestors(oncotree_code, oncotree_code_ancestors, target_oncotree)
    min_length = min([len(ancestor_list) for ancestor_list in oncotree_code_to_ancestors_mapping.values()])
    # look across lists to find the earliest point where codes differ
    closest_common_parent = get_earliest_common_parent(min_length, oncotree_code_to_ancestors_mapping.values())
    return closest_common_parent

#--------------------------------------------------------------
# used to construct list of ancestors - if parent exists, insert at beginning, and call again at a higher level
def get_ancestors(oncotree_code, oncotree_code_ancestors, target_oncotree):
    if target_oncotree[oncotree_code]["parent"]:
        parent_oncotree_code = target_oncotree[oncotree_code]["parent"]
        oncotree_code_ancestors.insert(0, parent_oncotree_code)
        return get_ancestors(parent_oncotree_code, oncotree_code_ancestors, target_oncotree)
    else:
        return oncotree_code_ancestors

#--------------------------------------------------------------
def get_earliest_common_parent(min_length, lists_of_all_ancestors):
    # set reference code as the code at index (min_length - 1) for first list in lists_of_all_ancestors
    oncotree_code = lists_of_all_ancestors[0][min_length - 1]
    # skip first list in lists_of_all_ancestors
    for l in lists_of_all_ancestors[1:]:
        if min_length > 0:
            if l[min_length - 1] != oncotree_code:
                # at first mismatch, decrement min_length to move earlier in the list
                min_length -= 1
                return get_earliest_common_parent(min_length, lists_of_all_ancestors)
    return oncotree_code 

#--------------------------------------------------------------
def write_to_output_file(translated_input_file_mapped_list, output_file, header, header_and_comment_lines):
    line_num = 0
    with open(output_file, "w") as f:
        for record in translated_input_file_mapped_list:
            while line_num in header_and_comment_lines:
                f.write(header_and_comment_lines[line_num])
                line_num += 1
            formatted_data = map(lambda x: record.get(x,''), header)
            f.write('\t'.join(formatted_data) + '\n')
            line_num += 1

#--------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--auto-mapping-enabled", help = "enable automatic resolution of ambiguous mappings", action = "store_true")
    parser.add_argument("-i", "--input-file", help = "source file provided by user", required = True)
    parser.add_argument("-o", "--output-file", help = "destination file to write out new file contents", required = True)
    parser.add_argument("-s", "--source-version", help = "current oncotree version used in the source file", required = True)
    parser.add_argument("-t", "--target-version", help = "oncotree version to be mapped to in the destination file", required = True)
    args = parser.parse_args()

    auto_mapping_enabled = args.auto_mapping_enabled
    input_file = args.input_file
    output_file = args.output_file
    source_version = args.source_version
    target_version = args.target_version

    if not os.path.isfile(input_file):
        print >> sys.stderr, "Error: Input file (%s) can not be found" % (input_file)
        sys.exit(1)

    if source_version == target_version:
        print >> sys.stderr, "Error: Source oncotree version (%s) and target oncotree version (%s) are the same.  There is no need to convert this file." % (source_version, target_version)
        sys.exit(1)

    source_index, target_index = get_oncotree_version_indexes(source_version, target_version)

    if source_index == -1:
        print >> sys.stderr, "ERROR: Source version (%s) is not a valid oncotree version" % (source_version)
        sys.exit(1)
    if target_index == -1:
        print >> sys.stderr, "ERROR: Target version (%s) is not a valid oncotree version" % (target_version)
        sys.exit(1)

    is_backwards_mapping = target_index < source_index

    input_file_mapped_list, header, header_and_comment_lines = load_input_file(input_file)
    source_oncotree = load_oncotree_version(source_version)
    target_oncotree = load_oncotree_version(target_version)
    translated_input_file_mapped_list = translate_oncotree_codes(input_file_mapped_list, source_oncotree, target_oncotree, auto_mapping_enabled, is_backwards_mapping)
    write_to_output_file(translated_input_file_mapped_list, output_file, header, header_and_comment_lines)

    for logged_code in GLOBAL_LOG_MAP:
        test = GLOBAL_LOG_MAP[logged_code]
        if test["target_code"]:
            print "\nONCOTREE_CODE (%s)\n===================" % logged_code
            print "Translated to target oncotree code: %s" % test["target_code"]
        if test["closest_common_parent"]:
            print "\nONCOTREE_CODE (%s)\n===================" % logged_code
            if test["valid_related_oncotree_codes"]:
                print "Unable to directly map to the target oncotree version, find closest parents based on: %s" % ', '.join(test["valid_related_oncotree_codes"]) 
            if test["mappable_target_oncotree_codes"]:
                print "Mapped to multiple codes in target oncotree version, find closest parents based on: %s" % ', '.join(test["mappable_target_oncotree_codes"])
            print "Resolved to closest shared parent: %s" % test["closest_common_parent"]

if __name__ == '__main__':
   main()
