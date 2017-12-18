#!/usr/bin/env python3

import json
import sys
import subprocess
import re
import traceback

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

def handle(params, local):
    device = None
    for item in retrieve_available_devices():
        if item["path"].startswith(params["device"]):
            device = item
            break;

    if not device:
        raise RuntimeError("No device '{}' found".format(params["device"]))

    local.update({
        "capacity": device["capacity"],
        "used": device["used"]
    })

    percentage = (device["used"] * 100) // device["capacity"]
    if percentage > params["threshold"]:
        return {"status": "red", "local": local}
    else:
        return {"status": "green", "local": local}

try:
    # Parse params
    if len(sys.argv) != 2:
        raise RuntimeError("invalid arguments")
    args = json.loads(sys.argv[1])

    # Check the params
    assert isinstance(args["params"], dict), "missing params"
    assert isinstance(args["local"], dict), "missing local"
    assert isinstance(args["params"]["device"], str), "missing device parameter"

    # Extract the data from args
    params, local = args["params"], args["local"]

    # Execute the script main logic
    result = handle(params, local)

    # Format the result
    result = json.dumps(result)

    # Return the output
    print(result, file=sys.stdout, flush=True)
    sys.exit(0);
except RuntimeError as e:
    print(json.dumps({"hint": str(e)}), file=sys.stderr, flush=True)
    sys.exit(-1)

except Exception as e:
    traceback.print_exc(file=sys.stderr)
    sys.exit(-1);
