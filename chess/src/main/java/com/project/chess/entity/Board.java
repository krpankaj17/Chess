package com.project.chess.entity;

import com.project.chess.enums.Color;
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
    Square fromSquare = getSquare(move.getFrom());
    Square toSquare = getSquare(move.getTo());

    if (fromSquare == null || toSquare == null) {
        return;
    }

    fromSquare.setPiece(move.getPiece());
    toSquare.setPiece(move.getCapturedPiece());
}


    public void movePiece(Move move) {
        Square fromSquare = getSquare(move.getFrom());
        Square toSquare = getSquare(move.getTo());

        if (fromSquare == null || toSquare == null || fromSquare.isEmpty()) {
            return;
        }

        Piece movingPiece = fromSquare.getPiece();
        move.setPiece(movingPiece);
        move.setCapturedPiece(toSquare.getPiece());

        fromSquare.setPiece(null);
        toSquare.setPiece(movingPiece);
        movingPiece.setHasMoved(true);
    }
}
