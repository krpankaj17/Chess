package com.project.chess.entity;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode

public class Square {
    private Position position;
    private Piece piece;

 public Square(Position position) {
        this.position = position;
        this.piece = null;
    }
    public boolean isEmpty() {
        return this.piece == null;
    }

}
