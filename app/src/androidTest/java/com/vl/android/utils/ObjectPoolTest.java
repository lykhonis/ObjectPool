package com.vl.android.utils;

import android.test.AndroidTestCase;

public class ObjectPoolTest extends AndroidTestCase {

    public void testAllocation() {
        ObjectPool pool = new ObjectPool(mFactory);

        Object[][] objects = new Object[3][1000];
        for (int i = 0; i < 1000; i++) {
            objects[0][i] = pool.acquire();
            objects[1][i] = pool.acquire(ObjectB.class);
            objects[2][i] = pool.acquire(ObjectC.class);
        }

        assertEquals(pool.inuse(), 3 * 1000);

        for (int i = 0; i < 1000; i++) {
            assertNotNull(objects[0][i]);
            assertNotNull(objects[1][i]);
            assertNotNull(objects[2][i]);
            pool.release(objects[0][i]);
            pool.release(objects[1][i]);
            pool.release(objects[2][i]);
        }

        assertEquals(pool.inuse(), 0);
        assertEquals(pool.size(ObjectA.class), 0);
        assertEquals(pool.sizeDefault(), 1000);
        assertEquals(pool.size(ObjectB.class), 1000);
        assertEquals(pool.size(ObjectC.class), 1000);

        pool.clear(ObjectB.class);
        assertEquals(pool.size(ObjectA.class), 0);
        assertEquals(pool.size(ObjectB.class), 0);
        assertEquals(pool.size(ObjectC.class), 1000);

        pool.clear();
        assertEquals(pool.size(ObjectA.class), 0);
        assertEquals(pool.size(ObjectB.class), 0);
        assertEquals(pool.size(ObjectC.class), 0);
    }

    static class ObjectA {}

    static class ObjectB {}

    static class ObjectC {}

    static final ObjectPool.Factory mFactory = new ObjectPool.Factory() {
        @Override
        public Object create(Class<?> type) {
            if (type == ObjectB.class) return new ObjectB();
            if (type == ObjectC.class) return new ObjectC();
            return new ObjectA();
        }
    };
}
