package cn.xpbootcamp.legacy_code.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@AllArgsConstructor
public class Order {
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private String orderId;
    private Double amount;

    public boolean isAmountInValid() {
        return this.amount < 0.0;
    }

    public boolean isCheckPass() {
        return ObjectUtils.isEmpty(getBuyerId()) || ObjectUtils.isEmpty(getSellerId()) || isAmountInValid();
    }
}
