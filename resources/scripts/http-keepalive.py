#!/usr/bin/env python3

import json
import sys
import urllib.request as http
import urllib.error
import time
import traceback

def perform_request(url, method):
    start = time.time()
    try:
        request = http.Request(url, method=method.upper())
        response = http.urlopen(request)
        return {"status": response.status,
                "latency": time.time() - start}
    except urllib.error.HTTPError as e:
        return {"status": e.code,
                "latency": time.time() - start}


def handle(params, local):
    url = params["url"]
    expected_statuses = params.get("expected-statuses", 200)
    http_method = params.get("http-method", "").lower()

    if isinstance(expected_statuses, list):
        expected_statuses = set(expected_statuses)
    elif instance(expected_statuses, int):
        expected_statuses = set([expected_statuses])
    else:
        raise RuntimeError("wrong arguments for expected-statuses option")

    if http_method not in {"get", "head", "option"}:
        raise RuntimeError("http method not allowed '{}'".format(http_method))

    result = perform_request(url, http_method)
    if result["status"] in expected_statuses:
        return {"status": "green", "local": result}
    else:
        return {"status": "red", "local": result}

try:
    # Parse params
    if len(sys.argv) <= 2:
        raise RuntimeError("invalid arguments")
    args = json.loads(sys.argv[1])

    # Check the params
    assert isinstance(args["local"], dict), "missing local"
    assert isinstance(args["params"], dict), "missing params"
    assert isinstance(args["params"]["url"], str), "missing url parameter"
    assert isinstance(args["params"]["http-method"], str), "missing http-method parameter"
    assert isinstance(args["params"]["expected-statuses"], list), "missing expected-statuses parameter"

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
    traceback.print_exc(file=sys.stderr)
    sys.exit(-1);
