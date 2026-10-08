package com.project.chess.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Position")
public class Square {
    @Id
    private Long id;
    @ManyToOne
    @JoinColumn(name = "position_id")
    private Position position;
   @ManyToOne
   @JoinColumn(name = "piece_id")
   private Piece piece;


}
