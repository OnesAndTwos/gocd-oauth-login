package com.tw.go.plugin.provider.bitbucket;

import org.brickred.socialauth.Profile;

import java.util.ArrayList;
import java.util.List;

public class BitBucketProfile extends Profile {

    private List<String> teams = new ArrayList<>();

    public List<String> getTeams() {
        return teams;
    }

    public void addTeam(String team) {
        this.teams.add(team);
    }
}
