package sc.player2019.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.framework.plugins.Player;
import sc.player2019.Starter;
import sc.plugin2019.Board;
import sc.plugin2019.Field;
import sc.plugin2019.GameState;
import sc.plugin2019.IGameHandler;
import sc.plugin2019.Move;
import sc.plugin2019.util.Constants;
import sc.plugin2019.util.GameRuleLogic;
import sc.shared.GameResult;
import sc.shared.InvalidGameStateException;
import sc.shared.InvalidMoveException;
import sc.shared.PlayerColor;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Das Herz des Clients: Eine sehr simple Logik, die ihre Zuege zufaellig
 * waehlt, aber gueltige Zuege macht. Ausserdem werden zum Spielverlauf
 * Konsolenausgaben gemacht.
 */
public class AlphaBot implements IGameHandler {

	private Starter client;
	private GameState gameState;
	private Player currentPlayer;
	private Move bestMove;
	private String bestMoveStr;
	private BoardRater initBoarderRater;

	private int aufrufe;

	private boolean SHOW = true; // Ausgabe komplett untersagen?
	private boolean SAVETXT = false; // Ausgabe auf Desktop speichern?
	private String SAVEFILE = "piranha"; // .txt
	private boolean SHOW_HEADER = true;
	private boolean SHOW_MOVES = true;
	private boolean SHOW_BOARD = true;
	private boolean SHOW_BOARDRATER = true;
	private boolean SHOW_SUMMARY = true;

	private int DEEP = 2; // 1 = kein AlphaBeta, sondern nur Zugbewertung
	private String[] moveList = new String[DEEP];
	List<String> outPut = new ArrayList<String>();

	private static final Logger log = LoggerFactory.getLogger(AlphaBot.class);

	public AlphaBot(Starter client) {
		this.client = client;
	}

	/**
	 * {@inheritDoc}
	 */
	public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {
		log.info("Das Spiel ist beendet.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onRequestAction() {
		final long timeStart = System.currentTimeMillis();

		outPut.clear();

		initBoarderRater = new BoardRater(gameState.getBoard());

		startAlphaBeta();

		sendAction(bestMove);

		justOutPutSummary(timeStart);
	}

	private void startAlphaBeta() {
		aufrufe = 0;

		boolean error = false;

		try {
			// Eigentlicher Aufruf der Alphabeta
			alphaBeta(Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, DEEP);
		} catch (InvalidGameStateException e) {
			error = true;
			e.printStackTrace();
		} catch (InvalidMoveException e) {
			error = true;
			e.printStackTrace();
		}
		// ERROR
		if (error) {
			if (bestMove.equals(null) == true) {
				ArrayList<Move> possibleMoves = GameRuleLogic.getPossibleMoves(gameState);
				bestMove = possibleMoves.get((int) (Math.random() * possibleMoves.size()));
				outPut.add("RND MOVE: " + bestMove.toString());
			}
		}
	}

	private int alphaBeta(int alpha, int beta, int tiefe) throws InvalidGameStateException, InvalidMoveException {
		++aufrufe;
		// Abbruchkriterium
		if ((tiefe == 0) || endOfGame()) {
			int value = rateAlphaBeta();
			if (SHOW_HEADER) {
				outPut.add("");
				outPut.add("***N*E*W***M*O*V*E***");
				outPut.add("Value: " + value + " - Tiefe: " + DEEP + " - Aufrufe: " + aufrufe + " - Turn: "
						+ gameState.getTurn() + " - Round: " + gameState.getRound());
			}
			if (SHOW_MOVES) {
				for (String moveStr : moveList) {
					outPut.add(moveStr);
				}
			}
			return value;
		}
		boolean PVgefunden = false;
		int best = Integer.MIN_VALUE + 1;
		ArrayList<Move> moves = GameRuleLogic.getPossibleMoves(gameState);

		for (Move move : moves) {
			moveList[DEEP - tiefe] = move.toString() + " "
					+ this.gameState.getBoard().getField(move.x, move.y).getState().toString();
			GameState g = this.gameState.clone();
			move.perform(this.gameState);
			int wert;
			if (PVgefunden) {
				wert = -alphaBeta(-alpha - 1, -alpha, tiefe - 1);
				if (wert > alpha && wert < beta)
					wert = -alphaBeta(-beta, -wert, tiefe - 1);
			} else
				wert = -alphaBeta(-beta, -alpha, tiefe - 1);
			this.gameState = g;
			if (wert > best) {
				if (wert >= beta)
					return wert;
				best = wert;
				if (tiefe == DEEP) {
					bestMove = new Move(move.x, move.y, move.direction);
					bestMoveStr = bestMove.toString() + " Value: " + best;
					outPut.add("NEW BEST MOVE: " + bestMoveStr);
				}
				if (wert > alpha) {
					alpha = wert;
					PVgefunden = true;
				}
			}
		}
		return best;
	}

	private int rateAlphaBeta() {
		int value = 0;

		// Show Board
		if (SHOW_BOARD) {
			showBoard(this.gameState.getBoard());
		}

		PlayerColor current;
		PlayerColor opponent;
		if (DEEP % 2 == 0) {
			current = this.gameState.getCurrentPlayer().getColor();
		} else {
			current = this.gameState.getOtherPlayer().getColor();
		}
		opponent = current.opponent();

		for (int i = 0; i < Const.BOARDSIZE; i++) {
			for (int j = 0; j < Const.BOARDSIZE; j++) {
				Field field = this.gameState.getBoard().getField(i, j);
				int i_ = i;
				int j_ = j;
				if (i_ >= (Const.BOARDSIZE / 2)) {
					i_ = Const.BOARDSIZE - i - 1;
				}
				if (j_ >= (Const.BOARDSIZE / 2)) {
					j_ = Const.BOARDSIZE - j - 1;
				}
				// Eigenen Piranha gefunden
				if (field.getState().toString() == current.toString()) {
					value += i_ + j_;
					// Piranha am Rand = doof
					if (i == 0 || j == 0 || i == Const.BOARDSIZE - 1 || j == Const.BOARDSIZE - 1) {
						value -= 2;
					}
				}
				//
			} // end of for j
		} // end of for i

		BoardRater boardRater = new BoardRater(this.gameState.getBoard());
		value += 2 * (boardRater.getPoints(current) - initBoarderRater.getPoints(current));
		value -= boardRater.getPoints(opponent) - initBoarderRater.getPoints(opponent);
		if (SHOW_BOARDRATER) {
			outPut.add(initBoarderRater.toString(boardRater));
		}

		return value;
	}

	private boolean endOfGame() {
		// TODO Es muss noch abgefragt werden, ob ein Spieler gewonnen hat (max.
		// Schwarmgroesse)
		return (this.gameState.getRound() == Constants.ROUND_LIMIT);
	}

	private void showBoard(Board board) {
		for (int y = Constants.BOARD_SIZE - 1; y >= 0; y--) {
			String outPutStr = "";
			for (int x = 0; x < Constants.BOARD_SIZE; x++) {
				Field f = board.getField(x, y);
				outPutStr += " ";
				switch (f.getState()) {
				case BLUE:
					outPutStr += "B";
					break;
				case RED:
					outPutStr += "R";
					break;
				case OBSTRUCTED:
					outPutStr += "X";
					break;
				default:
					outPutStr += "_";
					break;
				}
			}
			outPut.add(outPutStr);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Player player, Player otherPlayer) {
		currentPlayer = player;
		log.info("Spielerwechsel: " + player.getColor());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(GameState gameState) {
		this.gameState = gameState;
		currentPlayer = gameState.getCurrentPlayer();
		log.info("Zug: {} Spieler: {}", gameState.getTurn(), currentPlayer.getColor());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendAction(Move move) {
		client.sendMove(move);
	}

	private void justOutPutSummary(long timeStart) {
		// Zugzusammenfassung ausgeben
		if (SHOW_SUMMARY) {
			final long timeEnd = System.currentTimeMillis();
			outPut.add("");
			outPut.add("******************************");
			outPut.add("");
			outPut.add("Best Move: " + bestMoveStr);
			outPut.add("Lauftzeit: " + (timeEnd - timeStart) + "ms. Suchtiefe " + DEEP + " Aufrufe " + aufrufe);
		}
		// GESAMTAUSGABE
		if (SHOW) {
			for (String s : outPut) {
				System.out.println(s);
			}
		}
		// Speichern in Textfile
		if (SAVETXT) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(System.getProperty("user.home") + "/Desktop/" + SAVEFILE + ".txt");
				for (String str : outPut) {
					writer.write(str);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (writer != null)
						writer.close();
				} catch (IOException ioe) {
				}
			}
		}
	}

}
