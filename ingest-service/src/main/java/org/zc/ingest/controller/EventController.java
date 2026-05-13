package org.zc.ingest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zc.common.Result;
import org.zc.ingest.dto.EventReportRequest;
import org.zc.ingest.parser.HttpEventParser;
import org.zc.ingest.service.EventIngestService;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {
    private final HttpEventParser httpEventParser;
    private final EventIngestService eventIngestService;

    @PostMapping("/report")
    public Result<String> report(
        @Valid @RequestBody EventReportRequest request,
        @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        eventIngestService.ingest(httpEventParser.parse(request), traceId, "http");
        return Result.ok("accepted");
    }
}
