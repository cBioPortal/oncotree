# run all unit tests with:
#     scripts> python -m unittest discover
#
# Author: Manda Wilson

import unittest

from oncotree_to_oncotree import *

class TestCrossVersionOncotreeTranslator(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.original_version = cls.get_original_version()
        for code in (cls.original_version):
            GLOBAL_LOG_MAP[code] = {
                NEIGHBORS_FIELD : [],
                CHOICES_FIELD : [],
                CLOSEST_COMMON_PARENT_FIELD : "",
                IS_LOGGED_FLAG : False
            }
        cls.latest_version = cls.get_latest_version()
        for code in (cls.latest_version):
            GLOBAL_LOG_MAP[code] = {
                NEIGHBORS_FIELD : [],
                CHOICES_FIELD : [],
                CLOSEST_COMMON_PARENT_FIELD : "",
                IS_LOGGED_FLAG : False
            }

    # ---------------------------------------------------------------------------------
    # Original Code: SEZS (Child: FAKE_OLD_SS_CHILD)
    # New Code: SS Child: FAKE_OLD_SS_CHILD, FAKE_NEW_SS_CHILD)
    # Code is mapped through history. One new child is introduced in the future version (SS)
    # Tests simple direct mapping with and without introduction of child (forward, backward)

    # get possible codes (jumping forward through history)
    def test_get_possible_oncotree_code_forwards_history(self):
        self.run_get_possible_oncotree_code_test("SEZS", set(["SS"]), False)

    # get possible codes (jumping backwards through history)
    def test_get_possible_oncotree_code_backwards_history(self):
        self.run_get_possible_oncotree_code_test("SS", set(["SEZS"]), True)

    # get resolved string - single direct mapping, new children available
    def test_resolve_single_future_possible_target_oncotree_code_with_new_child(self):
        self.run_resolve_oncotree_codes_test("SEZS", set(["SS"]), False, format_oncotree_code_options("SEZS", "{SS}", 1), False)

    # get resolved string - single direct mapping, no new children
    def test_resolve_single_past_possible_target_oncotree_code(self):
        self.run_resolve_oncotree_codes_test("SS", set(["SEZS"]), True, "SEZS", True)

    def test_get_number_of_new_children_backwards(self):
        self.run_get_number_of_new_children_test("SS", set(["SEZS"]), True, 0)

    def test_get_number_of_new_children_fowards(self):
        self.run_get_number_of_new_children_test("SEZS", set(["SS"]), False, 1)
    # ---------------------------------------------------------------------------------
    # Original Code: ALL (Children: BALL, TALL, DALL)
    # New Code: BLL, TLL, DLL (No children)
    # Original code's children are precursors/history to new codes: precursor (BALL -> BLL, TALL -> TLL), history (DALL -> DLL)
    # ALL is in BLL, TLL revocations NOT DLL

    # ALL forward should map to "BLL" and "TLL" (not "DLL"), reverse mapping should not return "ALL"
    def test_get_possible_oncotree_code_forwards_two_revocations(self):
        self.run_get_possible_oncotree_code_test("ALL", set(["BLL", "TLL"]), False)

    def test_get_possible_oncotree_code_forwards_revocation_and_precursor(self):
        self.run_get_possible_oncotree_code_test("BALL", set(["BLL"]), False)

    def test_get_possible_oncotree_code_forwards_revocation_and_history(self):
        self.run_get_possible_oncotree_code_test("DALL", set(["DLL"]), False)

    # testing revocation - "BLL" should not include "ALL" as option
    def test_get_possible_oncotree_code_backwards_revocation_and_precursor(self):
        self.run_get_possible_oncotree_code_test("BLL", set(["BALL"]), True)

    def test_get_possible_oncotree_code_backwards_revocation_and_history(self):
        self.run_get_possible_oncotree_code_test("DLL", set(["DALL"]), True)

    def test_resolve_multiple_future_possible_target_oncotree_codes_no_new_children(self):
        self.run_resolve_oncotree_codes_test("ALL", set(["BLL", "TLL"]), False, format_oncotree_code_options("ALL", "{BLL,TLL}", 0), False)
    # ---------------------------------------------------------------------------------
    # Original Code: GMUCM
    # New Code: URMM
    # Original code was revoked and replaced with URMM
    # URMM is effectively a new node and cannot be mapped directly back

    def test_get_possible_oncotree_code_backwards_revocation(self):
        self.run_get_possible_oncotree_code_test("URMM", set([]), True)

    def test_get_possible_oncotree_code_forwards_revocations(self):
        self.run_get_possible_oncotree_code_test("GMUCM", set(["URMM"]), False)

    # going backwards - no choices, search neighborhood - skip BLADDER because not mappable - map non-immediate neighbor TISSUE
    def test_resolve_no_past_possible_target_oncotree_codes(self):
        self.run_resolve_oncotree_codes_test("URMM", set(), True, format_oncotree_code_options("URMM", "Neighborhood: TISSUE", 0), False)
    # ---------------------------------------------------------------------------------
    # Original Code: CLL, SLL
    # New Code: CLLSLL
    # Original code was combined into CLLSLL (both are precursors)
    # CLLSLL can be mapped back to either

    def test_get_possible_oncotree_code_backwards_merged_precusors(self):
        self.run_get_possible_oncotree_code_test("CLLSLL", set(["CLL", "SLL"]), True)

    def test_get_possible_oncotree_code_forwards_merged_precusors(self):
        self.run_get_possible_oncotree_code_test("CLL", set(["CLLSLL"]), False)

    def test_resolve_multiple_past_possible_target_oncotree_codes(self):
        self.run_resolve_oncotree_codes_test("CLLSLL", set(["CLL", "SLL"]), True, format_oncotree_code_options("CLLSLL", "{CLL,SLL}", 0), False)
    # ---------------------------------------------------------------------------------
    # Original Code: PTCL, PTCLNOS
    # New Code: PTCL (originally PTCLNOS)
    # PTCL (the meaning) was revoked - PTCLNOS (the meaning) stayed but was renamed to PTCL

    def test_get_possible_oncotree_code_forwards_revoked_and_renamed(self):
        self.run_get_possible_oncotree_code_test("PTCL", set(["PTCL"]), False)

    def test_get_possible_oncotree_code_forwards_revoked_and_renamed(self):
        self.run_get_possible_oncotree_code_test("PTCLNOS", set(["PTCL"]), False)

    # in this test, we see that the revoked parent node (PTCL) is not chosen as a valid backwards mapping. Only the history (PTCLNOS) of the test node (PTCL) is considered valid
    def test_get_possible_oncotree_code_backwards_revoked_and_renamed(self):
        self.run_get_possible_oncotree_code_test("PTCL", set(["PTCLNOS"]), True)
    # ---------------------------------------------------------------------------------
    # Original Code: TNKL (parent), CTCL, TNKL_CHILD, PTCL (children)
    # New Code: TNKL gone, CTCL renmaed to MYCF, PTCL revoked by PTCLNOS (renamed PTCL), TNKL_CHILD renamed to TNKL
    # TNKL can't be mapped forward - possible set should be 0

    # Tests for TNKL
    def test_get_no_possible_oncotree_code_forwards_multiple_children(self):
        self.run_get_possible_oncotree_code_test("TNKL", set(), False)

    def test_resolve_multiple_past_possible_target_oncotree_codes(self):
        self.run_resolve_oncotree_codes_test("TNKL", set(), False, format_oncotree_code_options("TNKL", "Neighborhood: MYCF,PTCL,TISSUE,TNKL_CHILD2", 0), False)

    def test_get_possible_oncotree_code_forwards_precursors(self):
        self.run_get_possible_oncotree_code_test("CTCL", set(["MYCF"]), False)

    def test_get_possible_oncotree_code_backwards_precursors(self):
        self.run_get_possible_oncotree_code_test("MYCF", set(["CTCL"]), True)
    # ---------------------------------------------------------------------------------
    # Tests for getting valid neighboring OncoTree codes

    # TNKL_NEW_CHILD parent MTNN is not in target (original) version
    # TNKL_GRANDCHILD is in both
    def test_get_neighbor_invalid_parent_valid_children(self):
        self.run_get_neighboring_target_oncotree_codes_test(["TNKL_NEW_CHILD"], True, set(["TNKL_GRANDCHILD"]))

    # TNKL_NEW_CHILD2 parent MTNN is not in target (original)  version
    # skip to grandparent "TISSUE"
    # no children
    def test_get_neighbor_invalid_parent_invalid_children(self):
        self.run_get_neighboring_target_oncotree_codes_test(["TNKL_NEW_CHILD2"], True, set(["TISSUE"]))

    # FAKE_NEW_SS_CHILD has no children
    # parent ("SS") maps back to "SEZS" in target version
    def test_get_neighbor_valid_parent_invalid_children(self):
        self.run_get_neighboring_target_oncotree_codes_test(["FAKE_NEW_SS_CHILD"], True, set(["SEZS"]))

    # BLADDER has children CLLSLL, URMM
    # URMM revoked GMUCM (GMUCM is no longer valid) - don't map back
    # CLLSLL maps back to CLL or SLL
    def test_get_neighbor_partially_valid_children_valid_parent(self):
        self.run_get_neighboring_target_oncotree_codes_test(["BLADDER"], True, set(["CLL", "SLL", "TISSUE"]))
    # ---------------------------------------------------------------------------------
    # Tests for getting closest common parent for a set of nodes

    def test_get_closest_common_parent_for_distant_nodes(self):
        self.run_get_closest_common_parent_test(["TNKL_GRANDCHILD", "CLL", "PTCLNOS", "MEL"], self.original_version, "TISSUE")

    def test_get_closest_common_parent_for_parent_child_nodes(self):
        self.run_get_closest_common_parent_test(["SEZS", "CTCL", "FAKE_OLD_SS_CHILD"], self.original_version, "CTCL")

    def test_get_closest_common_parent_for_related_child_nodes(self):
        self.run_get_closest_common_parent_test(["TALL", "BALL", "DALL"], self.original_version, "ALL")
    # ---------------------------------------------------------------------------------
    # Test functions
    def run_get_possible_oncotree_code_test(self, test_oncotree_code, expected_possible_oncotree_codes, is_backwards_mapping):
        source_version = self.original_version if not is_backwards_mapping else self.latest_version
        target_version = self.latest_version if not is_backwards_mapping else self.original_version
        actual_output = get_possible_target_oncotree_codes(source_version[test_oncotree_code], target_version, is_backwards_mapping)
        self.assertEqual(expected_possible_oncotree_codes, actual_output)

    def run_resolve_oncotree_codes_test(self, source_oncotree_code, possible_target_oncotree_codes, is_backwards_mapping, expected_oncotree_code_option, expected_is_easily_resolved):
        source_version = self.original_version if not is_backwards_mapping else self.latest_version
        target_version = self.latest_version if not is_backwards_mapping else self.original_version
        actual_oncotree_code_option, actual_is_easily_resolved = resolve_possible_target_oncotree_codes(source_oncotree_code, possible_target_oncotree_codes, source_version, target_version, is_backwards_mapping)
        self.assertEqual(expected_is_easily_resolved, actual_is_easily_resolved)
        # since neighborhood returns a set with no order, check contents instead
        # XYZ -> Neighborhood: X,Y,Z
        # split ['XYZ -> Neighborhood', 'X,Y,Z']
        # split ['X,Y,Z'] to ['X', 'Y', 'Z']
        if "Neighborhood" in expected_oncotree_code_option:
            expected_oncotree_codes = expected_oncotree_code_option.split("Neighborhood: ")[1].split(",")
            actual_oncotree_codes = actual_oncotree_code_option.split("Neighborhood: ")[1].split(",")
            self.assertEqual(set(expected_oncotree_codes), set(actual_oncotree_codes))
        else:
            self.assertEqual(expected_oncotree_code_option, actual_oncotree_code_option)

    def run_get_neighboring_target_oncotree_codes_test(self, source_oncotree_codes, is_backwards_mapping, expected_neighbors):
        source_version = self.original_version if not is_backwards_mapping else self.latest_version
        target_version = self.latest_version if not is_backwards_mapping else self.original_version
        actual_neighbors = get_neighboring_target_oncotree_codes(source_oncotree_codes, source_version, target_version, True, is_backwards_mapping)
        self.assertEqual(expected_neighbors, actual_neighbors)

    def run_get_number_of_new_children_test(self, source_oncotree_code, possible_target_oncotree_codes, is_backwards_mapping, expected_number_of_new_children):
        source_version = self.original_version if not is_backwards_mapping else self.latest_version
        target_version = self.latest_version if not is_backwards_mapping else self.original_version
        actual_number_of_new_children = get_number_of_new_children(source_oncotree_code, possible_target_oncotree_codes, source_version, target_version)
        self.assertEqual(expected_number_of_new_children, actual_number_of_new_children)

    def run_get_closest_common_parent_test(self, possible_target_oncotree_codes, target_oncotree, expected_closest_common_parent):
        actual_closest_common_parent = get_closest_common_parent(possible_target_oncotree_codes, target_oncotree)
        self.assertEqual(expected_closest_common_parent, actual_closest_common_parent)

    @classmethod
    def get_original_version(cls):
        return {
            "ALL" : {"code": "ALL", "children": ["TALL", "BALL", "DALL"], "parent": "LEUK", "revocations": [], "precursors": [], "history": []},
            "BALL" : {"code": "BALL", "children": [], "parent": "ALL", "revocations": [], "precursors": [], "history": []},
            "CLL" : {"code": "CLL", "children": [], "parent": "LEUK", "revocations": [], "precursors": [], "history": []},
            "CTCL" : {"code": "CTCL", "children": ["SEZS"], "parent": "TNKL", "revocations": [], "precursors": [], "history": []},
            "DALL" : {"code": "DALL", "children": [], "parent": "ALL", "revocations": [], "precursors": [], "history": []},
            "FAKE_OLD_SS_CHILD" : {"code": "FAKE_OLD_SS_CHILD", "children": [], "parent": "SEZS", "revocations": [], "precursors": [], "history": [""]},
            "GMUCM" : {"code": "GMUCM", "children": [], "parent": "MEL", "revocations": [], "precursors": [], "history": []},
            "LEUK" : {"code": "LEUK", "children": ["ALL", "CLL"], "parent": "TISSUE", "revocations": [], "precursors": [], "history": []},
            "MEL" : {"code": "MEL", "children": ["GMUCM", "SLL"], "parent": "TISSUE", "revocations": [], "precursors": [], "history": []},
            "PTCL" : {"code": "PTCL", "children": ["PCTLNOS"], "parent": "TNKL", "revocations": [], "precursors": [], "history": []},
            "PTCLNOS" : {"code": "PTCLNOS", "children": [], "parent": "PTCL", "revocations": [], "precursors": [], "history": []},
            "SEZS" : {"code": "SEZS", "children": ["FAKE_OLD_SS_CHILD"], "parent": "CTCL", "revocations": [], "precursors": [], "history": []},
            "SLL" : {"code": "SLL", "children": [], "parent": "MEL", "revocations": [], "precursors": [], "history": []},
            "TALL" : {"code": "TALL", "children": [], "parent": "ALL", "revocations": [], "precursors": [], "history": []},
            "TISSUE" : {"code": "TISSUE", "children": ["LEUK", "MEL", "TNKL"], "parent": "", "revocations": [], "precursors": [], "history": []},
            "TNKL" : {"code": "TNKL", "children": ["PTCL", "CTCL", "TNKL_CHILD"], "parent": "TISSUE", "revocations": [], "precursors": [], "history": []},
            "TNKL_CHILD" : {"code": "TNKL_CHILD", "children": [], "parent": "TNKL", "revocations": [], "precursors": [], "history": []},
            "TNKL_GRANDCHILD" : {"code": "TNKL_GRANDCHILD", "children": [], "parent": "TNKL_CHILD", "revocations": [], "precursors": [], "history": []}
        }

    @classmethod
    def get_latest_version(cls):
        return {
            "BLADDER" : {"code": "BLADDER", "children": ["URMM", "CLLSLL"], "parent": "TISSUE", "revocations": [], "precursors": [], "history": []},
            "BLL" : {"code": "BLL", "children": [], "parent": "LNM", "revocations": ["ALL"], "precursors": ["BALL"], "history": []},
            "CLLSLL" : {"code": "CLLSLL", "children": [], "parent": "BLADDER", "revocations": [], "precursors": ["SLL", "CLL"], "history": []},
            "DLL" : {"code": "DLL", "children": [], "parent": "LNM", "revocations": [], "precursors": [], "history": ["DALL"]},
            "FAKE_NEW_SS_CHILD" : {"code": "FAKE_NEW_SS_CHILD", "children": [], "parent": "SS", "revocations": [], "precursors": [], "history": [""]},
            "FAKE_OLD_SS_CHILD" : {"code": "FAKE_OLD_SS_CHILD", "children": [], "parent": "SS", "revocations": [], "precursors": [], "history": [""]},
            "LNM" : {"code": "LNM", "children": ["BLL"], "parent": "TISSUE", "revocations": [], "precursors": [], "history": []},
            "MTNN" : {"code": "MTNN", "children": ["SS", "PTCL", "MYCF"], "parent": "TISSUE", "revocations": [], "precursors": [], "history": []},
            "MYCF" : {"code": "MYCF", "children": [], "parent": "MTNN", "revocations": [], "precursors": ["CTCL"], "history": []},
            "PTCL" : {"code": "PTCL", "children": [], "parent": "MTNN", "revocations": ["PTCL"], "precursors": [], "history": ["PTCLNOS"]},
            "SS" : {"code": "SS", "children": ["FAKE_NEW_SS_CHILD", "FAKE_OLD_SS_CHILD"], "parent": "MTNN", "revocations": [], "precursors": [], "history": ["SEZS"]},
            "TLL" : {"code": "TLL", "children": [], "parent": "LNM", "revocations": ["ALL"], "precursors": ["TALL"], "history": []},
            "TISSUE" : {"code": "TISSUE", "children": ["MTNN", "BLADDER", "LNM"], "parent": "", "revocations": [], "precursors": [], "history": []},
            "TNKL_GRANDCHILD" : {"code": "TNKL_GRANDCHILD", "children": [], "parent": "TNKL_NEW_CHILD", "revocations": [], "precursors": [], "history": []},
            "TNKL_NEW_CHILD" : {"code": "TNKL_NEW_CHILD", "children": ["TNKL_GRANDCHILD"], "parent": "MTNN", "revocations": [], "precursors": [], "history": [""]},
            "TNKL_NEW_CHILD2" : {"code": "TNKL_NEW_CHILD2", "children": [], "parent": "MTNN", "revocations": [], "precursors": [], "history": [""]},
            "TNKL_CHILD2" : {"code": "TNKL_CHILD2", "children": ["TNKL_GRANDCHILD"], "parent": "MTNN", "revocations": [], "precursors": [], "history": ["TNKL_CHILD"]},
            "URMM" : {"code": "URMM", "children": [], "parent": "BLADDER", "revocations": ["GMUCM"], "precursors": [], "history": []}
        }

if __name__ == '__main__':
    unittest.main()
