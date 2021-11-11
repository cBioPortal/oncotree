#!/usr/bin/env python

import sys
 
class Error(Exception):
    """Base class for exceptions in this module."""
    pass

class RdfUriAmbiguityError(Error):
    """Exception raised when the set of concept URI contains ambiguities when simplified

    Attributes:
        simplified_uri -- the simple form of the concept URI (stripping away hostname and all path)
        message -- explanation of the error
    """
    def __init__(self, simplified_uri, message):
        self.simplified_uri = simplified_uri
        self.message = message

class RdfUriDuplicationError(Error):
    """Exception raised when a concept URI is used for two separate concept definitions

    Attributes:
        concept_uri -- the concept URI
    """
    def __init__(self, concept_uri):
        self.concept_uri = concept_uri

class RdfUndefinedParentLinkError(Error):
    """Exception raised when one or more concept URI has a broader property linking to an undefined parent concept

    Attributes:
        concept_uri -- the concept URI
        parent_uri_without_definition -- the linked parent URI which is undefined
    """
    def __init__(self, children_concept_uri_list, parent_uri_without_definition):
        self.children_concept_uri_list = children_concept_uri_list
        self.parent_uri_without_definition = parent_uri_without_definition

def get_lines_from_file(inputfilename):
    """Reads all lines in a file and returns them as a list
    """
    inputfile = open(inputfilename, "r")
    lines = inputfile.readlines()
    inputfile.close()
    return lines

def line_begins_concept_block(line):
    """Returns True if the passed line looks like the start of an rdf OWL concept block
    """
    return line.strip().startswith("<rdf:Description rdf:about=\"")

def line_ends_concept_block(line):
    """Returns True if the passed line looks like the end of an rdf OWL concept block
    """
    return line.strip().startswith("</rdf:Description>")

def get_concept_line_blocks(lines):
    """Scans a list of rdf lines and extracts a list of contained rdf OWL concept blocks. Non concept entities are discarded.
    """
    concept_line_blocks = []
    next_block = []
    inside_block = False
    for line in lines:
        if inside_block:
            next_block.append(line)
            if line_ends_concept_block(line):
                concept_line_blocks.append(next_block)
                next_block = []
                inside_block = False
        else:
            if line_begins_concept_block(line):
                next_block.append(line)
                inside_block = True
    if inside_block:
        sys.stderr.write("Error : concept block not terminated:\n" + next_block + "\n")
        sys.exit(1)
    return concept_line_blocks

def get_concept_id_from_block_beginning(line):
    """Extracts the concept definition URI from an rdf OWL concept block first line
    """
    no_prefix_line = line.strip()[28:]
    quote_pos = no_prefix_line.find('"')
    if quote_pos == -1:
        sys.stderr.write("Error : malformed concept uri : " + line + "\n")
        sys.exit(1)
    return no_prefix_line[0:quote_pos]

def line_is_concept_parent_line(line):
    """Returns True if the line looks like it links to the parent concept of the current concept.
    """
    return line.strip().startswith("<skos:broader rdf:resource=\"");

def get_parent_concept_id_from_parent_line(line):
    """Extracts the concept link URI from an rdf OWL parent line
    """
    no_prefix_line = line.strip()[28:]
    quote_pos = no_prefix_line.find('"')
    if quote_pos == -1:
        sys.stderr.write("Error : malformed broader concept uri : " + line + "'" + no_prefix_line + "'" + "\n")
        sys.exit(1)
    return no_prefix_line[0:quote_pos]

def get_parent_concept_id_from_block(concept_block):
    """Scans a concept block and locates and returns the URL of a linked parent concept, or None if not found
    """
    for line in concept_block:
        if line_is_concept_parent_line(line):
            return get_parent_concept_id_from_parent_line(line)
    return None

def get_concept_uri_to_child_uri_list_map(concept_blocks):
    """Scans all concept blocks in an rdf OWL file and returns a map from parent concept URI to [ list of children concept URIs - maybe empty ]
    """
    concept_to_child_map = {}
    all_defined_concepts = set()
    for concept_block in concept_blocks:
        child_concept_id = get_concept_id_from_block_beginning(concept_block[0])
        if child_concept_id in all_defined_concepts:
            raise RdfUriDuplicationError(child_concept_id)
        all_defined_concepts.add(child_concept_id)
        parent_concept_id = get_parent_concept_id_from_block(concept_block)
        if (parent_concept_id):
            if parent_concept_id in concept_to_child_map:
                concept_to_child_map[parent_concept_id].append(child_concept_id)
            else:
                children = []
                children.append(child_concept_id)
                concept_to_child_map[parent_concept_id] = children
    # verify each parent seen is defined too
    for parent in concept_to_child_map:
        if parent not in all_defined_concepts:
            raise RdfUndefinedParentLinkError(concept_to_child_map[parent], parent)
    # put childless nodes into map with empty child list
    for concept in all_defined_concepts:
        if concept not in concept_to_child_map:
            concept_to_child_map[concept] = []
    return concept_to_child_map

def strip_uri_host_and_path(concept_uri):
    """remotes the host and path from a URI, returning only the final resource name
    """
    if concept_uri is None:
        return None
    rightmost_slash_position = concept_uri.rfind('/')
    rightmost_colon_position = concept_uri.rfind(':')
    simplified_start = max(0, rightmost_slash_position + 1, rightmost_colon_position + 1)
    return concept_uri[simplified_start:]

def get_simple_concept_uri_to_full_uri_map(concept_uri_list):
    """Scans a list of concept URI and returns a map from simple URI (dropping all path prefix) to full URI (including the path prefix)
    """
    simple_uri_to_full_uri_map = {}
    for concept_uri in concept_uri_list:
        simple_uri = strip_uri_host_and_path(concept_uri)
        if simple_uri in simple_uri_to_full_uri_map:
            existing_uri_with_simple_uri = simple_uri_to_full_uri_map[simple_uri]
            message = "the input RDF file contains these two concept URI which both have the same simplified URI : '%s' , '%s'" % (existing_uri_with_simple_uri , concept_uri)
            raise RdfUriAmbiguityError(simple_uri, message)
        simple_uri_to_full_uri_map[simple_uri] = concept_uri
    return simple_uri_to_full_uri_map

def usage():
    sys.stderr.write("usage : " + sys.argv[0] + " input_filename Uri1 Uri2 ... (Uri like ONC000001)\n")

def exit_if_not_all_filter_uri_are_present(filter_uri_set, simple_to_full_uri_map):
    missing_uri_list = []
    for filter_uri in filter_uri_set:
        if filter_uri not in simple_to_full_uri_map:
            missing_uri_list.append(filter_uri)
    if missing_uri_list:
        sys.stderr.write("error : the following URI were specified to be filtered but do not exist in the RDF file : " + ",".join(missing_uri_list) + "\n")
        sys.exit(1)

def exit_if_any_filter_uri_has_children(filter_uri_set, concept_to_child_map, simple_to_full_uri_map):
    uri_with_children_list = []
    for filter_uri in filter_uri_set:
        filter_full_uri = simple_to_full_uri_map[filter_uri]
        children_list = concept_to_child_map[filter_full_uri]
        if children_list:
            uri_with_children_list.append(filter_uri)
    if uri_with_children_list:
        sys.stderr.write("error : the following URI were specified to be filtered but have children which would be orphaned in the RDF file : " + ",".join(uri_with_children_list) + "\n")
        sys.exit(1)

def print_filtered_file(filelines, filter_uri_set, simple_to_full_uri_map):
    filter_full_uri_set = set()
    for filter_uri in filter_uri_set:
        filter_full_uri_set.add(simple_to_full_uri_map[filter_uri])
    filter_active = False
    for line in filelines:
        if not filter_active:
            if line_begins_concept_block(line):
                concept_id = get_concept_id_from_block_beginning(line)
                if concept_id in filter_full_uri_set:
                    filter_active = True
            if not filter_active:
                sys.stdout.write(line)
        else:
            if line_ends_concept_block(line):
                filter_active = False

def main():
    try:
        if len(sys.argv) < 2:
            usage()
            sys.exit(1)
        filename = sys.argv[1]
        filter_uri_set = set(sys.argv[2:])
        filelines = get_lines_from_file(filename)
        concept_blocks = get_concept_line_blocks(filelines)
        concept_to_child_map = get_concept_uri_to_child_uri_list_map(concept_blocks)
        simple_to_full_uri_map = get_simple_concept_uri_to_full_uri_map(concept_to_child_map.keys())
        exit_if_not_all_filter_uri_are_present(filter_uri_set, simple_to_full_uri_map)
        exit_if_any_filter_uri_has_children(filter_uri_set, concept_to_child_map, simple_to_full_uri_map)
        print_filtered_file(filelines, filter_uri_set, simple_to_full_uri_map)
    except RdfUriAmbiguityError as e:
        sys.stderr.write("RdfUriAmbiguityError :\n")
        sys.stderr.write("  simplified_uri : " + str(e.simplified_uri) + "\n")
        sys.stderr.write("  message : " + str(e.message) + "\n")
        return 1
    except RdfUriDuplicationError as e:
        sys.stderr.write("RdfUriDuplicationError :\n")
        sys.stderr.write("  concept_uri : " + str(e.concept_uri) + "\n")
        return 1
    except RdfUndefinedParentLinkError as e:
        sys.stderr.write("RdfUndefinedParentLinkError :\n")
        sys.stderr.write("  children_concept_uri_list : " + str(e.children_concept_uri_list) + "\n")
        sys.stderr.write("  parent_uri_without_definition : " + str(e.parent_uri_without_definition) + "\n")
        return 1
    return 0

if __name__ == "__main__":
    sys.exit(main())
