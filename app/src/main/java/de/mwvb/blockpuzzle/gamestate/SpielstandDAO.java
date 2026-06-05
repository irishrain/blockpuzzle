package de.mwvb.blockpuzzle.gamestate;

import androidx.annotation.NonNull;

import de.mwvb.blockpuzzle.global.AbstractDAO;

/**
 * Game state DAO
 */
public final class SpielstandDAO extends AbstractDAO<Spielstand> {
    private static final String OLD_GAME_ID = "OLD_GAME";

    @NonNull
    public Spielstand loadOldGame() {
        return load(OLD_GAME_ID);
    }

    public void saveOldGame(Spielstand ss) {
        save(OLD_GAME_ID, ss);
    }

    public void deleteOldGame() {
        delete(OLD_GAME_ID);
    }

    @Override
    protected Class<Spielstand> getTClass() {
        return Spielstand.class;
    }
}
