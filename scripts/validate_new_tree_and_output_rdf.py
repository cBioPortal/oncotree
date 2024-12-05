#!/usr/bin/env python3

# Copyright (c) 2024 Memorial Sloan-Kettering Cancer Center.
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
import csv
from deepdiff import DeepDiff
#from deepdiff import DeepSearch
#from deepdiff import grep
import os
from pprint import pprint # TODO delete?
import sys

LABEL = "Primary Concept"
RESOURCE_URI = "Resource URI"
ONCOTREE_CODE = "notation (SKOS)"
SCHEME_URI = "skos:inScheme URI"
STATUS = "Status"
INTERNAL_ID = "clinicalCasesSubset (OncoTree Tumor Type)"
COLOR = "color (OncoTree Tumor Type)"
MAIN_TYPE = "mainType (OncoTree Tumor Type)"
PRECURSORS = "precursors (OncoTree Tumor Type)"
PREFERRED_LABEL = "preferred label (SKOS)"
REVOCATIONS = "revocations (OncoTree Tumor Type)"
PARENT_RESOURCE_URI = "has broader (SKOS) URI"
PARENT_LABEL = "has broader (SKOS)"
# TODO explain exactly how to generate this file in Graphite here and in usage
EXPECTED_HEADER = [RESOURCE_URI, LABEL, SCHEME_URI, STATUS, INTERNAL_ID, COLOR, MAIN_TYPE, ONCOTREE_CODE, PRECURSORS, PREFERRED_LABEL, REVOCATIONS, PARENT_RESOURCE_URI, PARENT_LABEL]

def confirm_change(message):
    print(f"\n{message}")
    #answer = input("Enter [y]es if this change was intentional, [n]o if not: ")
    answer = input("Enter [y]es if the changes were intentional, [n]o if not: ")
    if answer.lower() in ["y","yes"]:
        return True 
    return False

def construct_pretty_label_for_row(internal_id, code, label):
    return f"{internal_id}: {label} ({code})"

def confirm_changes(original_oncotree, modified_oncotree):
    # get three sets of INTERNAL_IDs:
    # 1) ones that have been removed
    # 2) ones that are new
    # 3) ones that are still there
    # then handle each of the three sets
    original_internal_id_set = set(original_oncotree.keys())
    modified_internal_id_set = set(modified_oncotree.keys())

    removed_internal_ids = original_internal_id_set - modified_internal_id_set
    new_internal_ids = modified_internal_id_set - original_internal_id_set
    in_both_internal_ids = original_internal_id_set & modified_internal_id_set

    #print(removed_internal_ids)
    #print(new_internal_ids)
    #print(in_both_internal_ids)

    all_changes_are_intentional = True

    print("\nRemoved internal ids:")
    if removed_internal_ids:
        for internal_id in removed_internal_ids:
            data = original_oncotree[internal_id]
            pretty_label = construct_pretty_label_for_row(data[INTERNAL_ID], data[ONCOTREE_CODE], data[LABEL])
            #message = f"Removed concept '{pretty_label}'"
            #all_changes_are_intentional &= confirm_change(message)
            print(f"\t{pretty_label}")
    else:
        print("\tNone")

    print("\nNew internal ids:")
    if new_internal_ids:
        for internal_id in new_internal_ids:
            data = modified_oncotree[internal_id]
            pretty_label = construct_pretty_label_for_row(data[INTERNAL_ID], data[ONCOTREE_CODE], data[LABEL])
            #message = f"A new concept has been added '{pretty_label}' - are you absolutely sure this is a new concept?"
            #all_changes_are_intentional &= confirm_change(message)
            print(f"\t{pretty_label}")
    else:
        print("\tNone")

    # compare all deleted and new internal ids to see if any are really the same - TODO what counts as "the same"?
    print("\nInternal ids that changed when no other data has changed ... are these really new concepts that cover different sets of cancer cases?")
    found_id_change_with_no_data_change = False
    for pair in {(x, y) for x in removed_internal_ids for y in new_internal_ids}:
        #print(pair)
        original_data = original_oncotree[pair[0]]
        modified_data = modified_oncotree[pair[1]]
        # TODO refactor and redo this section
        diff = DeepDiff(original_data, modified_data, ignore_order=True)
        #print(diff)
        # remove the change we know about
        diff['values_changed'] = {x : diff['values_changed'][x] for x in diff['values_changed'].keys() if x != f"root['{INTERNAL_ID}']"}
        #print(diff)
 
        if not diff['values_changed']: # TODO do we care about anything besides values_changed?
            found_id_change_with_no_data_change = True
            original_pretty_label = construct_pretty_label_for_row(original_data[INTERNAL_ID], original_data[ONCOTREE_CODE], original_data[LABEL])
            modified_pretty_label = construct_pretty_label_for_row(modified_data[INTERNAL_ID], modified_data[ONCOTREE_CODE], modified_data[LABEL])
            # TODO what changes really are important?  probably not color for example
            #message = f"An internal id has changed from '{original_pretty_label}' to '{modified_pretty_label}' but all other data remains the same.  Is this really a new concept?"
            #all_changes_are_intentional &= confirm_change(message)
            print(f"\t'{original_pretty_label}' -> '{modified_pretty_label}'")
    if not found_id_change_with_no_data_change:
        print("\tNone")

    # now we look at all interal ids that are in both files, what has changed about the data?
    code_change_messages = []
    parent_change_messages = []
    for internal_id in in_both_internal_ids:
        original_data = original_oncotree[internal_id]
        modified_data = modified_oncotree[internal_id]
        original_pretty_label = construct_pretty_label_for_row(original_data[INTERNAL_ID], original_data[ONCOTREE_CODE], original_data[LABEL])
        modified_pretty_label = construct_pretty_label_for_row(modified_data[INTERNAL_ID], modified_data[ONCOTREE_CODE], modified_data[LABEL])

        # see what has changed 
        diff = DeepDiff(original_data, modified_data, ignore_order=True)
        if diff:
            #print(diff)
            if original_data[RESOURCE_URI] != modified_data[RESOURCE_URI]:
                print(f"ERROR: Resource URI has changed for '{modified_pretty_label}', this is not allowed", file=sys.stderr)
                sys.exit(1) 

            if original_data[ONCOTREE_CODE] != modified_data[ONCOTREE_CODE]:
                #message = f"The oncotree code/label has changed from '{original_pretty_label}' to '{modified_pretty_label}' but our internal id has not changed.  This is allowed as long as you are sure this covers the same set of cancer cases"
                #all_changes_are_intentional &= confirm_change(message) 
                code_change_messages.append(f"\t'{original_pretty_label}' -> '{modified_pretty_label}'")

            if original_data[PARENT_LABEL] != modified_data[PARENT_LABEL]:
                parent_change_messages.append(f"\tchild: '{original_pretty_label}' parent: '{original_data[PARENT_LABEL]}' -> child: '{modified_pretty_label}' parent: '{modified_data[PARENT_LABEL]}'")
      
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
 
        # TODO make sure two labels are always the same
        # confirm that if a INTERNAL_ID has been deleted, it has either:
        #  1) been made a revocation on an existing INTERNAL_ID - this is a concept that has been absorbed by another concept
        #  2) been made a precusor for a new INTERNAL_ID - this is a concept that being replaced by a new concept
        #  3) does not exist as anything anymore - is this allowed?
    #if not all_changes_are_intentional:
    if not confirm_change("\nPlease confirm that all of the above changes are intentional."):
        print("ERROR: You  have said that not all changes are intentional.  Please correct your input file and run this script again.", file=sys.stderr)
        sys.exit(2)

def output_rdf_file(oncotree):
    print("TODO: output RDF file")

def get_oncotree(csv_file):
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        internal_id_to_data = {} 
        for row in reader:
            internal_id = row[INTERNAL_ID]
            pretty_label = construct_pretty_label_for_row(internal_id, row[ONCOTREE_CODE], row[LABEL])
            # TODO move to validation section
            if row[STATUS] != 'Published':
                print(f"WARNING: do not know what to do with node '{pretty_label}' which has a status of '{row[STATUS]}', excluding it from the output file")
            internal_id_to_data[internal_id] = row
        return internal_id_to_data

def validate_csv_file(csv_file):
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        actual_header = reader.fieldnames
        if actual_header != EXPECTED_HEADER:
            print(f"ERROR: missing the following expected fields from input file '{csv_file}': {set(EXPECTED_HEADER) - set(actual_header)}", file=sys.stderr)
            sys.exit(1)
        # TODO add much more validation!
        # TODO uniqueness:
        #        * INTERNAL_ID
        #        * ONCOTREE_CODE
        #        * LABEL
        #        * PREFERRED_LABEL
        # validate all nodes have parent - that tree is still valid - and that we only include 'Published' nodes in this?
        # TODO think about status - should we even be looking at it? why would the new data be 'Published'
 
def usage(parser, message):
    if message:
        print(message, file=sys.stderr)
    parser.print_help(file=sys.stderr)
    sys.exit(1)

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("-o", "--original-file", help = "original csv file from Graphite", required = True)
    parser.add_argument("-m", "--modified-file", help = "modified csv file from user", required = True)
    args = parser.parse_args()

    original_file = args.original_file
    modified_file = args.modified_file

    if not original_file or not modified_file: 
        usage(parse, f"ERROR: missing file arguments, given original file '{original_file}' and modified file '{modified_file}'")

    if not os.path.isfile(original_file):
        print(f"ERROR: cannot access original file {original_file}", file=sys.stderr)
        sys.exit(1)

    if not os.path.isfile(modified_file):
        print(f"ERROR: cannot access modified file {modified_file}", file=sys.stderr)
        sys.exit(1)
    return original_file, modified_file

def main():
    original_file, modified_file = get_args()
    validate_csv_file(original_file)
    validate_csv_file(modified_file)
    original_oncotree = get_oncotree(original_file)
    modified_oncotree = get_oncotree(modified_file)
    confirm_changes(original_oncotree, modified_oncotree)
    output_rdf_file(modified_oncotree)

if __name__ == '__main__':
   main()
