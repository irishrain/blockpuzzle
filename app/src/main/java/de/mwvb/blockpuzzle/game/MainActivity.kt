package de.mwvb.blockpuzzle.game

import android.content.ClipDescription
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.mwvb.blockpuzzle.R
import de.mwvb.blockpuzzle.databinding.ActivityMainBinding
import de.mwvb.blockpuzzle.gamepiece.GamePiece
import de.mwvb.blockpuzzle.gamepiece.GamePieceTouchListener
import de.mwvb.blockpuzzle.gamepiece.GamePieceView
import de.mwvb.blockpuzzle.gamestate.SpielstandDAO
import de.mwvb.blockpuzzle.global.AbstractDAO
import de.mwvb.blockpuzzle.global.messages.MessageFactory
import de.mwvb.blockpuzzle.playingfield.Action
import de.mwvb.blockpuzzle.playingfield.IPlayingFieldView
import de.mwvb.blockpuzzle.playingfield.PlayingFieldView
import de.mwvb.blockpuzzle.playingfield.QPosition
import de.mwvb.blockpuzzle.playingfield.gravitation.ShakeService

/**
 * GameActivity
 *
 * It has still the name MainActivity because I'm not sure how to change the name
 * without destroying everything.
 */
class MainActivity : AppCompatActivity(), IGameView {
    private lateinit var binding: ActivityMainBinding
    private lateinit var gameEngine: GameEngine
    private lateinit var shakeService : ShakeService
    private var messages: MessageFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // INIT PHASE
        if (Build.VERSION.SDK_INT >= 21) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.navigationBackground)
        }

        // SUPER PHASE
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AbstractDAO.init(this)

        // REST OF METHOD PHASE
        (binding.placeholder1 as ViewGroup).addView(GamePieceView(baseContext, 1, false))
        (binding.placeholder2 as ViewGroup).addView(GamePieceView(baseContext, 2, false))
        (binding.placeholder3 as ViewGroup).addView(GamePieceView(baseContext, 3, false))
        (binding.parking      as ViewGroup).addView(GamePieceView(baseContext, -1, true))

        initTouchListener(1)
        initTouchListener(2)
        initTouchListener(3)
        initTouchListener(-1)
        binding.playingField.setOnDragListener(createDragListener(false)) // Drop Event für Spielfeld
        binding.parking.setOnDragListener(createDragListener(true)) // Drop Event fürs Parking
        initNewGameButton() // also Undo button
    }

    // Activity reactivated
    override fun onResume() {
        super.onResume()
        try {
            initGameEngine()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.javaClass.toString() + ": " + e.message + "\n" + e.stackTrace[0].toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun initGameEngine() {
        gameEngine = GameEngineFactory().create(this)

        shakeService = ShakeService(gameEngine)
        shakeService.setActive(true)
        shakeService.initShakeDetection(this)

        when (gameEngine.topButtonMode) {
            TopButtonMode.UNDO -> {
                binding.newGame.visibility = View.VISIBLE
                binding.newGame.setText(R.string.undo)
                binding.newGame.setBackgroundColor(ContextCompat.getColor(this, R.color.colorGrey))
            }
            TopButtonMode.NO_BUTTON -> binding.newGame.visibility = View.INVISIBLE
            else -> binding.newGame.visibility = View.VISIBLE
        }
    }

    // Activity goes sleeping
    override fun onPause() {
        gameEngine.save()

        shakeService.setActive(false)

        super.onPause()
    }

    /** Drag or rotate game piece */
    private fun initTouchListener(index: Int) {
        getGamePieceView(index).setOnTouchListener(object : GamePieceTouchListener(index, resources) {
            override fun up(view: View?, event: MotionEvent?) {
                getGamePieceView(index).endDragMode()
                gameEngine.rotate(index)
            }

            override fun isDragAllowed(): Boolean {
                return !gameEngine.isLostGame && gameEngine.isDragAllowed
            }
        })
    }

    /** mainly for dropping the game piece */
    private fun createDragListener(targetIsParking: Boolean): View.OnDragListener {
        return View.OnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DROP -> dropped(event, targetIsParking)
                DragEvent.ACTION_DRAG_ENTERED -> {
                    if (targetIsParking) {
                        getGamePieceView(-1).onDragEnter()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_LOCATION -> true
                DragEvent.ACTION_DRAG_EXITED -> {
                    if (targetIsParking) {
                        getGamePieceView(-1).onDragLeave()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    try {
                        if (targetIsParking) {
                            getGamePieceView(-1).onDragLeave()
                        }
                        // Da ich nicht weiß, welcher ausgeblendet ist, blende ich einfach alle ein.
                        getGamePieceView(1).endDragMode()
                        getGamePieceView(2).endDragMode()
                        getGamePieceView(3).endDragMode()
                        getGamePieceView(-1).endDragMode()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "F/dragEnd: " + e.message, Toast.LENGTH_LONG).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dropped(event: DragEvent, targetIsParking: Boolean): Boolean {
        try {
            val item = event.clipData.getItemAt(0)
            val index: Int = item.text.toString().toInt()
            val gamePiece = getGamePieceView(index).gamePiece
            var xy : QPosition = QPosition(-1, -1)

            if (!targetIsParking) {
                // geg.: px, ges.: SpielfeldView Koordinaten (0 - 9)
                xy = calculatePlayingFieldCoordinates(event, gamePiece)
            }
            gameEngine.dispatch(targetIsParking, DropActionModel(index, gamePiece, xy))

            if (gameEngine.rebuild) {
                gameEngine.rebuild = false;
                gameEngine.isDragAllowed = false // will be set to true in GameEngine construction
                @Suppress("DEPRECATION")
                Handler().postDelayed({ initGameEngine() }, 1200)
            }
        } catch (e: DoesNotWorkException) {
            binding.playingField.soundService.doesNotWork()
            Toast.makeText(this, R.string.gehtNicht, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "MA:" + e.message, Toast.LENGTH_LONG).show()
        }
        return true
    }

    private fun calculatePlayingFieldCoordinates(event: DragEvent, gamePiece: GamePiece): QPosition {
        val f = resources.displayMetrics.density
        var x = event.x / f // px -> dp
        var y = event.y / f
        // jetzt in Spielfeld Koordinaten umrechnen
        val br = PlayingFieldView.w / gameEngine.blocks
        x /= br
        y = y / br - 2 - (gamePiece.maxY - gamePiece.minY)
        return QPosition(x.toInt(), y.toInt())
    }

    private fun initNewGameButton() {
        binding.newGame.setOnClickListener {
            when (gameEngine.topButtonMode) {
                TopButtonMode.NEW_GAME -> startNewGameDispatch()
                TopButtonMode.UNDO     -> undo()
                else                   -> {}
            }
        }
    }

    private fun startNewGameDispatch() {
        if (gameEngine.isLostGame || gameEngine.lessScore()) {
            startNewGame()
        } else {
            val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
            dialog.setTitle(resources.getString(R.string.startNewGame))
            dialog.setPositiveButton(resources.getString(android.R.string.ok)) { _, _ -> startNewGame() }
            dialog.setNegativeButton(resources.getString(android.R.string.cancel), null)
            dialog.show()
        }
    }

    private fun startNewGame() {
        SpielstandDAO().deleteOldGame()
        initGameEngine()
    }

    private fun undo() {
        try {
            gameEngine.undo()
        } catch (e: DoesNotWorkException) {
            binding.playingField.soundService.doesNotWork()
            Toast.makeText(this, R.string.gehtNicht, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getSpecialAction(specialState: Int): Action {
        return Action {}
    }

    override fun showScore(text: String) {
        binding.info.text = text
    }

    override fun showMoves(text: String) {
        binding.infoDisplay.text = text
    }

    override fun showPlanetNumber(number: Int) {
        binding.territoryName.text = resources.getString(R.string.planetNo, number)
        binding.territoryName.visibility = View.VISIBLE
    }

    override fun showTerritoryName(resId: Int) {
        val text = resources.getText(resId).trim()
        binding.territoryName.text = text
        binding.territoryName.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE // hide label to save space
    }

    override fun getPlayingFieldView(): IPlayingFieldView {
        return binding.playingField
    }

    override fun getGamePieceView(index: Int): GamePieceView {
        return when (index) {
            1 -> (binding.placeholder1 as ViewGroup).getChildAt(0) as GamePieceView
            2 -> (binding.placeholder2 as ViewGroup).getChildAt(0) as GamePieceView
            3 -> (binding.placeholder3 as ViewGroup).getChildAt(0) as GamePieceView
            -1 -> (binding.parking     as ViewGroup).getChildAt(0) as GamePieceView
            else -> throw RuntimeException()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun shake() {
        binding.playingField.soundService.shake()
    }

    override fun playSound(number: Int) {
        binding.playingField.soundService.playSound(number)
    }

    override fun getMessages(): MessageFactory {
        if (messages == null) {
            messages = MessageFactory(this)
        }
        return messages!!
    }
}
