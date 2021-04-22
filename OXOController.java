import java.util.regex.Matcher;
import java.util.regex.Pattern;

class OXOController
{
    private final OXOModel model;
    private int currentPlayerIndex = 0;
    private int occupiedCells = 0;

    public OXOController(OXOModel model)
    {
        this.model = model;
        //initialising the current player to the the player with index 0
        model.setCurrentPlayer(model.getPlayerByNumber(currentPlayerIndex));
    }

    public void handleIncomingCommand(String commandString) throws InvalidCellIdentifierException, CellAlreadyTakenException, CellDoesNotExistException
    {
        //if there is a winner or the game is drawn, will return in order to ignore incoming commands
        if(model.getWinner() != null || model.isGameDrawn())
        {
            return;
        }
        //getting the position from the command
        Position position = new Position(commandString);
        //checking whether the position is a valid move
        if(!isValidPosition(position))
        {
            throw new CellDoesNotExistException(position.getRow(), position.getCol());
        }
        //setting the cell of the position to the current player
        setCell(position, model.getCurrentPlayer());
        //keeping count of the number of cells taken
        occupiedCells++;
        //updateGameStatus checks whether the game is drawn or won and sets the winner
        updateGameStatus(model.getCurrentPlayer(), position);
        setNextPlayer();
    }

    private void updateGameStatus(OXOPlayer player, Position position)
    {
        //checking whether the position is a winning move for the player and setting the winner
        if(isWinner(player, position))
        {
            model.setWinner(player);
        }
        //Game is drawn if there is no winner and all cells have been occupied
        else if(occupiedCells == (model.getNumberOfRows() * model.getNumberOfColumns()))
        {
            model.setGameDrawn();
        }
    }

    private void setCell(Position position, OXOPlayer player) throws CellAlreadyTakenException
    {   //checks whether the cell hasn't already been taken and sets the cellOwner or throws an exception
        if(model.getCellOwner(position.getRow(), position.getCol()) == null)
        {
            model.setCellOwner(position.getRow(), position.getCol(), player);
        }
        else
        {
            throw new CellAlreadyTakenException(position.getRow(), position.getCol());
        }
    }

    private boolean isWinner(OXOPlayer player, Position position)
    {
        //checks in row, column and both diagonals whether a player has won on that line
        //a line can be a row, a column or a diagonal
        return playerWonOnLine(player, position, Line.HORIZONTAL) ||
               playerWonOnLine(player, position, Line.VERTICAL) ||
               playerWonOnLine(player, position, Line.LEFT_DIAGONAL) ||
               playerWonOnLine(player, position, Line.RIGHT_DIAGONAL);
    }

    private boolean playerWonOnLine(OXOPlayer player, Position position, Line line)
    {
        // adding the count of consecutive occupied cells for each direction of a line
        // adding 1 for the current position as well
        // returns true if the count is bigger or equal to the winning threshold
        int countOnLine = countInDirection(player, position, line.getForward()) + countInDirection(player, position, line.getBackwards()) + 1;
        return countOnLine >= model.getWinThreshold();
    }

    //direction can't be 0,0
    private int countInDirection(OXOPlayer player, Position position, Direction direction)
    {
        //returns the count of consecutive occupied cells int the given direction for the given player starting from position
        //does not add the position to the count
        int row = position.getRow();
        int col = position.getCol();
        int count = 0;

        //checks whether the position into the direction is valid and whether the cell is owned by the player. If so, it increments the count by 1
        while(nextCellIsOwnedBySamePlayer(row + direction.getRowDirection(), col + direction.getColumnDirection(), player))
        {
            count++;
            row += direction.getRowDirection();
            col += direction.getColumnDirection();
        }

        return count;
    }

    private boolean nextCellIsOwnedBySamePlayer(int row, int col, OXOPlayer player)
    {
        return isValidPosition(row, col) &&
                model.getCellOwner(row, col) == player;
    }

    private void setNextPlayer()
    {
        //getting the next player by increasing i or resetting i to 0 after every player hat a turn by taking the modulus
        currentPlayerIndex = (currentPlayerIndex+1)%model.getNumberOfPlayers();
        model.setCurrentPlayer(model.getPlayerByNumber(currentPlayerIndex));
    }

    //overloading the isValidPosition method to except a position as well as row,col coordinates
    private boolean isValidPosition(Position position)
    {
        return isValidPosition(position.getRow(), position.getCol());
    }

    private boolean isValidPosition(int row, int col)
    {   //checking whether the position is on the board
        return row < model.getNumberOfRows() &&
               col < model.getNumberOfColumns() &&
               row >= 0 &&
               col >= 0;
    }

    // creates a Line Enum that takes a direction
    private enum Line {
        HORIZONTAL(new Direction(0,1)),
        VERTICAL(new Direction(1, 0)),
        LEFT_DIAGONAL(new Direction(1,1)),
        RIGHT_DIAGONAL(new Direction(-1,1));

        private final Direction forward;

        Line(Direction forward) {
            this.forward = forward;
        }

        public Direction getForward()
        {
            return forward;
        }

        public Direction getBackwards()
        {
            return forward.negate();
        }
    }
    // class for 2d direction objects
    private static class Direction
    {
        private final int rowDirection;
        private final int columnDirection;

        Direction(int rowDirection, int columnDirection)
        {
            this.rowDirection = rowDirection;
            this.columnDirection = columnDirection;
        }

        public int getRowDirection() {
            return rowDirection;
        }

        public int getColumnDirection() {
            return columnDirection;
        }

        public Direction negate()
        {
            return new Direction(-rowDirection, -columnDirection);
        }
    }

    // position class takes a string that is only valid if it fits the regex pattern
    private static class Position
    {
        private final int row;
        private final int col;

        private final Pattern commandPattern = Pattern.compile("(\\p{Alpha})(\\d)");
        private final static String alphabet = "abcdefghijklmnopqrstuvwxyz";

        Position(String commandString) throws InvalidCellIdentifierException
        {
            Matcher match = commandPattern.matcher(commandString);
            if (!match.matches())
            {
                throw new InvalidCellIdentifierException("<Invalid command format>", commandString);
            }

            char rowChar = match.group(1).toLowerCase().charAt(0);
            int row = alphabet.indexOf(rowChar);

            int col = Integer.parseInt(match.group(2)) - 1;

            this.row = row;
            this.col = col;
        }

        public int getRow()
        {
            return row;
        }

        public int getCol()
        {
            return col;
        }
    }
}
