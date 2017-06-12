package com.tw.go.plugin.provider.bitbucket;

import com.tw.go.plugin.User;

import java.util.List;

public class BitBucketUser extends User {
    private final List<String> teams;

    public BitBucketUser(String username, String displayName, String emailId, List<String> teams) {
        super(username, displayName, emailId);
        this.teams = teams;
    }

    public boolean belongsToTeam(String team) {
        return teams.contains(team);
    }

    public boolean belongsToOneOfTheTeams(List<String> teams) {
        for(String team : teams) {
           if(belongsToTeam(team)) return true;
        }

        return false;
    }
}
