package de.mwvb.blockpuzzle.game

class GameEngineFactory {

    /**
     * Returns initialized game engine for the classic puzzle game ("Altes Spiel").
     */
    fun create(view: IGameView): GameEngine {
        return GameEngineBuilder().build(view)
    }
}
