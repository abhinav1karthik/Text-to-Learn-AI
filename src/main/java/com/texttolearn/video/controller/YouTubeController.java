package com.texttolearn.video.controller;

import com.texttolearn.video.dto.YouTubeVideoSearchResponse;
import com.texttolearn.video.service.YouTubeVideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {

    private final YouTubeVideoService youTubeVideoService;

    public YouTubeController(YouTubeVideoService youTubeVideoService) {
        this.youTubeVideoService = youTubeVideoService;
    }

    @GetMapping
    YouTubeVideoSearchResponse searchVideos(
            @RequestParam String query,
            @RequestParam(required = false) Integer maxResults
    ) {
        return youTubeVideoService.searchEducationalVideos(query, maxResults);
    }
}
