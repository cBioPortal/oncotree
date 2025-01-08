#!/usr/bin/env python3

# Copyright (c) 2025 Memorial Sloan-Kettering Cancer Center.
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

# TODO maybe only show changes to precursors/revocations?
# instead of the whole history

from collections import defaultdict
from deepdiff import DeepDiff
import argparse
import csv
import graphite
import os
import requests
import sys

GITHUB_RESOURCE_URI_TO_ONCOCODE_MAPPING_FILE_URL = "https://raw.githubusercontent.com/cBioPortal/oncotree/refs/heads/master/resources/resource_uri_to_oncocode_mapping.txt"
HELP_FOR_FILE_FORMAT = "In the Graphite 'Concept Manager' left sidebar 'Hierarchy' tab, select your oncotree version, then click on the 'Export' tab in the main panel.  For 'File Format' select 'CSV (Dynamic Property Columns)'. In the 'Include' section uncheck everything except 'Non-primary concept URI' and 'Status'.  In the 'Select Properties to Export' all fields in both 'OncoTree Tumor Type' and 'SKOS' should be selected."
EXPECTED_HEADER = [graphite.CSV_RESOURCE_URI, \
    graphite.CSV_LABEL, \
    graphite.CSV_SCHEME_URI, \
    graphite.CSV_STATUS, \
    graphite.CSV_INTERNAL_ID, \
    graphite.CSV_COLOR, \
    graphite.CSV_MAIN_TYPE, \
    graphite.CSV_ONCOTREE_CODE, \
    graphite.CSV_PRECURSORS, \
    graphite.CSV_PREFERRED_LABEL, \
    graphite.CSV_REVOCATIONS, \
    graphite.CSV_PARENT_RESOURCE_URI, \
    graphite.CSV_PARENT_LABEL]
EXPECTED_HEADER_MODIFIED_FILE = EXPECTED_HEADER + [graphite.CSV_PARENT_ONCOTREE_CODE]
REQUIRED_FIELDS = [graphite.CSV_LABEL, \
    graphite.CSV_SCHEME_URI, \
    graphite.CSV_STATUS, \
    graphite.CSV_INTERNAL_ID, \
    graphite.CSV_COLOR, \
    graphite.CSV_MAIN_TYPE, \
    graphite.CSV_ONCOTREE_CODE, \
    graphite.CSV_PREFERRED_LABEL]
TISSUE_NODE_REQUIRED_FIELDS = [graphite.CSV_RESOURCE_URI, \
    graphite.CSV_LABEL, \
    graphite.CSV_SCHEME_URI, \
    graphite.CSV_STATUS, \
    graphite.CSV_INTERNAL_ID, \
    graphite.CSV_ONCOTREE_CODE, \
    graphite.CSV_PREFERRED_LABEL]

def confirm_change(message):
    print(f"\n{message}")
    answer = input("Enter [y]es if the changes were intentional, [n]o if not: ")
    if answer.lower() in ["y","yes"]:
        return True 
    return False

def construct_pretty_label_for_row(internal_id, code, label):
    return f"{internal_id}: {label} ({code})"

# TODO move these comments somewhere
# C01 + C02 + C03 -> C04
# C01, C02, and C03 become precursors to C04
# C05 -> C06 + C07 + C08
# C05 is a precursor to C06, C07, and C08
# you can have one concept be a precursor to many concepts
# C01 + C02 + C03 -> C01
# C02 and CO3 become revocations in C01
# don't revoke anything with precursors (according to Rob's document "Oncotree History Modeling") - check that anything in revocations is not a precursor
# a concept can only be revoked by a pre-existing concept

def get_parent_internal_id(child_internal_id, parent_resource_uri, parent_oncotree_code, oncotree_codes_to_internal_ids, resource_uri_to_internal_ids):
    """This will use either the parent oncotree code, or if that isn't given, the parent resource uri
    to look up the parent's internal id.  If the parent cannot be found this will throw an error."""
    # TODO have we validated these already? If so, skip the error checks?
    if parent_oncotree_code:
        if parent_oncotree_code in oncotree_codes_to_internal_ids:    
            return oncotree_codes_to_internal_ids[parent_oncotree_code]
        else:
            print(f"Error: '{graphite.CSV_PARENT_ONCOTREE_CODE}' '{parent_oncotree_code}' not found in modified file", file=sys.stderr)
            sys.exit(1)
    if not parent_resource_uri:
        print(f"Error: either '{graphite.CSV_PARENT_ONCOTREE_CODE}' or '{graphite.CSV_PARENT_RESOURCE_URI}' are required in the modified file, missing for '{child_internal_id}'", file=sys.stderr) 
        sys.exit(1)
    # we must have the parent_resource_uri instead
    return resource_uri_to_internal_ids[parent_resource_uri]
        
def get_oncotree_code_from_internal_id(oncotree_codes_to_internal_ids, internal_id):
    for code in oncotree_codes_to_internal_ids:
        if oncotree_codes_to_internal_ids[code] == internal_id:
            return code
    print(f"Error: could not find an oncotree code for internal id {internal_id}", file=sys.stderr)
    sys.exit(1)

def confirm_changes(original_oncotree,
                    modified_oncotree,
                    precursor_id_to_internal_ids,
                    revocation_id_to_internal_ids,
                    original_resource_uri_to_internal_ids,
                    modified_resource_uri_to_internal_ids,
                    internal_id_to_oncocodes,
                    oncotree_codes_to_internal_ids):
    original_internal_id_set = set(original_oncotree.keys())
    modified_internal_id_set = set(modified_oncotree.keys())

    # get three sets of INTERNAL_IDs:
    # 1) ones that have been removed
    # 2) ones that are new
    # 3) ones that are still there
    # then handle each of the three sets
    removed_internal_ids = original_internal_id_set - modified_internal_id_set
    new_internal_ids = modified_internal_id_set - original_internal_id_set
    in_both_internal_ids = original_internal_id_set & modified_internal_id_set

    print("\nRemoved internal ids:")
    if removed_internal_ids:
        for internal_id in sorted(removed_internal_ids):
            data = original_oncotree[internal_id]
            pretty_label = construct_pretty_label_for_row(data[graphite.CSV_INTERNAL_ID], data[graphite.CSV_ONCOTREE_CODE], data[graphite.CSV_LABEL])
            print(f"\t{pretty_label}")
        print(f"\n****** All removed Oncotree nodes must be manually deleted from Graphite")
    else:
        print("\tNone")

    print("\nNew internal ids:")
    if new_internal_ids:
        for internal_id in sorted(new_internal_ids):
            data = modified_oncotree[internal_id]
            pretty_label = construct_pretty_label_for_row(data[graphite.CSV_INTERNAL_ID], data[graphite.CSV_ONCOTREE_CODE], data[graphite.CSV_LABEL])
            if data[graphite.CSV_RESOURCE_URI]:
                # we could allow this but we would have to make sure the resource uri is new and is valid - so let's not
                print(f"Error: you cannot have a '{graphite.CSV_RESOURCE_URI}' for a new oncotree node '{pretty_label}'", file=sys.stderr) 
                sys.exit(1)
            # show parent in "new" nodes
            parent_internal_id = get_parent_internal_id(internal_id, data[graphite.CSV_PARENT_RESOURCE_URI], data[graphite.CSV_PARENT_ONCOTREE_CODE], oncotree_codes_to_internal_ids, modified_resource_uri_to_internal_ids)
            parent_data = modified_oncotree[parent_internal_id]
            parent_pretty_label = construct_pretty_label_for_row(parent_internal_id, parent_data[graphite.CSV_ONCOTREE_CODE], parent_data[graphite.CSV_LABEL])
            print(f"\t{pretty_label} has parent {parent_pretty_label}")
    else:
        print("\tNone")

    print("\nPrecurors:")
    if precursor_id_to_internal_ids:
        for precursor_id in sorted(precursor_id_to_internal_ids.keys()):
            precursor_code = internal_id_to_oncocodes[precursor_id] if precursor_id in internal_id_to_oncocodes else "unknown"
            # are any current concepts precursors? they shouldn't be
            if precursor_id in modified_internal_id_set:
                print(f"Error: '{precursor_id}' ('{precursor_code}') is a precuror to '{','.join(precursor_id_to_internal_ids[precursor_id])}' but '{precursor_id}' is still in this file as a current record", file=sys.stderr)
                sys.exit(1)
            precursor_of_set = precursor_id_to_internal_ids[precursor_id]
            for internal_id in precursor_of_set:
                data = modified_oncotree[internal_id]
                pretty_label = construct_pretty_label_for_row(data[graphite.CSV_INTERNAL_ID], data[graphite.CSV_ONCOTREE_CODE], data[graphite.CSV_LABEL])
                print(f"\t'{precursor_id}' ('{precursor_code}') -> '{pretty_label}'")
    else:
        print("\tNone")

    print("\nRevocations:")
    if revocation_id_to_internal_ids: 
        for revocation_id in sorted(revocation_id_to_internal_ids.keys()):
            revocation_code = internal_id_to_oncocodes[revocation_id] if revocation_id in internal_id_to_oncocodes else "unknown"
            if revocation_id in modified_internal_id_set:
                print(f"Error: '{revocation_id}' ('{revocation_code}') has been revoked by '{','.join(revocation_id_to_internal_ids[revocation_id])}' but '{revocation_id}' is still in this file as a current record", file=sys.stderr)
                sys.exit(1)
            if revocation_id in precursor_id_to_internal_ids:
                print(f"Error: Revocation '{revocation_id}' ('{revocation_code}') cannot also be a precursor", file=sys.stderr)
                sys.exit(1)
            revocation_of_set = revocation_id_to_internal_ids[revocation_id]
            for internal_id in revocation_of_set: 
                if internal_id in new_internal_ids:
                    print(f"Error: '{revocation_id}' ('{revocation_code}') revokes '{internal_id}' but '{internal_id}' is a new concept. Only a pre-existing concept can revoke something", file=sys.stderr)
                    sys.exit(1)
                data = modified_oncotree[internal_id]
                pretty_label = construct_pretty_label_for_row(data[graphite.CSV_INTERNAL_ID], data[graphite.CSV_ONCOTREE_CODE], data[graphite.CSV_LABEL])
                print(f"\t'{revocation_id}' ('{revocation_code}') -> '{pretty_label}'")
    else:
        print("\tNone")

    # compare all deleted and new internal ids to see if any are really the same - TODO what counts as "the same"?
    print("\nInternal ids that changed when no other data has changed ... are these really new concepts that cover different sets of cancer cases?")
    found_id_change_with_no_data_change = False
    # compare all removed ids to new ids to see if any have the same data
    for pair in {(x, y) for x in removed_internal_ids for y in new_internal_ids}:
        original_data = original_oncotree[pair[0]]
        modified_data = modified_oncotree[pair[1]]
        diff = DeepDiff(original_data, modified_data, ignore_order=True)
        # remove the change we know about (the internal id)
        diff['values_changed'] = {x : diff['values_changed'][x] for x in diff['values_changed'].keys() if x != f"root['{graphite.CSV_INTERNAL_ID}']"}
 
        if not diff['values_changed']: # TODO do we care about anything besides values_changed?
            found_id_change_with_no_data_change = True
            original_pretty_label = construct_pretty_label_for_row(original_data[graphite.CSV_INTERNAL_ID], original_data[graphite.CSV_ONCOTREE_CODE], original_data[graphite.CSV_LABEL])
            modified_pretty_label = construct_pretty_label_for_row(modified_data[graphite.CSV_INTERNAL_ID], modified_data[graphite.CSV_ONCOTREE_CODE], modified_data[graphite.CSV_LABEL])
            # TODO what changes really are important?  probably not color for example
            print(f"\t'{original_pretty_label}' -> '{modified_pretty_label}'")
    if not found_id_change_with_no_data_change:
        print("\tNone")

    # now we look at all interal ids that are in both files, what has changed about the data?
    code_change_messages = []
    parent_change_messages = []
    for internal_id in in_both_internal_ids:
        original_data = original_oncotree[internal_id]
        modified_data = modified_oncotree[internal_id]
        original_pretty_label = construct_pretty_label_for_row(original_data[graphite.CSV_INTERNAL_ID], original_data[graphite.CSV_ONCOTREE_CODE], original_data[graphite.CSV_LABEL])
        modified_pretty_label = construct_pretty_label_for_row(modified_data[graphite.CSV_INTERNAL_ID], modified_data[graphite.CSV_ONCOTREE_CODE], modified_data[graphite.CSV_LABEL])

        # confirm we have resource uri in original file, we don't have to check modified file because we check if it has changed
        if original_data[graphite.CSV_RESOURCE_URI].strip() == "":
            print(f"ERROR: Resource URI is required for all records in the original file but is missing for '{original_pretty_label}'", file=sys.stderr)
            sys.exit(1) 

        if original_data[graphite.CSV_RESOURCE_URI] != modified_data[graphite.CSV_RESOURCE_URI]:
            print(f"ERROR: Resource URI has changed for '{modified_pretty_label}', this is not allowed", file=sys.stderr)
            sys.exit(1) 

        if original_data[graphite.CSV_ONCOTREE_CODE] != modified_data[graphite.CSV_ONCOTREE_CODE]:
            code_change_messages.append(f"\t'{original_pretty_label}' -> '{modified_pretty_label}'")

        if internal_id != "ONC000001": # tissue has no parents
            # check if parent has changed
            # use oncotree codes (we will get either have the oncotree code, or will get it using the resource uri)
            modified_parent_oncotree_code = modified_data[graphite.CSV_PARENT_ONCOTREE_CODE]
            if not modified_parent_oncotree_code: 
                # then get the oncotree code using resource uri -> internal id -> oncotree code
                modified_parent_internal_id = get_parent_internal_id(internal_id,
                                                                     modified_data[graphite.CSV_PARENT_RESOURCE_URI],
                                                                     modified_data[graphite.CSV_PARENT_ONCOTREE_CODE],
                                                                     oncotree_codes_to_internal_ids,
                                                                     modified_resource_uri_to_internal_ids)
                modified_parent_oncotree_code = get_oncotree_code_from_internal_id(oncotree_codes_to_internal_ids, modified_parent_internal_id)
    
            # get the original parent oncotree code
            # in the original file we will have to look up the oncotree code using resource uri -> internal id -> oncotree code
            original_parent_internal_id = get_parent_internal_id(internal_id,
                                                                 original_data[graphite.CSV_PARENT_RESOURCE_URI],
                                                                 None, # this file doesn't have a parent oncotree code column
                                                                 oncotree_codes_to_internal_ids,
                                                                 original_resource_uri_to_internal_ids)
            original_parent_oncotree_code = get_oncotree_code_from_internal_id(oncotree_codes_to_internal_ids, original_parent_internal_id)
            if original_parent_oncotree_code != modified_parent_oncotree_code:
                parent_change_messages.append(f"\tchild: '{original_pretty_label}' parent: '{original_parent_oncotree_code}' -> child: '{modified_pretty_label}' parent: '{modified_parent_oncotree_code}'")
        

    print("\nOncotree code/label changes with no internal id change.  This is allowed as long as the new code/label covers the exact same set of cancer cases") 
    if code_change_messages:
        for message in code_change_messages:
            print(message)
    else:
        print("\tNone")

    print("\nParent change")
    if parent_change_messages:
        for message in parent_change_messages:
            print(message)
    else:
        print("\tNone")

    if not confirm_change("\nPlease confirm that all of the above changes are intentional."):
        print("ERROR: You  have said that not all changes are intentional.  Please correct your input file and run this script again.", file=sys.stderr)
        sys.exit(2)

def output_rdf_file(oncotree, output_filename):
    graphite.write_rdf(oncotree, output_filename)

def get_oncotree_data_from_csv_file(csv_file):
    """Reads a Graphite CSV file and returns the following maps:
        internal_id_to_data,
        resource_uri_to_internal_ids,
        precursor_id_to_internal_ids,
        revocation_id_to_internal_ids,
        oncotree_codes_to_internal_ids"""
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        internal_id_to_data = {} 
        resource_uri_to_internal_ids = {}
        precursor_id_to_internal_ids = defaultdict(set)
        revocation_id_to_internal_ids = defaultdict(set)
        oncotree_codes_to_internal_ids = {}
        for row in reader:
            internal_id = row[graphite.CSV_INTERNAL_ID]
            pretty_label = construct_pretty_label_for_row(internal_id, row[graphite.CSV_ONCOTREE_CODE], row[graphite.CSV_LABEL])
            if row[graphite.CSV_STATUS] != 'Published':
                print(f"WARNING: do not know what to do with node '{pretty_label}' which has a status of '{row[graphite.CSV_STATUS]}', excluding it from the output file")
            else:
                internal_id_to_data[internal_id] = row
                oncotree_codes_to_internal_ids[row[graphite.CSV_ONCOTREE_CODE]] = row[graphite.CSV_INTERNAL_ID]
                if row[graphite.CSV_RESOURCE_URI]:
                    print(f"row[graphite.CSV_RESOURCE_URI]={row[graphite.CSV_RESOURCE_URI]}")
                    resource_uri_to_internal_ids[row[graphite.CSV_RESOURCE_URI]] = row[graphite.CSV_INTERNAL_ID]
                if row[graphite.CSV_PRECURSORS]:
                    for precursor_id in row[graphite.CSV_PRECURSORS].split(): # space separated
                        precursor_id_to_internal_ids[precursor_id].add(row[graphite.CSV_INTERNAL_ID])
                if row[graphite.CSV_REVOCATIONS]:
                    for revocation_id in row[graphite.CSV_REVOCATIONS].split(): # space separated
                        revocation_id_to_internal_ids[revocation_id].add(row[graphite.CSV_INTERNAL_ID])
        return internal_id_to_data, \
            resource_uri_to_internal_ids, \
            precursor_id_to_internal_ids, \
            revocation_id_to_internal_ids, \
            oncotree_codes_to_internal_ids

def field_is_required(field, field_name, internal_id, csv_file):
    if not field:
        print(f"{field_name} is a required field, it is empty for the '{internal_id}' record in '{csv_file}'", file=sys.stderr)
        sys.exit(1)

def field_is_unique(field, field_name, column_set, internal_id, csv_file):
    # don't count "" duplicates -- these should be dealt with in required field check
    if field != "" and field in column_set:
        print(f"{field_name} must be unique.  There is more than one record with '{field}' in '{csv_file}'", file=sys.stderr)
        sys.exit(1)

def parent_oncotree_code_is_valid(row, 
                                  oncotree_codes_to_internal_ids):
    return row[graphite.CSV_PARENT_ONCOTREE_CODE] in oncotree_codes_to_internal_ids
   
def parent_definition_in_conflict(row,
                                  oncotree_codes_to_internal_ids,              
                                  resource_uri_to_internal_ids): 
    """There is a conflict if we have both the parent oncotree code
       and the parent resource uri and they don't point to the same child"""
    return (row[graphite.CSV_PARENT_RESOURCE_URI] and
            row[graphite.CSV_PARENT_ONCOTREE_CODE] and
            resource_uri_to_internal_ids[row[graphite.CSV_PARENT_RESOURCE_URI]] != \
                oncotree_codes_to_internal_ids[row[graphite.CSV_PARENT_ONCOTREE_CODE]])

def parent_resource_uri_and_label_are_valid(row,
                                            child_to_parent_resource_uris,
                                            child_uri_to_child_label)
    """Both the parent label and URI must be defined in this file
        and the parent URI must point to a child with the label we
        have defined for the parent."""
    return (row[graphite.CSV_PARENT_RESOURCE_URI] in child_to_parent_resource_uris and
            row[graphite.CSV_PARENT_LABEL] == child_uri_to_child_label[row[graphite.CSV_PARENT_RESOURCE_URI]])

def parent_is_defined(oncotree_code,
                      parent_resource_uri,
                      parent_label,
                      parent_oncotree_code):
    if not (parent_oncotree_code or (parent_resource_uri and parent_label)):
        print(f"'{oncotree_code}' does not have a parent defined either by the '{graphite.CSV_PARENT_ONCOTREE_CODE}' or both '{graphite.CSV_PARENT_RESOURCE_URI}' and '{graphite.CSV_PARENT_LABEL}'")
        sys.exit(1)

def using_oncotree_code_to_define_parent(row):
 return graphite.CSV_PARENT_ONCOTREE_CODE in row and row[graphite.CSV_PARENT_ONCOTREE_CODE]

def is_tissue_node(row):
    return row[graphite.CSV_ONCOTREE_CODE] == "TISSUE"

def validate_csv_file(csv_file,
                      expected_header,
                      resource_uri_to_internal_ids,
                      oncotree_codes_to_internal_ids):
    # read the file once to do some checks and also to collect data
    # so we can do more checks in a second pass at reading the file
    # load all child->parent relationships
    # TODO get this from earlier too
    child_uri_to_child_label = {} # make sure the parent uri + label match the child uri + label pair

    # these fields are required and must be unique
    # TODO get these from somewhere else
    resource_uri_set = set([])
    internal_id_set = set([])
    oncotree_code_set = set([])
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        actual_header = reader.fieldnames
        missing_fields = set(expected_header) - set(actual_header)
        if missing_fields:
            print(f"ERROR: missing the following expected fields from input file '{csv_file}': {missing_fields}", file=sys.stderr)
            sys.exit(1)

        for row in reader:
            # save child->parent relationships
            child_uri_to_child_label[row[graphite.CSV_RESOURCE_URI]] = row[graphite.CSV_LABEL]
            child_to_parent_labels[row[graphite.CSV_LABEL]] = row[graphite.CSV_PARENT_LABEL] 
            oncotree_codes_to_internal_ids[row[graphite.CSV_ONCOTREE_CODE]] = row[graphite.CSV_INTERNAL_ID]

            # check all colunns are not empty
            required_fields = TISSUE_NODE_REQUIRED_FIELDS if is_tissue_node(row) else REQUIRED_FIELDS
            for field in required_fields:
                field_is_required(row[field], field, row[graphite.CSV_INTERNAL_ID], csv_file)  
           
            if not is_tissue_node(row):
                parent_oncotree_code = "" if graphite.CSV_PARENT_ONCOTREE_CODE not in row else row[graphite.CSV_PARENT_ONCOTREE_CODE] 
                parent_is_defined(row[graphite.CSV_ONCOTREE_CODE], row[graphite.CSV_PARENT_RESOURCE_URI], row[graphite.CSV_PARENT_LABEL], parent_oncotree_code)

            # check these columns are unique
            field_is_unique(row[graphite.CSV_RESOURCE_URI], graphite.CSV_RESOURCE_URI, resource_uri_set, row[graphite.CSV_INTERNAL_ID], csv_file)
            field_is_unique(row[graphite.CSV_INTERNAL_ID], graphite.CSV_INTERNAL_ID, internal_id_set, row[graphite.CSV_ONCOTREE_CODE], csv_file)
            field_is_unique(row[graphite.CSV_ONCOTREE_CODE], graphite.CSV_ONCOTREE_CODE, oncotree_code_set, row[graphite.CSV_INTERNAL_ID], csv_file)

            resource_uri_set.add(row[graphite.CSV_RESOURCE_URI])
            internal_id_set.add(row[graphite.CSV_INTERNAL_ID])
            oncotree_code_set.add(row[graphite.CSV_ONCOTREE_CODE])

    # now read again once we have collected all ids etc. so we can look things up and make sure references are correct
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        label_mismatch_errors = []
        parent_invalid_errors = []
        for row in reader:
            if row[graphite.CSV_LABEL] != row[graphite.CSV_PREFERRED_LABEL]:
                label_mismatch_errors.append(f"{row[graphite.CSV_INTERNAL_ID]}: '{row[graphite.CSV_LABEL]}' != '{row[graphite.CSV_PREFERRED_LABEL]}'")
            if is_tissue_node(row):
                # the TISSUE node cannot have any parents set
                if (row[graphite.CSV_PARENT_RESOURCE_URI] or
                    row[graphite.CSV_PARENT_LABEL] or
                    using_oncotree_code_to_define_parent(row)):
                    print(f"The 'TISSUE' node must not have any of these fields set: '{graphite.CSV_PARENT_RESOURCE_URI}', '{graphite.CSV_PARENT_LABEL}', '{graphite.CSV_PARENT_LABEL}' but at least one is in '{csv_file}'", file=sys.stderr)
                    sys.exit(1)
            # this isn't the TISSUE node
            # we are defining the parent using the oncotree code, so check that is valid
            elif using_oncotree_code_to_define_parent(row):
                if not parent_oncotree_code_is_valid(row, oncotree_codes_to_internal_ids): 
                    parent_invalid_errors.append(f"Child '{row[graphite.CSV_ONCOTREE_CODE]}' has parent oncotree code '{row[graphite.CSV_PARENT_ONCOTREE_CODE]}' which doesn't map to anything in file '{csv_file}'")
                elif parent_definition_in_conflict(row,
                                                   oncotree_codes_to_internal_ids,
                                                   resource_uri_to_internal_ids):
                    parent_invalid_errors.append(f"Error: Child '{row[graphite.CSV_ONCOTREE_CODE]}' has parent oncotree code '{row[graphite.CSV_PARENT_ONCOTREE_CODE]}' which maps to internal id '{oncotree_codes_to_internal_ids[row[graphite.CSV_PARENT_ONCOTREE_CODE]]}' this is in conflict with the resource uri defined for the parent '{row[graphite.CSV_PARENT_RESOURCE_URI]}' which maps to internal id '{resource_uri_to_internal_ids[row[graphite.CSV_PARENT_RESOURCE_URI]]}'.  Which one is the parent?")
            # we are defining the parent with the resource uri + label
            # we need to make sure the parent resource uri/label pair
            # matches what exists in the file
            elif not parent_resource_uri_and_label_are_valid(row,
                   child_to_parent_resource_uris,
                   child_uri_to_child_label,
                   child_to_parent_labels):
                parent_invalid_errors.append(f"{row[graphite.CSV_INTERNAL_ID]}: URI '{row[graphite.CSV_PARENT_RESOURCE_URI]}' and label '{row[graphite.CSV_PARENT_LABEL]}'")

    if label_mismatch_errors:
        print(f"ERROR: '{graphite.CSV_LABEL}' and '{graphite.CSV_PREFERRED_LABEL}' columns must be identical.  Mis-matched fields in '{csv_file}':")
        for message in label_mismatch_errors:
            print(f"\t{message}")
    
    if parent_invalid_errors:
        print(f"ERROR: Invalid parents found in '{csv_file}'.  Check that the oncotree code is valid if it was entered, or if you are using the resource uri/label pair, that they are defined in '{csv_file}'")
        for message in parent_invalid_errors:
            print(f"\t{message}")

    if label_mismatch_errors or parent_invalid_errors:
        sys.exit(1)

def get_internal_id_to_oncocodes():
    response = requests.get(GITHUB_RESOURCE_URI_TO_ONCOCODE_MAPPING_FILE_URL) 
    if response.status_code != 200:
        print(f"Error: Failed to download GitHub raw resource uri to oncoode file. Status code was '{response.status_code}'.  Please confirm this is the correct url: '{GITHUB_RESOURCE_URI_TO_ONCOCODE_MAPPING_FILE_URL}'", file=sys.stderr)
        sys.exit(1)
    internal_id_to_oncocodes = {}
    for row in response.text.splitlines():
        fields = row.split()
        if fields[1] == "hasCode":
            internal_id_to_oncocodes[fields[0]] = fields[2]    
    return internal_id_to_oncocodes
 
def usage(parser, message):
    if message:
        print(message, file=sys.stderr)
    parser.print_help(file=sys.stderr)
    sys.exit(1)

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("-o", \
        "--original-file", \
        help = f"Original csv file from Graphite. {HELP_FOR_FILE_FORMAT}", \
        required = True)
    parser.add_argument("-m", \
        "--modified-file", \
        help = f"Modified csv file from user.  This should be a modified copy of the original csv file from Graphite, \
        with an additional column at the end called '{graphite.CSV_PARENT_ONCOTREE_CODE}'.", \
        required = True)
    parser.add_argument("-t", \
        "--output-file", \
        help = f"Generated rdf file to be uploaded to Graphite.", \
        required = True)
    args = parser.parse_args()

    original_file = args.original_file
    modified_file = args.modified_file
    output_file = args.output_file

    if not original_file or not modified_file: 
        usage(parser, f"ERROR: missing file arguments, given original file '{original_file}' and modified file '{modified_file}'")

    if not os.path.isfile(original_file):
        usage(parser, f"ERROR: cannot access original file {original_file}")
        sys.exit(1)

    if os.path.isfile(output_file):
        usage(parser, f"ERROR: {output_file} already exists and will be overwritten")
        sys.exit(1)

    if not os.path.isfile(modified_file):
        usage(parser, f"ERROR: cannot access modified file {modified_file}")
        sys.exit(1)
    
    return original_file, modified_file, output_file

def main():
    # 0. get the command line arguments
    original_file, modified_file, output_file = get_args()

    # 1. query Github for the offical internal id to oncotree code mapping
    internal_id_to_oncocodes = get_internal_id_to_oncocodes()

    # 2. read the csv data and put into various data structures to look things up later
    original_oncotree, \
        original_resource_uri_to_internal_ids, \
        _, \
        _, \
        original_oncotree_codes_to_internal_ids = get_oncotree_data_from_csv_file(original_file)
    modified_oncotree, \
        modified_resource_uri_to_internal_ids, \
        precursor_id_to_internal_ids, \
        revocation_id_to_internal_ids, \
        modified_oncotree_codes_to_internal_ids = get_oncotree_data_from_csv_file(modified_file)

    # 3. validate
    validate_csv_file(original_file, \ 
                      EXPECTED_HEADER, \ 
                      original_resource_uri_to_internal_ids, \ 
                      original_oncotree_codes_to_internal_ids)
    validate_csv_file(modified_file, \ 
                      EXPECTED_HEADER_MODIFIED_FILE, \ 
                      modified_resource_uri_to_internal_ids, \ 
                      modified_oncotree_codes_to_internal_ids)

    # 4. say what has changed and confirm they are wanted
    confirm_changes(original_oncotree,
                    modified_oncotree,
                    precursor_id_to_internal_ids,
                    revocation_id_to_internal_ids,
                    original_resource_uri_to_internal_ids,
                    modified_resource_uri_to_internal_ids,
                    internal_id_to_oncocodes,
                    oncotree_codes_to_internal_ids)

    # 5. generate rdf from the data
    output_rdf_file(modified_oncotree, output_file)

if __name__ == '__main__':
   main()
