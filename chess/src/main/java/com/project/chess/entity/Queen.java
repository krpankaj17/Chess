package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;

import java.util.List;

public class Queen extends Piece {

    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Queen(Color color) {
        super(color, PieceType.QUEEN);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        return getSlidingMoves(board, from, DIRECTIONS);
    }
}
