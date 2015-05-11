package com.vl.android.utils;

import android.util.SparseArray;

public class ObjectPool<T> {

    static final int POOL_INITIAL_CAPACITY = 4;
    static final int DEFAULT_TYPE = 0;

    final SparseArray<Object[]> mPool;
    SlowObjectMap mInuse;
    Factory<T> mFactory;

    public ObjectPool() {
        this(null);
    }

    public ObjectPool(Factory<T> factory) {
        mFactory = factory;
        mPool = new SparseArray<>(POOL_INITIAL_CAPACITY);
        mInuse = new SlowObjectMap(POOL_INITIAL_CAPACITY);
    }

    public String debug() {
        return "pool: size=" + mPool.size() + "\n" +
                "inuse: size=" + mInuse.size() + ", capacity=" + mInuse.capacity() + "\n" +
                poolDebug();
    }

    String poolDebug() {
        StringBuilder builder = new StringBuilder();
        int size = mPool.size();
        for (int i = 0; i < size; i++) {
            Object[] pool = mPool.valueAt(i);
            int count = 0;
            for (Object aPool : pool) {
                if (aPool != null) count++;
            }
            builder.append("pool: ")
                   .append(i)
                   .append(", size=")
                   .append(count)
                   .append(", capacity=")
                   .append(pool.length)
                   .append('\n');
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public T acquire(int type) {
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
            if (object == null) object = create(type);
            mInuse.put(object, type);
            return (T) object;
        }
    }

    public T acquire() {
        return acquire(DEFAULT_TYPE);
    }

    public void release(T object) {
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

    protected T create(int type) {
        return mFactory == null ? null : mFactory.create(type);
    }

    public interface Factory<T> {
        T create(int type);
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