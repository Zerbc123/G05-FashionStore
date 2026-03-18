package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.edu.fpt.fashionstore.service.AccountService;

@Controller
public class TestController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/test-account")
    @ResponseBody
    public String testAccountConnection() {
        return accountService.testAccountConnection().replace("\n", "<br>");
    }
}
