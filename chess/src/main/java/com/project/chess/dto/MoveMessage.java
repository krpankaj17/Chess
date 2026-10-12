package com.project.chess.dto;

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
public class MoveMessage {
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private String promotionPiece;
}
