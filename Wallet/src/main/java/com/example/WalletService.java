package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    ObjectMapper objectMapper;

    @KafkaListener(topics = {"create_wallet"},groupId = "friends_group")
    public void createWallet(String message) throws JsonProcessingException {

        JSONObject walletRequest = objectMapper.readValue(message,JSONObject.class);
        String userName = (String) walletRequest.get("username");

        WalletEntity wallet = WalletEntity.builder().userName(userName).balance(0).build();
        walletRepository.save(wallet);
    }

    @KafkaListener(topics = {"update_wallet"},groupId = "friends_group")
    private void updateWallet(String message) throws JsonProcessingException {


        JSONObject walletRequest = objectMapper.readValue(message, JSONObject.class);
        String fromUser = (String) walletRequest.get("fromUser");
        String toUser = (String) walletRequest.get("toUser");
        int transactionAmount = (Integer) walletRequest.get("amount");

        String transactionID = (String) walletRequest.get("transactionID");

        WalletEntity sendersWallet = walletRepository.findByUserName(fromUser);

        if (sendersWallet.getBalance() >= transactionAmount) {


            walletRepository.updateWallet(transactionAmount,toUser);
            walletRepository.updateWallet(-1*transactionAmount,fromUser);


            JSONObject transactionReq = new JSONObject();
            transactionReq.put("transactionID", transactionID);
            transactionReq.put("TransactionStatus", "SUCCESS");
            String sendMessage = transactionReq.toString();
            kafkaTemplate.send("update_wallet",sendMessage);


        } else {

            JSONObject transactionReq = new JSONObject();
            transactionReq.put("transactionID", transactionID);
            transactionReq.put("TransactionStatus", "FAILED");
            String sendMessage = transactionReq.toString();
            kafkaTemplate.send("update_wallet",sendMessage);


        }
    }



//    public WalletEntity incrementWallet(String userName, int amount){
//        WalletEntity wallet = walletRepository.findByUserName(userName);
//        int Amount = wallet.getAmount() + amount;
//        int ID = wallet.getID();
//
//
//        WalletEntity updatedWallet = WalletEntity.builder().ID(ID).amount(Amount).build();
//        walletRepository.save(updatedWallet);
//        return updatedWallet;
//    }
//    public WalletEntity decrementWallet(String userName, int amount){
//
//        WalletEntity wallet = walletRepository.findByUserName(userName);
//        int Amount = wallet.getAmount() - amount;
//        int ID = wallet.getID();
//
//
//        WalletEntity updatedWallet = WalletEntity.builder().ID(ID).amount(Amount).build();
//        walletRepository.save(updatedWallet);
//        return updatedWallet;
//    }
}
