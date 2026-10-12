package com.project.chess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchResult {
    private boolean matched;
    private String gameId;
    private String whitePlayer;
    private String blackPlayer;
}
