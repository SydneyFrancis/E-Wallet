package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    KafkaTemplate<String ,String > kafkaTemplate;
    @Autowired
    TransactionRepository transactionRepository;
    public void createTransaction(TransactionRequest transactionRequest){

        TransactionEntity transactionEntity = TransactionEntity.builder().fromUser(transactionRequest.getFromUser())
                .toUser(transactionRequest.getToUser())
                .amount(transactionRequest.getAmount()).transactionStatus(TransactionStatus.PENDING)
                .transactionID(String.valueOf(UUID.randomUUID())).transactionTime(String.valueOf(new Date())).build();

        transactionRepository.save(transactionEntity);

        JSONObject walletReq = new JSONObject();
        walletReq.put("fromUser",transactionRequest.getFromUser());
        walletReq.put("toUser",transactionRequest.getToUser());
        walletReq.put("amount",transactionRequest.getAmount());
        walletReq.put("transactionID",transactionEntity.getTransactionID());

        String message = walletReq.toString();

        kafkaTemplate.send("update_wallet",message);
    }

    @KafkaListener(topics = {"update_transaction"},groupId = "friends_group")
    public void updateTransaction(String message) throws JsonProcessingException {


        JSONObject transactionRequest = objectMapper.readValue(message,JSONObject.class);

        TransactionStatus transactionStatus = (TransactionStatus) transactionRequest.get("TransactionStatus");

        String transactionID = (String)transactionRequest.get("transactionID");

        TransactionEntity t = transactionRepository.findByTransactionID(transactionID);

        t.setTransactionStatus(transactionStatus);

        transactionRepository.save(t);

    }
}
