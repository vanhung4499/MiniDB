package com.hnv99.minidb.backend.vm;


import com.hnv99.minidb.common.Error;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Maintains a dependency wait graph for deadlock detection.
 */
public class LockTable {

    private Map<Long, List<Long>> x2u;  // List of UIDs that a certain XID
    private Map<Long, Long> u2x;        // UID held by a certain XID
    private Map<Long, List<Long>> wait; // List of XIDs waiting for a UID
    private Map<Long, Lock> waitLock;   // Locks of XIDs waiting for resources
    private Map<Long, Long> waitU;      // UID that XIDs are waiting for
    private Lock lock;

    public LockTable() {
        x2u = new HashMap<>();
        u2x = new HashMap<>();
        wait = new HashMap<>();
        waitLock = new HashMap<>();
        waitU = new HashMap<>();
        lock = new ReentrantLock();
    }

    // Returns null if there is no need to wait, otherwise returns the lock object
    // Will cause deadlock and throw an exception
    public Lock add(long xid, long uid) throws Exception {
        lock.lock();
        try {
            if(isInList(x2u, xid, uid)) {
                return null;
            }
            if(!u2x.containsKey(uid)) {
                u2x.put(uid, xid);
                putIntoList(x2u, xid, uid);
                return null;
            }
            waitU.put(xid, uid);
            //putIntoList(wait, xid, uid);
            putIntoList(wait, uid, xid);
            if(hasDeadLock()) {
                waitU.remove(xid);
                removeFromList(wait, uid, xid);
                throw Error.DeadlockException;
            }
            Lock l = new ReentrantLock();
            l.lock();
            waitLock.put(xid, l);
            return l;

        } finally {
            lock.unlock();
        }
    }

    public void remove(long xid) {
        lock.lock();
        try {
            List<Long> l = x2u.get(xid);
            if(l != null) {
                while(!l.isEmpty()) {
                    Long uid = l.remove(0);
                    selectNewXID(uid);
                }
            }
            waitU.remove(xid);
            x2u.remove(xid);
            waitLock.remove(xid);

        } finally {
            lock.unlock();
        }
    }

    // 从等待队列中选择一个xid来占用uid
    private void selectNewXID(long uid) {
        u2x.remove(uid);
        List<Long> l = wait.get(uid);
        if(l == null) return;
        assert !l.isEmpty();

        while(!l.isEmpty()) {
            long xid = l.remove(0);
            if(!waitLock.containsKey(xid)) {
                continue;
            } else {
                u2x.put(uid, xid);
                Lock lo = waitLock.remove(xid);
                waitU.remove(xid);
                lo.unlock();
                break;
            }
        }

        if(l.isEmpty()) wait.remove(uid);
    }

    private Map<Long, Integer> xidStamp;
    private int stamp;

    private boolean hasDeadLock() {
        xidStamp = new HashMap<>();
        stamp = 1;
        for(long xid : x2u.keySet()) {
            Integer s = xidStamp.get(xid);
            if(s != null && s > 0) {
                continue;
            }
            stamp ++;
            if(dfs(xid)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfs(long xid) {
        Integer stp = xidStamp.get(xid);
        if(stp != null && stp == stamp) {
            return true;
        }
        if(stp != null && stp < stamp) {
            return false;
        }
        xidStamp.put(xid, stamp);

        Long uid = waitU.get(xid);
        if(uid == null) return false;
        Long x = u2x.get(uid);
        assert x != null;
        return dfs(x);
    }

    private void removeFromList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        List<Long> l = listMap.get(uid0);
        if(l == null) return;
        Iterator<Long> i = l.iterator();
        while(i.hasNext()) {
            long e = i.next();
            if(e == uid1) {
                i.remove();
                break;
            }
        }
        if(l.isEmpty()) {
            listMap.remove(uid0);
        }
    }

    private void putIntoList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        if(!listMap.containsKey(uid0)) {
            listMap.put(uid0, new ArrayList<>());
        }
        listMap.get(uid0).add(0, uid1);
    }

    private boolean isInList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        List<Long> l = listMap.get(uid0);
        if(l == null) return false;
        for (long e : l) {
            if (e == uid1) {
                return true;
            }
        }
        return false;
    }

}
