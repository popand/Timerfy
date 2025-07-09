package com.timerfy.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RoomIdGenerator {
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ID_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();
    
    public String generateRoomId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
    
    public boolean isValidRoomId(String roomId) {
        if (roomId == null || roomId.length() != ID_LENGTH) {
            return false;
        }
        
        return roomId.chars()
                .allMatch(c -> CHARACTERS.indexOf(c) >= 0);
    }
}