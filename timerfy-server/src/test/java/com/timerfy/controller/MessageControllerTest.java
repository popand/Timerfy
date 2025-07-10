package com.timerfy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timerfy.dto.CreateMessageRequest;
import com.timerfy.dto.UpdateMessageRequest;
import com.timerfy.model.Message;
import com.timerfy.model.MessagePriority;
import com.timerfy.service.RoomService;
import com.timerfy.websocket.MessageEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private MessageEventListener messageEventListener;

    private Message testMessage;
    private final String TEST_ROOM_ID = "ABC123";
    private final String TEST_MESSAGE_ID = "message-1";

    @BeforeEach
    void setUp() {
        testMessage = new Message("Test message", MessagePriority.HIGH, "#FF0000");
        testMessage.setId(TEST_MESSAGE_ID);
    }

    @Test
    void createMessage_ShouldCreateMessageSuccessfully() throws Exception {
        // Given
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("New test message");
        request.setPriority(MessagePriority.HIGH);
        request.setColor("#FF0000");
        request.setAutoShow(true);
        request.setDuration(5000L);

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.addMessageToRoom(eq(TEST_ROOM_ID), any(Message.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("New test message"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.color").value("#FF0000"))
                .andExpect(jsonPath("$.data.autoShow").value(true))
                .andExpect(jsonPath("$.data.displayDuration").value(5000));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).addMessageToRoom(eq(TEST_ROOM_ID), any(Message.class));
        verify(messageEventListener).handleMessageCreated(eq(TEST_ROOM_ID), any(Message.class));
    }

    @Test
    void createMessage_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("Test message");
        request.setPriority(MessagePriority.NORMAL);
        request.setColor("#FF0000");

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService, never()).addMessageToRoom(anyString(), any(Message.class));
        verify(messageEventListener, never()).handleMessageCreated(anyString(), any(Message.class));
    }

    @Test
    void createMessage_ShouldReturn500WhenCreationFails() throws Exception {
        // Given
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("Test message");
        request.setPriority(MessagePriority.NORMAL);
        request.setColor("#FF0000");

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.addMessageToRoom(eq(TEST_ROOM_ID), any(Message.class))).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CREATE_FAILED"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).addMessageToRoom(eq(TEST_ROOM_ID), any(Message.class));
        verify(messageEventListener, never()).handleMessageCreated(anyString(), any(Message.class));
    }

    @Test
    void createMessage_ShouldValidateRequiredFields() throws Exception {
        // Given
        CreateMessageRequest request = new CreateMessageRequest();
        // Missing required text field

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).roomExists(anyString());
        verify(roomService, never()).addMessageToRoom(anyString(), any(Message.class));
    }

    @Test
    void updateMessage_ShouldUpdateMessageSuccessfully() throws Exception {
        // Given
        UpdateMessageRequest request = new UpdateMessageRequest();
        request.setText("Updated message");
        request.setVisible(false);
        request.setPriority(MessagePriority.CRITICAL);
        request.setColor("#00FF00");

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));
        when(roomService.updateMessageInRoom(TEST_ROOM_ID, testMessage)).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/messages/{messageId}", TEST_ROOM_ID, TEST_MESSAGE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("Updated message"))
                .andExpect(jsonPath("$.data.visible").value(false))
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.data.color").value("#00FF00"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService).updateMessageInRoom(TEST_ROOM_ID, testMessage);
    }

    @Test
    void updateMessage_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        UpdateMessageRequest request = new UpdateMessageRequest();
        request.setText("Updated message");

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/messages/{messageId}", TEST_ROOM_ID, TEST_MESSAGE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService, never()).getMessageFromRoom(anyString(), anyString());
        verify(roomService, never()).updateMessageInRoom(anyString(), any(Message.class));
    }

    @Test
    void updateMessage_ShouldReturn404WhenMessageNotFound() throws Exception {
        // Given
        UpdateMessageRequest request = new UpdateMessageRequest();
        request.setText("Updated message");

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/messages/{messageId}", TEST_ROOM_ID, TEST_MESSAGE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MESSAGE_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService, never()).updateMessageInRoom(anyString(), any(Message.class));
    }

    @Test
    void updateMessage_ShouldHandlePartialUpdate() throws Exception {
        // Given
        UpdateMessageRequest request = new UpdateMessageRequest();
        request.setText("Updated message only");
        // Only updating text, other fields should remain unchanged

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));
        when(roomService.updateMessageInRoom(TEST_ROOM_ID, testMessage)).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/messages/{messageId}", TEST_ROOM_ID, TEST_MESSAGE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("Updated message only"))
                .andExpect(jsonPath("$.data.priority").value("HIGH")) // Should remain unchanged
                .andExpect(jsonPath("$.data.color").value("#FF0000")); // Should remain unchanged

        verify(roomService).updateMessageInRoom(TEST_ROOM_ID, testMessage);
    }

    @Test
    void deleteMessage_ShouldDeleteMessageSuccessfully() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));
        when(roomService.removeMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}/messages/{messageId}", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Message deleted successfully"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService).removeMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
    }

    @Test
    void deleteMessage_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}/messages/{messageId}", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService, never()).getMessageFromRoom(anyString(), anyString());
        verify(roomService, never()).removeMessageFromRoom(anyString(), anyString());
    }

    @Test
    void deleteMessage_ShouldReturn404WhenMessageNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}/messages/{messageId}", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MESSAGE_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService, never()).removeMessageFromRoom(anyString(), anyString());
    }

    @Test
    void showMessage_ShouldShowMessageSuccessfully() throws Exception {
        // Given
        testMessage.setVisible(false); // Initially hidden
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));
        when(roomService.updateMessageInRoom(TEST_ROOM_ID, testMessage)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages/{messageId}/show", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visible").value(true));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService).updateMessageInRoom(TEST_ROOM_ID, testMessage);
    }

    @Test
    void showMessage_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages/{messageId}/show", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService, never()).getMessageFromRoom(anyString(), anyString());
        verify(roomService, never()).updateMessageInRoom(anyString(), any(Message.class));
    }

    @Test
    void showMessage_ShouldReturn404WhenMessageNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages/{messageId}/show", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MESSAGE_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService, never()).updateMessageInRoom(anyString(), any(Message.class));
    }

    @Test
    void hideMessage_ShouldHideMessageSuccessfully() throws Exception {
        // Given
        testMessage.setVisible(true); // Initially visible
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));
        when(roomService.updateMessageInRoom(TEST_ROOM_ID, testMessage)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages/{messageId}/hide", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visible").value(false));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService).updateMessageInRoom(TEST_ROOM_ID, testMessage);
    }

    @Test
    void hideMessage_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages/{messageId}/hide", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService, never()).getMessageFromRoom(anyString(), anyString());
        verify(roomService, never()).updateMessageInRoom(anyString(), any(Message.class));
    }

    @Test
    void hideMessage_ShouldReturn404WhenMessageNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages/{messageId}/hide", TEST_ROOM_ID, TEST_MESSAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MESSAGE_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID);
        verify(roomService, never()).updateMessageInRoom(anyString(), any(Message.class));
    }

    @Test
    void createMessage_ShouldHandleOptionalFields() throws Exception {
        // Given
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("Simple message");
        request.setPriority(MessagePriority.LOW);
        request.setColor("#0000FF");
        // autoShow and duration not set

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.addMessageToRoom(eq(TEST_ROOM_ID), any(Message.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("Simple message"))
                .andExpect(jsonPath("$.data.priority").value("LOW"))
                .andExpect(jsonPath("$.data.color").value("#0000FF"));

        verify(roomService).addMessageToRoom(eq(TEST_ROOM_ID), any(Message.class));
        verify(messageEventListener).handleMessageCreated(eq(TEST_ROOM_ID), any(Message.class));
    }

    @Test
    void allEndpoints_ShouldHandleCorsHeaders() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.getMessageFromRoom(TEST_ROOM_ID, TEST_MESSAGE_ID)).thenReturn(Optional.of(testMessage));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/messages/{messageId}/show", TEST_ROOM_ID, TEST_MESSAGE_ID)
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}