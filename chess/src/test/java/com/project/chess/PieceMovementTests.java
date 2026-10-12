package com.project.chess;

import com.project.chess.entity.*;
import com.project.chess.enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PieceMovementTests {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.initializeStandardBoard();
    }

    @Test
    void testKnightInitialMoves() {
        Piece knight = board.getPiece(new Position(0, 1));
        assertNotNull(knight);
        assertEquals(Color.WHITE, knight.getColor());

        List<Position> moves = knight.getAvailableMoves(board, new Position(0, 1));
        assertEquals(2, moves.size());
        assertTrue(moves.contains(new Position(2, 0)));
        assertTrue(moves.contains(new Position(2, 2)));
    }

    @Test
    void testPawnInitialMoves() {
        Piece pawn = board.getPiece(new Position(1, 4));
        assertNotNull(pawn);

        List<Position> moves = pawn.getAvailableMoves(board, new Position(1, 4));
        assertEquals(2, moves.size());
        assertTrue(moves.contains(new Position(2, 4)));
        assertTrue(moves.contains(new Position(3, 4)));
    }

    @Test
    void testBishopMovementOnOpenDiagonal() {
        board.setPiece(new Position(1, 3), null);

        Piece bishop = board.getPiece(new Position(0, 2));
        assertNotNull(bishop);

        List<Position> moves = bishop.getAvailableMoves(board, new Position(0, 2));
        assertTrue(moves.contains(new Position(1, 3)));
        assertTrue(moves.contains(new Position(2, 4)));
    }

    @Test
    void testRookMovementOnOpenColumn() {
        board.setPiece(new Position(1, 0), null);

        Piece rook = board.getPiece(new Position(0, 0));
        assertNotNull(rook);

        List<Position> moves = rook.getAvailableMoves(board, new Position(0, 0));
        assertTrue(moves.contains(new Position(1, 0)));
        assertTrue(moves.contains(new Position(2, 0)));
        assertTrue(moves.contains(new Position(6, 0)));
        assertFalse(moves.contains(new Position(7, 0)));
    }

    @Test
    void testKingMoves() {
        Board emptyBoard = new Board();
        King king = new King(Color.WHITE);
        emptyBoard.setPiece(new Position(4, 4), king);

        List<Position> moves = king.getAvailableMoves(emptyBoard, new Position(4, 4));
        assertEquals(8, moves.size());
    }
}
