package com.project.chess.entity;
import lombok.*;
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Position {
    private Integer row;
    private Integer column;

    public Boolean isValid(){
        return this.row >= 0 && this.row < 8 && this.column >= 0 && this.column < 8;
    }
}

