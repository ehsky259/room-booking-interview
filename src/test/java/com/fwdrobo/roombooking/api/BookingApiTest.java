package com.fwdrobo.roombooking.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mockMvc;

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
                        .param("start", "2030-01-15T10:30:00")
                        .param("end", "2030-01-15T11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
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
