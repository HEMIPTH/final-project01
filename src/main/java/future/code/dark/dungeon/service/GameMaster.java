package future.code.dark.dungeon.service;

import future.code.dark.dungeon.config.Configuration;
import future.code.dark.dungeon.domen.Coin;
import future.code.dark.dungeon.domen.DynamicObject;
import future.code.dark.dungeon.domen.Enemy;
import future.code.dark.dungeon.domen.Exit;
import future.code.dark.dungeon.domen.GameObject;
import future.code.dark.dungeon.domen.Map;
import future.code.dark.dungeon.domen.Player;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static future.code.dark.dungeon.config.Configuration.*;

public class GameMaster {

    private final Image victoryImage;

    private boolean won = false;

    private static GameMaster instance;

    private final Map map;
    private final List<GameObject> gameObjects;

    private final int totalCoins;

    public static synchronized GameMaster getInstance() {
        if (instance == null) {
            instance = new GameMaster();
        }
        return instance;
    }

    private GameMaster() {
        try {
            this.map = new Map(Configuration.MAP_FILE_PATH);
            this.gameObjects = initGameObjects(map.getMap());
            this.totalCoins = getCoins().size();
            this.victoryImage = new ImageIcon(VICTORY_SPRITE).getImage();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void act(int x, int y) {
        Optional<GameObject> currentObject = gameObjects.stream()
                .filter(gameObject -> gameObject.getXPosition() == x && gameObject.getYPosition() == y)
                .findFirst();
        if (currentObject.isPresent()) {
            if (currentObject.get() instanceof Coin) {
                gameObjects.remove(currentObject.get());
            } else if (currentObject.get() instanceof Exit) {
                this.won = true;
            }
        }
    }

    private List<GameObject> initGameObjects(char[][] map) {
        List<GameObject> gameObjects = new ArrayList<>();
        Consumer<GameObject> addGameObject = gameObjects::add;
        Consumer<Enemy> addEnemy = enemy -> {if (ENEMIES_ACTIVE) gameObjects.add(enemy);};

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                switch (map[i][j]) {
                    case EXIT_CHARACTER -> addGameObject.accept(new Exit(j, i));
                    case COIN_CHARACTER -> addGameObject.accept(new Coin(j, i));
                    case ENEMY_CHARACTER -> addEnemy.accept(new Enemy(j, i));
                    case PLAYER_CHARACTER -> addGameObject.accept(new Player(j, i));
                }
            }
        }

        return gameObjects;
    }

    public void renderFrame(Graphics graphics) {
        getMap().render(graphics);
        getStaticObjects().forEach(gameObject -> gameObject.render(graphics));
        getEnemies().forEach(gameObject -> gameObject.render(graphics));
        getPlayer().render(graphics);
        graphics.setColor(Color.WHITE);
        graphics.drawString(getPlayer().toString(), 10, 20);
        int picked = totalCoins - getCoins().size();
        String score = picked + " | " + getCoins().size();
        graphics.drawString(score, 10, 50);
        if (won) {
            graphics.drawImage(victoryImage, 0, 0, null);
        }
    }

    public Player getPlayer() {
        return (Player) gameObjects.stream()
                .filter(gameObject -> gameObject instanceof Player)
                .findFirst()
                .orElseThrow();
    }

    private List<GameObject> getStaticObjects() {
        return gameObjects.stream()
                .filter(gameObject -> !(gameObject instanceof DynamicObject))
                .collect(Collectors.toList());
    }

    private List<Enemy> getEnemies() {
        return gameObjects.stream()
                .filter(gameObject -> gameObject instanceof Enemy)
                .map(gameObject -> (Enemy) gameObject)
                .collect(Collectors.toList());
    }

    private List<Coin> getCoins() {
        return gameObjects.stream()
                .filter(gameObject -> gameObject instanceof Coin)
                .map(gameObject -> (Coin) gameObject)
                .collect(Collectors.toList());
    }


    public Map getMap() {
        return map;
    }

    public boolean canExit() {
        return getCoins().size() == 0;
    }
}
