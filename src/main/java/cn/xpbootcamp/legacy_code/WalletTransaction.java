package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.entity.Order;
import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import org.apache.commons.lang3.StringUtils;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
    private String id;
    private Order order;
    private Long createdTimestamp;
    private STATUS status;
    private String walletTransactionId;
    private WalletService walletService;


    public WalletTransaction(String preAssignedId, Order order) {
        initId(preAssignedId);
        this.order = order;
        this.status = STATUS.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
    }

    public void setWalletService(WalletServiceImpl walletService) {
        this.walletService = walletService;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public boolean execute() throws InvalidTransactionException {
        if (order.isCheckPass()) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        if (isExecuted()) {
            return true;
        }

        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(id);

            if (!isLocked) {
                return false;
            }
            if (isExecuted()) {
                return true;
            }
            if (isExpired()) {
                this.status = STATUS.EXPIRED;
                return false;
            }
            String walletTransactionId = walletService.moveMoney(id, order.getBuyerId(), order.getSellerId(), order.getAmount());
            return updateAfterMoveMoney(walletTransactionId);
        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(id);
            }
        }
    }

    private boolean updateAfterMoveMoney(String walletTransactionId) {
        if (StringUtils.isEmpty(walletTransactionId)) {
            this.status = STATUS.FAILED;
            return false;
        } else {
            this.walletTransactionId = walletTransactionId;
            this.status = STATUS.EXECUTED;
            return true;
        }
    }


    private boolean isExecuted() {
        return STATUS.EXECUTED.equals(status);
    }

    private void initId(String preAssignedId) {
        this.id = StringUtils.isEmpty(preAssignedId) ? IdGenerator.generateTransactionId() : preAssignedId;
        String START_T = "t_";
        this.id = this.id.startsWith(START_T) ? this.id : START_T + preAssignedId;
    }

    private boolean isExpired() {
        int expiredTimeInMs = 1728000000;
        return System.currentTimeMillis() - createdTimestamp > expiredTimeInMs;
    }

}