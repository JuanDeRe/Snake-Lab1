package co.eci.snake.concurrency;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Snake;
import co.eci.snake.core.engine.PauseController;

import java.util.concurrent.ThreadLocalRandom;

public final class SnakeRunner implements Runnable {
  private final Snake snake;
  private final Board board;
  private final int baseSleepMs = 80;
  private final int turboSleepMs = 40;
  private int turboTicks = 0;
  private final PauseController pauseController;

  public SnakeRunner(Snake snake, Board board, PauseController pauseController) {
    this.snake = snake;
    this.board = board;
    this.pauseController = pauseController;
  }

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted() || snake.isAlive()) {
        checkPaused();
        maybeTurn();
        var res = board.step(snake);
        if (res == Board.MoveResult.HIT_SNAKE){
          kill();
          continue;
        } else if (res == Board.MoveResult.HIT_OBSTACLE) {
          randomTurn();
        } else if (res == Board.MoveResult.ATE_TURBO) {
          turboTicks = 100;
        }
        int sleep = (turboTicks > 0) ? turboSleepMs : baseSleepMs;
        if (turboTicks > 0) turboTicks--;
        Thread.sleep(sleep);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private void kill() {
    snake.die();
  }

  private void checkPaused() {
      try {
          pauseController.waitIfPaused();
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      }
  }

  private void maybeTurn() {
    if (snake.isBot()){
      double p = (turboTicks > 0) ? 0.05 : 0.10;
      if (ThreadLocalRandom.current().nextDouble() < p) randomTurn();
    }
  }

  private void randomTurn() {
    var dirs = Direction.values();
    snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
  }
}
