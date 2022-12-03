package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    SimpleMailMessage simpleMailMessage;

    @Autowired
    ObjectMapper objectMapper;

    @KafkaListener(topics = ("send_email"),groupId = "friends_group")
    public void sendEmailMessage(String message) throws JsonProcessingException {

        JSONObject emailReq = objectMapper.readValue(message,JSONObject.class);

        String emailid = (String) emailReq.get("email");
        String messageBody = (String) emailReq.get("message");

        simpleMailMessage.setTo(emailid);
        simpleMailMessage.setText(message);
        simpleMailMessage.setSubject("Transaction mail");

        javaMailSender.send(simpleMailMessage);
    }
}
