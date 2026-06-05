package de.mwvb.blockpuzzle.global

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.mwvb.blockpuzzle.game.MainActivity

/**
 * Launcher activity.
 *
 * Initializes the data layer, runs the data migration if necessary and then
 * starts the classic puzzle game ("Altes Spiel") directly. There is no game
 * mode selection anymore.
 */
class StartScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AbstractDAO.init(this)

        val migration = Migration5to6()
        if (migration.isNecessary) {
            println("Data migration to version 6 is necessary.")
            migration.migrate(this)
            // IF the migration crashes it's better also to crash the app, because we don't want to loose the old game state.
            println("Migration finished.")
        }

        val gd = GlobalData.get()
        gd.gameType = GameType.OLD_GAME
        gd.save()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
