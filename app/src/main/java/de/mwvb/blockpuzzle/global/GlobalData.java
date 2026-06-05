package de.mwvb.blockpuzzle.global;

public class GlobalData {
    private GameType gameType = GameType.NOT_SELECTED;
    private String playername = null;
    private boolean playernameEntered = false;
    private boolean gameSounds = true;
    private boolean sunMode = false;
    private int currentPlanet = 1;
    // Trophies
    private int platinumTrophies = 0;
    private String lastTrophyDate = null;

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public String getPlayername() {
        return playername;
    }

    public void setPlayername(String playername) {
        this.playername = playername;
    }

    public boolean isPlayernameEntered() {
        return playernameEntered;
    }

    public void setPlayernameEntered(boolean playernameEntered) {
        this.playernameEntered = playernameEntered;
    }

    public boolean isGameSounds() {
        return gameSounds;
    }

    public void setGameSounds(boolean gameSounds) {
        this.gameSounds = gameSounds;
    }

    public boolean isSunMode() {
        return sunMode;
    }

    public void setSunMode(boolean sunMode) {
        this.sunMode = sunMode;
    }

    public int getCurrentPlanet() {
        return currentPlanet;
    }

    public void setCurrentPlanet(int currentPlanet) {
        this.currentPlanet = currentPlanet;
    }

    public int getPlatinumTrophies() {
        return platinumTrophies;
    }

    public void setPlatinumTrophies(int platinumTrophies) {
        this.platinumTrophies = platinumTrophies;
    }

    public String getLastTrophyDate() {
        return lastTrophyDate;
    }

    public void setLastTrophyDate(String lastTrophyDate) {
        this.lastTrophyDate = lastTrophyDate;
    }

    public static GlobalData get() {
        return new GlobalDataDAO().load();
    }

    public void save() {
        new GlobalDataDAO().save(this);
    }
}
