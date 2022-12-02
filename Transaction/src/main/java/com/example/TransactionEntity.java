package com.example;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ID;

    private String transactionID = UUID.randomUUID().toString();

    private String fromUser;

    private String toUser;

    private int amount;

    private TransactionStatus transactionStatus;

    private String transactionTime;
}
