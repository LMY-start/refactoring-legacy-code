package cn.xpbootcamp.legacy_code.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {
    private long id;
    private double balance;

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
