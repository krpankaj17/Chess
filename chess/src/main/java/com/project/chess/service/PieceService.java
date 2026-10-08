package com.project.chess.service;

import com.project.chess.entity.Board;
import com.project.chess.entity.Piece;
import com.project.chess.entity.Position;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PieceService {
    Boolean isValidMove(Board board, Position from , Position to, Piece piece);
    List<Position>  getAvailableMoves(Board board,Position from,Piece piece);
}
