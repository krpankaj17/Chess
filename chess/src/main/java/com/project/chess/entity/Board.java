package com.project.chess.entity;

import lombok.*;
import java.util.List;
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Board {


    List<List<Integer>> fixedBoard = List.of(
            List.of(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0),
            List.of(0, 0, 0, 0, 0, 0, 0, 0)
    );



}
