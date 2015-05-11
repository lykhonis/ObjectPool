package com.vl.android.utils;

import android.util.SparseArray;

/**
 * Object Pool is thread-safe pattern to simplify access and reuse common objects. Particular object
 * pool supports creation of object by using factory pattern as well as multiple type of object sets
 */
public class ObjectPool {

    static final int POOL_INITIAL_CAPACITY = 4;
    static final int DEFAULT_TYPE = 0;

    final SparseArray<Object[]> mPool;
    SlowObjectMap mInuse;
    Factory mFactory;

    /**
     * Create empty thread-safe object pool. Override {@link #create(int)} to create new objects
     */
    public ObjectPool() {
        this(null);
    }

    /**
     * Create empty thread-safe object pool w {@link com.vl.android.utils.ObjectPool.Factory}
     *
     * @param factory Factory
     */
    public ObjectPool(Factory factory) {
        mFactory = factory;
        mPool = new SparseArray<>(POOL_INITIAL_CAPACITY);
        mInuse = new SlowObjectMap(POOL_INITIAL_CAPACITY);
    }

    /**
     * Acquire object in pool or create new if does not exist
     *
     * @param type Type of object set
     * @return Object from set type
     */
    @SuppressWarnings("unchecked")
    public <T> T acquire(int type) {
        synchronized (mPool) {
            Object[] pool = mPool.get(type);
            if (pool == null) {
                mPool.put(type, pool = new Object[POOL_INITIAL_CAPACITY]);
            }
            Object object = null;
            int size = pool.length;
            for (int i = 0; i < size; i++) {
                if (pool[i] != null) {
                    object = pool[i];
                    pool[i] = null;
                    break;
                }
            }
            if (object == null && (object = create(type)) == null) {
                throw new NullPointerException("Create has to return non-null object!");
            }
            mInuse.put(object, type);
            return (T) object;
        }
    }

    int size(int type) {
        int size = 0;
        Object[] pool = mPool.get(type);
        if (pool != null) {
            for (Object object : pool) {
                if (object != null) size++;
            }
        }
        return size;
    }

    /**
     * Removes all objects of set type
     *
     * @param type Type of object set
     */
    public void clear(int type) {
        synchronized (mPool) {
            Object[] pool = mPool.get(type);
            if (pool != null) clear(pool);
        }
    }

    /**
     * Removes all objects and sets
     */
    public void clear() {
        synchronized (mPool) {
            int size = mPool.size();
            for (int i = 0; i < size; i++) {
                clear(mPool.valueAt(i));
            }
        }
    }

    /**
     * Acquire object in pool or create new if does not exist
     *
     * @return Object from set type
     */
    public <T> T acquire() {
        return acquire(DEFAULT_TYPE);
    }

    /**
     * Release object acquired from pool back
     *
     * @param object Object to release back to pool
     */
    public void release(Object object) {
        synchronized (mPool) {
            int index = mInuse.indexOf(object);
            if (index >= 0) {
                int type = mInuse.removeAt(index);
                Object[] pool = mPool.get(type);
                if (pool == null) {
                    mPool.put(type, pool = new Object[POOL_INITIAL_CAPACITY]);
                }
                int size = pool.length;
                for (int i = 0; i < size; i++) {
                    if (pool[i] == null) {
                        pool[i] = object;
                        return;
                    }
                }
                pool = grow(pool, idealObjectArraySize(size * 2));
                pool[size] = object;
                mPool.put(type, pool);
            }
        }
    }

    /**
     * Create new object for type set
     *
     * @param type Type of object set
     * @return Non-null object
     */
    protected Object create(int type) {
        return mFactory == null ? null : mFactory.create(type);
    }

    /**
     * Factory to create objects for pool
     */
    public interface Factory {
        /**
         * Create new object for type set
         *
         * @param type Type of object set
         * @return Non-null object
         */
        Object create(int type);
    }

    static class SlowObjectMap {

        Object[] mKeys;
        int[] mValues;
        int mSize;

        public SlowObjectMap(int initialSize) {
            mKeys = new Object[initialSize];
            mValues = new int[mKeys.length];
        }

        public int size() {
            return mSize;
        }

        public int capacity() {
            return mKeys.length;
        }

        public void put(Object key, int value) {
            if (mKeys.length == mSize) {
                int size = idealObjectArraySize(mKeys.length * 2);
                mKeys = grow(mKeys, size);
                mValues = grow(mValues, size);
            }
            int size = mKeys.length;
            for (int i = 0; i < size; i++) {
                if (mKeys[i] == null) {
                    mKeys[i] = key;
                    mValues[i] = value;
                    mSize++;
                    return;
                }
            }
            throw new IllegalStateException("Map is corrupted");
        }

        public int indexOf(Object key) {
            int size = mKeys.length;
            for (int i = 0; i < size; i++) {
                if (mKeys[i] == key) return i;
            }
            return -1;
        }

        public int removeAt(int index) {
            mKeys[index] = null;
            mSize--;
            return mValues[index];
        }
    }

    static void clear(Object[] array) {
        int size = array.length;
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
    }

    static Object[] grow(Object[] array, int size) {
        Object[] result = new Object[size];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    static int[] grow(int[] array, int size) {
        int[] result = new int[size];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    static int idealObjectArraySize(int need) {
        return idealByteArraySize(need * 4) / 4;
    }

    static int idealByteArraySize(int need) {
        for (int i = 4; i < 32; i++)
            if (need <= (1 << i) - 12)
                return (1 << i) - 12;
        return need;
    }
}