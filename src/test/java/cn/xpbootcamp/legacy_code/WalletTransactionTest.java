package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.repository.UserRepositoryImpl;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;
import org.junit.jupiter.api.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import javax.transaction.InvalidTransactionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

class WalletTransactionTest {


    @Test
    void should_execute_return_true_when_given_a_new_wallet_transaction_with_locked() throws InvalidTransactionException {

        String preAssignedId = "";
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 3L;
        String orderId = "OID-123";
        double amount = 15d;
        User buyer = new User(buyerId, 100d);
        User seller = new User(sellerId, 100d);

        RedisDistributedLock mockRedisDistributedLock = PowerMockito.mock(RedisDistributedLock.class);
        Whitebox.setInternalState(RedisDistributedLock.class, "INSTANCE", mockRedisDistributedLock);
        PowerMockito.when(mockRedisDistributedLock.lock(anyString())).thenReturn(true);


        UserRepositoryImpl mockUserRepository = PowerMockito.mock(UserRepositoryImpl.class);
        PowerMockito.when(mockUserRepository.find(buyerId)).thenReturn(buyer);
        PowerMockito.when(mockUserRepository.find(sellerId)).thenReturn(seller);


        WalletTransaction walletTransaction = new WalletTransaction(preAssignedId, buyerId, sellerId, productId, orderId, amount);
        walletTransaction.setWalletService(new WalletServiceImpl(mockUserRepository));
        boolean result = walletTransaction.execute();

        assertTrue(result);
        assertEquals(85, buyer.getBalance());
        assertEquals(115, seller.getBalance());

    }

    @Test
    void should_execute_return_false_when_given_a_new_wallet_transaction_with_unlocked() throws InvalidTransactionException {
        String preAssignedId = "";
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 3L;
        String orderId = "OID-123";
        double amount = 15d;
        User buyer = new User(buyerId, 100d);
        User seller = new User(sellerId, 100d);

        RedisDistributedLock mockRedisDistributedLock = PowerMockito.mock(RedisDistributedLock.class);
        Whitebox.setInternalState(RedisDistributedLock.class, "INSTANCE", mockRedisDistributedLock);
        PowerMockito.when(mockRedisDistributedLock.lock(anyString())).thenReturn(false);


        UserRepositoryImpl mockUserRepository = PowerMockito.mock(UserRepositoryImpl.class);
        PowerMockito.when(mockUserRepository.find(anyLong())).thenReturn(new User(buyerId, 5d));


        WalletTransaction walletTransaction = new WalletTransaction(preAssignedId, buyerId, sellerId, productId, orderId, amount);
        walletTransaction.setWalletService(new WalletServiceImpl(mockUserRepository));
        boolean result = walletTransaction.execute();

        assertFalse(result);
        assertEquals(100, buyer.getBalance());
        assertEquals(100, seller.getBalance());
    }

    @Test
    void should_execute_return_false_when_given_a_expired_wallet_transaction() throws InvalidTransactionException {
        String preAssignedId = "LB-123";
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 3L;
        String orderId = "OID-123";
        double amount = 15d;
        User buyer = new User(buyerId, 100d);
        User seller = new User(sellerId, 100d);

        RedisDistributedLock mockRedisDistributedLock = PowerMockito.mock(RedisDistributedLock.class);
        Whitebox.setInternalState(RedisDistributedLock.class, "INSTANCE", mockRedisDistributedLock);
        PowerMockito.when(mockRedisDistributedLock.lock(anyString())).thenReturn(true);


        UserRepositoryImpl mockUserRepository = PowerMockito.mock(UserRepositoryImpl.class);
        PowerMockito.when(mockUserRepository.find(buyerId)).thenReturn(buyer);
        PowerMockito.when(mockUserRepository.find(sellerId)).thenReturn(seller);

        WalletTransaction walletTransaction = new WalletTransaction(preAssignedId, buyerId, sellerId, productId, orderId, amount);
        walletTransaction.setWalletService(new WalletServiceImpl(mockUserRepository));

        walletTransaction.setCreatedTimestamp(System.currentTimeMillis() - 2729000000L);
        boolean result = walletTransaction.execute();

        assertFalse(result);
        assertEquals(100, buyer.getBalance());
        assertEquals(100, seller.getBalance());

    }

    @Test
    void should_execute_return_false_when_the_buyer_balance_is_not_enough() throws InvalidTransactionException {
        String preAssignedId = "";
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 3L;
        String orderId = "OID-123";
        double amount = 15d;
        User buyer = new User(buyerId, 10d);
        User seller = new User(sellerId, 100d);

        RedisDistributedLock mockRedisDistributedLock = PowerMockito.mock(RedisDistributedLock.class);
        Whitebox.setInternalState(RedisDistributedLock.class, "INSTANCE", mockRedisDistributedLock);
        PowerMockito.when(mockRedisDistributedLock.lock(anyString())).thenReturn(false);


        UserRepositoryImpl mockUserRepository = PowerMockito.mock(UserRepositoryImpl.class);
        PowerMockito.when(mockUserRepository.find(anyLong())).thenReturn(new User(buyerId, 5d));


        WalletTransaction walletTransaction = new WalletTransaction(preAssignedId, buyerId, sellerId, productId, orderId, amount);
        walletTransaction.setWalletService(new WalletServiceImpl(mockUserRepository));
        boolean result = walletTransaction.execute();

        assertFalse(result);
        assertEquals(10, buyer.getBalance());
        assertEquals(100, seller.getBalance());
    }

}