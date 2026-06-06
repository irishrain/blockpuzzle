package de.mwvb.blockpuzzle.game.place;

import de.mwvb.blockpuzzle.game.GameEngineInterface;
import de.mwvb.blockpuzzle.gamestate.GameState;
import de.mwvb.blockpuzzle.playingfield.FilledRows;
import de.mwvb.blockpuzzle.playingfield.PlayingField;
import de.mwvb.blockpuzzle.playingfield.gravitation.GravitationAction;
import de.mwvb.blockpuzzle.playingfield.gravitation.GravitationData;

/**
 * Clear rows: add score and clear the filled rows
 */
public class ClearRowsPlaceAction implements IPlaceAction {

    @Override
    public void perform(PlaceActionModel info) {
        addScoreForClearedRows(info);
        clearRows(info);
    }

    protected void addScoreForClearedRows(PlaceActionModel info) {
        GameState gs = info.getGs();
        FilledRows f = info.getFilledRows();
        gs.addScore(f.getHits() * info.getDefinition().getHitsScoreFactor());
        rowsAdditionalBonus(f.getXHits(), f.getYHits(), info);
    }

    protected void rowsAdditionalBonus(int xrows, int yrows, PlaceActionModel info) {
        if (!info.getDefinition().isRowsAdditionalBonusEnabled()) {
            return;
        }
        int bonus = 0;
        switch (xrows + yrows) {
            case 0:
            case 1: break; // 0-1 kein Bonus
            // Bonuspunkte wenn mehr als 2 Rows gleichzeitig abgeräumt werden.
            // Fällt mir etwas schwer zu entscheiden wieviel Punkte das jeweils wert ist.
            case 2:  bonus = 12; break;
            case 3:  bonus = 17; break;
            case 4:  bonus = 31; break;
            case 5:  bonus = 44; break;
            default: bonus = 22; break;
        }
        if (xrows > 0 && yrows > 0) {
            bonus += 10;
        }
        info.getGs().addScore(bonus);
        // TO-DO Reihe mit gleicher Farbe (ohne oldOneColor) könnte weiteren Bonus auslösen.
    }

    /**
     * Clears the full rows. Gravity is disabled: the blocks above a cleared row
     * stay where they are instead of falling down. After the clear animation we
     * only re-evaluate which game pieces still fit into the playing field (the
     * step the gravitation action used to perform at its end).
     */
    protected void clearRows(PlaceActionModel info) {
        GameEngineInterface engine = info.getGameEngineInterface();
        info.getPlayingField().clearRows(info.getFilledRows(), engine::checkIfNoMoveIsPossible);
    }

    public void executeGravitation(GravitationData gravitation, GameEngineInterface possibleMovesChecker, PlayingField playingField, int gravitationStartRow) {
        new GravitationAction(gravitation, possibleMovesChecker, playingField, gravitationStartRow).execute();
    }
}
