package org.esprit.services;

import java.util.List;

import org.esprit.models.BetSession;

public interface IBetSessionService {
    
    void addBetSession(BetSession betSession);
    void updateBetSession(BetSession betSession);
    void deleteBetSession(int id);
    BetSession getBetSessionById(int id);
    List<BetSession> getAllBetSessions();
}
