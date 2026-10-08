package com.project.chess.service;

import com.project.chess.entity.Position;
import org.springframework.stereotype.Service;

@Service
public class SquareService {
   private final BoardService boardService ;
   public SquareService(BoardService boardService){
       this.boardService = boardService;
   }
    public Boolean isEmpty(Position position){
      return   !boardService.isOccupied(position);

    }
}
