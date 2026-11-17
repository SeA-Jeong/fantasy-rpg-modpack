"""check_mods.py 자체 검사 (표준 라이브러리 unittest). 실행: cd tools && python3 -m unittest test_check_mods"""
import io
import os
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout

sys.path.insert(0, os.path.dirname(__file__))
import check_mods  # noqa: E402


def make_jar(path, mod_id, version, deps):
    lines = ['modLoader="javafml"', 'loaderVersion="[1,)"', 'license="x"', "[[mods]]",
             f'modId="{mod_id}"', f'version="{version}"']
    for d in deps:
        lines += [f"[[dependencies.{mod_id}]]", f'modId="{d[0]}"', f'type="{d[1]}"', f'versionRange="{d[2]}"', 'side="BOTH"']
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("META-INF/neoforge.mods.toml", "\n".join(lines))


class RangeTest(unittest.TestCase):
    def test_ranges(self):
        r = check_mods.in_range
        self.assertTrue(r("21.1.266", "[21.1.0,)"))
        self.assertFalse(r("21.1.228", "[21.1.266,)"))
        self.assertTrue(r("15.0.140", "[15.0.0,)"))
        self.assertTrue(r("2.0.4+1.21.1", "[1,)"))
        self.assertFalse(r("19.51.0.418", "[19.53.0.425,)"))
        self.assertTrue(r("19.70.0.469", "[19.53.0.425,)"))
        self.assertTrue(r("1.21.1", "[1.21.1,1.22)"))
        self.assertFalse(r("1.22", "[1.21.1,1.22)"))
        self.assertTrue(r("1.0.0", "[1.0.0]"))
        self.assertIsNone(r("1.0", "[${x},)"))


class FolderTest(unittest.TestCase):
    def run_dir(self, jars):
        with tempfile.TemporaryDirectory() as d:
            for name, mid, ver, deps in jars:
                make_jar(os.path.join(d, name), mid, ver, deps)
            buf = io.StringIO()
            with redirect_stdout(buf):
                code = check_mods.main([d])
            return code, buf.getvalue()

    def test_missing_required(self):
        code, out = self.run_dir([("bc.jar", "bettercombat", "2.4.0", [("playeranimator", "required", "[1,)"), ("cloth_config", "required", "[15.0.0,)")])])
        self.assertEqual(1, code)
        self.assertIn("playeranimator", out)

    def test_optional_out_of_range_is_error(self):
        code, out = self.run_dir([
            ("xmod.jar", "ftbxmodcompat", "21.1.14", [("jei", "optional", "[19.53.0.425,)")]),
            ("jei.jar", "jei", "19.51.0.418", []),
        ])
        self.assertEqual(1, code)
        self.assertIn("범위", out)

    def test_ok(self):
        code, _ = self.run_dir([
            ("bc.jar", "bettercombat", "2.4.0", [("playeranimator", "required", "[1,)"), ("cloth_config", "required", "[15.0.0,)"), ("neoforge", "required", "[21.0,)")]),
            ("pa.jar", "playeranimator", "2.0.4+1.21.1", []),
            ("cc.jar", "cloth_config", "15.0.140", []),
        ])
        self.assertEqual(0, code)


if __name__ == "__main__":
    unittest.main()
