package com.project.chess.entity;

import com.project.chess.enums.MoveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Move {
    private Position from;
    private Position to;
    private Piece piece;
    private Piece capturedPiece;
    private MoveType moveType;
    private com.project.chess.enums.PieceType promotionPiece;

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
        this.moveType = MoveType.NORMAL;
    }

   
}
