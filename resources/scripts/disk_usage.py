#!/usr/bin/env python3

import json
import sys
import subprocess
import re

def retrieve_available_devices():
    result = subprocess.run(["df", "-l"],  stdout=subprocess.PIPE)
    stdout = result.stdout.decode("utf-8")

    for line in stdout.splitlines()[1:]:
        parts = re.split("\s+", line)
        if len(parts) < 3:
            continue

        yield {
            "path": parts[0],
            "capacity": int(parts[1]),
            "used": int(parts[2])
        }

def handle(params):
    device = None

    for item in retrieve_available_devices():
        if item["path"].startswith(params["device"]):
            device = item
            break;

    if not device:
        raise RuntimeError("no device found")

    return {
        "capacity": device["capacity"],
        "used": device["used"]
    }

try:
    # Parse params
    if len(sys.argv) != 2:
        raise RuntimeError("invalid arguments")
    params = json.loads(sys.argv[1])

    # Check the params
    assert isinstance(params["device"], str), "missing `device` parameter"

    # Execute the script main logic
    result = handle(params)

    # Return the output
    print(json.dumps(result), file=sys.stdout, flush=True)
    sys.exit(0);
except Exception as e:
    print(json.dumps({"error": str(e)}), file=sys.stderr, flush=True)
    sys.exit(-1);
