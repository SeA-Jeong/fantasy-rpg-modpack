"""build_mrpack.py·install_mrpack.py 자체 검사. 실행: cd tools && python3 -m unittest test_mrpack"""
import hashlib
import io
import json
import os
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout

sys.path.insert(0, os.path.dirname(__file__))
import build_mrpack  # noqa: E402
import install_mrpack  # noqa: E402


class MrpackTest(unittest.TestCase):
    def test_build_and_install(self):
        with tempfile.TemporaryDirectory() as d:
            ext = os.path.join(d, "ext.jar")
            data = b"external-mod"
            open(ext, "wb").write(data)
            ours = os.path.join(d, "ours")
            os.makedirs(ours)
            open(os.path.join(ours, "royalqol-0.2.0.jar"), "wb").write(b"ours")
            dp = os.path.join(d, "datapacks", "fantasyrpg_test")
            os.makedirs(dp)
            open(os.path.join(dp, "pack.mcmeta"), "w").write("{}")
            os.makedirs(os.path.join(d, "datapacks", "fantasyrpg_content"))
            resolved = {"resolved": [{"name": "Ext", "file": "ext.jar", "from": "file://" + ext,
                                      "sha1": hashlib.sha1(data).hexdigest(), "sha512": hashlib.sha512(data).hexdigest(),
                                      "size": len(data)}], "failed": []}
            lock = {"minecraft": "1.21.1", "neoforge": "21.1.266"}
            out = os.path.join(d, "t.mrpack")
            with redirect_stdout(io.StringIO()) as buf:
                build_mrpack.build(lock, resolved, ours, os.path.join(d, "datapacks"), None, "0.0.1", out)
            self.assertIn("허용 목록 밖", buf.getvalue())  # file:// 주소는 경고
            with zipfile.ZipFile(out) as z:
                idx = json.loads(z.read("modrinth.index.json"))
                self.assertEqual({"minecraft": "1.21.1", "neoforge": "21.1.266"}, idx["dependencies"])
                self.assertEqual("mods/ext.jar", idx["files"][0]["path"])
                self.assertIn("overrides/mods/royalqol-0.2.0.jar", z.namelist())
                self.assertIn("overrides/datapacks/fantasyrpg_test/pack.mcmeta", z.namelist())
            inst = os.path.join(d, "inst")
            with redirect_stdout(io.StringIO()):
                self.assertEqual(0, install_mrpack.main([out, inst]))
            self.assertEqual(data, open(os.path.join(inst, "mods", "ext.jar"), "rb").read())
            self.assertTrue(os.path.exists(os.path.join(inst, "mods", "royalqol-0.2.0.jar")))

            # 해시가 틀리면 실패
            open(ext, "wb").write(b"tampered")
            with redirect_stdout(io.StringIO()):
                self.assertEqual(1, install_mrpack.main([out, os.path.join(d, "inst2")]))

    def test_refuses_failed_fetch(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(SystemExit):
                build_mrpack.build({"minecraft": "1.21.1", "neoforge": "21.1.266"},
                                   {"resolved": [], "failed": ["X: 404"]}, d, None, None, "0", os.path.join(d, "o.mrpack"))


if __name__ == "__main__":
    unittest.main()
