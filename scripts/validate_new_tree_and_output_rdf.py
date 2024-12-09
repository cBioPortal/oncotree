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

# TODO think about status - should we even be looking at it? why would the new data be 'Published'

import argparse
from collections import defaultdict
import csv
from deepdiff import DeepDiff
#from deepdiff import DeepSearch
#from deepdiff import grep
import os
#from pprint import pprint # TODO delete?
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
REQUIRED_FIELDS = [RESOURCE_URI, LABEL, SCHEME_URI, STATUS, INTERNAL_ID, COLOR, MAIN_TYPE, ONCOTREE_CODE, PREFERRED_LABEL, PARENT_RESOURCE_URI, PARENT_LABEL]
TISSUE_NODE_REQUIRED_FIELDS = [RESOURCE_URI, LABEL, SCHEME_URI, STATUS, INTERNAL_ID, ONCOTREE_CODE, PREFERRED_LABEL]

def confirm_change(message):
    print(f"\n{message}")
    answer = input("Enter [y]es if the changes were intentional, [n]o if not: ")
    if answer.lower() in ["y","yes"]:
        return True 
    return False

def construct_pretty_label_for_row(internal_id, code, label):
    return f"{internal_id}: {label} ({code})"

# C01 + C02 + C03 -> C04
# C01, C02, and C03 become precursors to C04
# C05 -> C06 + C07 + C08
# C05 is a precursor to C06, C07, and C08
# you can have one concept be a precursor to many concepts
def get_all_precursors(csv_file):
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        precursor_id_to_internal_ids = defaultdict(set)
        for row in reader:
            if row[PRECURSORS]:
                for precursor_id in row[PRECURSORS].split(): # space separated
                    precursor_id_to_internal_ids[precursor_id].add(row[INTERNAL_ID])
        return precursor_id_to_internal_ids

# C01 + C02 + C03 -> C01
# C02 and CO3 become revocations in C01
# don't revoke anything with precursors (according to Rob's document "Oncotree History Modeling") - check that anything in revocations is not a precursor
# a concept can only be revoked by a pre-existing concept
def get_all_revocations(csv_file):
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        revocation_id_to_internal_ids = defaultdict(set)
        for row in reader:
            if row[REVOCATIONS]:
                for revocation_id in row[REVOCATIONS].split(): # space separated
                    revocation_id_to_internal_ids[revocation_id].add(row[INTERNAL_ID])
        return revocation_id_to_internal_ids   

def confirm_changes(original_oncotree, modified_oncotree, precursor_id_to_internal_ids, revocation_id_to_internal_ids):
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

    #print(removed_internal_ids)
    #print(new_internal_ids)
    #print(in_both_internal_ids)

    all_changes_are_intentional = True

    print("\nRemoved internal ids:")
    if removed_internal_ids:
        for internal_id in sorted(removed_internal_ids):
            data = original_oncotree[internal_id]
            pretty_label = construct_pretty_label_for_row(data[INTERNAL_ID], data[ONCOTREE_CODE], data[LABEL])
            print(f"\t{pretty_label}")
    else:
        print("\tNone")

    print("\nNew internal ids:")
    if new_internal_ids:
        for internal_id in sorted(new_internal_ids):
            data = modified_oncotree[internal_id]
            pretty_label = construct_pretty_label_for_row(data[INTERNAL_ID], data[ONCOTREE_CODE], data[LABEL])
            print(f"\t{pretty_label}")
    else:
        print("\tNone")

    print("\nPrecurors:")
    if precursor_id_to_internal_ids:
        for precursor_id in sorted(precursor_id_to_internal_ids.keys()):
            # are any current concepts precursors? they shouldn't be
            if precursor_id in modified_internal_id_set:
                print(f"Error: '{precursor_id}' is a precuror to '{','.join(precursor_id_to_internal_ids[precursor_id])}' but '{precursor_id}' is still in this file as a current record", file=sys.stderr)
                sys.exit(1)
            precursor_of_set = precursor_id_to_internal_ids[precursor_id]
            for internal_id in precursor_of_set:
                data = modified_oncotree[internal_id]
                pretty_label = construct_pretty_label_for_row(data[INTERNAL_ID], data[ONCOTREE_CODE], data[LABEL])
                print(f"\t'{precursor_id}' -> '{pretty_label}'")
    else:
        print("\tNone")

    print("\nRevocations:")
    if revocation_id_to_internal_ids: 
        for revocation_id in sorted(revocation_id_to_internal_ids.keys()):
            if revocation_id in modified_internal_id_set:
                print(f"Error: '{revocation_id}' has been revoked by '{','.join(revocation_id_to_internal_ids[revocation_id])}' but '{revocation_id}' is still in this file as a current record", file=sys.stderr)
                sys.exit(1)
            if revocation_id in precursor_id_to_internal_ids:
                print(f"Error: Revocation '{revocation_id}' cannot also be a precursor", file=sys.stderr)
                sys.exit(1)
            revocation_of_set = revocation_id_to_internal_ids[revocation_id]
            for internal_id in revocation_of_set: 
                if internal_id in new_internal_ids:
                    print(f"Error: '{revocation_id}' revokes '{internal_id}' but '{internal_id}' is a new concept. Only a pre-existing concept can revoke something", file=sys.stderr)
                    sys.exit(1)
                data = modified_oncotree[internal_id]
                pretty_label = construct_pretty_label_for_row(data[INTERNAL_ID], data[ONCOTREE_CODE], data[LABEL])
                print(f"\t'{revocation_id}' -> '{pretty_label}'")
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

def field_is_required(field, field_name, internal_id, csv_file):
    if not field:
        print(f"{field_name} is a required field, it is empty for the '{internal_id}' record in '{csv_file}'", file=sys.stderr)
        sys.exit(1)

def field_is_unique(field, field_name, column_set, internal_id, csv_file):
    if field in column_set:
        print(f"{field_name} must be unique.  There is more than one record with '{field}' in '{csv_file}'", file=sys.stderr)
        sys.exit(1)

def validate_csv_file(csv_file):
    # load all child->parent relationships
    # also check header and uniqueness and required values for some columns
    child_to_parent_resource_uris = {}
    child_to_parent_labels = {}
    child_uri_to_child_label = {} # make sure the parent uri + label match the child uri + label pair

    # these fields are required and must be unique
    resource_uri_set = set([])
    internal_id_set = set([])
    oncotree_code_set = set([])
    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        actual_header = reader.fieldnames
        if actual_header != EXPECTED_HEADER:
            print(f"ERROR: missing the following expected fields from input file '{csv_file}': {set(EXPECTED_HEADER) - set(actual_header)}", file=sys.stderr)
            sys.exit(1)

        for row in reader:
            # save child->parent relationships
            child_to_parent_resource_uris[row[RESOURCE_URI]] = row[PARENT_RESOURCE_URI] 
            child_uri_to_child_label[row[RESOURCE_URI]] = row[LABEL]
            child_to_parent_labels[row[LABEL]] = row[PARENT_LABEL] 

            # check all colunns are not empty
            required_fields = TISSUE_NODE_REQUIRED_FIELDS if row[ONCOTREE_CODE] == "TISSUE" else REQUIRED_FIELDS
            for field in required_fields:
                field_is_required(row[field], field, row[INTERNAL_ID], csv_file)  

            # check these columns are unique
            field_is_unique(row[RESOURCE_URI], RESOURCE_URI, resource_uri_set, row[INTERNAL_ID], csv_file)
            field_is_unique(row[INTERNAL_ID], INTERNAL_ID, internal_id_set, row[ONCOTREE_CODE], csv_file)
            field_is_unique(row[ONCOTREE_CODE], ONCOTREE_CODE, oncotree_code_set, row[INTERNAL_ID], csv_file)

            resource_uri_set.add(row[RESOURCE_URI])
            internal_id_set.add(row[INTERNAL_ID])
            oncotree_code_set.add(row[ONCOTREE_CODE])

    with open(csv_file, 'r', encoding='utf-8-sig') as file:
        reader = csv.DictReader(file)
        label_mismatch_errors = []
        parent_invalid_errors = []
        for row in reader:
            if row[LABEL] != row[PREFERRED_LABEL]:
                label_mismatch_errors.append(f"{row[INTERNAL_ID]}: '{row[LABEL]}' != '{row[PREFERRED_LABEL]}'")
            if row[ONCOTREE_CODE] != "TISSUE" and \
                 (row[PARENT_LABEL] not in child_to_parent_labels \
                  or row[PARENT_RESOURCE_URI] not in child_to_parent_resource_uris \
                  or (child_uri_to_child_label[row[PARENT_RESOURCE_URI]] != row[PARENT_LABEL])): # check that the parent URI + label pair match the child URI + label pair
 
                parent_invalid_errors.append(f"{row[INTERNAL_ID]}: URI '{row[PARENT_RESOURCE_URI]}' and label '{row[PARENT_LABEL]}'")

    if label_mismatch_errors:
        print(f"ERROR: '{LABEL}' and '{PREFERRED_LABEL}' columns must be identical.  Mis-matched fields in '{csv_file}':")
        for message in label_mismatch_errors:
            print(f"\t{message}")
    
    if parent_invalid_errors:
        print(f"ERROR: Invalid parents found in '{csv_file}'.  Either the parent '{PARENT_RESOURCE_URI}' or the parent '{PARENT_LABEL}' cannot be found in '{csv_file}', or the ('{PARENT_RESOURCE_URI}', '{PARENT_LABEL}') parent pair doesn't match the child  ('{RESOURCE_URI}', '{LABEL}') child pair.")
        for message in parent_invalid_errors:
            print(f"\t{message}")

    if label_mismatch_errors or parent_invalid_errors:
        sys.exit(1)

 
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
    precursor_id_to_internal_ids = get_all_precursors(modified_file)
    revocation_id_to_internal_ids = get_all_revocations(modified_file)
    confirm_changes(original_oncotree, modified_oncotree, precursor_id_to_internal_ids, revocation_id_to_internal_ids)
    output_rdf_file(modified_oncotree)

if __name__ == '__main__':
   main()
