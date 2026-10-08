package com.project.chess.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Player")

public class MoveHistory {
    @Id
    private Long id;
    @ManyToOne
    @JoinColumn(name = "move_id")
    private Move move;
    
}
