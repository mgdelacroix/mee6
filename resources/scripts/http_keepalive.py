#!/usr/bin/env python3

import json
import sys
import urllib.request as http
import urllib.error
import time

def handle(params):
    start = time.time()
    try:
        request = http.Request(params["url"], method=params["method"].upper())
        response = http.urlopen(request)

        return {"status": response.status,
                "latency": time.time() - start}
    except urllib.error.HTTPError as e:
        return {"status": e.code,
                "latency": time.time() - start}

try:
    # Parse params
    if len(sys.argv) != 2:
        raise RuntimeError("invalid arguments")
    params = json.loads(sys.argv[1])

    # Check the params
    assert isinstance(params["url"], str), "missing url parameter"
    assert isinstance(params["method"], str), "missing method parameter"

    # Execute the script main logic
    result = handle(params)

    # Return the output
    print(json.dumps(result), file=sys.stdout, flush=True)
    sys.exit(0);
except Exception as e:
    print(json.dumps({"error": str(e)}), file=sys.stderr, flush=True)
    sys.exit(-1);
