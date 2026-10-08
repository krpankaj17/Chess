package com.project.chess.service;

import com.project.chess.entity.Board;
import com.project.chess.entity.Piece;
import com.project.chess.entity.Position;
import org.springframework.stereotype.Service;

import java.util.List;
@Service(RookService.BEAN_ID)
public class RookService implements PieceService{
    public static  final String BEAN_ID = "rookService";
    @Override
    public Boolean isValidMove(Board board, Position from, Position to, Piece piece) {
        return null;
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from, Piece piece) {
        return List.of();
    }
}
