package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;

import java.util.List;

public class Bishop extends Piece {

    private static final int[][] DIRECTIONS = {
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Bishop(Color color) {
        super(color, PieceType.BISHOP);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        return getSlidingMoves(board, from, DIRECTIONS);
    }
}
