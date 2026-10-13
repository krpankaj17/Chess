package com.project.chess;

import com.project.chess.controller.GameWebSocketController;
import com.project.chess.dto.GameStateMessage;
import com.project.chess.dto.MoveMessage;
import com.project.chess.entity.ChessGame;
import com.project.chess.entity.User;
import com.project.chess.enums.Color;
import com.project.chess.enums.GameStatus;
import com.project.chess.repository.UserRepository;
import com.project.chess.service.EloService;
import com.project.chess.service.GameSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameSessionAndWebSocketTests {

    private GameSessionManager gameSessionManager;
    private SimpMessagingTemplate messagingTemplate;
    private EloService eloService;
    private UserRepository userRepository;
    private GameWebSocketController controller;

    @BeforeEach
    void setUp() {
        gameSessionManager = new GameSessionManager();
        messagingTemplate = mock(SimpMessagingTemplate.class);
        eloService = new EloService();
        userRepository = mock(UserRepository.class);
        com.project.chess.service.ChessRuleService ruleService = new com.project.chess.service.ChessRuleService();
        com.project.chess.service.ChessGameService gameService = new com.project.chess.service.ChessGameService(ruleService);

        controller = new GameWebSocketController(gameSessionManager, messagingTemplate, eloService, userRepository, gameService);
    }

    @Test
    void testGameSessionCreation() {
        ChessGame game = gameSessionManager.createGame("g1", "alice", "bob");
        assertNotNull(game);
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals("alice", game.getWhitePlayer().getName());
        assertEquals("bob", game.getBlackPlayer().getName());
        assertEquals(1, gameSessionManager.getActiveGameCount());
    }

    @Test
    void testProcessMoveBroadcastsState() {
        gameSessionManager.createGame("g1", "alice", "bob");

        Principal alicePrincipal = () -> "alice";
        MoveMessage moveMsg = MoveMessage.builder()
                .fromRow(1).fromCol(4)
                .toRow(3).toCol(4)
                .build();

        controller.processMove("g1", moveMsg, alicePrincipal);

        ChessGame game = gameSessionManager.getGame("g1");
        assertEquals(Color.BLACK, game.getCurrentTurn());

        ArgumentCaptor<GameStateMessage> captor = ArgumentCaptor.forClass(GameStateMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/g1"), captor.capture());

        GameStateMessage sent = captor.getValue();
        assertEquals("g1", sent.getGameId());
        assertEquals("BLACK", sent.getCurrentTurn());
        assertFalse(sent.isGameOver());
    }

    @Test
    void testResignUpdatesWinnerAndElo() {
        gameSessionManager.createGame("g2", "alice", "bob");

        User aliceUser = User.builder().username("alice").eloRating(1200).build();
        User bobUser = User.builder().username("bob").eloRating(1200).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(aliceUser));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bobUser));

        Principal alicePrincipal = () -> "alice";
        controller.resign("g2", alicePrincipal);

        ArgumentCaptor<GameStateMessage> captor = ArgumentCaptor.forClass(GameStateMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/g2"), captor.capture());

        GameStateMessage sent = captor.getValue();
        assertTrue(sent.isGameOver());
        assertEquals("bob", sent.getWinner());
        assertEquals(1184, sent.getWhiteElo());
        assertEquals(1216, sent.getBlackElo());
    }
}
