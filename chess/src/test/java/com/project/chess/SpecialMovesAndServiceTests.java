package com.project.chess;

import com.project.chess.entity.ChessGame;
import com.project.chess.entity.Move;
import com.project.chess.entity.Pawn;
import com.project.chess.entity.Piece;
import com.project.chess.entity.Player;
import com.project.chess.entity.Position;
import com.project.chess.enums.Color;
import com.project.chess.enums.MoveType;
import com.project.chess.enums.PieceType;
import com.project.chess.service.ChessGameService;
import com.project.chess.service.ChessRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpecialMovesAndServiceTests {

    private ChessRuleService ruleService;
    private ChessGameService gameService;
    private ChessGame game;
    private Player white;
    private Player black;

    @BeforeEach
    void setUp() {
        ruleService = new ChessRuleService();
        gameService = new ChessGameService(ruleService);
        white = Player.builder().name("Alice").color(Color.WHITE).build();
        black = Player.builder().name("Bob").color(Color.BLACK).build();
        game = gameService.createGame(white, black);
    }

    @Test
    void testWhiteKingsideCastlingSuccess() {
        // Clear bishop and knight between King and h-file Rook
        game.getBoard().setPiece(new Position(0, 5), null);
        game.getBoard().setPiece(new Position(0, 6), null);

        List<Position> kingMoves = gameService.getLegalMoves(game, new Position(0, 4));
        assertTrue(kingMoves.contains(new Position(0, 6)), "Should allow Kingside Castling");

        Move castleMove = Move.builder()
                .from(new Position(0, 4))
                .to(new Position(0, 6))
                .build();

        boolean moved = gameService.makeMove(game, castleMove);
        assertTrue(moved);
        assertEquals(MoveType.CASTLING, castleMove.getMoveType());

        // Verify King and Rook final positions
        Piece king = game.getBoard().getPiece(new Position(0, 6));
        assertNotNull(king);
        assertEquals(PieceType.KING, king.getType());

        Piece rook = game.getBoard().getPiece(new Position(0, 5));
        assertNotNull(rook);
        assertEquals(PieceType.ROOK, rook.getType());

        assertNull(game.getBoard().getPiece(new Position(0, 4)));
        assertNull(game.getBoard().getPiece(new Position(0, 7)));
    }

    @Test
    void testWhiteQueensideCastlingSuccess() {
        // Clear knight, bishop, and queen
        game.getBoard().setPiece(new Position(0, 1), null);
        game.getBoard().setPiece(new Position(0, 2), null);
        game.getBoard().setPiece(new Position(0, 3), null);

        List<Position> kingMoves = gameService.getLegalMoves(game, new Position(0, 4));
        assertTrue(kingMoves.contains(new Position(0, 2)), "Should allow Queenside Castling");

        Move castleMove = Move.builder()
                .from(new Position(0, 4))
                .to(new Position(0, 2))
                .build();

        boolean moved = gameService.makeMove(game, castleMove);
        assertTrue(moved);
        assertEquals(MoveType.CASTLING, castleMove.getMoveType());

        // Verify King and Rook positions
        Piece king = game.getBoard().getPiece(new Position(0, 2));
        assertNotNull(king);
        assertEquals(PieceType.KING, king.getType());

        Piece rook = game.getBoard().getPiece(new Position(0, 3));
        assertNotNull(rook);
        assertEquals(PieceType.ROOK, rook.getType());

        assertNull(game.getBoard().getPiece(new Position(0, 0)));
        assertNull(game.getBoard().getPiece(new Position(0, 4)));
    }

    @Test
    void testCastlingBlockedWhenKingHasMoved() {
        game.getBoard().setPiece(new Position(0, 5), null);
        game.getBoard().setPiece(new Position(0, 6), null);

        Piece king = game.getBoard().getPiece(new Position(0, 4));
        king.setHasMoved(true);

        List<Position> kingMoves = gameService.getLegalMoves(game, new Position(0, 4));
        assertFalse(kingMoves.contains(new Position(0, 6)), "Cannot castle if king has moved");
    }

    @Test
    void testCastlingBlockedWhenSquareUnderAttack() {
        game.getBoard().setPiece(new Position(0, 5), null);
        game.getBoard().setPiece(new Position(0, 6), null);
        // Remove white and black pawns on column 5 to allow open vertical line
        game.getBoard().setPiece(new Position(1, 5), null);
        game.getBoard().setPiece(new Position(6, 5), null);

        // Put an enemy Black Rook attacking square (0, 5) from (7, 5)
        Piece blackRook = game.getBoard().getPiece(new Position(7, 0));
        game.getBoard().setPiece(new Position(7, 5), blackRook);

        List<Position> kingMoves = gameService.getLegalMoves(game, new Position(0, 4));
        assertFalse(kingMoves.contains(new Position(0, 6)), "Cannot castle through check/attack");
    }

    @Test
    void testEnPassantCapture() {
        // 1. e4
        gameService.makeMove(game, Move.builder().from(new Position(1, 4)).to(new Position(3, 4)).build());
        // 1... a6
        gameService.makeMove(game, Move.builder().from(new Position(6, 0)).to(new Position(5, 0)).build());
        // 2. e5
        gameService.makeMove(game, Move.builder().from(new Position(3, 4)).to(new Position(4, 4)).build());
        // 2... d5 (Black 2-step pawn jump to d5, adjacent to e5 pawn)
        gameService.makeMove(game, Move.builder().from(new Position(6, 3)).to(new Position(4, 3)).build());

        // White pawn at (4, 4) should have en passant move to (5, 3)
        List<Position> pawnMoves = gameService.getLegalMoves(game, new Position(4, 4));
        assertTrue(pawnMoves.contains(new Position(5, 3)), "En passant move to (5, 3) should be legal");

        Move enPassantMove = Move.builder()
                .from(new Position(4, 4))
                .to(new Position(5, 3))
                .build();

        boolean moved = gameService.makeMove(game, enPassantMove);
        assertTrue(moved);
        assertEquals(MoveType.EN_PASSANT, enPassantMove.getMoveType());

        // White pawn now at (5, 3)
        Piece movingPawn = game.getBoard().getPiece(new Position(5, 3));
        assertNotNull(movingPawn);
        assertEquals(Color.WHITE, movingPawn.getColor());

        // Captured black pawn at (4, 3) must be cleared!
        assertNull(game.getBoard().getPiece(new Position(4, 3)), "Captured black pawn at (4, 3) must be removed");
    }

    @Test
    void testPromotionMoveType() {
        // Setup a white pawn at (6, 0) and clear (7, 0)
        game.getBoard().setPiece(new Position(7, 0), null);
        game.getBoard().setPiece(new Position(6, 0), new Pawn(Color.WHITE));

        Move promoMove = Move.builder()
                .from(new Position(6, 0))
                .to(new Position(7, 0))
                .promotionPiece(PieceType.QUEEN)
                .build();

        assertTrue(gameService.makeMove(game, promoMove));
        assertEquals(MoveType.PROMOTION, promoMove.getMoveType());

        Piece promoted = game.getBoard().getPiece(new Position(7, 0));
        assertNotNull(promoted);
        assertEquals(PieceType.QUEEN, promoted.getType());
        assertEquals(Color.WHITE, promoted.getColor());
    }

    @Test
    void testCaptureMoveType() {
        // 1. e4
        gameService.makeMove(game, Move.builder().from(new Position(1, 4)).to(new Position(3, 4)).build());
        // 1... d5
        gameService.makeMove(game, Move.builder().from(new Position(6, 3)).to(new Position(4, 3)).build());

        // 2. exd5
        Move captureMove = Move.builder()
                .from(new Position(3, 4))
                .to(new Position(4, 3))
                .build();

        assertTrue(gameService.makeMove(game, captureMove));
        assertEquals(MoveType.CAPTURE, captureMove.getMoveType());
    }

    @Test
    void testBlackEnPassantCapture() {
        // 1. d4 (1, 3 -> 3, 3)
        gameService.makeMove(game, Move.builder().from(new Position(1, 3)).to(new Position(3, 3)).build());
        // 1... h6 (6, 7 -> 5, 7)
        gameService.makeMove(game, Move.builder().from(new Position(6, 7)).to(new Position(5, 7)).build());
        // 2. d5 (3, 3 -> 4, 3)
        gameService.makeMove(game, Move.builder().from(new Position(3, 3)).to(new Position(4, 3)).build());
        // 2... c5 (6, 2 -> 4, 2) - Black pawn at (4, 2)
        gameService.makeMove(game, Move.builder().from(new Position(6, 2)).to(new Position(4, 2)).build());
        // 3. h3 (1, 7 -> 2, 7)
        gameService.makeMove(game, Move.builder().from(new Position(1, 7)).to(new Position(2, 7)).build());
        // 3... c4 (4, 2 -> 3, 2) - Black pawn reaches row 3
        gameService.makeMove(game, Move.builder().from(new Position(4, 2)).to(new Position(3, 2)).build());
        // 4. b4 (1, 1 -> 3, 1) - White pawn jumps 2 steps from row 1 to row 3, adjacent to black pawn at (3, 2)
        gameService.makeMove(game, Move.builder().from(new Position(1, 1)).to(new Position(3, 1)).build());

        // Black pawn at (3, 2) should have en passant move to (2, 1)
        List<Position> legalMoves = gameService.getLegalMoves(game, new Position(3, 2));
        assertTrue(legalMoves.contains(new Position(2, 1)), "Black en passant to (2, 1) should be legal");

        Move enPassantMove = Move.builder()
                .from(new Position(3, 2))
                .to(new Position(2, 1))
                .build();

        assertTrue(gameService.makeMove(game, enPassantMove));
        assertEquals(MoveType.EN_PASSANT, enPassantMove.getMoveType());

        // Black pawn now at (2, 1)
        Piece movingPawn = game.getBoard().getPiece(new Position(2, 1));
        assertNotNull(movingPawn);
        assertEquals(Color.BLACK, movingPawn.getColor());

        // White pawn at (3, 1) must be removed!
        assertNull(game.getBoard().getPiece(new Position(3, 1)), "Captured white pawn at (3, 1) must be removed");
    }

    @Test
    void testEnPassantExpiresIfSkipped() {
        // 1. e4
        gameService.makeMove(game, Move.builder().from(new Position(1, 4)).to(new Position(3, 4)).build());
        // 1... a6
        gameService.makeMove(game, Move.builder().from(new Position(6, 0)).to(new Position(5, 0)).build());
        // 2. e5
        gameService.makeMove(game, Move.builder().from(new Position(3, 4)).to(new Position(4, 4)).build());
        // 2... d5 (Black 2-step jump)
        gameService.makeMove(game, Move.builder().from(new Position(6, 3)).to(new Position(4, 3)).build());

        // White skips en passant and plays h3 instead
        gameService.makeMove(game, Move.builder().from(new Position(1, 7)).to(new Position(2, 7)).build());
        // Black plays h6
        gameService.makeMove(game, Move.builder().from(new Position(6, 7)).to(new Position(5, 7)).build());

        // Now on White's next turn, en passant to (5, 3) must NO LONGER be legal!
        List<Position> moves = gameService.getLegalMoves(game, new Position(4, 4));
        assertFalse(moves.contains(new Position(5, 3)), "En passant should expire if not taken immediately");
    }

    @Test
    void testWhiteEnPassantByClickingEnemyPawnDirectly() {
        // 1. e4 (1, 4 -> 3, 4)
        gameService.makeMove(game, Move.builder().from(new Position(1, 4)).to(new Position(3, 4)).build());
        // 1... a6 (6, 0 -> 5, 0)
        gameService.makeMove(game, Move.builder().from(new Position(6, 0)).to(new Position(5, 0)).build());
        // 2. e5 (3, 4 -> 4, 4)
        gameService.makeMove(game, Move.builder().from(new Position(3, 4)).to(new Position(4, 4)).build());
        // 2... d5 (6, 3 -> 4, 3) - Black pawn double-step
        gameService.makeMove(game, Move.builder().from(new Position(6, 3)).to(new Position(4, 3)).build());

        // Player clicks directly on enemy pawn at (4, 3)
        Move directClickMove = Move.builder()
                .from(new Position(4, 4))
                .to(new Position(4, 3))
                .build();

        boolean moved = gameService.makeMove(game, directClickMove);
        assertTrue(moved, "En passant by clicking enemy pawn directly should be accepted");
        assertEquals(MoveType.EN_PASSANT, directClickMove.getMoveType());
        assertEquals(new Position(5, 3), directClickMove.getTo(), "Destination must be remapped to diagonal square (5, 3)");

        // White pawn now at (5, 3)
        Piece whitePawn = game.getBoard().getPiece(new Position(5, 3));
        assertNotNull(whitePawn);
        assertEquals(Color.WHITE, whitePawn.getColor());

        // Enemy black pawn at (4, 3) must be removed
        assertNull(game.getBoard().getPiece(new Position(4, 3)), "Enemy black pawn at (4, 3) must be removed");
    }

    @Test
    void testBlackEnPassantByClickingEnemyPawnDirectly() {
        // 1. d4 (1, 3 -> 3, 3)
        gameService.makeMove(game, Move.builder().from(new Position(1, 3)).to(new Position(3, 3)).build());
        // 1... h6 (6, 7 -> 5, 7)
        gameService.makeMove(game, Move.builder().from(new Position(6, 7)).to(new Position(5, 7)).build());
        // 2. d5 (3, 3 -> 4, 3)
        gameService.makeMove(game, Move.builder().from(new Position(3, 3)).to(new Position(4, 3)).build());
        // 2... c5 (6, 2 -> 4, 2)
        gameService.makeMove(game, Move.builder().from(new Position(6, 2)).to(new Position(4, 2)).build());
        // 3. h3 (1, 7 -> 2, 7)
        gameService.makeMove(game, Move.builder().from(new Position(1, 7)).to(new Position(2, 7)).build());
        // 3... c4 (4, 2 -> 3, 2) - Black pawn reaches row 3
        gameService.makeMove(game, Move.builder().from(new Position(4, 2)).to(new Position(3, 2)).build());
        // 4. b4 (1, 1 -> 3, 1) - White pawn jumps 2 steps to (3, 1)
        gameService.makeMove(game, Move.builder().from(new Position(1, 1)).to(new Position(3, 1)).build());

        // Black player clicks directly on white pawn at (3, 1)
        Move directClickMove = Move.builder()
                .from(new Position(3, 2))
                .to(new Position(3, 1))
                .build();

        assertTrue(gameService.makeMove(game, directClickMove));
        assertEquals(MoveType.EN_PASSANT, directClickMove.getMoveType());
        assertEquals(new Position(2, 1), directClickMove.getTo(), "Destination must be remapped to diagonal square (2, 1)");

        // Black pawn now at (2, 1)
        Piece blackPawn = game.getBoard().getPiece(new Position(2, 1));
        assertNotNull(blackPawn);
        assertEquals(Color.BLACK, blackPawn.getColor());

        // Enemy white pawn at (3, 1) must be removed
        assertNull(game.getBoard().getPiece(new Position(3, 1)), "Captured white pawn at (3, 1) must be removed");
    }

    @Test
    void testEnPassantBlockedByHorizontalPin() {
        // Place White King on (4, 7) [h5]
        game.getBoard().setPiece(new Position(0, 4), null);
        game.getBoard().setPiece(new Position(4, 7), new com.project.chess.entity.King(Color.WHITE));

        // Place White Pawn on (4, 4) [e5]
        game.getBoard().setPiece(new Position(1, 4), null);
        game.getBoard().setPiece(new Position(4, 4), new Pawn(Color.WHITE));

        // Place Black Rook on (4, 0) [a5]
        game.getBoard().setPiece(new Position(7, 0), null);
        game.getBoard().setPiece(new Position(4, 0), new com.project.chess.entity.Rook(Color.BLACK));

        // Clear all pieces between a5 and h5 along row 4
        for (int c = 1; c < 7; c++) {
            if (c != 4) {
                game.getBoard().setPiece(new Position(4, c), null);
            }
        }

        // It is Black's turn: Bob plays d7 -> d5 (6, 3 -> 4, 3)
        game.switchTurn();
        Move blackDoubleStep = Move.builder().from(new Position(6, 3)).to(new Position(4, 3)).build();
        assertTrue(gameService.makeMove(game, blackDoubleStep));

        // Now it's White's turn.
        // If White pawn captures en passant to (5, 3), both (4, 4) and (4, 3) become empty,
        // which leaves the White King on (4, 7) exposed to the Black Rook on (4, 0).
        List<Position> whitePawnMoves = gameService.getLegalMoves(game, new Position(4, 4));
        assertFalse(whitePawnMoves.contains(new Position(5, 3)), "En passant must be illegal due to horizontal pin on King");
    }

    @Test
    void testEnPassantDeliversCheck() {
        // 1. e4
        gameService.makeMove(game, Move.builder().from(new Position(1, 4)).to(new Position(3, 4)).build());
        // 1... a6
        gameService.makeMove(game, Move.builder().from(new Position(6, 0)).to(new Position(5, 0)).build());
        // 2. e5
        gameService.makeMove(game, Move.builder().from(new Position(3, 4)).to(new Position(4, 4)).build());
        // Position Black King at (6, 4) [e7]
        game.getBoard().setPiece(new Position(7, 4), null);
        game.getBoard().setPiece(new Position(6, 4), new com.project.chess.entity.King(Color.BLACK));

        // 2... f5 (6, 5 -> 4, 5) - Black pawn double-step
        Move blackPawnMove = Move.builder().from(new Position(6, 5)).to(new Position(4, 5)).build();
        assertTrue(gameService.makeMove(game, blackPawnMove));

        // White pawn at (4, 4) captures en passant to (5, 5).
        // From (5, 5), White pawn attacks (6, 4) where Black King is!
        Move epMove = Move.builder().from(new Position(4, 4)).to(new Position(5, 5)).build();
        assertTrue(gameService.makeMove(game, epMove));
        assertEquals(com.project.chess.enums.GameStatus.CHECK, game.getStatus(), "En passant move should deliver check");
    }
}
