package com.geekbrains.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class GameScreen implements Screen {
    private enum Status {
        PLAY, PAUSE, SHOP;
    }

    private SpriteBatch batch;
    private Map map;
    private Bomberman player;
    private BotEmitter botEmitter;
    private AnimationEmitter animationEmitter;
    private BombEmitter bombEmitter;
    private BitmapFont guiFont, pauseFont;
    private Camera camera;
    private Stage stage;
    private Stage stagePause;
    private Skin skin;
    private Group upgradeGroup, pauseGroupe;
    private Status currentStatus;
    private int lvl = 1;
    private int maxLvl = 3;



    public GameScreen(SpriteBatch batch, Camera camera) {
        this.batch = batch;
        this.camera = camera;
    }

    public Map getMap() {
        return map;
    }

    public BombEmitter getBombEmitter() {
        return bombEmitter;
    }

    public AnimationEmitter getAnimationEmitter() {
        return animationEmitter;
    }

    public Bomberman getPlayer() {
        return player;
    }

    public BotEmitter getBotEmitter() {
        return botEmitter;
    }

    @Override
    public void show() {
        guiFont = Assets.getInstance().getAssetManager().get("gomarice48.ttf", BitmapFont.class);
        pauseFont = Assets.getInstance().getAssetManager().get("gomarice62.ttf", BitmapFont.class);
        createGUI();
        createPause();
        map = new Map();
        animationEmitter = new AnimationEmitter();
        bombEmitter = new BombEmitter(this);
        botEmitter = new BotEmitter(this);
        player = new Bomberman(this);
        startLevel(lvl);
    }

    public void saveGame() {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(Gdx.files.local("save.dat").write(false));
            out.writeObject(botEmitter);
            out.writeObject(bombEmitter);
            out.writeObject(map);
            out.writeObject(player);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadGame() {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(Gdx.files.local("save.dat").read());
            botEmitter = (BotEmitter) in.readObject();
            bombEmitter = (BombEmitter) in.readObject();
            map = (Map) in.readObject();
            player = (Bomberman) in.readObject();
            botEmitter.reloadResources(this);
            bombEmitter.reloadResources(this);
            map.reloadResources();
            player.reloadResources(this);
            animationEmitter.reset();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void render(float delta) {
        update(delta);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        map.render(batch);
        player.render(batch);
        botEmitter.render(batch);
        bombEmitter.render(batch);
        animationEmitter.render(batch);
        ScreenManager.getInstance().resetCamera();
        player.renderGUI(batch, guiFont);
        batch.end();
        stage.draw();
        stagePause.draw();
    }

    public void startLevel(int level) {
        animationEmitter.reset();
        botEmitter.reset();
        bombEmitter.reset();
        currentStatus = Status.PLAY;
        map.loadMap(level);
        player.startNewLevel();
    }

    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            saveGame();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            loadGame();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            currentStatus = Status.PAUSE;

        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            currentStatus = Status.PLAY;

        }
        cameraUpdate();
        if (currentStatus == Status.PLAY) {
            player.update(dt);
            if (map.checkCellForBonus(player.getCellX(), player.getCellY())) {
                map.clearCell(player.getCellX(), player.getCellY());
                currentStatus = Status.SHOP;
            }
            botEmitter.update(dt);
            bombEmitter.update(dt);
            animationEmitter.update(dt);
            if (map.checkCellForKey(player.getCellX(), player.getCellY())) {
                lvl++;
                if (lvl<=maxLvl){
                startLevel(lvl);
                }else {startLevel(1); lvl=1;
//                    ScreenManager.getInstance().changeScreen(ScreenManager.ScreenType.GAME_OVER);
                }
            }
        }
        upgradeShop(dt);
        upgradePause(dt);
        stage.act(dt);
        stagePause.act(dt);
    }
    public void upgradePause(float dt) {
        if (currentStatus == Status.PAUSE && pauseGroupe.getY() < 100.0f) {
            pauseGroupe.setY(pauseGroupe.getY() + 1000.0f * dt);
        }
        if (currentStatus == Status.PLAY && pauseGroupe.getY() > -800.0f) {
            pauseGroupe.setY(pauseGroupe.getY() - 1000.0f * dt);
        }
    }

    public void upgradeShop(float dt) {
        if (currentStatus == Status.SHOP && upgradeGroup.getY() > 300.0f) {
            upgradeGroup.setY(upgradeGroup.getY() - 600.0f * dt);
        }
        if (currentStatus == Status.PLAY && upgradeGroup.getY() < 800.0f) {
            upgradeGroup.setY(upgradeGroup.getY() + 600.0f * dt);
        }
    }

    public void cameraUpdate() {
        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        if (camera.position.x < Rules.WORLD_WIDTH / 2) {
            camera.position.x = Rules.WORLD_WIDTH / 2;
        }
        if (camera.position.y < Rules.WORLD_HEIGHT / 2) {
            camera.position.y = Rules.WORLD_HEIGHT / 2;
        }
        if (camera.position.x > map.getMapX() * Rules.CELL_SIZE - Rules.WORLD_WIDTH / 2) {
            camera.position.x = map.getMapX() * Rules.CELL_SIZE - Rules.WORLD_WIDTH / 2;
        }
        if (camera.position.y > map.getMapY() * Rules.CELL_SIZE - Rules.WORLD_HEIGHT / 2) {
            camera.position.y = map.getMapY() * Rules.CELL_SIZE - Rules.WORLD_HEIGHT / 2;
        }
        camera.update();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        ScreenManager.getInstance().resize(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }
    public void createPause() {
        stagePause = new Stage(ScreenManager.getInstance().getViewport(), batch);
        Gdx.input.setInputProcessor(stage);
        pauseGroupe = new Group();
        pauseGroupe.setPosition(Rules.WORLD_WIDTH / 2-400, -800);
        skin = new Skin();
        skin.addRegions(Assets.getInstance().getAtlas());
        skin.add("titleFont", pauseFont);
        TextButton.TextButtonStyle textTitleStyle = new TextButton.TextButtonStyle();
        textTitleStyle.font = pauseFont;
        skin.add("pauseFont", guiFont);
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.getDrawable("simpleButton");
        textButtonStyle.font = guiFont;
        skin.add("simpleSkin", textButtonStyle);
        Image image = new Image(new Texture("pause.png"));
        Button pause = new TextButton("Pause", textTitleStyle);
        Button btnNewGame = new TextButton("New Game", skin, "simpleSkin");
        Button btnExitGame = new TextButton("Exit Game", skin, "simpleSkin");
        Button btnContinue = new TextButton("Continue", skin, "simpleSkin");
        Button btnSaveGame = new TextButton("Save game", skin, "simpleSkin");
        Button btnLoadGame = new TextButton("Load game", skin, "simpleSkin");
        pause.setPosition(340,400);
        btnContinue.setPosition(240, 50);
        btnNewGame.setPosition(70, 250);
        btnExitGame.setPosition(70, 150);
        btnSaveGame.setPosition(410, 250);
        btnLoadGame.setPosition(410, 150);
        pauseGroupe.addActor(image);
        pauseGroupe.addActor(pause);
        pauseGroupe.addActor(btnNewGame);
        pauseGroupe.addActor(btnExitGame);
        pauseGroupe.addActor(btnContinue);
        pauseGroupe.addActor(btnSaveGame);
        pauseGroupe.addActor(btnLoadGame);
        btnNewGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startLevel(1);
            }
        });
        btnExitGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        btnContinue.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentStatus = Status.PLAY;
                }
                });
        btnSaveGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveGame();
            }
        });
        btnLoadGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadGame();
            }
        });
        stagePause.addActor(pauseGroupe);
    }


    public void createGUI() {
        stage = new Stage(ScreenManager.getInstance().getViewport(), batch);
        Gdx.input.setInputProcessor(stage);
        skin = new Skin();
        skin.addRegions(Assets.getInstance().getAtlas());

        upgradeGroup = new Group();
        upgradeGroup.setPosition(Rules.WORLD_WIDTH / 2 - 200, 800);
        Image image = new Image(skin.getDrawable("upgPanel"));
        Button upgHp = new Button(skin.getDrawable("upgHp"));
        Button upgBombRadius = new Button(skin.getDrawable("upgRad"));
        Button upgScore = new Button(skin.getDrawable("upgScore"));
        Button upgLife = new Button(skin.getDrawable("upgLife"));
        upgHp.setPosition(0, 0);
        upgBombRadius.setPosition(100, 0);
        upgScore.setPosition(200, 0);
        upgLife.setPosition(300, 0);
        upgradeGroup.addActor(image);
        upgradeGroup.addActor(upgHp);
        upgradeGroup.addActor(upgBombRadius);
        upgradeGroup.addActor(upgScore);
        upgradeGroup.addActor(upgLife);
        upgHp.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                player.upgrade(Bomberman.UpgradeType.HP);
                currentStatus = Status.PLAY;
            }
        });
        upgBombRadius.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                player.upgrade(Bomberman.UpgradeType.BOMBRADIUS);
                currentStatus = Status.PLAY;
            }
        });
        upgScore.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                player.upgrade(Bomberman.UpgradeType.MONEY);
                currentStatus = Status.PLAY;
            }
        });
        upgLife.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                player.upgrade(Bomberman.UpgradeType.LIFE);
                currentStatus = Status.PLAY;
            }
        });
        stage.addActor(upgradeGroup);

        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            final Group arrowGroup = new Group();
            Button buttonLeft = new Button(skin.getDrawable("leftArrow"));
            Button buttonRight = new Button(skin.getDrawable("rightArrow"));
            Button buttonUp = new Button(skin.getDrawable("upArrow"));
            Button buttonDown = new Button(skin.getDrawable("downArrow"));
            buttonLeft.setPosition(10, 10);
            buttonUp.setPosition(120, 120);
            buttonRight.setPosition(230, 10);
            buttonDown.setPosition(120, 10);
            arrowGroup.setPosition(50, 50);
            Button[] buttons = new Button[]{buttonLeft, buttonRight, buttonUp, buttonDown};
            final char[] chars = new char[]{'L', 'R', 'U', 'D'};
            for (int i = 0; i < buttons.length; i++) {
                final int innerI = i;
                arrowGroup.addActor(buttons[i]);
                buttons[i].addListener(new ClickListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        player.setPrefferedDirection(chars[innerI]);
                        return true;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                        player.setPrefferedDirection('-');
                    }
                });
            }
            Button buttonBomb = new Button(skin.getDrawable("bombButton"));
            buttonBomb.setPosition(1080, 10);
            buttonBomb.addListener(new ClickListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    player.setupBomb();
                    return true;
                }
            });
            arrowGroup.addActor(buttonBomb);
            // arrowGroup.setColor(1,1,1,1f);
            stage.addActor(arrowGroup);
        }
    }
}
