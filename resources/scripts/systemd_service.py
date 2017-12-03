#!/usr/bin/env python3

import json
import sys
import subprocess

def retrieve_last_log(params):
    result = subprocess.run(["journalctl", "-r", "-n", "10", "-u", params["name"]],
                            stdout=subprocess.PIPE)
    return result.stdout.decode("utf-8")

def handle(params):
    result = subprocess.run(["systemctl", "status", params["name"]],
                            stdout=subprocess.PIPE)
    return {"status": "up" if result.returncode == 0 else "down",
            "lastlog": retrieve_last_log(params)}

try:
    # Parse params
    if len(sys.argv) != 2:
        raise RuntimeError("invalid arguments")
    params = json.loads(sys.argv[1])

    # Check the params
    assert isinstance(params["name"], str), "missing `name` parameter"

    # Execute the script main logic
    result = handle(params)

    # Return the output
    print(json.dumps(result), file=sys.stdout, flush=True)
    sys.exit(0);
except Exception as e:
    print(json.dumps({"error": str(e)}), file=sys.stderr, flush=True)
    sys.exit(-1);
