package com.jamierf.jsonrpc.filter;

import java.util.Optional;

import com.jamierf.jsonrpc.RequestMethod;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.api.Result;

public interface RequestHandler {
	Optional<Result<?>> handle(final RequestMethod method, final Parameters<String, ?> params);
}
