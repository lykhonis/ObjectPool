package com.vl.android.utils;

import android.support.v4.util.SimpleArrayMap;

/**
 * Object Pool is thread-safe pattern to simplify access and reuse common objects. Particular object
 * pool supports creation of object by using factory pattern as well as multiple type of object sets
 */
public class ObjectPool {

    static final int POOL_INITIAL_CAPACITY = 4;

    static final class DefaultClass {}

    static final Class<?> DEFAULT_TYPE = DefaultClass.class;

    final SimpleArrayMap<Class<?>, Object[]> mPool;
    Object[] mInuse;
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
        mPool = new SimpleArrayMap<>(POOL_INITIAL_CAPACITY);
        mInuse = new Object[POOL_INITIAL_CAPACITY];
    }

    /**
     * Acquire object in pool or create new if does not exist
     *
     * @param type Type of object set
     * @return Object from set type
     */
    @SuppressWarnings("unchecked")
    public <T> T acquire(Class<T> type) {
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
            size = mInuse.length;
            for (int i = 0; i < size; i++) {
                if (mInuse[i] == null) {
                    return (T) (mInuse[i] = object);
                }
            }
            mInuse = grow(mInuse, idealObjectArraySize(size * 2));
            return (T) (mInuse[size] = object);
        }
    }

    int inuse() {
        int size = 0;
        for (Object object : mInuse) {
            if (object != null) size++;
        }
        return size;
    }

    int sizeDefault() {
        return size(DEFAULT_TYPE);
    }

    int size(Class<?> type) {
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
    public void clear(Class<?> type) {
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
                Object[] pool = mPool.valueAt(i);
                if (pool != null) clear(pool);
            }
        }
    }

    /**
     * Acquire object in pool or create new if does not exist
     *
     * @return Object from set type
     */
    @SuppressWarnings("unchecked")
    public <T> T acquire() {
        return (T) acquire(DEFAULT_TYPE);
    }

    /**
     * Release object acquired from pool back
     *
     * @param object Object to release back to pool
     */
    public void release(Object object) {
        synchronized (mPool) {
            int index = indexOf(mInuse, object);
            if (object != null && index >= 0) {
                mInuse[index] = null;
                Class<?> type = object.getClass();
                if (!mPool.containsKey(type)) type = DEFAULT_TYPE;
                Object[] pool = mPool.get(type);
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
    protected Object create(Class<?> type) {
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
        Object create(Class<?> type);
    }

    static int indexOf(Object[] array, Object object) {
        int size = array.length;
        for (int i = 0; i < size; i++) {
            if (array[i] == object) return i;
        }
        return -1;
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