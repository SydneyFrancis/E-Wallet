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

        callNotificationService(t);

    }

    private void callNotificationService(TransactionEntity transaction) {
        // FETCH EMAIL FROM USER SERVICE

        String transactionId = transaction.getTransactionID();
        String fromUser = transaction.getFromUser();
        String toUser = transaction.getToUser();

        URI url = URI.create("http://localhost:2612/getUser?username="+fromUser);
        HttpEntity httpEntity = new HttpEntity(new HttpHeaders());

        JSONObject fromUserObject = restTemplate.exchange(url, HttpMethod.GET,httpEntity,JSONObject.class).getBody();

        String senderName = (String)fromUserObject.get("name");
        String senderEmail = (String)fromUserObject.get("email");

        url = URI.create("http://localhost:2612/getUser?username="+toUser);
        JSONObject toUserObject = restTemplate.exchange(url, HttpMethod.GET,httpEntity,JSONObject.class).getBody();

        String receiverEmail = (String)toUserObject.get("email");
        String receiverName = (String)toUserObject.get("name");

        // SEND THE EMAIL AND MESSAGE TO NOTIFICATIONS-SERVICE VIA KAFKA
        JSONObject emailRequest = new JSONObject();

        //SENDER should always receive email
        String senderMessageBody = String.format("Hi %s the transaction with transactionId %s has been %s of Rs %d",
                senderName,transactionId,transaction.getTransactionStatus(),transaction.getAmount());

        emailRequest.put("email", senderEmail);
        emailRequest.put("message" , senderMessageBody);

        String message = emailRequest.toString() ;

        kafkaTemplate.send("send_email", message);

        // RECEIVER WILL GET MAIL ONLY WHEN TRANSACTION IS SUCCESSFULL
        if(transaction.getTransactionStatus().equals("SUCCESS")) {

            String receiverMessageBody = String.format("Hi %s you have received an amount of %d from %s",
                    receiverName,transaction.getAmount(),senderName);

            emailRequest.put("email", senderEmail);
            emailRequest.put("message" , senderMessageBody);

            message = emailRequest.toString();

            kafkaTemplate.send("send_email",message);
        }
    }
}
