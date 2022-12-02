package com.example;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionRequest {
    private String fromUser;
    private String toUser;
    private int amount;
}
