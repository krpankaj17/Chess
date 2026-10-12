package com.project.chess;

import com.project.chess.dto.MatchResult;
import com.project.chess.service.EloService;
import com.project.chess.service.GameSessionManager;
import com.project.chess.service.MatchmakingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MatchmakingAndEloTests {

    private EloService eloService;
    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        eloService = new EloService();
        matchmakingService = new MatchmakingService(new GameSessionManager());
    }

    @Test
    void testEqualRatingWin() {
        int[] newRatings = eloService.calculateNewRatings(1200, 1200, 1.0);
        assertEquals(1216, newRatings[0]);
        assertEquals(1184, newRatings[1]);
    }

    @Test
    void testEqualRatingDraw() {
        int[] newRatings = eloService.calculateNewRatings(1200, 1200, 0.5);
        assertEquals(1200, newRatings[0]);
        assertEquals(1200, newRatings[1]);
    }

    @Test
    void testUpsetWin() {
        int[] newRatings = eloService.calculateNewRatings(1000, 1400, 1.0);
        assertTrue(newRatings[0] > 1025);
        assertTrue(newRatings[1] < 1375);
    }

    @Test
    void testMatchmakingSimilarEloPairs() {
        MatchResult res1 = matchmakingService.findMatch("alice", 1200);
        assertFalse(res1.isMatched());
        assertEquals(1, matchmakingService.getQueueSize());

        MatchResult res2 = matchmakingService.findMatch("bob", 1240);
        assertTrue(res2.isMatched());
        assertNotNull(res2.getGameId());
        assertEquals(0, matchmakingService.getQueueSize());

        Optional<MatchResult> statusAlice = matchmakingService.checkStatus("alice");
        assertTrue(statusAlice.isPresent());
        assertEquals(res2.getGameId(), statusAlice.get().getGameId());
    }

    @Test
    void testMatchmakingDistantEloDoesNotPair() {
        MatchResult res1 = matchmakingService.findMatch("beginner", 800);
        assertFalse(res1.isMatched());

        MatchResult res2 = matchmakingService.findMatch("grandmaster", 2200);
        assertFalse(res2.isMatched());
        assertEquals(2, matchmakingService.getQueueSize());
    }
}
