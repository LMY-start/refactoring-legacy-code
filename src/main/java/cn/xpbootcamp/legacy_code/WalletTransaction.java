package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import org.apache.commons.lang3.StringUtils;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private String orderId;
    private Long createdTimestamp;
    private Double amount;
    private STATUS status;
    private String walletTransactionId;
    private WalletService walletService;


    public WalletTransaction(String preAssignedId, Long buyerId, Long sellerId, Long productId, String orderId, Double amount) {
        initId(preAssignedId);
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.orderId = orderId;
        this.status = STATUS.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
        this.amount = amount;
    }

    public void setWalletService(WalletServiceImpl walletService) {
        this.walletService = walletService;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public boolean execute() throws InvalidTransactionException {
        if (buyerId == null || (sellerId == null || amount < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        if (status == STATUS.EXECUTED) return true;
        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(id);

            if (!isLocked) {
                return false;
            }
            if (status == STATUS.EXECUTED) return true;
            if (isExpired()) {
                this.status = STATUS.EXPIRED;
                return false;
            }
            String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);
            if (walletTransactionId != null) {
                this.walletTransactionId = walletTransactionId;
                this.status = STATUS.EXECUTED;
                return true;
            } else {
                this.status = STATUS.FAILED;
                return false;
            }
        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(id);
            }
        }
    }

    private void initId(String preAssignedId) {
        this.id = StringUtils.isEmpty(preAssignedId) ? preAssignedId : IdGenerator.generateTransactionId();
        String START_T = "t_";
        this.id = this.id.startsWith(START_T) ? this.id : START_T + preAssignedId;
    }

    private boolean isExpired() {
        int expiredTimeInMs = 1728000000;
        return System.currentTimeMillis() - createdTimestamp > expiredTimeInMs;
    }

}