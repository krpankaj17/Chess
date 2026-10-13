package com.project.chess.controller;

import com.project.chess.dto.GameStateMessage;
import com.project.chess.dto.MoveMessage;
import com.project.chess.entity.ChessGame;
import com.project.chess.entity.Move;
import com.project.chess.entity.Position;
import com.project.chess.entity.User;
import com.project.chess.enums.Color;
import com.project.chess.enums.GameStatus;
import com.project.chess.enums.PieceType;
import com.project.chess.repository.UserRepository;
import com.project.chess.service.EloService;
import com.project.chess.service.GameSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameSessionManager gameSessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final EloService eloService;
    private final UserRepository userRepository;
    private final com.project.chess.service.ChessGameService chessGameService;

    @MessageMapping("/game/{gameId}/move")
    public void processMove(@DestinationVariable String gameId,
                            @Payload MoveMessage moveMsg,
                            Principal principal) {
        ChessGame game = gameSessionManager.getGame(gameId);
        if (game == null || game.isGameOver()) {
            return;
        }

        if (principal != null) {
            String sender = principal.getName();
            boolean isWhite = sender.equals(game.getWhitePlayer().getName());
            boolean isBlack = sender.equals(game.getBlackPlayer().getName());

            if (!isWhite && !isBlack) {
                return;
            }

            if ((game.getCurrentTurn() == Color.WHITE && !isWhite) ||
                (game.getCurrentTurn() == Color.BLACK && !isBlack)) {
                return;
            }
        }

        Position from = new Position(moveMsg.getFromRow(), moveMsg.getFromCol());
        Position to = new Position(moveMsg.getToRow(), moveMsg.getToCol());

        PieceType promoType = null;
        if (moveMsg.getPromotionPiece() != null) {
            try {
                promoType = PieceType.valueOf(moveMsg.getPromotionPiece().toUpperCase());
            } catch (Exception ignored) {}
        }

        Move move = Move.builder().from(from).to(to).promotionPiece(promoType).build();

        boolean moved = chessGameService.makeMove(game, move);
        if (!moved) {
            return;
        }

        boolean isCheck = game.getStatus() == GameStatus.CHECK || game.getStatus() == GameStatus.CHECKMATE;
        String checkedColor = isCheck ? game.getCurrentTurn().name() : null;

        Integer rFromRow = move.getRookFrom() != null ? move.getRookFrom().getRow() : null;
        Integer rFromCol = move.getRookFrom() != null ? move.getRookFrom().getColumn() : null;
        Integer rToRow = move.getRookTo() != null ? move.getRookTo().getRow() : null;
        Integer rToCol = move.getRookTo() != null ? move.getRookTo().getColumn() : null;

        Integer capRow = move.getCapturedPiecePosition() != null ? move.getCapturedPiecePosition().getRow() : null;
        Integer capCol = move.getCapturedPiecePosition() != null ? move.getCapturedPiecePosition().getColumn() : null;

        GameStateMessage msg = GameStateMessage.builder()
                .gameId(gameId)
                .currentTurn(game.getCurrentTurn().name())
                .status(game.getStatus().name())
                .fromRow(move.getFrom().getRow())
                .fromCol(move.getFrom().getColumn())
                .toRow(move.getTo().getRow())
                .toCol(move.getTo().getColumn())
                .movedPiece(move.getPiece().getType().name())
                .capturedPiece(move.getCapturedPiece() != null ? move.getCapturedPiece().getType().name() : null)
                .promotionPiece(move.getPromotionPiece() != null ? move.getPromotionPiece().name() : null)
                .moveType(move.getMoveType() != null ? move.getMoveType().name() : "NORMAL")
                .rookFromRow(rFromRow)
                .rookFromCol(rFromCol)
                .rookToRow(rToRow)
                .rookToCol(rToCol)
                .capturedSquareRow(capRow)
                .capturedSquareCol(capCol)
                .check(isCheck)
                .checkedKingColor(checkedColor)
                .gameOver(game.isGameOver())
                .build();

        if (game.isGameOver()) {
            handleGameOver(game, msg);
        }

        messagingTemplate.convertAndSend("/topic/game/" + gameId, msg);
    }

    @MessageMapping("/game/{gameId}/resign")
    public void resign(@DestinationVariable String gameId,
                       @Payload(required = false) java.util.Map<String, String> payload,
                       Principal principal) {
        ChessGame game = gameSessionManager.getGame(gameId);
        if (game == null || game.isGameOver()) {
            return;
        }

        String resigningUser = null;
        if (principal != null) {
            resigningUser = principal.getName();
        } else if (payload != null && payload.containsKey("playerUsername")) {
            resigningUser = payload.get("playerUsername");
        }

        if (resigningUser == null) {
            return;
        }

        boolean whiteResigned = resigningUser.equals(game.getWhitePlayer().getName());
        boolean blackResigned = resigningUser.equals(game.getBlackPlayer().getName());

        if (!whiteResigned && !blackResigned) {
            return;
        }

        game.setStatus(GameStatus.CHECKMATE);
        String winner = whiteResigned ? game.getBlackPlayer().getName() : game.getWhitePlayer().getName();

        GameStateMessage msg = GameStateMessage.builder()
                .gameId(gameId)
                .status("RESIGNATION")
                .gameOver(true)
                .winner(winner)
                .build();

        updateEloAfterGame(game.getWhitePlayer().getName(), game.getBlackPlayer().getName(), whiteResigned ? 0.0 : 1.0, msg);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, msg);
    }

    public void resign(String gameId, Principal principal) {
        resign(gameId, null, principal);
    }

    private void handleGameOver(ChessGame game, GameStateMessage msg) {
        if (game.getStatus() == GameStatus.CHECKMATE) {
            Color winnerColor = (game.getCurrentTurn() == Color.WHITE) ? Color.BLACK : Color.WHITE;
            String winner = (winnerColor == Color.WHITE) ? game.getWhitePlayer().getName() : game.getBlackPlayer().getName();
            msg.setWinner(winner);

            double scoreWhite = (winnerColor == Color.WHITE) ? 1.0 : 0.0;
            updateEloAfterGame(game.getWhitePlayer().getName(), game.getBlackPlayer().getName(), scoreWhite, msg);
        } else if (game.getStatus() == GameStatus.STALEMATE || game.getStatus() == GameStatus.DRAW) {
            updateEloAfterGame(game.getWhitePlayer().getName(), game.getBlackPlayer().getName(), 0.5, msg);
        }
    }

    private void updateEloAfterGame(String whiteUsername, String blackUsername, double scoreWhite, GameStateMessage msg) {
        Optional<User> whiteOpt = userRepository.findByUsername(whiteUsername);
        Optional<User> blackOpt = userRepository.findByUsername(blackUsername);

        if (whiteOpt.isPresent() && blackOpt.isPresent()) {
            User white = whiteOpt.get();
            User black = blackOpt.get();

            int[] newRatings = eloService.calculateNewRatings(white.getEloRating(), black.getEloRating(), scoreWhite);
            white.setEloRating(newRatings[0]);
            black.setEloRating(newRatings[1]);

            userRepository.save(white);
            userRepository.save(black);

            msg.setWhiteElo(newRatings[0]);
            msg.setBlackElo(newRatings[1]);
        }
    }
}
