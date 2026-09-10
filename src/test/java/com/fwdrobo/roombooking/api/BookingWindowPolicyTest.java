package com.fwdrobo.roombooking.api;

import com.fwdrobo.roombooking.domain.BookingWindowPolicy;
import com.fwdrobo.roombooking.domain.BookingWindowResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

@SpringBootTest
public class BookingWindowPolicyTest {

    @Autowired
    private BookingWindowPolicy bookingWindowPolicy;

    /*
    start为空
    预期结果：MISSING_BOUNDARY
     */
    @Test
    void start_is_missing() {
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 12, 0);
        BookingWindowResult result = bookingWindowPolicy.evaluate(null, end);
        assertEquals(BookingWindowResult.MISSING_BOUNDARY, result);
    }

    /*
    end为空
    预期结果：MISSING_BOUNDARY
     */
    @Test
    void end_is_missing() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 12, 0);
        BookingWindowResult result = bookingWindowPolicy.evaluate(start, null);
        assertEquals(BookingWindowResult.MISSING_BOUNDARY, result);
    }

    /*
    start晚于end
    预期结果：END_NOT_AFTER_START
     */
    @Test
    void start_is_after_end() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 12, 30);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 12, 0);
        BookingWindowResult result = bookingWindowPolicy.evaluate(start, end);
        assertEquals(BookingWindowResult.END_NOT_AFTER_START, result);
    }

    /*
    下边界:30
    预期结果：Valid
    判断过程：end与start的差值为30分钟，恰好符合边界情况
     */
    @Test
    void minutes_is_30() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 12, 30);
        BookingWindowResult result = bookingWindowPolicy.evaluate(start, end);
        assertEquals(BookingWindowResult.VALID, result);
    }

    /*
        上边界:120
        预期结果：Valid
        判断过程：end与start的差值为120分钟，恰好符合边界情况
         */
    @Test
    void minutes_is_120() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 14, 0);
        BookingWindowResult result = bookingWindowPolicy.evaluate(start, end);
        assertEquals(BookingWindowResult.VALID, result);
    }

    /*
    下边界相邻值:25
    预期结果：DURATION_OUT_OF_RANGE
    判断过程：end与start的差值为25分钟，低于下边界30分钟
     */
    @Test
    void minutes_is_25() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 12, 25);
        BookingWindowResult result = bookingWindowPolicy.evaluate(start, end);
        assertEquals(BookingWindowResult.DURATION_OUT_OF_RANGE, result);
    }

    /*
    上边界相邻值:125
    预期结果：DURATION_OUT_OF_RANGE
    判断过程：end与start的差值为125分钟，高于上边界120分钟
     */
    @Test
    void minutes_is_125() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 14, 5);
        BookingWindowResult result = bookingWindowPolicy.evaluate(start, end);
        assertEquals(BookingWindowResult.DURATION_OUT_OF_RANGE, result);
    }

    /*
    同时违反start晚于end、时长超出30-120的范围
    预期结果：END_NOT_AFTER_START
    判断过程：由于start晚于end且判断逻辑靠前，因此返回END_NOT_AFTER_START而不是DURATION_OUT_OF_RANGE
     */
    @Test
    void minutes_is_valid() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 14, 5);
        BookingWindowResult result = bookingWindowPolicy.evaluate(start, end);
        assertEquals(BookingWindowResult.DURATION_OUT_OF_RANGE, result);
    }
}