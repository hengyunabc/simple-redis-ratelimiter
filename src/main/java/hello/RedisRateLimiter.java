package hello;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.support.atomic.RedisAtomicLong;

public class RedisRateLimiter {

    RedisAtomicLong counter;

    long permitsPerSecond;

    public RedisRateLimiter(RedisAtomicLong counter, long permitsPerSecond) {
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
        
        if(value > permitsPerSecond){
            Long expire = counter.getExpire();
            // 防止 value == 1时的expire操作没有成功
            if (expire == -1 || expire > 1) {
                counter.expire(1, TimeUnit.SECONDS);
            }
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
