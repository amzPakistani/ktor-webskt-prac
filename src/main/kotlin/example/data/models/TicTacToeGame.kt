package example.data.models

import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TicTacToeGame {
    private val state = MutableStateFlow(GameState())
    private val gameScope = CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val playerSockets = ConcurrentHashMap<Char, WebSocketSession>()
    private var delayGameJob: Job? = null

    init {
        state.onEach(::broadcast).launchIn(gameScope)
    }

    fun connectPlayer(session: WebSocketSession):Char?{
        val isPlayerX = state.value.connectedPlayers.any{it=='X'}
        val player = if(isPlayerX) 'O' else 'X'

        state.update {
            if(state.value.connectedPlayers.contains(player)){
                return null
            }
            if(!playerSockets.contains(player)){
                playerSockets[player]=session
            }
            it.copy(connectedPlayers = it.connectedPlayers+player)
        }
        return player
    }

    fun disconnectPlayer(player:Char){
        playerSockets.remove(player)
        state.update {
            it.copy(connectedPlayers = it.connectedPlayers-player)

        }
    }

    suspend fun broadcast(state: GameState){ //WebSocket communication is text-based
        playerSockets.values.forEach { socket ->
            socket.send(Json.encodeToString(state))
        }
    }

    fun finishTurn(player: Char, x:Int, y:Int){
        if(state.value.field[x][y]!= null||state.value.winningPlayer!=null){
            return
        }
        if(state.value.playerAtTurn==player){
            return
        }
        val currentPlayer = state.value.playerAtTurn
        state.update {
            val newField = it.field.also {field ->
                field[y][x] = currentPlayer
            }
            val isBoardFull = newField.all {
                it.all {
                    it!=null
                }
            }

        }
    }

}