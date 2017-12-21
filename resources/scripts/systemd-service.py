#!/usr/bin/env python3

import json
import sys
import subprocess
import traceback


def retrieve_last_log(params):
    result = subprocess.run(["journalctl", "-r", "-n", "10", "-u", params["name"]],
                            stderr=subprocess.PIPE,
                            stdout=subprocess.PIPE)
    return result.stdout.decode("utf-8")

def handle(params, local):
    result = subprocess.run(["systemctl", "status", params["service"]],
                            stderr=subprocess.PIPE,
                            stdout=subprocess.PIPE)

    service_status = "up" if result.returncode == 0 else "down"
    lastlog = retrieve_last_log(params)

    local.update({"status": service_status,
                  "lastlog": lastlog})

    if service_status == "up":
        return {"status": "green", "local": local}
    else:
        return {"status": "red", "local": local}

try:
    # Parse params
    if len(sys.argv) <= 2:
        raise RuntimeError("invalid arguments")
    args = json.loads(sys.argv[1])

    # Check the params
    assert isinstance(args["local"], dict), "missing local"
    assert isinstance(args["params"], dict), "missing params"
    assert isinstance(args["params"]["service"], str), "missing service parameter"

    # Extract the data from args
    params, local = args["params"], args["local"]

    # Execute the script main logic
    result = handle(params, local)

    # Format the result
    result = json.dumps(result)

    # Return the output
    print(result, file=sys.stdout, flush=True)
    sys.exit(0);
except Exception as e:
    print(json.dumps({"error": str(e)}), file=sys.stderr, flush=True)
    sys.exit(-1);
