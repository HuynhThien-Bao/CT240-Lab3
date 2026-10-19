package com.ctu.service;

public interface PaymentGateway {
    boolean charge(String creditCard, double amount);
}
