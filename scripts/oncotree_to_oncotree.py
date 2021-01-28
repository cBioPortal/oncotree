#!/usr/bin/env python3

# Copyright (c) 2019 Memorial Sloan-Kettering Cancer Center.
#
# This library is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
# documentation provided hereunder is on an "as is" basis, and
# Memorial Sloan-Kettering Cancer Center
# has no obligations to provide maintenance, support,
# updates, enhancements or modifications.  In no event shall
# Memorial Sloan-Kettering Cancer Center
# be liable to any party for direct, indirect, special,
# incidental or consequential damages, including lost profits, arising
# out of the use of this software and its documentation, even if
# Memorial Sloan-Kettering Cancer Center
# has been advised of the possibility of such damage.

import argparse
import os
import sys
import urllib.request
import json

ONCOTREE_WEBSITE_URL = "http://oncotree.mskcc.org/#/home?version="
ONCOTREE_API_URL_BASE_DEFAULT = "http://oncotree.mskcc.org/api/"
ONCOTREE_VERSION_ENDPOINT = "versions"
ONCOTREE_TUMORTYPES_ENDPOINT = "tumorTypes"
VERSION_API_IDENTIFIER_FIELD = "api_identifier"
VERSION_RELEASE_DATE_FIELD = "release_date"
METADATA_HEADER_PREFIX = "#"
PASSTHROUGH_ONCOTREE_CODE_LIST = ["NA"] # These codes will be passed through the converter without examination or alteration
TOOL_VERSION_NUMBER = "1.2"

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
def fetch_oncotree_versions(oncotree_api_url_base):
    # fetch available onctree versions from api
    oncotree_version_endpoint_url = oncotree_api_url_base + ONCOTREE_VERSION_ENDPOINT
    response = urllib.request.urlopen(oncotree_version_endpoint_url)
    if response.getcode() != 200:
        print("ERROR (HttpStatusCode %d): Unable to retrieve OncoTree versions." % (response.getcode()), file=sys.stderr)
        sys.exit(1)
    return json.loads(response.read())

#--------------------------------------------------------------
def validate_input_oncotree_versions(oncotree_versions_list, source_version, target_version):
    valid_version_identifiers = [version[VERSION_API_IDENTIFIER_FIELD] for version in oncotree_versions_list]
    if not source_version in valid_version_identifiers:
        print("ERROR: Source version (%s) is not a valid OncoTree version" % (source_version), file=sys.stderr)
        sys.exit(1)
    if not target_version in valid_version_identifiers:
        print("ERROR: Source version (%s) is not a valid OncoTree version" % (target_version), file=sys.stderr)
        sys.exit(1)

#--------------------------------------------------------------
def validate_and_fetch_oncotree_version_release_dates(source_version, target_version, oncotree_api_url_base):
    if source_version == target_version:
        print("Error: Source OncoTree version (%s) and target OncoTree version (%s) are the same.  There is no need to convert this file." % (source_version, target_version), file=sys.stderr)
    oncotree_versions_list = fetch_oncotree_versions(oncotree_api_url_base)

    # validate source and target versions
    validate_input_oncotree_versions(oncotree_versions_list, source_version, target_version)

    # return release dates of source and target versions from available OncoTree versions
    source_oncotree_version_release_date = -1
    target_oncotree_version_release_date = -1
    for version in oncotree_versions_list:
        if version[VERSION_API_IDENTIFIER_FIELD] == source_version:
            source_oncotree_version_release_date = version[VERSION_RELEASE_DATE_FIELD]
        if version[VERSION_API_IDENTIFIER_FIELD] == target_version:
            target_oncotree_version_release_date = version[VERSION_RELEASE_DATE_FIELD]
    return source_oncotree_version_release_date, target_oncotree_version_release_date

#--------------------------------------------------------------
def load_oncotree_version(oncotree_version_name, oncotree_api_url_base):
    oncotree_nodes = {}
    oncotree_tumortypes_endpoint = oncotree_api_url_base + ONCOTREE_TUMORTYPES_ENDPOINT + "?version=" + oncotree_version_name
    response = urllib.request.urlopen(oncotree_tumortypes_endpoint)
    if response.getcode() != 200:
        print("ERROR (HttpStatusCode %d): Unable to retrieve OncoTree version %s." % (response.getcode(), oncotree_version_name), file=sys.stderr)
        sys.exit(1)
    for json_oncotree_node in json.loads(response.read()):
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
def load_source_file(source_file):
    header = get_header(source_file)
    header_length = len(header)
    headers_processed = False
    source_file_mapped_list = []
    header_and_comment_lines = {}
    header_line_number = 0

    if "ONCOTREE_CODE" not in header:
        print("ERROR: Input file is missing column 'ONCOTREE_CODE'.", file=sys.stderr)
        sys.exit(1)

    with open(source_file, "r") as data_file:
        for line_number, line in enumerate(data_file):
            if '\r' in line:
                print("ERROR: source file (%s) is not in the required format (tab delimited, newline line breaks). carriage return characters encountered." % (source_file), file=sys.stderr)
                sys.exit(1)
            if line.startswith(METADATA_HEADER_PREFIX) or len(line.rstrip()) == 0:
                header_and_comment_lines[line_number] = line
                continue
            if not headers_processed:
                headers_processed = True
                header_line_number = line_number
                header_and_comment_lines[line_number] = line
                continue
            if len(line.split('\t')) != header_length:
                print("ERROR: Current row has a different number of columns than header row: %s" % line, file=sys.stderr)
                sys.exit(1)
            data = dict(zip(header, map(str.strip, line.split('\t'))))
            source_file_mapped_list.append(data)

    # This column has to be added after since zip was functioning on index
    new_oncotree_code_index = header.index("ONCOTREE_CODE") + 1
    header.insert(new_oncotree_code_index, "ONCOTREE_CODE_OPTIONS")

    # add new column (ONCOTREE_CODE_OPTIONS)
    for line_number in range(header_line_number):
        header_and_comment_lines[line_number] = add_new_column(header_and_comment_lines[line_number], new_oncotree_code_index, "")
    header_and_comment_lines[header_line_number] = add_new_column(header_and_comment_lines[header_line_number], new_oncotree_code_index, "ONCOTREE_CODE_OPTIONS")
    # TODO: do the same thing for commented out lines inserted in random points throughout file (will have to make same changes for writing out records)
    return source_file_mapped_list, header, header_and_comment_lines

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
def remove_new_column(row, new_column_index):
    updated_row = row.split('\t')
    if len(updated_row) < new_column_index:
        return row
    column_removed_at_end = (len(updated_row) - 1) == new_column_index
    # column was added at the end, remove new column and add return character back
    if column_removed_at_end:
        updated_row[new_column_index - 1] = updated_row[new_column_index - 1] + "\n"
        del updated_row[new_column_index]
    else:
        del updated_row[new_column_index]
    return '\t'.join(updated_row)

#--------------------------------------------------------------
# Uses a list of dictionaries, each dictionary represents a record/row
# Attempts to translate "ONCOTREE_CODE" value to target version equivalent
# Codes which map successfully (direct mapping w/o ambiguity or possible children) are placed in ONCOTREE_CODE column (ONCOTREE_CODE_OPTIONS empty)
# Codes which map ambiguously (no/possible mappings and/or new children) are placed in ONCOTREE_CODE_OPTIONS (ONCOTREE_CODE empty)
def translate_oncotree_codes(source_file_mapped_list, source_oncotree, target_oncotree, is_backwards_mapping):
    for record in source_file_mapped_list:
        source_oncotree_code = record["ONCOTREE_CODE"]
        # initialize summary log for OncoTree code
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
    return source_file_mapped_list

#--------------------------------------------------------------
# Given a "source" OncoTree code, return a string which can be in the following:
# 1) single code (single mapping, no children), True
# 2) single code but with children (single mapping, new children), False
# 3) multiple directly mapped options (w/ or w/o children), False
# 4) multiple related options (closest parents/children, don't include children), False
def get_oncotree_code_options(source_oncotree_code, source_oncotree, target_oncotree, is_backwards_mapping):
    if source_oncotree_code in PASSTHROUGH_ONCOTREE_CODE_LIST:
        return source_oncotree_code, True
    if source_oncotree_code not in source_oncotree:
        GLOBAL_LOG_MAP[source_oncotree_code][CHOICES_FIELD] = ["???"]
        GLOBAL_LOG_MAP[source_oncotree_code][IS_LOGGED_FLAG] = True
        if len(source_oncotree_code) == 0:
            return "ONCOTREE_CODE column blank : use a valid OncoTree code or \"NA\"", False
        else:
            return ("%s -> ???, OncoTree code not in source OncoTree version" % (source_oncotree_code)), False
    source_oncotree_node = source_oncotree[source_oncotree_code]
    # get a set of possible codes that source code has been directly mapped to
    possible_target_oncotree_codes = get_possible_target_oncotree_codes(source_oncotree_node, target_oncotree, is_backwards_mapping)
    # resolve set of codes (cannot use possible_target_oncotree_nodes anymore)
    target_oncotree_code, is_easily_resolved = resolve_possible_target_oncotree_codes(source_oncotree_code, possible_target_oncotree_codes, source_oncotree, target_oncotree, is_backwards_mapping)
    return target_oncotree_code, is_easily_resolved

#--------------------------------------------------------------
# Given a "source" OncoTree code, return a set of directly mappable "target" OncoTree codes (though history, precursors, revocations)
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
            print("ERROR: Future OncoTree has multiple codes with code %s in history" % (source_oncotree_code), file=sys.stderr)
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
        # at this point, no matches - check if source code exists in future OncoTree
        if source_oncotree_code in target_oncotree:
            possible_target_oncotree_codes.add(source_oncotree_code)
        return possible_target_oncotree_codes

#--------------------------------------------------------------
# exclusively for mapping in backward direction
# looking through 'related nodes (history, precursors)', return if found in target (past) OncoTree
# i.e SLLCLL (SLL precusor, CLL precursor) -> SLL, CLL
def get_past_oncotree_codes_for_related_codes(source_oncotree_node, target_oncotree, field):
    return [past_oncotree_code for past_oncotree_code in source_oncotree_node[field] if past_oncotree_code in target_oncotree]

#--------------------------------------------------------------
# exclusively for mapping in forward direction
# returns codes where source code is related in target (future) OncoTree
# i.e ALL -> BLL (ALL revocation), TLL (ALL revocation)
def get_future_related_oncotree_codes_for_source_code(source_oncotree_code, target_oncotree, field):
    return [future_oncotree_code for future_oncotree_code, future_oncotree_node in target_oncotree.items() if source_oncotree_code in future_oncotree_node[field]]

#--------------------------------------------------------------
# Given a set of OncoTree codes, return a formatted string with following info (if available):
# 1a) (== 1 choice) directly mapped target OncoTree code
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
        # log if OncoTree code has changed
        return oncotree_code_options, is_easily_resolved

    # case with 0 direct mappings, return neighborhood + log common parent
    if len(possible_target_oncotree_codes) == 0:
        neighboring_target_oncotree_codes = get_neighboring_target_oncotree_codes([source_oncotree_code], source_oncotree, target_oncotree, True, is_backwards_mapping)
        oncotree_code_options = format_oncotree_code_options(source_oncotree_code, "Neighborhood: " + ','.join(neighboring_target_oncotree_codes), number_of_new_children)
        closest_common_parent = get_closest_common_parent(neighboring_target_oncotree_codes, target_oncotree)
        if not GLOBAL_LOG_MAP[source_oncotree_code][IS_LOGGED_FLAG]:
            GLOBAL_LOG_MAP[source_oncotree_code][NEIGHBORS_FIELD].extend(neighboring_target_oncotree_codes)
            GLOBAL_LOG_MAP[source_oncotree_code][CLOSEST_COMMON_PARENT_FIELD] = closest_common_parent
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
# will return self is source code is in target version
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
    # for every possible target OncoTree code - construct an ordered list of ancestors
    for oncotree_code in possible_target_oncotree_codes:
        oncotree_code_ancestors = [oncotree_code]
        oncotree_code_to_ancestors_mapping[oncotree_code] = get_ancestors(oncotree_code, oncotree_code_ancestors, target_oncotree)
    min_length = min([len(ancestor_list) for ancestor_list in oncotree_code_to_ancestors_mapping.values()])
    # look across lists to find the earliest point where codes differ
    closest_common_parent = get_earliest_common_parent(min_length, list(oncotree_code_to_ancestors_mapping.values()))
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
    for list_of_ancestors in lists_of_all_ancestors[1:]:
        if min_length > 0:
            if list_of_ancestors[min_length - 1] != oncotree_code:
                # at first mismatch, decrement min_length to move earlier in the list
                min_length -= 1
                return get_earliest_common_parent(min_length, lists_of_all_ancestors)
    return oncotree_code

#--------------------------------------------------------------
def write_to_target_file(translated_source_file_mapped_list, target_file, header, header_and_comment_lines):
    all_easily_resolved = True
    oncotree_code_options_index = header.index("ONCOTREE_CODE_OPTIONS")
    for record in translated_source_file_mapped_list:
        if record["ONCOTREE_CODE_OPTIONS"]:
            all_easily_resolved = False
            break
    if all_easily_resolved:
        header.remove("ONCOTREE_CODE_OPTIONS")
        for line_number in range(len(header_and_comment_lines)):
            header_and_comment_lines[line_number] = remove_new_column(header_and_comment_lines[line_number], oncotree_code_options_index)

    line_num = 0
    with open(target_file, "w") as f:
        for record in translated_source_file_mapped_list:
            while line_num in header_and_comment_lines:
                f.write(header_and_comment_lines[line_num])
                line_num += 1
            formatted_data = map(lambda x: record.get(x,''), header)
            f.write('\t'.join(formatted_data) + '\n')
            line_num += 1
    print("Primary target file written to %s" % (target_file), file=sys.stdout)

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
def write_summary_file(target_file, source_version, target_version):
    oncotree_url = ONCOTREE_WEBSITE_URL + target_version
    # Break logged nodes into subcategories and sort (alphabetically and resolution type)
    # For each category, codes with more granular codes introduced are shown first
    unmappable_codes = sorted([unmappable_code for unmappable_code, unmappable_node in GLOBAL_LOG_MAP.items() if unmappable_node[NEIGHBORS_FIELD]])
    ambiguous_codes = sorted([ambiguous_code for ambiguous_code, ambiguous_node in GLOBAL_LOG_MAP.items() if len(ambiguous_node[CHOICES_FIELD]) > 1], key = lambda k: sort_by_resolution_method(k, GLOBAL_LOG_MAP[k]))
    partially_resolved_codes = sorted([resolved_code for resolved_code, resolved_node in GLOBAL_LOG_MAP.items() if len(resolved_node[CHOICES_FIELD]) == 1 and resolved_node[CLOSEST_COMMON_PARENT_FIELD] and ("???" not in resolved_node[CHOICES_FIELD])], key = lambda k: sort_by_resolution_method(k, GLOBAL_LOG_MAP[k]))
    completely_resolved_codes = sorted([resolved_code for resolved_code, resolved_node in GLOBAL_LOG_MAP.items() if len(resolved_node[CHOICES_FIELD]) == 1 and not resolved_node[CLOSEST_COMMON_PARENT_FIELD] and ("???" not in resolved_node[CHOICES_FIELD])])
    unrecognized_codes = sorted([unrecognized_code for unrecognized_code, unrecognized_node in GLOBAL_LOG_MAP.items() if ("???" in unrecognized_node[CHOICES_FIELD]) ], key = lambda k: sort_by_resolution_method(k, GLOBAL_LOG_MAP[k]))

    html_summary_file = os.path.splitext(target_file)[0] + "_summary.html"
    with open(html_summary_file, "w") as f:
        # General info
        f.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<title>Mapping Summary</title>\n<meta charset=\"UTF-8\">\n<style>\nbody {font-family:Arial; line-height:1.4}\n\n</style>\n</head><body>\n")
        f.write("<h1>Mapping Summary</h1>\n")
        f.write("<p>Tool version: <b>v.%s</b><br>" % (TOOL_VERSION_NUMBER))
        f.write("Mapped <b>%s</b> to <b>%s</b><br>" % (source_version, target_version))
        f.write("All resolutions should be made with version: <b><a href=\"%s\">%s</a></b>\n" % (oncotree_url, target_version))
        f.write("<h3>Contents</h3><ul>\n");
        if unrecognized_codes:
            f.write("<li><a href=\"#unrecognized_header\">Codes which were not present in source OncoTree version</a> (action required)</li>\n")
        if unmappable_codes:
            f.write("<li><a href=\"#not_mapped_header\">Codes that could not be mapped to a code</a> (action required)</li>\n")
        if ambiguous_codes:
            f.write("<li><a href=\"#mapped_to_multiple_header\">Codes mapped to multiple codes</a> (action required)</li>\n")
        if partially_resolved_codes:
            f.write("<li><a href=\"#partially_mapped_header\">Codes mapped to exactly one code, more granular codes introduced</a> (please review)</li>\n")
        if completely_resolved_codes:
            f.write("<li><a href=\"#completely_mapped_header\">Codes mapped to exactly one code</a> (no action necessary)</li>\n")
        f.write("</ul>&nbsp;\n&nbsp;\n")
        # Unrecognized codes - action required, but not guidance. Just list them
        if unrecognized_codes:
            f.write("<hr><h2 id=\"unrecognized_header\">The following codes were not present in the source OncoTree version:</h2>\n")
        for oncotree_code in unrecognized_codes:
            f.write("<p><b>Original Code</b>: %s<br>\n" % ("&lt;blank&gt;" if len(oncotree_code) == 0 else oncotree_code))
            f.write("<b>New Code cannot be determined\n")
        # Unmappable codes - printed first since they MUST be resolved with manual tree exploration
        if unmappable_codes:
            f.write("<hr><h2 id=\"not_mapped_header\">The following codes could not be mapped to a code in the target version and require additional resolution:</h2>\n")
        for oncotree_code in unmappable_codes:
            f.write("<p><b>Original Code</b>: %s<br>\n" % (oncotree_code))
            f.write("<b>Closest Neighbors</b>: %s<br>\n" % ','.join(GLOBAL_LOG_MAP[oncotree_code][NEIGHBORS_FIELD]))
            f.write("To resolve, please refer to closest shared parent node %s and its descendants <a href=\"%s\">here</a><br></p>\n" % ((GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]), (oncotree_url + "&search_term=(" + GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD] + ")")))
        # Ambiguous codes - printed second since they MUST be resolved but already provide choices
        if ambiguous_codes:
            f.write("<hr><h2 id=\"mapped_to_multiple_header\">The following codes mapped to multiple codes in the target version. Please select from provided choices:</h2>\n")
        for oncotree_code in ambiguous_codes:
            f.write("<p><b>Original Code</b>: %s<br>\n" % (oncotree_code))
            f.write("<b>Choices</b>: %s<br>\n" % ','.join(GLOBAL_LOG_MAP[oncotree_code][CHOICES_FIELD]))
            if GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]:
                f.write("*Warning: Target version has introduced more granular nodes.<br>\n")
                f.write("You can examine the closest shared parent node %s and its descendants <a href=\"%s\">here</a><br></p>\n" % ((GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]), (oncotree_url + "&search_term=(" + GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD] + ")")))
        # Directly mapped codes - no action required, might want to explore more granular choices
        if partially_resolved_codes:
            f.write("<hr><h2 id=\"partially_mapped_header\">The following codes mapped to exactly one code but more granular codes have been introduced:</h2>\n")
        for oncotree_code in partially_resolved_codes:
            f.write("<p><b>Original Code</b>: %s<br>\n" % (oncotree_code))
            f.write("<b>New Code</b>: %s<br>\n" % ','.join(GLOBAL_LOG_MAP[oncotree_code][CHOICES_FIELD]))
            if GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]:
                f.write("*Warning: Target version has introduced more granular nodes.<br>\n")
                f.write("You can examine the closest shared parent node %s and its descendants <a href=\"%s\">here</a><br></p>\n" % ((GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD]), (oncotree_url + "&search_term=(" + GLOBAL_LOG_MAP[oncotree_code][CLOSEST_COMMON_PARENT_FIELD] + ")")))
        # Directly mapped codes - no action required, might want to explore more granular choices
        if completely_resolved_codes:
            f.write("<hr><h2 id=\"completely_mapped_header\">The following codes mapped to exactly one code:</h2>\n")
        for oncotree_code in completely_resolved_codes:
            f.write("<p><b>Original Code</b>: %s<br>\n" % (oncotree_code))
            f.write("<b>New Code</b>: %s<br>\n" % ','.join(GLOBAL_LOG_MAP[oncotree_code][CHOICES_FIELD]))
    print("Mapping summary HTML file written out to %s" % (html_summary_file), file=sys.stdout)

def usage(parser, message):
    if message:
        print(message, file=sys.stderr)
    print(parser.print_help(), file=sys.stderr)
    sys.exit(1)

#--------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--auto-mapping-enabled", help = "enable automatic resolution of ambiguous mappings", action = "store_true")
    parser.add_argument("-i", "--source-file", help = "source file provided by user", required = True)
    parser.add_argument("-o", "--target-file", help = "destination file to write out new file contents", required = True)
    parser.add_argument("-s", "--source-version", help = "current OncoTree version used in the source file", required = True)
    parser.add_argument("-t", "--target-version", help = "OncoTree version to be mapped to in the destination file", required = True)
    parser.add_argument("-u", "--oncotree-url", required = False, help=argparse.SUPPRESS)
    args = parser.parse_args()

    source_file = args.source_file
    target_file = args.target_file
    source_version = args.source_version
    target_version = args.target_version
    oncotree_url = args.oncotree_url

    oncotree_api_url_base = ONCOTREE_API_URL_BASE_DEFAULT
    if oncotree_url:
        oncotree_api_url_base = oncotree_url

    if not source_file or not target_file or not source_version or not target_version:
        usage(parse, "Error: missing arguments")

    if not os.path.isfile(source_file):
        print("Error: cannot access source file (%s) : no such file" % (source_file), file=sys.stderr)
        sys.exit(1)

    source_oncotree_version_release_date, target_oncotree_version_release_date = validate_and_fetch_oncotree_version_release_dates(source_version, target_version, oncotree_api_url_base)
    is_backwards_mapping = target_oncotree_version_release_date < source_oncotree_version_release_date # determines directionality of source - target OncoTree mapping
    source_file_mapped_list, header, header_and_comment_lines = load_source_file(source_file)
    source_oncotree = load_oncotree_version(source_version, oncotree_api_url_base)
    target_oncotree = load_oncotree_version(target_version, oncotree_api_url_base)
    translated_source_file_mapped_list = translate_oncotree_codes(source_file_mapped_list, source_oncotree, target_oncotree, is_backwards_mapping)
    write_to_target_file(translated_source_file_mapped_list, target_file, header, header_and_comment_lines)
    write_summary_file(target_file, source_version, target_version)
    print("OncoTree version conversion completed.", file=sys.stdout)

if __name__ == '__main__':
   main()
