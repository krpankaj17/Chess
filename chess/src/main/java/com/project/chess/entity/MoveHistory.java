package com.project.chess.entity;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MoveHistory {
    private final List<Move> moves = new ArrayList<>();

    public void add(Move move) {
        moves.add(move);
    }

    public Move undo() {
        if (moves.isEmpty()) {
            return null;
        }
        return moves.removeLast();
    }

    public Move getLastMove() {
        if (moves.isEmpty()) {
            return null;
        }
        return moves.getLast();
    }
}
