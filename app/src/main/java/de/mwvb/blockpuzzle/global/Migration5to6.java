package de.mwvb.blockpuzzle.global;

import android.content.Context;
import android.content.SharedPreferences;

import org.jetbrains.annotations.NotNull;

import de.mwvb.blockpuzzle.gamestate.GamePlayState;
import de.mwvb.blockpuzzle.gamestate.Spielstand;
import de.mwvb.blockpuzzle.gamestate.SpielstandDAO;

/**
 * Data migration from V5 to V6. From SharedPrefs to JSON files. From IPersistence to AbstractDAO. From API controlled to object oriented.
 */
public class Migration5to6 {
    // Global data ----
    private static final String GLOBAL_OLD_GAME = "/oldGame";
    private static final String GLOBAL_PLAYERNAME = "/playername";
    private static final String GLOBAL_PLAYERNAME_ENTERED = "/playernameEntered";
    private static final String GLOBAL_GAME_SOUNDS = "/gameSounds";
    private static final String GLOBAL_CURRENT_PLANET = "/currentPlanet";
    private static final String GLOBAL_PLATINUM_TROPHY = "/trophyPlatinum"; // galaxy wide
    private static final String GLOBAL_LAST_TROPHY_DATE = "/trophyLastDate";// galaxy wide
    // Game specific data ----
    private static final String SCORE = "score";
    private static final String DELTA = "delta";
    private static final String MOVES = "moves";
    private static final String EMPTY_SCREEN_BONUS_ACTIVE = "emptyScreenBonusActive";
    private static final String DAILY_DATE = "dailyDate";
    private static final String GAME_OVER = "gameOver";
    private static final String HIGHSCORE_SCORE = "highscore";
    private static final String HIGHSCORE_MOVES = "highscoreMoves";
    private static final String GAMEPIECEVIEW = "gamePieceView";
    private static final String PLAYINGFIELD = "playingField";
    private static final String GRAVITATION_ROWS = "gravitationRows";
    private static final String GRAVITATION_EXCLUSIONS = "gravitationExclusions";
    private static final String GRAVITATION_PLAYED_SOUND = "gravitationPlayedSound"; // real boolean
    private static final String OWNER_NAME = "owner_name";
    private static final String OWNER_SCORE = "owner_score";
    private static final String OWNER_MOVES = "owner_moves";
    private static final String NEXT_ROUND = "nextRound";

    private SharedPreferences pref;
    private String prefix = "";
    private final GlobalDataDAO globalDataDAO = new GlobalDataDAO();
    private final SpielstandDAO spielstandDAO = new SpielstandDAO();

    public boolean isNecessary() {
        return !globalDataDAO.exists();
    }

    public void migrate(Context context) {
        System.out.println("-------------------MIGRATION---------------------");
        pref = context.getSharedPreferences("GAMEDATA_2", Context.MODE_PRIVATE);
        if (pref.getAll() == null || pref.getAll().isEmpty()) {
            return; // not necessary because first use of app
        }

        GlobalData g = new GlobalData();
        g.setCurrentPlanet(getInt(GLOBAL_CURRENT_PLANET, 1));
        g.setGameSounds(getBoolean(GLOBAL_GAME_SOUNDS, true));
        g.setPlayername(getString(GLOBAL_PLAYERNAME));
        g.setPlayernameEntered(getBoolean(GLOBAL_PLAYERNAME_ENTERED));
        g.setGameType(getGameType());
        g.setPlatinumTrophies(getInt(GLOBAL_PLATINUM_TROPHY));
        g.setLastTrophyDate(getString(GLOBAL_LAST_TROPHY_DATE));
        globalDataDAO.save(g);

        Spielstand ss0 = new Spielstand();
        prefix = "";
        mapSpielstand(ss0);
        spielstandDAO.saveOldGame(ss0);

        pref = null;
        System.out.println("--------- Migration erfolgreich ---------------");
    }

    @NotNull
    private GameType getGameType() {
        switch (getInt(GLOBAL_OLD_GAME)) {
            case 1:
            case 2: // former Stone Wars save -> continue with the classic game
                return GameType.OLD_GAME;
            default: // 0
                return GameType.NOT_SELECTED;
        }
    }

    private void mapSpielstand(Spielstand ss) {
        // TODO Spiele durchrechnen um den Status sicher zu bekommen. Eine GameDefinition müsste ja zu einem Spielstand sagen können,
        //      ob PLAYING, LOST oder WON. Und das ohne View Komponenten.
        ss.setState(getBoolean(GAME_OVER) ? GamePlayState.LOST_GAME : GamePlayState.PLAYING);

        ss.setScore(getInt(SCORE, -9999));
        ss.setMoves(getInt(MOVES));
        ss.setDelta(getInt(DELTA));
        ss.setEmptyScreenBonusActive(getBoolean(EMPTY_SCREEN_BONUS_ACTIVE));
        ss.setDailyDate(getString(DAILY_DATE));
        ss.setHighscore(getInt(HIGHSCORE_SCORE));
        ss.setHighscoreMoves(getInt(HIGHSCORE_MOVES));
        ss.setGamePieceView1(getString(GAMEPIECEVIEW + 1));
        ss.setGamePieceView2(getString(GAMEPIECEVIEW + 2));
        ss.setGamePieceView3(getString(GAMEPIECEVIEW + 3));
        ss.setGamePieceViewP(getString(GAMEPIECEVIEW + -1));
        ss.setPlayingField(getString(PLAYINGFIELD));
        String rows = getString(GRAVITATION_ROWS);
        if (rows != null && rows.contains("/")) rows = ""; // alten bug fixen
        ss.setGravitationRows(rows);
        ss.setGravitationExclusions(getString(GRAVITATION_EXCLUSIONS));

        String n = name(GRAVITATION_PLAYED_SOUND);
        boolean gps = pref.getBoolean(n, false);
        ss.setGravitationPlayedSound(gps);

        ss.setOwnerName(getString(OWNER_NAME));
        ss.setOwnerScore(getInt(OWNER_SCORE));
        ss.setOwnerMoves(getInt(OWNER_MOVES));
        ss.setNextRound(getInt(NEXT_ROUND));
    }

    private String getString(String name) {
        return pref.getString(name(name), "");
    }

    private int getInt(String name) {
        return getInt(name, 0);
    }

    private int getInt(String name, int pDefault) {
        return pref.getInt(name(name), pDefault);
    }

    // boolean saved as int (0, 1)
    private boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    // boolean saved as int (0, 1)
    private boolean getBoolean(String name, boolean pDefault) {
        return getInt(name, pDefault ? 1 : 0) == 1;
    }

    private String name(String name) {
        if (name.startsWith("/")) { // global parameter -> don't prepend game-specific suffix
            return name.substring(1);
        }
        return prefix + name;
    }

}
