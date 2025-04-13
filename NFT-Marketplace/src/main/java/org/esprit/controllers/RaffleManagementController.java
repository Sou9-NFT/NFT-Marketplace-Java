package org.esprit.controllers;

import org.esprit.models.User;

public class RaffleManagementController {
    private User currentUser;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}