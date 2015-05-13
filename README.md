## Object Pool for Android
It is crucial on Android to use system resources carefully and smart. Excessive consumption of heap memory will bring to **OutOfMemory** exception and numerous encounters **garbage collector** (GC). **GC** will take precious time from UI thread and it will lead to dropping UI frames that provides bad user experience.

One of the common ways to solve the issue by reusing already allocated objects that prevents of triggering **GC**.

From Wiki:
> The object pool pattern is a software creational design pattern that uses a set of initialized objects kept ready to use – a "pool" – rather than allocating and destroying them on demand. A client of the pool will request an object from the pool and perform operations on the returned object. When the client has finished, it returns the object to the pool rather than destroying it; this can be done manually or automatically. Object pools are primarily used for performance: in some circumstances, object pools significantly improve performance. Object pools complicate object lifetime, as objects obtained from and returned to a pool are not actually created or destroyed at this time, and thus require care in implementation.

#### Usage
##### Init
Create `ObjectPool` to host objects. Object pool provides 2 ways of allocation new objects.

- Override `create` method
```
ObjectPool pool = new ObjectPool() {
  @Override
  protected Object create(Class<?> type) {
    // return type.newInstance();
  }
};
```

- Provide `Factory` to the pool
```
ObjectPool.Factory factory = new ObjectPool.Factory() {
    @Override
    public Object create(Class<?> type) {
        // return type.newInstance();
    }
};
ObjectPool pool = new ObjectPool(factory);
```

##### Acquire
The pool can host multiple types of object sets. Each object has unique key (eg type) represented by `Class<?> type`.

- Acquire default set object `pool.acquire()`. In this case **default type** is used, ignore `type` value on `create` method.
- Acquire object by type `pool.acquire(MyObject.class)`

##### Release
Make sure to release unused object back to the pool before removing reference to it by calling `pool.release(MyObject)`. **Pool always keeps reference to each previously acquired object.**

##### Clear
Clear is optional operation for object pool based on specific implementation. Call it with type to clear specific set `pool.clear(MyObject.class)` otherwise to clear all `pool.clear()` available objects in pool. **Pool will keep references of used objects.**
