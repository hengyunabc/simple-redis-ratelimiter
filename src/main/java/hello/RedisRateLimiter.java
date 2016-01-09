package hello;

import java.util.concurrent.TimeUnit;

public class RedisRateLimiter {

    MyRedisAtomicLong counter;

    long permitsPerSecond;

    public RedisRateLimiter(MyRedisAtomicLong counter, long permitsPerSecond) {
        this.counter = counter;
        this.permitsPerSecond = permitsPerSecond;
    }

    // public double acquire(){
    //
    // }
    //
    // double acquire(int permits){
    //
    // }
    //
    // double getRate(){
    //
    // }

    boolean tryAcquire() {
        return tryAcquire(1);
    }

    boolean tryAcquire(int permits) {
        long value = counter.addAndGet(permits);
        if (value == 1) {
            counter.expire(1, TimeUnit.SECONDS);
        }
        Long expire = counter.getExpire(TimeUnit.MILLISECONDS);
        // 防止 value == 0时的expire操作没有成功
        if (expire == -1) {
            counter.expire(1, TimeUnit.SECONDS);
        }

        return value <= permitsPerSecond;
    }

    // boolean tryAcquire(int permits, long timeout, TimeUnit unit){
    //
    // }
    //
    // boolean tryAcquire(long timeout, TimeUnit unit) {
    //
    // }
}
