package com.fwdrobo.roombooking.api;

import com.fwdrobo.roombooking.repository.InMemoryBookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InMemoryBookingRepository bookingRepository;

    /*
    与 booking-2021 重叠，必须返回 false
     */
    @Test
    void hasWindowOverlapsFirst() throws Exception {
        mockMvc.perform(get("/rooms/room-202/availability")
                        .param("start", "2030-01-15T10:15:00")
                        .param("end", "2030-01-15T10:45:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    /*
    与 booking-2022 重叠，必须返回 false
     */
    @Test
    void hasWindowOverlapsSecond() throws Exception {
        mockMvc.perform(get("/rooms/room-202/availability")
                        .param("start", "2030-01-15T12:15:00")
                        .param("end", "2030-01-15T12:45:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    /*
    未发生时间段重叠，返回true
     */
    @Test
    void  noWindowOverlaps() throws Exception {
        mockMvc.perform(get("/rooms/room-202/availability")
                        .param("start", "2030-01-15T14:00:00")
                        .param("end", "2030-01-15T14:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    /*
        成功预约room-202房间
     */
    @Test
    void sucessfulBooking() throws Exception {
        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "start": "2030-01-15T14:00:00",
                              "end": "2030-01-15T14:30:00"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("booking-2023"))
                .andExpect(jsonPath("$.roomId").value("room-202"))
                .andExpect(jsonPath("$.start").value("2030-01-15T14:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-15T14:30:00"))
                .andExpect(header().exists("Location"));
    }

    /*
    无法预约，因为时间窗口非法，且失败请求并未改变数据
     */
    @Test
    void failedBooking_invalidWindow() throws Exception {
        long beforeCount = bookingRepository.countByRoomId("room-202");
        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start": "2030-01-15T14:00:00",
                          "end": "2030-01-15T14:20:00"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_BOOKING_WINDOW"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/rooms/room-202/bookings"));
        long afterCount = bookingRepository.countByRoomId("room-202");
        assertEquals(beforeCount, afterCount);
    }

    /*
    预约失败，因为room并不存在，且并未改变数据
     */
    @Test
    void failedBooking_notExistRoom() throws Exception {
        long beforeCount = bookingRepository.countByRoomId("room-202");
        mockMvc.perform(post("/rooms/room-missing/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start": "2030-01-15T14:00:00",
                          "end": "2030-01-15T14:30:00"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/rooms/room-missing/bookings"));
        long afterCount = bookingRepository.countByRoomId("room-202");
        assertEquals(beforeCount, afterCount);
    }

    /*
    边界测试,对应房间下预约数+1
     */
    @Test
    void successfulBooking_boundaryTest() throws Exception {
        long beforeCount = bookingRepository.countByRoomId("room-202");
        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start": "2030-01-15T10:30:00",
                          "end": "2030-01-15T11:00:00"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.roomId").value("room-202"))
                .andExpect(jsonPath("$.start").value("2030-01-15T10:30:00"))
                .andExpect(jsonPath("$.end").value("2030-01-15T11:00:00"))
                .andExpect(header().exists("Location"));
        long afterCount = bookingRepository.countByRoomId("room-202");
        assertEquals(beforeCount + 1, afterCount);
    }

    @Test
    void returnsExistingBooking() throws Exception {
        mockMvc.perform(get("/rooms/room-101/bookings/booking-1011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("booking-1011"))
                .andExpect(jsonPath("$.roomId").value("room-101"))
                .andExpect(jsonPath("$.start").value("2030-01-15T09:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-15T09:30:00"));
    }

    @Test
    void returnsNotFoundForMissingRoom() throws Exception {
        mockMvc.perform(get("/rooms/room-missing/bookings/booking-1011"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/rooms/room-missing/bookings/booking-1011"));
    }
}
