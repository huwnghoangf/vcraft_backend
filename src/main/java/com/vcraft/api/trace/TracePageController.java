package com.vcraft.api.trace;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TracePageController {
    @GetMapping("/trace/{batchId}")
    public String trace() { return "forward:/trace.html"; }
}
