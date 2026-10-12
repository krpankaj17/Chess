package com.project.chess.entity;

import com.project.chess.enums.Color;
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
public class Player {
    private String name;
    private Color color;

    public Move makeMove(Position from, Position to) {
        return Move.builder().from(from).to(to).build();
    }
}
