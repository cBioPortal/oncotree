import argparse
import csv
import os
import requests
import sys

ONCOTREE_WEBSITE_URL = "http://oncotree.mskcc.org/#/home?version="
#ONCOTREE_API_URL_BASE = "http://oncotree.mskcc.org/api/"
ONCOTREE_API_URL_BASE = "http://dashi-dev.cbio.mskcc.org:8080/manda-oncotree/api/"
ONCOTREE_VERSION_ENDPOINT = ONCOTREE_API_URL_BASE + "versions"
ONCOTREE_TUMORTYPES_ENDPOINT = ONCOTREE_API_URL_BASE + "tumorTypes"
VERSION_API_IDENTIFIER_FIELD = "api_identifier"
METADATA_HEADER_PREFIX = "#"

# field names used for navigating tumor types
CHILDREN_CODES_FIELD = "children"
HISTORY_FIELD = "history"
ONCOTREE_CODE_FIELD = "code"
PARENT_CODE_FIELD = "parent"
PRECURSORS_FIELD = "precursors"
REVOCATIONS_FIELD = "revocations"

# logging fields
GLOBAL_LOG_MAP = {}
CLOSEST_COMMON_PARENT_FIELD = "closest_common_parent"
CHOICES_FIELD = "choices"
NEIGHBORS_FIELD = "neighbors"
IS_LOGGED_FLAG = "logged"

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
        new_node[PARENT_CODE_FIELD] = json_oncotree_node[PARENT_CODE_FIELD]
        new_node[PRECURSORS_FIELD] = json_oncotree_node[PRECURSORS_FIELD]
        new_node[REVOCATIONS_FIELD] = json_oncotree_node[REVOCATIONS_FIELD]
        new_node[HISTORY_FIELD] = json_oncotree_node[HISTORY_FIELD]
        new_node[ONCOTREE_CODE_FIELD] = json_oncotree_node[ONCOTREE_CODE_FIELD]
        new_node[CHILDREN_CODES_FIELD] = []
        oncotree_nodes[json_oncotree_node[ONCOTREE_CODE_FIELD]] = new_node
    # second pass, add in children
    for oncotree_node in oncotree_nodes.values():
        try:
            oncotree_nodes[oncotree_node[PARENT_CODE_FIELD]][CHILDREN_CODES_FIELD].append(oncotree_node[ONCOTREE_CODE_FIELD])
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
# Takes a data_clinical_sample.txt file
# Saves header/commented lines as strings {row number: row}
# Additional processing to add ONCOTREE_CODE_OPTIONS column
def load_input_file(input_file):
    header = get_header(input_file)
    headers_processed = False
    input_file_mapped_list = []
    header_and_comment_lines = {}
    header_line_number = 0

    new_oncotree_code_index = header.index("ONCOTREE_CODE") + 1
    header.insert(new_oncotree_code_index, "ONCOTREE_CODE_OPTIONS")
    
    with open(input_file) as data_file:
        for line_number, line in enumerate(data_file):
            if line.startswith(METADATA_HEADER_PREFIX) or len(line.rstrip()) == 0:
                header_and_comment_lines[line_number] = line
                continue
            if not headers_processed:
                headers_processed = True
                header_line_number = line_number
                header_and_comment_lines[line_number] = line
                continue
            data = dict(zip(header, map(str.strip, line.split('\t'))))
            input_file_mapped_list.append(data)
   
    # add new column (ONCOTREE_CODE_OPTIONS)
    for line_number in range(header_line_number):
        header_and_comment_lines[line_number] = add_new_column(header_and_comment_lines[line_number], new_oncotree_code_index, "")
    header_and_comment_lines[header_line_number] = add_new_column(header_and_comment_lines[header_line_number], new_oncotree_code_index, "ONCOTREE_CODE_OPTIONS")
    return input_file_mapped_list, header, header_and_comment_lines

#--------------------------------------------------------------
def add_new_column(row, new_column_index, column_name):
    updated_row = row.split('\t')
    # row is just a sentence (not a tab-delimited string), skip adding
    if len(updated_row) < (new_column_index - 1):
        return row
    column_added_at_end = len(updated_row) == new_column_index
    # column is being added at the end, add a return character and remove from previous last column
    if column_added_at_end:
        updated_row[new_column_index -1] = updated_row[new_column_index - 1].rstrip()
        updated_row.insert(new_column_index, column_name + "\n")
    else:
        updated_row.insert(new_column_index, column_name)
    return '\t'.join(updated_row)

#--------------------------------------------------------------
# Uses a list of dictionaries, each dictionary represents a record/row
# Attempts to translate "ONCOTREE_CODE" value to target version equivalent
# Codes which map successfully (direct mapping w/o ambiguity or possible children) are placed in ONCOTREE_CODE column (ONCOTREE_CODE_OPTIONS empty)
# Codes which map ambiguously (no/possible mappings and/or new children) are placed in ONCOTREE_CODE_OPTIONS (ONCOTREE_CODE empty)
def translate_oncotree_codes(input_file_mapped_list, source_oncotree, target_oncotree, is_backwards_mapping):
    for record in input_file_mapped_list:
        source_oncotree_code = record["ONCOTREE_CODE"]
        # initialize summary log for oncotree code
        if source_oncotree_code not in GLOBAL_LOG_MAP:
            GLOBAL_LOG_MAP[source_oncotree_code] = {
                NEIGHBORS_FIELD : [], 
                CHOICES_FIELD : [], 
                CLOSEST_COMMON_PARENT_FIELD : "",
                IS_LOGGED_FLAG : False
            }
        translated_oncotree_code, is_easily_resolved  = get_oncotree_code_options(source_oncotree_code, source_oncotree, target_oncotree, is_backwards_mapping)
        if is_easily_resolved:
            record["ONCOTREE_CODE"]  = translated_oncotree_code
            record["ONCOTREE_CODE_OPTIONS"] = ""
        else:
            record["ONCOTREE_CODE"] = ""
            record["ONCOTREE_CODE_OPTIONS"] = translated_oncotree_code
    return input_file_mapped_list

#--------------------------------------------------------------
# Given a "source" oncotree code, return a string which can be in the following:
# 1) single code (single mapping, no children), True
# 2) single code but with children (single mapping, new children), False
# 3) multiple directly mapped options (w/ or w/o children), False
# 4) multiple related options (closest parents/children, don't include children), False
def get_oncotree_code_options(source_oncotree_code, source_oncotree, target_oncotree, is_backwards_mapping):
    if source_oncotree_code in ["N/A", "", "NA"]:
        return source_oncotree_code, True
    if source_oncotree_code not in source_oncotree:
        #print >> sys.stderr, "ERROR: Oncotree code (%s) can not be found in source oncotree. Please verify source version." % (source_oncotree_code)
        return source_oncotree_code, True
        #sys.exit(1)
    source_oncotree_node = source_oncotree[source_oncotree_code]
    # get a set of possible codes that source code has been directly mapped to
    possible_target_oncotree_codes = get_possible_target_oncotree_codes(source_oncotree_node, target_oncotree, is_backwards_mapping)
    # resolve set of codes (cannot use possible_target_oncotree_nodes anymore)
    target_oncotree_code, is_easily_resolved = resolve_possible_target_oncotree_codes(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, is_backwards_mapping)
    return target_oncotree_code, is_easily_resolved

#--------------------------------------------------------------
# Given a "source" oncotree code, return a set of directly mappable "target" oncotree codes (though history, precursors, revocations)
# Rules for determining set diff based on mapping direction
def get_possible_target_oncotree_codes(source_oncotree_node, target_oncotree, is_backwards_mapping):
    possible_target_oncotree_codes = set()
    source_oncotree_code = source_oncotree_node[ONCOTREE_CODE_FIELD]
    if is_backwards_mapping:        
        # Backwards mapping
        # codes in history is in the target version (Same URI - different name)
        possible_target_oncotree_codes.update(get_past_oncotree_codes_for_related_codes(source_oncotree_node, target_oncotree, HISTORY_FIELD))
        if not possible_target_oncotree_codes: # history overrides current code when history is present (e.g PTCLNOS)
            if source_oncotree_code in target_oncotree:
                possible_target_oncotree_codes.add(source_oncotree_code)
        # codes in precusors is in the target version
        possible_target_oncotree_codes.update(get_past_oncotree_codes_for_related_codes(source_oncotree_node, target_oncotree, PRECURSORS_FIELD))
        # skip checking codes in revocations - invalid codes which should not be considered 
        return possible_target_oncotree_codes
    else:
        # Forwards mapping
        # codes where source code is in history (this should at most be 1 node - because its the same URI)
        future_codes = get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, HISTORY_FIELD)
        if len(future_codes) > 1:
            print >> sys.stderr, "ERROR: Future oncotree has multiple codes with code %s in history" % (source_oncotree_code)
            sys.exit(1)
        if len(future_codes) == 1:
            possible_target_oncotree_codes.update(future_codes)
            return possible_target_oncotree_codes
        # codes where source code is in precursor (this can be more than 1, but can not intersect with revocations)
        possible_target_oncotree_codes.update(get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, PRECURSORS_FIELD))
        if len(possible_target_oncotree_codes) > 0:
            return possible_target_oncotree_codes
        # codes where source code is in revocations (this can be more than 1) 
        possible_target_oncotree_codes.update(get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, REVOCATIONS_FIELD))
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
# Given a set of oncotree codes, return a formatted string with following info (if available):
# 1a) (== 1 choice) directly mapped target oncotree code
# 1b) (>1 choices) possible choices
# 1c) (0 choices) neighborhood/related codes
# 2) whether or not there are children
#
# * Presence of children not checked for cases with 0 choices
#   since returned "options" are already in the general neighborhood
# * Additionally, if children are available, log the closest common parent for summary report
def resolve_possible_target_oncotree_codes(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, is_backwards_mapping):
    number_of_new_children = 0 
    # skip calculating number of children for case with no direct mappings
    if len(possible_target_oncotree_codes) != 0:
        number_of_new_children = get_number_of_new_children(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree)
    # is easily resolved if only one option with no children
    is_easily_resolved = (len(possible_target_oncotree_codes) == 1 and not number_of_new_children)   
    
    if is_easily_resolved:
        oncotree_code_options = possible_target_oncotree_codes.pop()
        if source_oncotree_code != oncotree_code_options and not GLOBAL_LOG_MAP[source_oncotree_code][IS_LOGGED_FLAG]:
                GLOBAL_LOG_MAP[source_oncotree_code][CHOICES_FIELD].append(oncotree_code_options)
                GLOBAL_LOG_MAP[source_oncotree_code][IS_LOGGED_FLAG] = True
        # log if oncotree code has changed
        return oncotree_code_options, is_easily_resolved

    # case with 0 direct mappings, return neighborhood + log common parent 
    if len(possible_target_oncotree_codes) == 0:
        neighboring_target_oncotree_codes = get_neighboring_target_oncotree_codes([source_oncotree_code], source_oncotree, target_oncotree, True, is_backwards_mapping)
        oncotree_code_options = format_oncotree_code_options(source_oncotree_code, "Neighborhood: " + ','.join(neighboring_target_oncotree_codes), number_of_new_children)
        closest_common_parent = get_closest_common_parent(neighboring_target_oncotree_codes, target_oncotree)
        if not GLOBAL_LOG_MAP[source_oncotree_code][IS_LOGGED_FLAG]:
            GLOBAL_LOG_MAP[source_oncotree_code][NEIGHBORS_FIELD].extend(neighboring_target_oncotree_codes)
            GLOBAL_LOG_MAP[source_oncotree_code][CLOSEST_COMMON_PARENT_FIELD]=closest_common_parent
    else:
        oncotree_code_options = format_oncotree_code_options(source_oncotree_code, "{%s}" % ','.join(possible_target_oncotree_codes), number_of_new_children)
        if not GLOBAL_LOG_MAP[source_oncotree_code][IS_LOGGED_FLAG]:
            GLOBAL_LOG_MAP[source_oncotree_code][CHOICES_FIELD].extend(possible_target_oncotree_codes)
            if number_of_new_children:
                closest_common_parent = get_closest_common_parent(possible_target_oncotree_codes, target_oncotree)
                GLOBAL_LOG_MAP[source_oncotree_code][CLOSEST_COMMON_PARENT_FIELD] = closest_common_parent
    GLOBAL_LOG_MAP[source_oncotree_code][IS_LOGGED_FLAG] = True
    return oncotree_code_options, is_easily_resolved

#--------------------------------------------------------------
def format_oncotree_code_options(source_oncotree_code, oncotree_code_options, number_of_new_children):
    to_return = "%s -> %s" % (source_oncotree_code, oncotree_code_options)
    if number_of_new_children:
        to_return = to_return + ", more granular choices introduced"
    return to_return

#--------------------------------------------------------------
# returns codes (in target version) which succesfully mapped from relatives
def get_neighboring_target_oncotree_codes(source_oncotree_codes, source_oncotree, target_oncotree, include_children, is_backwards_mapping):
    immediate_relatives = set()
    immediate_relatives_in_target_oncotree = set()
    # collect codes which are directly related (parents + children) in first iteration, just parents in second+ iteration
    for source_oncotree_code in source_oncotree_codes:
        if source_oncotree[source_oncotree_code][PARENT_CODE_FIELD]:
           immediate_relatives.add(source_oncotree[source_oncotree_code][PARENT_CODE_FIELD])
        if source_oncotree[source_oncotree_code][CHILDREN_CODES_FIELD] and include_children:
            immediate_relatives.update(source_oncotree[source_oncotree_code][CHILDREN_CODES_FIELD]) 
   
    for source_oncotree_code in source_oncotree_codes: 
        immediate_relatives_in_target_oncotree.update(get_possible_target_oncotree_codes(source_oncotree[source_oncotree_code], target_oncotree, is_backwards_mapping))
    
    # no immediate relative could be mapped backwards - try again with expanded search
    if not immediate_relatives_in_target_oncotree:
       return get_neighboring_target_oncotree_codes(immediate_relatives, source_oncotree, target_oncotree, False, is_backwards_mapping) 
    else: # at least one code was mapped successfully backwards
       return immediate_relatives_in_target_oncotree  

#--------------------------------------------------------------
# Returns number of new children (number of target children codes not in list of source children codes)
def get_number_of_new_children(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree):
    children_in_source = []
    children_in_target = []
    children_in_source = set(get_children([source_oncotree_code], children_in_source, source_oncotree))
    children_in_target = set(get_children(possible_target_oncotree_codes, children_in_target, target_oncotree))
    number_of_new_children = len(children_in_target - children_in_source)
    return number_of_new_children
    
#--------------------------------------------------------------
# Recusively builds of all children codes under a set of given onctoree codes
def get_children(oncotree_codes, all_children_codes, target_oncotree):
    children = []
    for oncotree_code in oncotree_codes:
        children.extend(target_oncotree[oncotree_code][CHILDREN_CODES_FIELD])
    if children:
        all_children_codes.extend(children)
        return get_children(children, all_children_codes, target_oncotree)
    else:
        return all_children_codes

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
    if target_oncotree[oncotree_code][PARENT_CODE_FIELD]:
        parent_oncotree_code = target_oncotree[oncotree_code][PARENT_CODE_FIELD]
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
# sorts logging map based on resolution type
# (e.g show unmappable nodes before ambiguous nodes
def sort_by_resolution_method(oncotree_code, logged_code):
    # no direct mappings first
    if logged_code[NEIGHBORS_FIELD]:
        return "0" + oncotree_code
    # has multiple possible choices and has children
    elif len(logged_code[CHOICES_FIELD]) > 1 and logged_code[CLOSEST_COMMON_PARENT_FIELD]:
        return "1" + oncotree_code
    # has multiple possible choices and has no children
    elif len(logged_code[CHOICES_FIELD]) > 1 and not logged_code[CLOSEST_COMMON_PARENT_FIELD]:
        return "2" + oncotree_code
    # has one choice and has children
    elif len(logged_code[CHOICES_FIELD]) == 1 and logged_code[CLOSEST_COMMON_PARENT_FIELD]:
        return "3" + oncotree_code
    else:
        return "4" + oncotree_code

#--------------------------------------------------------------
def write_summary_file(output_file, source_version, target_version):
    oncotree_url = ONCOTREE_WEBSITE_URL + target_version
    # Break logged nodes into subcategories and sort (alphabetically and resolution type)
    # For each category, codes with more granular codes introduced are shown first    
    unmappable_codes = sorted([unmappable_code for unmappable_code, unmappable_node in GLOBAL_LOG_MAP.items() if unmappable_node[NEIGHBORS_FIELD]])
    ambiguous_codes = sorted([ambiguous_code for ambiguous_code, ambiguous_node in GLOBAL_LOG_MAP.items() if len(ambiguous_node[CHOICES_FIELD]) > 1], key = lambda k: sort_by_resolution_method(k, GLOBAL_LOG_MAP[k]))
    resolved_codes = sorted([resolved_code for resolved_code, resolved_node in GLOBAL_LOG_MAP.items() if len(resolved_node[CHOICES_FIELD]) == 1], key = lambda k: sort_by_resolution_method(k, GLOBAL_LOG_MAP[k]))

    with open(output_file + ".log", "w") as f:
        # General info
        f.write("Mapping Summary\nSource Version: %s\nTarget Version: %s\n" % (source_version, target_version))
        f.write("\n** 'Closest shared parent node' refers to the most granular node available which shares ancestry with a set of nodes.\n")
        f.write("** All resolutions should be made through this version of the oncotree: %s\n" % (oncotree_url))
        f.write("\n===============================================================================================\n")
        
        # Unmappable codes - printed first since they MUST be resolved with manual tree exploration 
        f.write("The following codes could not be mapped to a code in the target version and require additional resolution:\n")
        for oncotree_code in unmappable_codes:
            f.write("\n\tOriginal Code: %s\n" % (oncotree_code))
            f.write("\tClosest Neighbors: %s\n" % ','.join(GLOBAL_LOG_MAP[oncotree_code][NEIGHBORS_FIELD]))
            f.write("\tTo resolve, please refer to closest shared parent node %s and its descendants.\n" % GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD])
        f.write("\n===============================================================================================\n")
        
        # Ambiguous codes - printed second since they MUST be resolved but already provide choices
        f.write("The following codes mapped to multiple codes in the target version. Please select from provided choices:\n")
        for oncotree_code in ambiguous_codes: 
            f.write("\n\tOriginal Code: %s\n" % (oncotree_code))
            f.write("\tChoices: %s\n" % ','.join(GLOBAL_LOG_MAP[oncotree_code][CHOICES_FIELD]))
            if GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]:
                f.write("\t*Warning: Target version has introduced more granular nodes.\n") 
                f.write("\t          You may want to examine the closest shared parent node %s and its descendants.\n" % (GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]))
        f.write("\n===============================================================================================\n")
        
        # Directly mapped codes - no action required, might want to explore more granular choices
        f.write("The following codes mapped to exactly one node.\n")
        for oncotree_code in resolved_codes:
            f.write("\n\tOriginal Code: %s\n" % (oncotree_code))
            f.write("\tNew Code: %s\n" % ','.join(GLOBAL_LOG_MAP[oncotree_code][CHOICES_FIELD]))
            if GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]:
                f.write("\t*Warning: Target version has introduced more granular nodes.\n") 
                f.write("\t          You may want to examine the closest shared parent node %s and its descendants.\n" % (GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]))
        
#--------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--auto-mapping-enabled", help = "enable automatic resolution of ambiguous mappings", action = "store_true")
    parser.add_argument("-i", "--input-file", help = "source file provided by user", required = True)
    parser.add_argument("-o", "--output-file", help = "destination file to write out new file contents", required = True)
    parser.add_argument("-s", "--source-version", help = "current oncotree version used in the source file", required = True)
    parser.add_argument("-t", "--target-version", help = "oncotree version to be mapped to in the destination file", required = True)
    args = parser.parse_args()

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
    translated_input_file_mapped_list = translate_oncotree_codes(input_file_mapped_list, source_oncotree, target_oncotree, is_backwards_mapping)
    write_to_output_file(translated_input_file_mapped_list, output_file, header, header_and_comment_lines)
    write_summary_file(output_file, source_version, target_version)

if __name__ == '__main__':
   main()
