#!/usr/bin/env python3
"""NeoForge 전용 서버를 실제로 띄워 모드 조합이 함께 로드되는지 확인한다 (CI용, 표준 라이브러리만).

사용:  python3 tools/smoke_server.py <설치된 서버 폴더> <mods 폴더> [--timeout 900] [--command "..."]...

1. 서버 폴더의 mods 를 지정한 mods 폴더 내용으로 바꾸고 eula·server.properties 를 쓴다.
2. run.sh nogui 로 서버를 띄우고 "Done (" 줄이 나올 때까지 기다린다.
3. 콘솔 명령(--command, 여러 개)을 보내고 출력에 기대 문자열이 있는지 본다 ("명령=>기대" 형식).
4. stop 으로 끄고 로그를 검사한다: 시작 실패, 모드 로딩 오류, mixin 적용 실패, 서버 스레드 예외.

결과: 0 = 통과, 1 = 실패. logs/latest.log 와 crash-reports 는 서버 폴더에 남는다.
이 검사는 서버 쪽 로딩만 본다. 클라이언트 화면·렌더링은 검증하지 않는다.
"""
import os
import re
import shutil
import subprocess
import sys
import threading
import time

FATAL_PATTERNS = [
    r"Mixin apply (?:for mod \S+ )?failed",
    r"MixinApplyError",
    r"Missing or unsupported mandatory dependencies",
    r"Loading errors encountered",
    r"Failed to (?:start|load) (?:the )?(?:minecraft )?server",
    r"Encountered an unexpected exception",
    r"---- Minecraft Crash Report ----",
    r"Exception in server tick loop",
    r"ModLoadingException",
]
WARN_PATTERNS = [r"\[[^\]]*/ERROR\]", r"Exception"]


def write(path, text):
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("server_dir")
    ap.add_argument("mods_dir")
    ap.add_argument("--timeout", type=int, default=900)
    ap.add_argument("--command", action="append", default=[], help="콘솔 명령=>출력에 있어야 할 문자열(선택)")
    ap.add_argument("--jvm-arg", action="append", default=[], help="user_jvm_args.txt 에 더할 JVM 인자 (--jvm-arg=-D… 처럼 = 로 붙여 씀)")
    a = ap.parse_args(argv)

    sd = os.path.abspath(a.server_dir)
    mods = os.path.join(sd, "mods")
    shutil.rmtree(mods, ignore_errors=True)
    os.makedirs(mods)
    for f in os.listdir(a.mods_dir):
        if f.endswith(".jar"):
            shutil.copy(os.path.join(a.mods_dir, f), mods)
    for d in ("world", "logs", "crash-reports", "config"):
        shutil.rmtree(os.path.join(sd, d), ignore_errors=True)
    write(os.path.join(sd, "eula.txt"), "eula=true\n")
    write(os.path.join(sd, "server.properties"),
          "online-mode=false\npvp=false\nlevel-seed=20261117\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\nmax-tick-time=-1\n")
    jvm = os.path.join(sd, "user_jvm_args.txt")
    write(jvm, "-Xmx3G\n" + "".join(x + "\n" for x in a.jvm_arg))

    print(f"서버 시작: 모드 {len(os.listdir(mods))}개")
    proc = subprocess.Popen(["bash", "run.sh", "nogui"], cwd=sd, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, bufsize=1)
    lines, done = [], threading.Event()

    def reader():
        for line in proc.stdout:
            lines.append(line)
            sys.stdout.write(line)
            if "Done (" in line and "For help" in line:
                done.set()
        done.set()

    threading.Thread(target=reader, daemon=True).start()
    started = done.wait(a.timeout) and proc.poll() is None
    results = []
    if started:
        print("\n=== 서버 시작 완료, 콘솔 명령 실행 ===")
        for spec in a.command:
            cmd, _, expect = spec.partition("=>")
            mark = len(lines)
            proc.stdin.write(cmd.strip() + "\n")
            proc.stdin.flush()
            time.sleep(4)
            out = "".join(lines[mark:])
            ok = (expect.strip() in out) if expect.strip() else not re.search(r"Unknown or incomplete command|Incorrect argument", out)
            results.append((cmd.strip(), expect.strip(), ok))
        proc.stdin.write("stop\n")
        proc.stdin.flush()
    try:
        proc.wait(timeout=180)
    except subprocess.TimeoutExpired:
        proc.kill()

    text = "".join(lines)
    fatal = [p for p in FATAL_PATTERNS if re.search(p, text)]
    warns = [l.rstrip() for l in lines if any(re.search(p, l) for p in WARN_PATTERNS)]

    print("\n=== 요약 ===")
    print(f"서버 시작: {'성공' if started else '실패'}")
    for cmd, expect, ok in results:
        print(f"명령 {cmd!r}: {'통과' if ok else '실패'}" + (f" (기대 문자열 {expect!r})" if expect else ""))
    for p in fatal:
        print(f"치명 패턴 발견: {p}")
    print(f"ERROR/Exception 줄: {len(warns)}개 (아래 최대 30줄)")
    for w in warns[:30]:
        print("  " + w[:300])
    ok = started and not fatal and all(r[2] for r in results)
    print(f"\n결과: {'통과' if ok else '실패'}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
