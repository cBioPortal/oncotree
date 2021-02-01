#!/bin/python

import os
import sys
import linecache
import csv

oncotree_code_converter_output = sys.argv[1]
if not os.path.exists(oncotree_code_converter_output):
    sys.stderr.write("Error, specified file not found: %s\n" % (oncotree_code_converter_output))
    sys.exit(1)

REQUIRED_HEADERS = ["ONCOTREE_CODE", "CANCER_TYPE", "CANCER_TYPE_DETAILED"]

# returns a dictionary from column name to list of column values
# exits if one of the columns has empty value
def get_required_columns(oncotree_code_converter_output):
    with open(oncotree_code_converter_output) as file:
        dictreader = csv.DictReader(file, delimiter = "\t")
        to_return = {header : [] for header in REQUIRED_HEADERS}
        for line in dictreader:
            for header in REQUIRED_HEADERS:
                try:
                    # add column value if value is not None or "" blank
                    # exits with error if whitespace/None is found (oncotree_code_converter should prevent this)
                    # exits with error if KeyError (one of the required headers wasn't written out)
                    if line[header].rstrip():
                        to_return[header].append(line[header])
                    else:
                        sys.stderr.write("Error, missing value in column %s\n" % (header))
                        sys.exit(1)
                except KeyError:
                    sys.stderr.write("Error, output file is missing some of the required header: %s\n" % (header))
                    sys.exit(1)
    return to_return

# checks if all returned column values are NA, if so, maybe indicate issue with returned data schema
columns_to_check = get_required_columns(oncotree_code_converter_output)
for column_name, column_values in columns_to_check.items():
    if all([True if column_value == "NA" else False for column_value in column_values]):
        sys.stderr.write("Error, all column values in %s are NA, please check returned OncoTree schema\n" % (column_name))
        sys.exit(1)
