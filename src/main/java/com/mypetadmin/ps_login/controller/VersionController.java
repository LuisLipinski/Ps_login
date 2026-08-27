package com.mypetadmin.ps_login.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {

    private final String commit;

    public VersionController(@Value("${app.git-commit:${RENDER_GIT_COMMIT:unknown}}") String commit) {
        this.commit = commit;
    }

    @GetMapping("/version")
    public String version() {
        return commit;
    }
}
