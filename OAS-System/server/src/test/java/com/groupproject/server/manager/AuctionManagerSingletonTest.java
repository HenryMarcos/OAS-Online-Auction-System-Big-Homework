package com.groupproject.server.manager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AuctionManagerSingletonTest {

    @Test
    @DisplayName("getInstance() luôn trả về cùng một đối tượng")
    void shouldReturnSameInstance() {

        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();

        assertSame(instance1, instance2,
                "Hai instance phải là cùng một đối tượng Singleton");
    }

    @Test
    @DisplayName("auctionMap phải được khởi tạo thành công")
    void shouldInitializeAuctionMap() throws Exception {

        AuctionManager manager = AuctionManager.getInstance();

        Field field = AuctionManager.class.getDeclaredField("auctionMap");
        field.setAccessible(true);

        Object value = field.get(manager);

        assertNotNull(value,
                "auctionMap không được phép null");

        assertTrue(value instanceof ConcurrentHashMap,
                "auctionMap phải là ConcurrentHashMap");
    }

    @Test
    @DisplayName("Constructor phải là private")
    void constructorShouldBePrivate() {

        Constructor<?>[] constructors =
                AuctionManager.class.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {

            assertTrue(
                    Modifier.isPrivate(constructor.getModifiers()),
                    "Constructor của Singleton phải là private"
            );
        }
    }

    @Test
    @DisplayName("Singleton phải an toàn trong môi trường đa luồng")
    void shouldBeThreadSafeSingleton() throws Exception {

        int threadCount = 50;

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        Set<AuctionManager> instances =
                new ConcurrentSkipListSet<>(
                        (a, b) -> System.identityHashCode(a)
                                - System.identityHashCode(b)
                );

        CountDownLatch latch =
                new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {

            executor.execute(() -> {

                AuctionManager instance =
                        AuctionManager.getInstance();

                instances.add(instance);

                latch.countDown();
            });
        }

        latch.await();

        executor.shutdown();

        assertEquals(
                1,
                instances.size(),
                "Chỉ được tồn tại đúng một instance của AuctionManager"
        );
    }

    @Test
    @DisplayName("Gọi getInstance() nhiều lần không được tạo đối tượng mới")
    void shouldNotCreateDifferentInstances() {

        AuctionManager first =
                AuctionManager.getInstance();

        for (int i = 0; i < 1000; i++) {

            AuctionManager current =
                    AuctionManager.getInstance();

            assertSame(
                    first,
                    current,
                    "Mọi lần gọi getInstance() phải trả về cùng một object"
            );
        }
    }
}