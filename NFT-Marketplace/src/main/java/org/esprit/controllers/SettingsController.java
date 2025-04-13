package org.esprit.controllers;

import org.esprit.models.User;

public class SettingsController {
    private User currentUser;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}