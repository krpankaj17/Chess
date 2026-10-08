package com.project.chess.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PieceFactory{
    private final Map<String, PieceService> pieceServiceMap;
    public PieceService getPieceService(String pieceType){
       return pieceServiceMap.get(pieceType);
    }
    
}
