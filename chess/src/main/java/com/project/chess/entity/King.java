package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {

    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public King(Color color) {
        super(color, PieceType.KING);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        List<Position> moves = new ArrayList<>();

        for (int[] dir : DIRECTIONS) {
            Position target = new Position(from.getRow() + dir[0], from.getColumn() + dir[1]);
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
