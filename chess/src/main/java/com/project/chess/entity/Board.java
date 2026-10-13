package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.MoveType;
import com.project.chess.enums.PieceType;
import lombok.Getter;

@Getter
public class Board {
    private final Square[][] squares;

    public Board() {
        this.squares = new Square[8][8];
        initializeEmptyBoard();
    }

    public void initializeEmptyBoard() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c] = new Square(new Position(r, c));
            }
        }
    }

    public void initializeStandardBoard() {
        initializeEmptyBoard();

        squares[0][0].setPiece(new Rook(Color.WHITE));
        squares[0][1].setPiece(new Knight(Color.WHITE));
        squares[0][2].setPiece(new Bishop(Color.WHITE));
        squares[0][3].setPiece(new Queen(Color.WHITE));
        squares[0][4].setPiece(new King(Color.WHITE));
        squares[0][5].setPiece(new Bishop(Color.WHITE));
        squares[0][6].setPiece(new Knight(Color.WHITE));
        squares[0][7].setPiece(new Rook(Color.WHITE));
        for (int c = 0; c < 8; c++) {
            squares[1][c].setPiece(new Pawn(Color.WHITE));
        }

        squares[7][0].setPiece(new Rook(Color.BLACK));
        squares[7][1].setPiece(new Knight(Color.BLACK));
        squares[7][2].setPiece(new Bishop(Color.BLACK));
        squares[7][3].setPiece(new Queen(Color.BLACK));
        squares[7][4].setPiece(new King(Color.BLACK));
        squares[7][5].setPiece(new Bishop(Color.BLACK));
        squares[7][6].setPiece(new Knight(Color.BLACK));
        squares[7][7].setPiece(new Rook(Color.BLACK));
        for (int c = 0; c < 8; c++) {
            squares[6][c].setPiece(new Pawn(Color.BLACK));
        }
    }

    public Square getSquare(Position position) {
        if (position == null || !position.isValid()) {
            return null;
        }
        return squares[position.getRow()][position.getColumn()];
    }

    public Piece getPiece(Position position) {
        Square square = getSquare(position);
        return square != null ? square.getPiece() : null;
    }

    public boolean isOccupied(Position position) {
        Square square = getSquare(position);
        return square != null && !square.isEmpty();
    }

    public void setPiece(Position position, Piece piece) {
        Square square = getSquare(position);
        if (square != null) {
            square.setPiece(piece);
        }
    }
    public void undoMove(Move move) {
        if (move == null || move.getFrom() == null || move.getTo() == null) {
            return;
        }

        Square fromSquare = getSquare(move.getFrom());
        Square toSquare = getSquare(move.getTo());

        if (fromSquare == null || toSquare == null) {
            return;
        }

        // Restore moving piece
        fromSquare.setPiece(move.getPiece());
        if (move.getPiece() != null) {
            move.getPiece().setHasMoved(move.isPiecePreviouslyMoved());
        }

        if (move.getMoveType() == MoveType.EN_PASSANT) {
            toSquare.setPiece(null);
            if (move.getCapturedPiecePosition() != null) {
                setPiece(move.getCapturedPiecePosition(), move.getCapturedPiece());
            }
        } else if (move.getMoveType() == MoveType.CASTLING) {
            toSquare.setPiece(null);
            if (move.getCastledRook() != null && move.getRookFrom() != null && move.getRookTo() != null) {
                setPiece(move.getRookTo(), null);
                setPiece(move.getRookFrom(), move.getCastledRook());
                move.getCastledRook().setHasMoved(move.isRookPreviouslyMoved());
            }
        } else {
            toSquare.setPiece(move.getCapturedPiece());
        }
    }

    public void movePiece(Move move) {
        Square fromSquare = getSquare(move.getFrom());
        Square toSquare = getSquare(move.getTo());

        if (fromSquare == null || toSquare == null || fromSquare.isEmpty()) {
            return;
        }

        Piece movingPiece = fromSquare.getPiece();
        move.setPiece(movingPiece);
        move.setPiecePreviouslyMoved(movingPiece.isHasMoved());

        // Auto-detect moveType if not set or default NORMAL
        if (move.getMoveType() == null || move.getMoveType() == MoveType.NORMAL) {
            if (movingPiece.getType() == PieceType.KING &&
                Math.abs(move.getTo().getColumn() - move.getFrom().getColumn()) == 2) {
                move.setMoveType(MoveType.CASTLING);
            } else if (movingPiece.getType() == PieceType.PAWN &&
                !move.getFrom().getColumn().equals(move.getTo().getColumn()) &&
                toSquare.isEmpty()) {
                move.setMoveType(MoveType.EN_PASSANT);
            } else if (!toSquare.isEmpty()) {
                move.setMoveType(MoveType.CAPTURE);
            } else {
                move.setMoveType(MoveType.NORMAL);
            }
        }

        if (move.getMoveType() == MoveType.EN_PASSANT) {
            Position capturedPos = new Position(move.getFrom().getRow(), move.getTo().getColumn());
            move.setCapturedPiecePosition(capturedPos);
            move.setCapturedPiece(getPiece(capturedPos));
            setPiece(capturedPos, null);

            fromSquare.setPiece(null);
            toSquare.setPiece(movingPiece);
        } else if (move.getMoveType() == MoveType.CASTLING) {
            move.setCapturedPiece(null);
            fromSquare.setPiece(null);
            toSquare.setPiece(movingPiece);

            int row = move.getFrom().getRow();
            Position rookFrom;
            Position rookTo;
            if (move.getTo().getColumn() == 6) { // Kingside
                rookFrom = new Position(row, 7);
                rookTo = new Position(row, 5);
            } else { // Queenside
                rookFrom = new Position(row, 0);
                rookTo = new Position(row, 3);
            }

            Piece rook = getPiece(rookFrom);
            if (rook != null) {
                move.setCastledRook(rook);
                move.setRookFrom(rookFrom);
                move.setRookTo(rookTo);
                move.setRookPreviouslyMoved(rook.isHasMoved());
                setPiece(rookFrom, null);
                setPiece(rookTo, rook);
                rook.setHasMoved(true);
            }
        } else {
            move.setCapturedPiece(toSquare.getPiece());
            move.setCapturedPiecePosition(move.getTo());
            fromSquare.setPiece(null);
            toSquare.setPiece(movingPiece);
        }

        movingPiece.setHasMoved(true);
    }
}
