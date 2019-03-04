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
        actual_oncotree_codes = convert_to_target_oncotree_code("SEZS", self.original_version, self.latest_version, False)
        expected_oncotree_codes = ["SS"]
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)

    def test_convert_to_target_oncotree_code_forwards_revocations(self):
        actual_oncotree_codes = convert_to_target_oncotree_code("PTCL", self.original_version, self.latest_version, False)
        expected_oncotree_codes = ["PTCL"]
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)
       
        self.fail("TODO add another test")

    def test_convert_to_target_oncotree_code_forwards_precursors(self):
        actual_oncotree_codes = convert_to_target_oncotree_code("CTCL", self.original_version, self.latest_version, False)
        expected_oncotree_codes = ["MYCF"]
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)
       
    def test_convert_to_target_oncotree_code_forwards_precursors_and_revocations(self):
        actual_oncotree_codes = convert_to_target_oncotree_code("BALL", self.original_version, self.latest_version, False)
        expected_oncotree_codes = ["BLL"]
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)

    def test_convert_to_target_oncotree_code_backwards_history(self):
        actual_oncotree_codes = convert_to_target_oncotree_code("SS", self.latest_version, self.original_version, False)
        expected_oncotree_codes = ["SEZS"]
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)

    def test_convert_to_target_oncotree_code_backwards_revocations(self):
        actual_oncotree_codes = convert_to_target_oncotree_code("PTCL", self.latest_version, self.original_version, False)
        expected_oncotree_codes = ["PTCLNOS"]
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)
       
        self.fail("TODO add another test")

    def test_convert_to_target_oncotree_code_backwards_precursors(self):
        actual_oncotree_codes = convert_to_target_oncotree_code("MYCF", self.latest_version, self.original_version, False)
        expected_oncotree_codes = ["CTCL"] # TODO convert to sets, order doesn't matter?, otherwise make sure order is right
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)
       
    def test_convert_to_target_oncotree_code_backwards_precursors_and_revocations(self):
        actual_oncotree_codes = convert_to_target_oncotree_code("BLL", self.latest_version, self.original_version, False)
        expected_oncotree_codes = ["BALL"] # TODO order?
        self.assertEqual(expected_oncotree_codes, actual_oncotree_codes)

    # TODO maybe throw exceptions?
    #def test_convert_to_target_oncotree_code_exception(self):
    #    source_code = "BLAH"
    #    source_oncotree = self.original_version
    #    target_oncotree = self.latest_version

    #    with self.assertRaises(Exception) as context:
    #        convert_to_target_oncotree_code(source_code, self.original_version, self.latest_version)

    #    self.assertTrue('This is broken' in context.exception)

    @classmethod
    def get_original_version(cls):
        return {
            "BALL" : {"code": "BALL", "parent": "ALL", "revocations": [], "precursors": [], "history": []},
            "ALL" : {"code": "ALL", "parent": "LEUK", "revocations": [], "precursors": [], "history": []},
            "SEZS" : {"code": "SEZS", "parent": "CTCL", "revocations": [], "precursors": [], "history": []},
            "PTCL" : {"code": "PTCL", "parent": "TNKL", "revocations": [], "precursors": [], "history": []},
            "PTCLNOS" : {"code": "PTCLNOS", "parent": "PTCL", "revocations": [], "precursors": [], "history": []},
            "CTCL" : {"code": "CTCL", "parent": "TNKL", "revocations": [], "precursors": [], "history": []}
        }

    @classmethod
    def get_latest_version(cls):
        return {
            "BLL" : {"code": "BLL", "parent": "LNM", "revocations": ["ALL"], "precursors": ["BALL"], "history": []},
            "SS" : {"code": "SS", "parent": "MTNN", "revocations": [], "precursors": [], "history": ["SEZS"]},
            "PTCL" : {"code": "PTCL", "parent": "MTNN", "revocations": ["PTCL"], "precursors": [], "history": ["PTCLNOS"]},
            "MYCF" : {"code": "MYCF", "parent": "MTNN", "revocations": [], "precursors": ["CTCL"], "history": []}
        }

    # TODO delete if we don't ever need
    #@classmethod
    #def tearDownClass(cls):

if __name__ == '__main__':
    unittest.main()
