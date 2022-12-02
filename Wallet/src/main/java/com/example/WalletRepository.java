package com.example;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WalletRepository extends JpaRepository<WalletEntity,Integer> {

    WalletEntity findByUserName(String userName);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update WalletEntity w set w.balance = w.balance + :amount where w.userName = :userName")
    void updateWallet(int amount,String userName);





}
