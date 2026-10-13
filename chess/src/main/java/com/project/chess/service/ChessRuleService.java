package com.project.chess.service;

import com.project.chess.entity.Board;
import com.project.chess.entity.Move;
import com.project.chess.entity.MoveHistory;
import com.project.chess.entity.Piece;
import com.project.chess.entity.Position;
import com.project.chess.enums.Color;
import com.project.chess.enums.MoveType;
import com.project.chess.enums.PieceType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChessRuleService {

    public List<Position> getLegalMoves(Board board, Position from, MoveHistory moveHistory) {
        Piece piece = board.getPiece(from);
        if (piece == null) {
            return List.of();
        }

        List<Position> candidateMoves = piece.getAvailableMoves(board, from);
        List<Position> legalMoves = new ArrayList<>();

        // Standard piece candidate moves (validated against leaving king in check)
        for (Position to : candidateMoves) {
            Move testMove = Move.builder().from(from).to(to).build();
            board.movePiece(testMove);

            boolean leavesKingInCheck = isCheck(board, piece.getColor());
            board.undoMove(testMove);

            if (!leavesKingInCheck) {
                legalMoves.add(to);
            }
        }

        // Special King moves: Castling
        if (piece.getType() == PieceType.KING) {
            int row = from.getRow();
            if (canCastleKingside(board, from, piece.getColor())) {
                legalMoves.add(new Position(row, 6));
            }
            if (canCastleQueenside(board, from, piece.getColor())) {
                legalMoves.add(new Position(row, 2));
            }
        }

        // Special Pawn moves: En Passant
        if (piece.getType() == PieceType.PAWN && moveHistory != null) {
            Position enPassantTarget = getEnPassantTarget(board, from, moveHistory.getLastMove());
            if (enPassantTarget != null) {
                Move testMove = Move.builder()
                        .from(from)
                        .to(enPassantTarget)
                        .moveType(MoveType.EN_PASSANT)
                        .build();
                board.movePiece(testMove);
                boolean leavesKingInCheck = isCheck(board, piece.getColor());
                board.undoMove(testMove);

                if (!leavesKingInCheck) {
                    legalMoves.add(enPassantTarget);
                    if (moveHistory.getLastMove() != null && moveHistory.getLastMove().getTo() != null) {
                        legalMoves.add(moveHistory.getLastMove().getTo());
                    }
                }
            }
        }

        return legalMoves;
    }

    public boolean canCastleKingside(Board board, Position kingPos, Color color) {
        Piece king = board.getPiece(kingPos);
        if (king == null || king.getType() != PieceType.KING || king.isHasMoved()) {
            return false;
        }

        int row = kingPos.getRow();
        if (kingPos.getColumn() != 4) {
            return false;
        }

        // Must not be in check currently
        if (isCheck(board, color)) {
            return false;
        }

        Position rookPos = new Position(row, 7);
        Piece rook = board.getPiece(rookPos);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != color || rook.isHasMoved()) {
            return false;
        }

        // Path squares (cols 5 and 6) must be empty
        if (board.getPiece(new Position(row, 5)) != null || board.getPiece(new Position(row, 6)) != null) {
            return false;
        }

        // Transit and destination squares must not be attacked
        Color enemyColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        if (isSquareUnderAttack(board, new Position(row, 5), enemyColor) ||
            isSquareUnderAttack(board, new Position(row, 6), enemyColor)) {
            return false;
        }

        return true;
    }

    public boolean canCastleQueenside(Board board, Position kingPos, Color color) {
        Piece king = board.getPiece(kingPos);
        if (king == null || king.getType() != PieceType.KING || king.isHasMoved()) {
            return false;
        }

        int row = kingPos.getRow();
        if (kingPos.getColumn() != 4) {
            return false;
        }

        // Must not be in check currently
        if (isCheck(board, color)) {
            return false;
        }

        Position rookPos = new Position(row, 0);
        Piece rook = board.getPiece(rookPos);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != color || rook.isHasMoved()) {
            return false;
        }

        // Path squares (cols 1, 2, 3) must be empty
        if (board.getPiece(new Position(row, 1)) != null ||
            board.getPiece(new Position(row, 2)) != null ||
            board.getPiece(new Position(row, 3)) != null) {
            return false;
        }

        // Transit (col 3) and destination (col 2) must not be attacked
        Color enemyColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        if (isSquareUnderAttack(board, new Position(row, 3), enemyColor) ||
            isSquareUnderAttack(board, new Position(row, 2), enemyColor)) {
            return false;
        }

        return true;
    }

    public Position getEnPassantTarget(Board board, Position pawnPos, Move lastMove) {
        if (lastMove == null) {
            return null;
        }

        Piece enemyPiece = lastMove.getPiece();
        if (enemyPiece == null && lastMove.getTo() != null) {
            enemyPiece = board.getPiece(lastMove.getTo());
        }
        if (enemyPiece == null || enemyPiece.getType() != PieceType.PAWN) {
            return null;
        }

        Piece myPawn = board.getPiece(pawnPos);
        if (myPawn == null || myPawn.getType() != PieceType.PAWN) {
            return null;
        }

        if (myPawn.getColor() == enemyPiece.getColor()) {
            return null;
        }

        if (lastMove.getFrom() == null || lastMove.getTo() == null) {
            return null;
        }

        // Enemy pawn must have just moved two squares
        int rowDiff = Math.abs(lastMove.getTo().getRow() - lastMove.getFrom().getRow());
        if (rowDiff != 2) {
            return null;
        }

        // Must be on the same rank as the enemy pawn
        if (!pawnPos.getRow().equals(lastMove.getTo().getRow())) {
            return null;
        }

        // Must be in an adjacent column
        if (Math.abs(pawnPos.getColumn() - lastMove.getTo().getColumn()) != 1) {
            return null;
        }

        int direction = (myPawn.getColor() == Color.WHITE) ? 1 : -1;
        Position target = new Position(pawnPos.getRow() + direction, lastMove.getTo().getColumn());
        if (target.isValid() && board.getPiece(target) == null) {
            return target;
        }

        return null;
    }

    public boolean isCheck(Board board, Color kingColor) {
        Position kingPosition = findKing(board, kingColor);
        if (kingPosition == null) {
            return false;
        }
        Color enemyColor = (kingColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return isSquareUnderAttack(board, kingPosition, enemyColor);
    }

    public boolean isSquareUnderAttack(Board board, Position target, Color byColor) {
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

    public Position findKing(Board board, Color color) {
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

    public boolean hasAnyLegalMoves(Board board, Color color, MoveHistory moveHistory) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPiece(pos);
                if (piece != null && piece.getColor() == color) {
                    if (!getLegalMoves(board, pos, moveHistory).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
