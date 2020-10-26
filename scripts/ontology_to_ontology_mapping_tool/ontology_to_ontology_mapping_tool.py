# Copyright (c) 2020 Memorial Sloan-Kettering Cancer Center.
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

import sys
import os
import io
import argparse
import pandas as pd
import requests

ONTOLOGY_MAPPINGS_FILE_URL = "https://raw.githubusercontent.com/cBioPortal/oncotree/master/scripts/ontology_to_ontology_mapping_tool/ontology_mappings.txt"
VALID_ONCOTREE_CODES_URL = "https://raw.githubusercontent.com/cBioPortal/oncotree/master/resources/resource_uri_to_oncocode_mapping.txt"
ACCEPTED_ONTOLOGIES = ['ONCOTREE_CODE', 'UMLS_CODE', 'NCIT_CODE', 'ICDO_TOPOGRAPHY_CODE', 'HEMEONC_CODE', 'ICDO_MORPHOLOGY_CODE']
NULL_VALUES = ['', 'NA']

def add_comments_column_and_log_data(mapped_data, target_file, source_code, target_code, source_file):
    comments = []
    completely_resolved_codes = {}
    ambiguous_codes = {}
    unrecognized_codes = []
    many_to_one_codes = {}
    mapped_codes_count = 0
    unmapped_codes_count = 0

    columns_to_groupby = mapped_data.columns[:len(mapped_data.columns)-1]
    grouped_data = mapped_data.groupby(list(columns_to_groupby), sort=False)[target_code].unique().apply(', '.join).reset_index()

    oncotree_codes_list = pd.read_csv(io.StringIO(requests.get(VALID_ONCOTREE_CODES_URL).content.decode('utf-8')), sep='\t', header=None, keep_default_na=False)
    valid_oncotree_codes = oncotree_codes_list.loc[oncotree_codes_list[1] == "hasCode"][2].str.upper().tolist()

    for sc, tc in zip(grouped_data[source_code], grouped_data[target_code]):
        if tc == "": #one_to_none mapping
            if source_code == "ONCOTREE_CODE" and sc not in valid_oncotree_codes:
                comments.append("Invalid code")
            else:
                comments.append((lambda sc: 'No mapping found' if sc not in NULL_VALUES else '')(sc))
            unrecognized_codes.append(sc)
            if sc not in NULL_VALUES:
                unmapped_codes_count += 1
        elif len(tc.split(', ')) > 1: #one_to_many mapping
            comments.append('Maps to multiple codes')
            ambiguous_codes[sc] = tc
            mapped_codes_count += 1
        else: #one_to_one mapping
            comments.append('')
            completely_resolved_codes[sc] = tc
            mapped_codes_count += 1

    #Also print many_to_one mapping to Log file (Multiple source codes map to one target code)
    many_to_one = mapped_data.groupby(target_code, sort=False)[source_code].unique().apply(', '.join).reset_index()
    for tc, sc in zip(many_to_one[target_code], many_to_one[source_code]):
        if tc != '' and len(sc.split(', ')) > 1:
            many_to_one_codes[tc] = sc

    grouped_data['COMMENTS'] = comments
    grouped_data.to_csv(target_file, sep='\t', index=False)

    log_file = open(os.path.splitext(target_file)[0] + "_summary.html", 'w')
    log_file.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<title>Ontology Mapping Summary</title>\n<meta charset=\"UTF-8\">\n<style>\nbody {font-family:Arial; line-height:1.4}\n\n</style>\n</head><body>\n")
    log_file.write("<h1>Ontology Mapping Summary</h1>\n")
    log_file.write("Source Ontology: <b>%s</b><br>" % (source_code))
    log_file.write("Target Ontology: <b>%s</b><br><br>" % (target_code))
    log_file.write("Mapped <b>%s</b> to <b>%s</b><br><br>" % (source_code, target_code))
    log_file.write("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'><tr><td> Total Mapped Entries </td><td><b> %s </b></td></tr><tr><td> Total Unmapped Entries </td><td><b> %s </b></td></tr></table>" % (mapped_codes_count, unmapped_codes_count))

    if completely_resolved_codes:
        log_file.write("<hr><h2 id=\"one_to_one_mapping\">The following source codes mapped to one target code:</h2>\n")
        for code in completely_resolved_codes:
            log_file.write("<p><b>Source Code</b>: %s<br>\n" % (code))
            log_file.write("<b>Target Code</b>: %s<br>\n" % completely_resolved_codes[code])
    if ambiguous_codes:
        log_file.write("<hr><h2 id=\"one_to_many_mapping\">The following source codes mapped to multiple target codes:</h2>\n")
        for code in ambiguous_codes:
            log_file.write("<p><b>Source Code</b>: %s<br>\n" % (code))
            log_file.write("<b>Target Codes</b>: %s<br>\n" % ambiguous_codes[code])
    if unrecognized_codes:
        log_file.write("<hr><h2 id=\"no_match\">The following source codes have no mapping available:</h2>\n")
        for code in unrecognized_codes:
            log_file.write("<p><b>Source Code: %s</b><br>\n" % ("&lt;blank&gt;" if len(code) == 0 else code))
            log_file.write("<b>Mapping not available for the code\n")
    if many_to_one_codes:
        log_file.write("<hr><h2 id=\"many_to_one_mapping\">The following target codes mapped to multiple source codes:</h2>\n")
        for code in many_to_one_codes:
            log_file.write("<p><b>Target Code</b>: %s<br>\n" % (code))
            log_file.write("<b>Source Codes</b>: %s<br>\n" % many_to_one_codes[code])

    log_file.close()

#Check if any of the ontology code headers are present in the clinical file
def validate_arguments(source_file, source_code, target_code):
    #1. Check if the source_code input by the user is valid.
    if source_code not in ACCEPTED_ONTOLOGIES:
        print("Invalid source code: \'%s\'. \nThe list of acceptable codes are:" % (source_code))
        for code in ACCEPTED_ONTOLOGIES:
            print(code)
        sys.exit(1)

    #2. Check if the target_code input by the user is valid.
    if target_code not in ACCEPTED_ONTOLOGIES:
        print("Invalid target code: \'%s\'. \nThe list of acceptable codes are:" % (target_code))
        for code in ACCEPTED_ONTOLOGIES:
            print(code)
        sys.exit(1)

    #3. Check if source_code and target_code are not the same
    if source_code == target_code:
        print("The source and target ontology columns to be mapped are the same.\nSource Ontology: %s\nTarget Ontology: %s" % (source_code, target_code))
        print("Please specify different ontologies to map on.\n")
        sys.exit(1)

    #4. CHeck if the user input file has the source code column.
    source_file.columns = map(str.upper, source_file.columns)
    if source_code not in source_file.columns:
        print("\nThe input file does not contain any ontology code columns or the headers do not match to accepted values. Please check. \n\nThe list of acceptable ontology headers are:")
        for code in ACCEPTED_ONTOLOGIES:
            print(code)
        print('\n')
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--source-file', required = True, help = 'This is the source file path. The source file must contain one of the ONCOTREE_CODE, NCIT_CODE, UMLS_CODE, ICDO_TOPOGRAPHY_CODE, ICDO_MORPHOLOGY_CODE or HEMEONC_CODE in the file header and it must contain codes corresponding to the Ontology System.', type = str)
    parser.add_argument('-o', '--target-file', required = True, help = 'This is the path to the target file that will be generated. It will contain the ontology mappings of source code in <source-file> to <target-code>.', type = str)
    parser.add_argument('-s', '--source-code', required = True, help = "This is the source ontology code in <source-file>. It must be one of the ONCOTREE_CODE, NCIT_CODE, UMLS_CODE, ICDO_TOPOGRAPHY_CODE, ICDO_MORPHOLOGY_CODE or HEMEONC_CODE.", type = str)
    parser.add_argument('-t', '--target-code', required = True, help = "This is the target ontology code that the script will attempt to map the source ontology code to. It must be one of the ONCOTREE_CODE, NCIT_CODE, UMLS_CODE, ICDO_TOPOGRAPHY_CODE, ICDO_MORPHOLOGY_CODE or HEMEONC_CODE.", type = str)
    args = parser.parse_args()

    target_file = args.target_file
    source_code = args.source_code.upper()
    target_code = args.target_code.upper()

    source_file = pd.read_csv(args.source_file, comment='#', sep='\t', header=0, keep_default_na=False).applymap(str) #HEMEONC_CODES are numbers.
    validate_arguments(source_file, source_code, target_code)

    oncotree_code_source_data = requests.get(ONTOLOGY_MAPPINGS_FILE_URL).content
    mappings_file = pd.read_csv(io.StringIO(oncotree_code_source_data.decode('utf-8')), sep='\t', header=0, keep_default_na=False).applymap(str)

    #For case insensitive merge.
    source_file[source_code] = source_file[source_code].str.upper()
    mappings_file[source_code] = mappings_file[source_code].str.upper()

    mapped_data = pd.merge(source_file, mappings_file[[source_code, target_code]], on=source_code, suffixes= ('_source_file', ''), sort=False, how='left').fillna('')
    print("Mapped the %s to %s.." % (source_code, target_code))

    add_comments_column_and_log_data(mapped_data, target_file, source_code, target_code, source_file)
    print("\nThe ontology mappings are written to: %s" % (target_file))
    print("The mapping summary is written to : %s" % (os.path.splitext(target_file)[0] + "_summary.html"))

if __name__ == '__main__':
    main()
