package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class WalletController {

    @Autowired
    WalletService walletService;


    @PostMapping("/addWallet")
    public void createWallet(@RequestParam("username") String username) throws JsonProcessingException {
        walletService.createWallet(username);
    }

}
