package com.project.chess.service;

import com.project.chess.entity.ChessGame;
import com.project.chess.entity.Player;
import com.project.chess.enums.Color;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {

    private final Map<String, ChessGame> activeGames = new ConcurrentHashMap<>();

    public ChessGame createGame(String gameId, String whiteUsername, String blackUsername) {
        Player white = Player.builder().name(whiteUsername).color(Color.WHITE).build();
        Player black = Player.builder().name(blackUsername).color(Color.BLACK).build();

        ChessGame game = new ChessGame(white, black);
        game.start();

        activeGames.put(gameId, game);
        return game;
    }

    public ChessGame getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public void removeGame(String gameId) {
        activeGames.remove(gameId);
    }

    public int getActiveGameCount() {
        return activeGames.size();
    }

    public Map.Entry<String, ChessGame> findActiveGameForUser(String username) {
        for (Map.Entry<String, ChessGame> entry : activeGames.entrySet()) {
            ChessGame game = entry.getValue();
            if (!game.isGameOver()) {
                if (username.equals(game.getWhitePlayer().getName()) ||
                    username.equals(game.getBlackPlayer().getName())) {
                    return entry;
                }
            }
        }
        return null;
    }

    public com.project.chess.dto.GameSnapshotDto getSnapshot(String gameId) {
        ChessGame game = activeGames.get(gameId);
        if (game == null) return null;

        com.project.chess.dto.GameSnapshotDto.PieceDto[][] boardDto = new com.project.chess.dto.GameSnapshotDto.PieceDto[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                com.project.chess.entity.Piece piece = game.getBoard().getPiece(new com.project.chess.entity.Position(r, c));
                if (piece != null) {
                    boardDto[r][c] = new com.project.chess.dto.GameSnapshotDto.PieceDto(piece.getType().name(), piece.getColor().name());
                }
            }
        }

        boolean isCheck = game.getStatus() == com.project.chess.enums.GameStatus.CHECK || game.getStatus() == com.project.chess.enums.GameStatus.CHECKMATE;
        String checkedColor = isCheck ? game.getCurrentTurn().name() : null;

        return com.project.chess.dto.GameSnapshotDto.builder()
                .gameId(gameId)
                .whitePlayer(game.getWhitePlayer().getName())
                .blackPlayer(game.getBlackPlayer().getName())
                .currentTurn(game.getCurrentTurn().name())
                .status(game.getStatus().name())
                .check(isCheck)
                .checkedKingColor(checkedColor)
                .gameOver(game.isGameOver())
                .winner(game.getStatus() == com.project.chess.enums.GameStatus.CHECKMATE ? 
                        ((game.getCurrentTurn() == Color.WHITE ? game.getBlackPlayer().getName() : game.getWhitePlayer().getName())) : null)
                .board(boardDto)
                .build();
    }
}
