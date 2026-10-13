package com.project.chess.service;

import com.project.chess.entity.ChessGame;
import com.project.chess.entity.Move;
import com.project.chess.entity.Piece;
import com.project.chess.entity.Player;
import com.project.chess.entity.Position;
import com.project.chess.enums.Color;
import com.project.chess.enums.GameStatus;
import com.project.chess.enums.MoveType;
import com.project.chess.enums.PieceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChessGameService {

    private final ChessRuleService chessRuleService;

    public ChessGame createGame(Player whitePlayer, Player blackPlayer) {
        ChessGame game = new ChessGame(whitePlayer, blackPlayer);
        game.start();
        return game;
    }

    public boolean makeMove(ChessGame game, Move move) {
        if (game == null || game.isGameOver()) {
            return false;
        }

        Piece movingPiece = game.getBoard().getPiece(move.getFrom());
        if (movingPiece == null || movingPiece.getColor() != game.getCurrentTurn()) {
            return false;
        }

        List<Position> legalMoves = chessRuleService.getLegalMoves(
                game.getBoard(),
                move.getFrom(),
                game.getMoveHistory()
        );

        if (!legalMoves.contains(move.getTo())) {
            return false;
        }

        // Handle Pawn En Passant target remapping & MoveType assignment
        if (movingPiece.getType() == PieceType.PAWN && game.getMoveHistory() != null) {
            Position epTarget = chessRuleService.getEnPassantTarget(game.getBoard(), move.getFrom(), game.getMoveHistory().getLastMove());
            if (epTarget != null) {
                Move lastMove = game.getMoveHistory().getLastMove();
                if (move.getTo().equals(epTarget)) {
                    move.setMoveType(MoveType.EN_PASSANT);
                } else if (lastMove != null && lastMove.getTo() != null && move.getTo().equals(lastMove.getTo())) {
                    // Player clicked enemy pawn directly: remap destination to diagonal en passant square!
                    move.setTo(epTarget);
                    move.setMoveType(MoveType.EN_PASSANT);
                }
            }
        }

        // Determine MoveType if not already explicitly assigned
        classifyMoveType(game, move, movingPiece);

        // Execute move on the board
        game.getBoard().movePiece(move);

        // Handle Pawn Promotion
        if (movingPiece.getType() == PieceType.PAWN) {
            boolean isWhiteEnd = movingPiece.getColor() == Color.WHITE && move.getTo().getRow() == 7;
            boolean isBlackEnd = movingPiece.getColor() == Color.BLACK && move.getTo().getRow() == 0;
            if (isWhiteEnd || isBlackEnd) {
                move.setMoveType(MoveType.PROMOTION);
                PieceType promoType = move.getPromotionPiece() != null ? move.getPromotionPiece() : PieceType.QUEEN;
                game.getBoard().setPiece(move.getTo(), PieceFactory.createPiece(promoType, movingPiece.getColor()));
                move.setPromotionPiece(promoType);
            }
        }

        // Record into game history
        game.getMoveHistory().add(move);

        // Switch turn & update game status
        game.switchTurn();
        updateGameStatus(game);

        return true;
    }

    public List<Position> getLegalMoves(ChessGame game, Position from) {
        if (game == null) {
            return List.of();
        }
        return chessRuleService.getLegalMoves(game.getBoard(), from, game.getMoveHistory());
    }

    public boolean isCheck(ChessGame game, Color kingColor) {
        if (game == null) {
            return false;
        }
        return chessRuleService.isCheck(game.getBoard(), kingColor);
    }

    public void updateGameStatus(ChessGame game) {
        boolean inCheck = chessRuleService.isCheck(game.getBoard(), game.getCurrentTurn());
        boolean hasMoves = chessRuleService.hasAnyLegalMoves(game.getBoard(), game.getCurrentTurn(), game.getMoveHistory());

        if (inCheck && !hasMoves) {
            game.setStatus(GameStatus.CHECKMATE);
        } else if (!inCheck && !hasMoves) {
            game.setStatus(GameStatus.STALEMATE);
        } else if (inCheck) {
            game.setStatus(GameStatus.CHECK);
        } else {
            game.setStatus(GameStatus.IN_PROGRESS);
        }
    }

    private void classifyMoveType(ChessGame game, Move move, Piece movingPiece) {
        if (move.getMoveType() != null && move.getMoveType() != MoveType.NORMAL) {
            return;
        }

        if (movingPiece.getType() == PieceType.KING &&
            Math.abs(move.getTo().getColumn() - move.getFrom().getColumn()) == 2) {
            move.setMoveType(MoveType.CASTLING);
        } else if (movingPiece.getType() == PieceType.PAWN) {
            boolean isWhiteEnd = movingPiece.getColor() == Color.WHITE && move.getTo().getRow() == 7;
            boolean isBlackEnd = movingPiece.getColor() == Color.BLACK && move.getTo().getRow() == 0;
            if (isWhiteEnd || isBlackEnd) {
                move.setMoveType(MoveType.PROMOTION);
            } else if (!move.getFrom().getColumn().equals(move.getTo().getColumn()) &&
                       game.getBoard().getPiece(move.getTo()) == null) {
                move.setMoveType(MoveType.EN_PASSANT);
            } else if (game.getBoard().getPiece(move.getTo()) != null) {
                move.setMoveType(MoveType.CAPTURE);
            } else {
                move.setMoveType(MoveType.NORMAL);
            }
        } else if (game.getBoard().getPiece(move.getTo()) != null) {
            move.setMoveType(MoveType.CAPTURE);
        } else {
            move.setMoveType(MoveType.NORMAL);
        }
    }
}
