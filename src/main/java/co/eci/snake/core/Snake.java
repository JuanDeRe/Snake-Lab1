package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Snake {
  private final Deque<Position> body = new ArrayDeque<>();
  private volatile Direction direction;
  private int maxLength = 5;
  private boolean paused;
  private final boolean isBot;
  private boolean isDead;
  private long bornTime;
  private long timeAlive;

  private Snake(Position start, Direction dir, boolean bot) {
    bornTime = System.currentTimeMillis();
    body.addFirst(start);
    this.direction = dir;
    this.paused = true;
    this.isBot = bot;
    this.isDead = false;
  }

  public static Snake of(int x, int y, Direction dir, boolean bot) {
    return new Snake(new Position(x, y), dir, bot);
  }

  public synchronized Direction direction() { return direction; }

  public void turn(Direction dir) {
    if (isDead || paused || (direction == Direction.UP && dir == Direction.DOWN) ||
        (direction == Direction.DOWN && dir == Direction.UP) ||
        (direction == Direction.LEFT && dir == Direction.RIGHT) ||
        (direction == Direction.RIGHT && dir == Direction.LEFT)) {
      return;
    }
    this.direction = dir;
  }

  public synchronized Position head() { return body.peekFirst(); }

  public synchronized Deque<Position> snapshot() { return new ArrayDeque<>(body); }

  public synchronized void advance(Position newHead, boolean grow) {
    if (!isDead){
      body.addFirst(newHead);
      if (grow) maxLength++;
      while (body.size() > maxLength) body.removeLast();
    }
  }

  public synchronized void pause(){
    paused = true;
  }

  public synchronized void resume(){
    paused = false;
    notifyAll();
  }

  public boolean isBot() {
    return isBot;
  }

  public synchronized boolean isAlive() {
    return !isDead;
  }

  public long timeAlive(){
    return timeAlive;
  }

  public synchronized void die(){
    this.isDead = true;
    timeAlive = System.currentTimeMillis() - bornTime;
  }
}
