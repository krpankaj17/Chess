package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Piece {
    private final Color color;
    private final PieceType type;
    private boolean hasMoved = false;

    public Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public abstract List<Position> getAvailableMoves(Board board, Position from);

    public boolean isValidMove(Board board, Position from, Position to) {
        return getAvailableMoves(board, from).contains(to);
    }

    protected List<Position> getSlidingMoves(Board board, Position from, int[][] directions) {
        List<Position> moves = new ArrayList<>();

        for (int[] dir : directions) {
            int r = from.getRow() + dir[0];
            int c = from.getColumn() + dir[1];

            while (true) {
                Position target = new Position(r, c);
                if (!target.isValid()) {
                    break;
                }

                Piece pieceAtTarget = board.getPiece(target);
                if (pieceAtTarget == null) {
                    moves.add(target);
                } else {
                    if (pieceAtTarget.getColor() != this.getColor()) {
                        moves.add(target);
                    }
                    break;
                }

                r += dir[0];
                c += dir[1];
            }
        }

        return moves;
    }
}