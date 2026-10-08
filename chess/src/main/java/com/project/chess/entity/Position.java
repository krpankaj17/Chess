package com.project.chess.entity;

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
@Table(name = "Position")
public class Position {
    @Id
    private Long id;
    private Integer row ;
    private  Integer column;
}
