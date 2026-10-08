package com.project.chess.entity;

import com.project.chess.enums.MoveType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Player")
public class Move {
    @Id
    private Long id;
    @ManyToOne
    @JoinColumn(name = "from_id")
    private  Position from ;
    @ManyToOne
    @JoinColumn(name = "to_id")
    private  Position to;
    @ManyToOne
    @JoinColumn(name = "piece_id")
    private Piece piece ;
    @ManyToOne
    @JoinColumn(name = "captured_piece_id")
    private Piece capturedPiece;
    private MoveType moveType ;

}
