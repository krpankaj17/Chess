package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;

import java.util.List;

public class Rook extends Piece {

    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    public Rook(Color color) {
        super(color, PieceType.ROOK);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        return getSlidingMoves(board, from, DIRECTIONS);
    }
}
