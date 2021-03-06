package com.pcg.roguelike.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;

public class GameWin implements Screen {

    private Stage stage;
    private TextureAtlas atlas;
    private Skin skin;
    private Table table;
    private TextButton buttonExit;
    private ImageButton buttonWin;
    
    private BitmapFont black;
    private Label heading;


    @Override
    public void show() {
        stage = new Stage();

        Gdx.input.setInputProcessor(stage);

        Texture tex = new Texture(Gdx.files.internal("win.png"));
        TextureRegion[][] playerTextures = TextureRegion.split(tex, 240, 240);        
        Sprite sprite = new Sprite(playerTextures[0][0]);
        sprite.setSize(240, 240);        
        
        ImageButton.ImageButtonStyle buttonWarriorStyle = new ImageButton.ImageButtonStyle();
        buttonWarriorStyle.imageUp = new SpriteDrawable(sprite);
        buttonWarriorStyle.pressedOffsetX = 1;
        buttonWarriorStyle.pressedOffsetY = -1;
        
        //button warrior
        buttonWin = new ImageButton(buttonWarriorStyle);
        buttonWin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ((Game) Gdx.app.getApplicationListener()).setScreen(new Play(1));
            }
        });             
        
        atlas = new TextureAtlas("ui/button.pack");
        skin = new Skin(atlas);

        table = new Table(skin);
        table.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // creating fonts
        black = new BitmapFont(Gdx.files.internal("font/black.fnt"), false);

        // creating buttons
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.getDrawable("button.up");
        textButtonStyle.pressedOffsetX = 1;
        textButtonStyle.pressedOffsetY = -1;
        textButtonStyle.font = black;


        // button exit
        buttonExit = new TextButton("Exit", textButtonStyle);
        buttonExit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // creating heading
        Label.LabelStyle headingStyle = new Label.LabelStyle(black, Color.WHITE);

        heading = new Label("You Win", headingStyle);
        heading.setFontScale(3);

        // putting stuff together
        table.add(heading);
        table.getCell(heading).spaceBottom(150);
        table.row();

        table.add(buttonWin).spaceBottom(25);
        table.row();
        
        table.add(buttonExit);
        table.getCell(buttonExit).spaceBottom(60);
        stage.addActor(table);
    }

    public void fontgenerator() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {

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

    @Override
    public void dispose() {

    }
}
