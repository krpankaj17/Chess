package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.GameStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChessGame {
    private Board board;
    private Player whitePlayer;
    private Player blackPlayer;
    private Color currentTurn;
    private GameStatus status;
    private MoveHistory moveHistory;

    public ChessGame(Player whitePlayer, Player blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.board = new Board();
        this.moveHistory = new MoveHistory();
        this.status = GameStatus.NOT_STARTED;
    }

    public void start() {
        board.initializeStandardBoard();
        currentTurn = Color.WHITE;
        status = GameStatus.IN_PROGRESS;
    }

    public void switchTurn() {
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    public boolean isGameOver() {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE || status == GameStatus.DRAW;
    }
}
