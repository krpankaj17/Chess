package com.project.chess.controller;

import com.project.chess.dto.MatchResult;
import com.project.chess.entity.User;
import com.project.chess.repository.UserRepository;
import com.project.chess.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;
    private final UserRepository userRepository;
    private final com.project.chess.service.GameSessionManager gameSessionManager;

    @PostMapping("/find")
    public ResponseEntity<MatchResult> findMatch(Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        MatchResult result = matchmakingService.findMatch(username, user.getEloRating());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/current")
    public ResponseEntity<com.project.chess.dto.GameSnapshotDto> getCurrentGame(Authentication auth) {
        String username = auth.getName();
        java.util.Map.Entry<String, com.project.chess.entity.ChessGame> active = gameSessionManager.findActiveGameForUser(username);
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(gameSessionManager.getSnapshot(active.getKey()));
    }

    @GetMapping("/status")
    public ResponseEntity<MatchResult> checkStatus(Authentication auth) {
        String username = auth.getName();
        return matchmakingService.checkStatus(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(MatchResult.builder().matched(false).build()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(Authentication auth) {
        String username = auth.getName();
        matchmakingService.cancel(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave(Authentication auth) {
        String username = auth.getName();
        matchmakingService.leaveGame(username);
        return ResponseEntity.ok().build();
    }
}
