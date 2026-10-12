package com.project.chess.service;

import org.springframework.stereotype.Service;

@Service
public class EloService {

    private static final int K_FACTOR = 32;

    public int[] calculateNewRatings(int ratingWhite, int ratingBlack, double scoreWhite) {
        double expectedWhite = 1.0 / (1.0 + Math.pow(10, (ratingBlack - ratingWhite) / 400.0));
        double expectedBlack = 1.0 - expectedWhite;

        double scoreBlack = 1.0 - scoreWhite;

        int newRatingWhite = (int) Math.round(ratingWhite + K_FACTOR * (scoreWhite - expectedWhite));
        int newRatingBlack = (int) Math.round(ratingBlack + K_FACTOR * (scoreBlack - expectedBlack));

        return new int[]{newRatingWhite, newRatingBlack};
    }
}
