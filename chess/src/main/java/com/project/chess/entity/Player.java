package com.project.chess.entity;

import com.project.chess.enums.Color;
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
public class Player {
    @Id
    private Long id;
    private  String name ;
    private String email;
    private Color color ;
    private Integer elo;
}
