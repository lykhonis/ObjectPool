package com.vl.android.utils;

import android.test.AndroidTestCase;

public class ObjectPoolTest extends AndroidTestCase {

    public void testAllocation() {
        ObjectPool pool = new ObjectPool(mFactory);

        Object[][] objects = new Object[3][1000];
        for (int i = 0; i < 1000; i++) {
            objects[0][i] = pool.acquire(ObjectA.TYPE);
            objects[1][i] = pool.acquire(ObjectB.TYPE);
            objects[2][i] = pool.acquire(ObjectC.TYPE);
        }

        assertEquals(pool.mInuse.size(), 3 * 1000);

        for (int i = 0; i < 1000; i++) {
            assertNotNull(objects[0][i]);
            assertNotNull(objects[1][i]);
            assertNotNull(objects[2][i]);
            pool.release(objects[0][i]);
            pool.release(objects[1][i]);
            pool.release(objects[2][i]);
        }

        assertEquals(pool.mInuse.size(), 0);
        assertEquals(pool.size(ObjectA.TYPE), 1000);
        assertEquals(pool.size(ObjectB.TYPE), 1000);
        assertEquals(pool.size(ObjectC.TYPE), 1000);

        pool.clear(ObjectA.TYPE);
        assertEquals(pool.size(ObjectA.TYPE), 0);
        assertEquals(pool.size(ObjectB.TYPE), 1000);
        assertEquals(pool.size(ObjectC.TYPE), 1000);

        pool.clear();
        assertEquals(pool.size(ObjectA.TYPE), 0);
        assertEquals(pool.size(ObjectB.TYPE), 0);
        assertEquals(pool.size(ObjectC.TYPE), 0);
    }

    static class ObjectA {
        static final int TYPE = 1;
    }

    static class ObjectB {
        static final int TYPE = 2;
    }

    static class ObjectC {
        static final int TYPE = 3;
    }

    static final ObjectPool.Factory mFactory = new ObjectPool.Factory() {
        @Override
        public Object create(int type) {
            if (type == ObjectA.TYPE) return new ObjectA();
            if (type == ObjectB.TYPE) return new ObjectB();
            if (type == ObjectC.TYPE) return new ObjectC();
            throw new RuntimeException("Invalid type: " + type);
        }
    };
}
