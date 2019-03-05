# run all unit tests with:
#     scripts> python -m unittest discover
#
# Author: Manda Wilson

import unittest

from cross_version_oncotree_translator import *

class TestCrossVersionOncotreeTranslator(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.original_version = cls.get_original_version()
        cls.latest_version = cls.get_latest_version()

    def test_convert_to_target_oncotree_code_forwards_history(self):
        self.run_convert_to_target_oncotree_code_test("SEZS", ["SS"], False)

    def test_convert_to_target_oncotree_code_forwards_revocations(self):
        self.run_convert_to_target_oncotree_code_test("PTCL", ["PTCL"], False)

    def test_convert_to_target_oncotree_code_forwards_revocations2(self):
        self.run_convert_to_target_oncotree_code_test("PTCLNOS", ["PTCL"], False)

    def test_convert_to_target_oncotree_code_forwards_revocations3(self):
        self.run_convert_to_target_oncotree_code_test("GMUCM", ["URMM"], False)

    def test_convert_to_target_oncotree_code_forwards_precursors(self):
        self.run_convert_to_target_oncotree_code_test("CTCL", ["MYCF"], False)

    def test_convert_to_target_oncotree_code_forwards_precursors_and_revocations(self):
        self.run_convert_to_target_oncotree_code_test("BALL", ["BLL"], False)

    def test_convert_to_target_oncotree_code_backwards_history(self):
        self.run_convert_to_target_oncotree_code_test("SS", ["SEZS"], True)

    # in this test, we see that the revoked parent node (PTCL) is not chosen as a valid backwards mapping. Only the history (PTCLNOS) of the test node (PTCL) is considered valid
    def test_convert_to_target_oncotree_code_backwards_revocations(self):
        self.run_convert_to_target_oncotree_code_test("PTCL", ["PTCLNOS"], True)
       
    def test_convert_to_target_oncotree_code_backwards_revocations2(self):
        self.run_convert_to_target_oncotree_code_test("URMM", [], True)

    def test_convert_to_target_oncotree_code_backwards_precursors(self):
        self.run_convert_to_target_oncotree_code_test("MYCF", ["CTCL"], True)

# TODO convert to sets, order doesn't matter?, otherwise make sure order is right --- add at least one test where the expected return is a list of codes -- test various orders
       
    def test_convert_to_target_oncotree_code_backwards_precursors_and_revocations(self):
        self.run_convert_to_target_oncotree_code_test("BLL", ["BALL"], True)

    def run_convert_to_target_oncotree_code_test(self, test_oncotree_code, expected_oncotree_code_list, is_backwards_mapping):
        expected_output = resolve_possible_target_oncotree_codes(test_oncotree_code, expected_oncotree_codes, {}, {}, False)
        actual_output = convert_to_target_oncotree_code(test_oncotree_code, self.latest_version, self.original_version, False, is_backwards_mapping)
        self.assertEqual(expected_output, actual_output)

    @classmethod
    def get_original_version(cls):
        return {
            "BALL" : {"code": "BALL", "parent": "ALL", "revocations": [], "precursors": [], "history": []},
            "ALL" : {"code": "ALL", "parent": "LEUK", "revocations": [], "precursors": [], "history": []},
            "SEZS" : {"code": "SEZS", "parent": "CTCL", "revocations": [], "precursors": [], "history": []},
            "PTCL" : {"code": "PTCL", "parent": "TNKL", "revocations": [], "precursors": [], "history": []},
            "PTCLNOS" : {"code": "PTCLNOS", "parent": "PTCL", "revocations": [], "precursors": [], "history": []},
            "CTCL" : {"code": "CTCL", "parent": "TNKL", "revocations": [], "precursors": [], "history": []},
            "GMUCM" : {"code": "GMUCM", "parent": "MEL", "revocations": [], "precursors": [], "history": []}
        }

    @classmethod
    def get_latest_version(cls):
        return {
            "BLL" : {"code": "BLL", "parent": "LNM", "revocations": ["ALL"], "precursors": ["BALL"], "history": []},
            "SS" : {"code": "SS", "parent": "MTNN", "revocations": [], "precursors": [], "history": ["SEZS"]},
            "PTCL" : {"code": "PTCL", "parent": "MTNN", "revocations": ["PTCL"], "precursors": [], "history": ["PTCLNOS"]},
            "MYCF" : {"code": "MYCF", "parent": "MTNN", "revocations": [], "precursors": ["CTCL"], "history": []},
            "URMM" : {"code": "URMM", "parent": "BLADDER", "revocations": ["GMUCM"], "precursors": [], "history": []}

        }

if __name__ == '__main__':
    unittest.main()
