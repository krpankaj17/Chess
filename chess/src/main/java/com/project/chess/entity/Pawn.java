package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        List<Position> moves = new ArrayList<>();

        int direction = (this.getColor() == Color.WHITE) ? 1 : -1;
        int startRow = (this.getColor() == Color.WHITE) ? 1 : 6;

        Position forwardOne = new Position(from.getRow() + direction, from.getColumn());
        if (forwardOne.isValid() && board.getPiece(forwardOne) == null) {
            moves.add(forwardOne);

            if (from.getRow() == startRow) {
                Position forwardTwo = new Position(from.getRow() + (2 * direction), from.getColumn());
                if (forwardTwo.isValid() && board.getPiece(forwardTwo) == null) {
                    moves.add(forwardTwo);
                }
            }
        }

        int[] captureCols = {from.getColumn() - 1, from.getColumn() + 1};
        for (int col : captureCols) {
            Position diag = new Position(from.getRow() + direction, col);
            if (diag.isValid()) {
                Piece pieceAtTarget = board.getPiece(diag);
                if (pieceAtTarget != null && pieceAtTarget.getColor() != this.getColor()) {
                    moves.add(diag);
                }
            }
        }

        return moves;
    }
}
