package com.project.chess;

import com.project.chess.entity.Knight;
import com.project.chess.entity.Pawn;
import com.project.chess.entity.Piece;
import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;
import com.project.chess.service.PieceFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PieceFactoryTest {

    @Test
    void testCreatePiece() {
        Piece pawn = PieceFactory.createPiece(PieceType.PAWN, Color.WHITE);
        assertNotNull(pawn);
        assertInstanceOf(Pawn.class, pawn);
        assertEquals(Color.WHITE, pawn.getColor());

        Piece knight = PieceFactory.createPiece(PieceType.KNIGHT, Color.BLACK);
        assertNotNull(knight);
        assertInstanceOf(Knight.class, knight);
        assertEquals(Color.BLACK, knight.getColor());
    }
}
