package com.project.chess;

import com.project.chess.entity.ChessGame;
import com.project.chess.entity.Move;
import com.project.chess.entity.Player;
import com.project.chess.entity.Position;
import com.project.chess.enums.Color;
import com.project.chess.enums.GameStatus;
import com.project.chess.service.ChessGameService;
import com.project.chess.service.ChessRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessGameTests {

    private ChessGameService gameService;
    private ChessGame game;
    private Player white;
    private Player black;

    @BeforeEach
    void setUp() {
        ChessRuleService ruleService = new ChessRuleService();
        gameService = new ChessGameService(ruleService);
        white = Player.builder().name("Alice").color(Color.WHITE).build();
        black = Player.builder().name("Bob").color(Color.BLACK).build();
        game = gameService.createGame(white, black);
    }

    @Test
    void testInitialGameState() {
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(Color.WHITE, game.getCurrentTurn());
        assertFalse(game.isGameOver());
    }

    @Test
    void testCannotMoveOpponentPiece() {
        Move illegalMove = Move.builder()
                .from(new Position(6, 4))
                .to(new Position(5, 4))
                .build();
        assertFalse(gameService.makeMove(game, illegalMove));
        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Test
    void testFoolsMateCheckmate() {
        assertTrue(gameService.makeMove(game, Move.builder().from(new Position(1, 5)).to(new Position(2, 5)).build()));
        assertEquals(Color.BLACK, game.getCurrentTurn());

        assertTrue(gameService.makeMove(game, Move.builder().from(new Position(6, 4)).to(new Position(4, 4)).build()));
        assertEquals(Color.WHITE, game.getCurrentTurn());

        assertTrue(gameService.makeMove(game, Move.builder().from(new Position(1, 6)).to(new Position(3, 6)).build()));
        assertEquals(Color.BLACK, game.getCurrentTurn());

        assertTrue(gameService.makeMove(game, Move.builder().from(new Position(7, 3)).to(new Position(3, 7)).build()));

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
        assertTrue(game.isGameOver());
        assertTrue(gameService.isCheck(game, Color.WHITE));

        assertFalse(gameService.makeMove(game, Move.builder().from(new Position(1, 0)).to(new Position(2, 0)).build()));
    }
}
