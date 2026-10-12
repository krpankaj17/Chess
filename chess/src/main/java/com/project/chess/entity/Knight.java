package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {
    private static final int[][] OFFSETS = {
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    public Knight(Color color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        List<Position> moves = new ArrayList<>();

        for (int[] offset : OFFSETS) {
            int targetRow = from.getRow() + offset[0];
            int targetCol = from.getColumn() + offset[1];
            Position target = new Position(targetRow, targetCol);

            if (target.isValid()) {
                Piece pieceAtTarget = board.getPiece(target);
                if (pieceAtTarget == null || pieceAtTarget.getColor() != this.getColor()) {
                    moves.add(target);
                }
            }
        }

        return moves;
    }
}
