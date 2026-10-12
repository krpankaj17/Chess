package com.project.chess.service;

import com.project.chess.entity.*;
import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;

public class PieceFactory {
    public static Piece createPiece(PieceType type, Color color) {
        return switch (type) {
            case PAWN -> new Pawn(color);
            case KNIGHT -> new Knight(color);
            case BISHOP -> new Bishop(color);
            case ROOK -> new Rook(color);
            case QUEEN -> new Queen(color);
            case KING -> new King(color);
        };
    }
}
