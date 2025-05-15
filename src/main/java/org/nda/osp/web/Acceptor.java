package org.nda.osp.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Acceptor {

    @GetMapping("/login/oauth2/code/oidc-client")
    public String accept() {
        return "Response";
    }
}
