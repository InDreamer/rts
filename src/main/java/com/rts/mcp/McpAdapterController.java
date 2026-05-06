package com.rts.mcp;

import com.rts.llm.ControlledLlmHarness;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
public class McpAdapterController {
    private final QueryService queryService;
    private final ControlledLlmHarness harness;

    public McpAdapterController(QueryService queryService, ControlledLlmHarness harness) {
        this.queryService = queryService;
        this.harness = harness;
    }

    @GetMapping("/tools")
    public Map<String, Object> tools() {
        return Map.of("tools", List.of(
                "rts_find_objects",
                "rts_read_object",
                "rts_get_dependencies",
                "rts_ask",
                "rts_get_trace"));
    }

    @PostMapping("/tools/rts_find_objects")
    public Object find(@RequestBody FindRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.find(new FindRequest(request.query(), request.scope(), request.objectTypes(), request.anchors(), request.limit(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/tools/rts_read_object")
    public Object read(@RequestBody ObjectContentRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.readContent(new ObjectContentRequest(request.uri(), request.purpose(), request.releaseId(), request.traceId(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_get_dependencies")
    public Object dependencies(@RequestBody DependenciesRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.dependencies(new DependenciesRequest(request.uri(), request.direction(), request.edgeType(), request.depth(), request.purpose(), request.releaseId(), request.callerId(), apiKey));
    }

    @PostMapping("/tools/rts_ask")
    public Object ask(@RequestBody AskRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return harness.ask(new AskRequest(request.query(), request.callerId(), apiKey, request.scopeHint(), request.outputMode(), request.maxToolCalls()));
    }

    @PostMapping("/tools/rts_get_trace")
    public Object trace(@RequestBody Map<String, String> request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.trace(request.get("trace_id"), request.get("caller_id"), apiKey);
    }
}
