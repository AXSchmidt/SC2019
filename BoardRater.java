package sc.player2019.logic;

import sc.plugin2019.Board;
import sc.plugin2019.util.GameRuleLogic;
import sc.shared.PlayerColor;

public class BoardRater {

	private int redFishesAtBorder = 0;
	private int blueFishesAtBorder = 0;
	private int redSwarmSize;
	private int blueSwarmSize;

	public BoardRater(Board board) {
		// Init SwarmSize
		redSwarmSize = GameRuleLogic.greatestSwarmSize(board, PlayerColor.RED);
		blueSwarmSize = GameRuleLogic.greatestSwarmSize(board, PlayerColor.BLUE);
		// Init FishesAtBorder
		for (int i = 0; i < Const.BOARDSIZE; i++) {
			if (board.getField(i, 0).getState().toString() == "RED") 
				redFishesAtBorder++;
			if (board.getField(i, Const.BOARDSIZE - 1).getState().toString() == "RED") 
				redFishesAtBorder++;
			if (board.getField(i, 0).getState().toString() == "BLUE") 
				blueFishesAtBorder++;
			if (board.getField(i, Const.BOARDSIZE - 1).getState().toString() == "BLUE") 
				blueFishesAtBorder++;
			if ((i > 0) && (i < Const.BOARDSIZE - 1)) {
				if (board.getField(0, i).getState().toString() == "RED") 
					redFishesAtBorder++;
				if (board.getField(Const.BOARDSIZE - 1, i).getState().toString() == "RED") 
					redFishesAtBorder++;				
				if (board.getField(0, i).getState().toString() == "BLUE") 
					blueFishesAtBorder++;
				if (board.getField(Const.BOARDSIZE - 1, i).getState().toString() == "BLUE") 
					blueFishesAtBorder++;				
			}
		}
	}

	public int getPoints(PlayerColor color) {
		if (color == PlayerColor.RED) {
			return (3 * redSwarmSize) - redFishesAtBorder;
		} else {
			return (3 * blueSwarmSize) - blueFishesAtBorder;
		}
	}

	public String toString(BoardRater sndRater) {
		String result = "BoardRating:\n";
		result += "RED  FishesAtBorder: (" + this.redFishesAtBorder + "/" + sndRater.redFishesAtBorder + ")\n";
		result += "BLUE FishesAtBorder: (" + this.blueFishesAtBorder + "/" + sndRater.blueFishesAtBorder + ")\n";
		result += "RED  SwarmSize:      (" + this.redSwarmSize + "/" + sndRater.redSwarmSize + ")\n";
		result += "BLUE SwarmSize:      (" + this.blueSwarmSize + "/" + sndRater.blueSwarmSize + ")";
		return result;
	}

}
