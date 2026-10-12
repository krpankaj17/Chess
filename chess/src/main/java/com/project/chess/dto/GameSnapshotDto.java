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
public class GameSnapshotDto {
    private String gameId;
    private String whitePlayer;
    private String blackPlayer;
    private String currentTurn;
    private String status;
    private boolean check;
    private String checkedKingColor;
    private boolean gameOver;
    private String winner;
    private PieceDto[][] board;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PieceDto {
        private String type;
        private String color;
    }
}
