package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

@Slf4j
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

    @KafkaListener(topics = {"response_wallet"},groupId = "friends_group")
    public void updateTransaction(String message) throws JsonProcessingException {


        JSONObject transactionRequest = objectMapper.readValue(message,JSONObject.class);

        TransactionStatus transactionStatus = TransactionStatus.valueOf(transactionRequest.get("TransactionStatus").toString());

        log.info("returning transaction status");

        String transactionID = transactionRequest.get("transactionID").toString();

        TransactionEntity t = transactionRepository.findByTransactionID(transactionID);

        t.setTransactionStatus(transactionStatus);

        transactionRepository.save(t);

        notificatioNService(t);

    }

    public void notificatioNService(TransactionEntity transactionEntity){

        String fromUser = transactionEntity.getFromUser();

        URI url = URI.create("http://localhost:2612/getUser?userName="+fromUser);
        HttpEntity httpEntity = new HttpEntity(new HttpHeaders());
        JSONObject fromUserObj = restTemplate.exchange(url, HttpMethod.GET,httpEntity,JSONObject.class).getBody();
        String fromEmail = (String) fromUserObj.get("email");


        String toUser = transactionEntity.getToUser();
        url = URI.create("http://localhost:2612/getUser?userName="+toUser);
        JSONObject toUserObj = restTemplate.exchange(url, HttpMethod.GET,httpEntity,JSONObject.class).getBody();
        String toEmail = (String) toUserObj.get("email");
    }
}
