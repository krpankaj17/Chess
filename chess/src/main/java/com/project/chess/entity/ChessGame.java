package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.GameStatus;
import com.project.chess.enums.PieceType;
import com.project.chess.service.PieceFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    public boolean makeMove(Move move) {
        if (isGameOver()) {
            return false;
        }

        Square fromSquare = board.getSquare(move.getFrom());
        if (fromSquare == null || fromSquare.isEmpty()) {
            return false;
        }

        Piece movingPiece = fromSquare.getPiece();
        if (movingPiece.getColor() != currentTurn) {
            return false;
        }

        List<Position> legalMoves = getLegalMoves(move.getFrom());
        if (!legalMoves.contains(move.getTo())) {
            return false;
        }

        board.movePiece(move);

        if (movingPiece.getType() == PieceType.PAWN) {
            boolean isWhiteEnd = movingPiece.getColor() == Color.WHITE && move.getTo().getRow() == 7;
            boolean isBlackEnd = movingPiece.getColor() == Color.BLACK && move.getTo().getRow() == 0;
            if (isWhiteEnd || isBlackEnd) {
                PieceType promoType = move.getPromotionPiece() != null ? move.getPromotionPiece() : PieceType.QUEEN;
                board.setPiece(move.getTo(), PieceFactory.createPiece(promoType, movingPiece.getColor()));
            }
        }

        moveHistory.add(move);

        switchTurn();
        updateGameStatus();
        return true;
    }

    public List<Position> getLegalMoves(Position from) {
        Piece piece = board.getPiece(from);
        if (piece == null) {
            return List.of();
        }

        List<Position> candidateMoves = piece.getAvailableMoves(board, from);
        List<Position> legalMoves = new ArrayList<>();

        for (Position to : candidateMoves) {
            Move testMove = Move.builder().from(from).to(to).build();
            board.movePiece(testMove);

            boolean leavesKingInCheck = isCheck(piece.getColor());
            board.undoMove(testMove);

            if (!leavesKingInCheck) {
                legalMoves.add(to);
            }
        }

        return legalMoves;
    }

    public boolean isCheck(Color kingColor) {
        Position kingPosition = findKing(kingColor);
        if (kingPosition == null) {
            return false;
        }
        Color enemyColor = (kingColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return isSquareUnderAttack(kingPosition, enemyColor);
    }

    public boolean isSquareUnderAttack(Position target, Color byColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPiece(pos);

                if (piece != null && piece.getColor() == byColor) {
                    if (piece.getType() == PieceType.PAWN) {
                        int dir = (byColor == Color.WHITE) ? 1 : -1;
                        if (target.getRow() == r + dir && Math.abs(target.getColumn() - c) == 1) {
                            return true;
                        }
                    } else if (piece.getAvailableMoves(board, pos).contains(target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Position findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPiece(pos);
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return pos;
                }
            }
        }
        return null;
    }

    public boolean hasAnyLegalMoves(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPiece(pos);
                if (piece != null && piece.getColor() == color) {
                    if (!getLegalMoves(pos).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void updateGameStatus() {
        boolean inCheck = isCheck(currentTurn);
        boolean hasMoves = hasAnyLegalMoves(currentTurn);

        if (inCheck && !hasMoves) {
            status = GameStatus.CHECKMATE;
        } else if (!inCheck && !hasMoves) {
            status = GameStatus.STALEMATE;
        } else if (inCheck) {
            status = GameStatus.CHECK;
        } else {
            status = GameStatus.IN_PROGRESS;
        }
    }

    public void switchTurn() {
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    public boolean isGameOver() {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE || status == GameStatus.DRAW;
    }
}
