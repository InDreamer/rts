package com.rts.api;

import com.rts.llm.ControlledLlmHarness;
import com.rts.model.CoreModels.L2Content;
import com.rts.model.CoreModels.QueryPlan;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.model.CoreModels.TraceRecord;
import com.rts.query.QueryRequests.AskRequest;
import com.rts.query.QueryRequests.DependenciesRequest;
import com.rts.query.QueryRequests.FindRequest;
import com.rts.query.QueryRequests.ObjectContentRequest;
import com.rts.query.QueryRequests.ObjectGetRequest;
import com.rts.query.QueryRequests.PlanRequest;
import com.rts.query.QueryRequests.QueryRequest;
import com.rts.query.QueryService;
import com.rts.query.QueryService.ObjectEnvelope;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QueryController {
    private final QueryService queryService;
    private final ControlledLlmHarness controlledLlmHarness;

    public QueryController(QueryService queryService, ControlledLlmHarness controlledLlmHarness) {
        this.queryService = queryService;
        this.controlledLlmHarness = controlledLlmHarness;
    }

    @PostMapping("/query/plan")
    public QueryPlan plan(@RequestBody PlanRequest request) {
        return queryService.plan(request);
    }

    @PostMapping("/find")
    public List<?> find(@RequestBody FindRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.find(new FindRequest(request.query(), request.scope(), request.objectTypes(), request.anchors(), request.limit(), request.callerId(), apiKey, request.outputMode()));
    }

    @PostMapping("/objects/get")
    public ObjectEnvelope getObject(@RequestBody ObjectGetRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.getObject(new ObjectGetRequest(request.uri(), request.releaseId(), request.traceId(), request.callerId(), apiKey));
    }

    @PostMapping("/objects/content")
    public L2Content getContent(@RequestBody ObjectContentRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.readContent(new ObjectContentRequest(request.uri(), request.purpose(), request.releaseId(), request.traceId(), request.callerId(), apiKey));
    }

    @PostMapping("/objects/dependencies")
    public Object dependencies(@RequestBody DependenciesRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.dependencies(new DependenciesRequest(request.uri(), request.direction(), request.edgeType(), request.depth(), request.purpose(), request.releaseId(), request.callerId(), apiKey));
    }

    @PostMapping("/query")
    public ServiceAnswer query(@RequestBody QueryRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.query(new QueryRequest(request.query(), request.callerId(), apiKey, request.scopeHint(), request.outputMode(), request.useLlm()));
    }

    @PostMapping("/ask")
    public ServiceAnswer ask(@RequestBody AskRequest request, @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return controlledLlmHarness.ask(new AskRequest(request.query(), request.callerId(), apiKey, request.scopeHint(), request.outputMode(), request.maxToolCalls()));
    }

    @GetMapping("/traces/{traceId}")
    public Optional<TraceRecord> trace(@PathVariable String traceId,
            @RequestHeader(name = "X-RTS-Caller-Id", required = false) String callerId,
            @RequestHeader(name = "X-RTS-API-Key", required = false) String apiKey) {
        return queryService.trace(traceId, callerId, apiKey);
    }
}
