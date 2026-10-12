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
public class GameStateMessage {
    private String gameId;
    private String currentTurn;
    private String status;
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private String movedPiece;
    private String capturedPiece;
    private String promotionPiece;
    private boolean check;
    private String checkedKingColor;
    private boolean gameOver;
    private String winner;
    private Integer whiteElo;
    private Integer blackElo;
}
