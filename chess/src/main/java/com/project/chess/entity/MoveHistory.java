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
        return moves.remove(moves.size() - 1);
    }

    public Move getLastMove() {
        if (moves.isEmpty()) {
            return null;
        }
        return moves.get(moves.size() - 1);
    }
}
