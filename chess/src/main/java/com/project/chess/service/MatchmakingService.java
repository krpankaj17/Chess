package com.project.chess.service;

import com.project.chess.dto.MatchRequest;
import com.project.chess.dto.MatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private static final int MAX_ELO_DIFFERENCE = 200;

    private final GameSessionManager gameSessionManager;
    private final List<MatchRequest> queue = new CopyOnWriteArrayList<>();
    private final Map<String, MatchResult> pendingMatches = new ConcurrentHashMap<>();

    public synchronized MatchResult findMatch(String username, int elo) {
        Map.Entry<String, com.project.chess.entity.ChessGame> active = gameSessionManager.findActiveGameForUser(username);
        if (active != null) {
            com.project.chess.entity.ChessGame g = active.getValue();
            return MatchResult.builder()
                    .matched(true)
                    .gameId(active.getKey())
                    .whitePlayer(g.getWhitePlayer().getName())
                    .blackPlayer(g.getBlackPlayer().getName())
                    .build();
        }

        if (pendingMatches.containsKey(username)) {
            return pendingMatches.remove(username);
        }

        for (MatchRequest waiting : queue) {
            if (waiting.getUsername().equals(username)) {
                return MatchResult.builder().matched(false).build();
            }
        }

        MatchRequest bestOpponent = null;
        int smallestDiff = Integer.MAX_VALUE;

        for (MatchRequest waiting : queue) {
            int diff = Math.abs(elo - waiting.getElo());
            if (diff <= MAX_ELO_DIFFERENCE && diff < smallestDiff) {
                smallestDiff = diff;
                bestOpponent = waiting;
            }
        }

        if (bestOpponent != null) {
            queue.remove(bestOpponent);

            String gameId = UUID.randomUUID().toString();
            gameSessionManager.createGame(gameId, username, bestOpponent.getUsername());

            MatchResult matchResult = MatchResult.builder()
                    .matched(true)
                    .gameId(gameId)
                    .whitePlayer(username)
                    .blackPlayer(bestOpponent.getUsername())
                    .build();

            pendingMatches.put(bestOpponent.getUsername(), matchResult);
            return matchResult;
        }

        queue.add(new MatchRequest(username, elo));
        return MatchResult.builder().matched(false).build();
    }

    public Optional<MatchResult> checkStatus(String username) {
        MatchResult pending = pendingMatches.remove(username);
        if (pending != null) {
            return Optional.of(pending);
        }

        Map.Entry<String, com.project.chess.entity.ChessGame> active = gameSessionManager.findActiveGameForUser(username);
        if (active != null) {
            com.project.chess.entity.ChessGame g = active.getValue();
            return Optional.of(MatchResult.builder()
                    .matched(true)
                    .gameId(active.getKey())
                    .whitePlayer(g.getWhitePlayer().getName())
                    .blackPlayer(g.getBlackPlayer().getName())
                    .build());
        }

        return Optional.empty();
    }

    public void cancel(String username) {
        queue.removeIf(req -> req.getUsername().equals(username));
        pendingMatches.remove(username);
    }

    public void leaveGame(String username) {
        Map.Entry<String, com.project.chess.entity.ChessGame> active = gameSessionManager.findActiveGameForUser(username);
        if (active != null) {
            gameSessionManager.removeGame(active.getKey());
        }
        cancel(username);
    }

    public int getQueueSize() {
        return queue.size();
    }
}
