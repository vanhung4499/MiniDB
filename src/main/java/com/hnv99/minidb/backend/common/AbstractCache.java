package com.hnv99.minidb.backend.common;

import com.hnv99.minidb.common.Error;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractCache implements a reference-counting cache strategy.
 */
public abstract class AbstractCache<T> {
    private HashMap<Long, T> cache;                     // Actual cached data
    private HashMap<Long, Integer> references;          // Number of references to each element
    private HashMap<Long, Boolean> getting;             // Threads currently getting a resource

    private int maxResource;                            // Maximum number of cached resources
    private int count = 0;                              // Number of elements in the cache
    private Lock lock;

    public AbstractCache(int maxResource) {
        this.maxResource = maxResource;
        cache = new HashMap<>();
        references = new HashMap<>();
        getting = new HashMap<>();
        lock = new ReentrantLock();
    }

    protected T get(long key) throws Exception {
        while (true) {
            lock.lock();
            if (getting.containsKey(key)) {
                // The requested resource is being retrieved by another thread
                lock.unlock();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                continue;
            }

            if (cache.containsKey(key)) {
                // Resource is in the cache, return it directly
                T obj = cache.get(key);
                references.put(key, references.get(key) + 1);
                lock.unlock();
                return obj;
            }

            // Try to retrieve the resource
            if (maxResource > 0 && count == maxResource) {
                lock.unlock();
                throw Error.CacheFullException;
            }
            count++;
            getting.put(key, true);
            lock.unlock();
            break;
        }

        T obj = null;
        try {
            obj = getForCache(key);
        } catch (Exception e) {
            lock.lock();
            count--;
            getting.remove(key);
            lock.unlock();
            throw e;
        }

        lock.lock();
        getting.remove(key);
        cache.put(key, obj);
        references.put(key, 1);
        lock.unlock();

        return obj;
    }

    /**
     * Forcefully release a cached item.
     */
    protected void release(long key) {
        lock.lock();
        try {
            int ref = references.get(key) - 1;
            if (ref == 0) {
                T obj = cache.get(key);
                releaseForCache(obj);
                references.remove(key);
                cache.remove(key);
                count--;
            } else {
                references.put(key, ref);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Close the cache, writing back all resources.
     */
    protected void close() {
        lock.lock();
        try {
            Set<Long> keys = cache.keySet();
            for (long key : keys) {
                T obj = cache.get(key);
                releaseForCache(obj);
                references.remove(key);
                cache.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * Behavior for retrieving a resource when it is not in the cache.
     */
    protected abstract T getForCache(long key) throws Exception;
    /**
     * Behavior for writing back a resource when it is evicted from the cache.
     */
    protected abstract void releaseForCache(T obj);
}
