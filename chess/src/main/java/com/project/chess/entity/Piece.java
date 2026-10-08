package com.project.chess.entity;

import com.project.chess.enums.Color;
import com.project.chess.enums.PieceType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Player")

public class Piece {
    @Id
    private Long id;
    private Color color;
    private PieceType type;

}
