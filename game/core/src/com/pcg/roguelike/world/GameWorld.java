package com.pcg.roguelike.world;

import box2dLight.RayHandler;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.pcg.roguelike.collision.CollisionSystem;
import com.pcg.roguelike.collision.action.DamageOnCollide;
import com.pcg.roguelike.collision.action.DisappearOnCollide;
import com.pcg.roguelike.entity.components.data.BossComponent;
import com.pcg.roguelike.entity.components.data.DDComponent;
import com.pcg.roguelike.entity.components.data.HealthComponent;
import com.pcg.roguelike.entity.components.data.LifetimeComponent;
import com.pcg.roguelike.entity.components.data.OwnerComponent;
import com.pcg.roguelike.entity.components.physics.BodyComponent;
import com.pcg.roguelike.entity.components.physics.DirectionComponent;
import com.pcg.roguelike.entity.components.physics.DirectionComponent.Direction;
import com.pcg.roguelike.entity.components.dynamic.MovementComponent;
import com.pcg.roguelike.entity.components.player.PlayerComponent;
import com.pcg.roguelike.entity.components.dynamic.ShootingComponent;
import com.pcg.roguelike.entity.components.dynamic.SpeedComponent;
import com.pcg.roguelike.entity.components.visual.SpriteComponent;
import com.pcg.roguelike.entity.components.data.WeaponComponent;
import com.pcg.roguelike.entity.components.dynamic.CollisionActionsComponent;
import com.pcg.roguelike.entity.components.visual.TextComponent;
import com.pcg.roguelike.entity.systems.CleanupSystem;
import com.pcg.roguelike.entity.systems.DirectionSystem;
import com.pcg.roguelike.entity.systems.HealthSystem;
import com.pcg.roguelike.entity.systems.HostilitySystem;
import com.pcg.roguelike.entity.systems.LifetimeSystem;
import com.pcg.roguelike.entity.systems.LightSystem;
import com.pcg.roguelike.entity.systems.MovementSystem;
import com.pcg.roguelike.entity.systems.PhysicsSystem;
import com.pcg.roguelike.entity.systems.PlayerAnimationSystem;
import com.pcg.roguelike.entity.systems.RenderingSystem;
import com.pcg.roguelike.entity.systems.ShootingSystem;
import com.pcg.roguelike.entity.systems.TextRenderingSystem;
import com.pcg.roguelike.item.weapon.EnergyStaff;
import com.pcg.roguelike.item.weapon.StoneSword;
import com.pcg.roguelike.item.weapon.Weapon;
import com.pcg.roguelike.world.generator.BSPGenerator;
import com.pcg.roguelike.world.generator.BSPGenerator.BSPTree;
import com.pcg.roguelike.world.generator.TextureGenerator;
import com.pcg.roguelike.world.placer.BossPlacer;
import com.pcg.roguelike.world.placer.MobPlacer;
import com.pcg.roguelike.world.placer.PlayerPlacer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import java.util.Random;

/**
 * Created by BugDeveloper on 16.11.2016.
 */
public class GameWorld {

    public static final int WIDTH = 60;
    public static final int HEIGHT = 30;
    public static final int TILE_SIZE = 32;
    public static final int TEXTURE_ANTIALIASING = 3;
    public static final int TEXTURE_VARIATIONS = 10;

    public static final float PIXELS_TO_METERS = 2;

    public static final short CATEGORY_PLAYER = 1;
    public static final short CATEGORY_WALL = 1 << 1;
    public static final short CATEGORY_PROJECTILE_PLAYER = 1 << 2;
    public static final short CATEGORY_PROJECTILE_ENEMY = 1 << 3;
    public static final short CATEGORY_ENEMY = 1 << 4;
    public static final short CATEGORY_LIGHT = 1 << 5;
    public static final short CATEGORY_BOSS = 1 << 6;
    public static final short CATEGORY_TEXT = 1 << 7;
    
    public static final short MASK_TEXT = 0;
    public static final short MASK_PLAYER = CATEGORY_WALL | CATEGORY_PROJECTILE_ENEMY | CATEGORY_ENEMY;
    public static final short MASK_MONSTER = CATEGORY_WALL | CATEGORY_PLAYER | CATEGORY_PROJECTILE_PLAYER | CATEGORY_ENEMY;
    public static final short MASK_PROJECTILE_ENEMY = CATEGORY_WALL | CATEGORY_PLAYER;
    public static final short MASK_PROJECTILE_PLAYER = CATEGORY_WALL | CATEGORY_ENEMY | CATEGORY_BOSS;
    public static final short MASK_LIGHT = CATEGORY_WALL | CATEGORY_ENEMY;
    public static final short MASK_BOSS = CATEGORY_WALL | CATEGORY_PLAYER | CATEGORY_PROJECTILE_PLAYER;

    private BSPGenerator gen;
    private TextureGenerator texGen;
    private TiledMap map;
    private Random rand;

    private World world;
    private Engine engine;

    private TiledMapRenderer mapRenderer;
    private Entity player;

    private ComponentMapper<BodyComponent> bm = ComponentMapper.getFor(BodyComponent.class);
    private ComponentMapper<MovementComponent> mm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<SpriteComponent> sc = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ShootingComponent> sm = ComponentMapper.getFor(ShootingComponent.class);
    private final ComponentMapper<HealthComponent> hm = ComponentMapper.getFor(HealthComponent.class);

    private SpriteBatch batch, batchText;
    private OrthographicCamera camera;
    private Box2DDebugRenderer debugRenderer;

    private int playerClass = 0;

    private List<Entity> removedEntities = new LinkedList<>();

    private RayHandler rayHandler;
    private LightSystem lightSystem;

    public static GameWorld instance;
    
    private final float BOSS_RADIUS = 8;
    
    public TiledMap getMap() {
        return map;
    }

    public World getBox2dWorld() {
        return world;
    }

    public GameWorld(int playerClass) {
        instance = this;
        
        this.playerClass = playerClass;

        rand = new Random(System.currentTimeMillis());
        this.debugRenderer = new Box2DDebugRenderer();
        this.world = new World(new Vector2(0, 0), true);
        texGen = new TextureGenerator(TILE_SIZE, TILE_SIZE, rand);
        gen = new BSPGenerator(WIDTH, HEIGHT, 5, 3, rand);

        this.engine = new Engine();
        PhysicsSystem physicsSystem = new PhysicsSystem(this);

        this.engine.addSystem(physicsSystem);
        this.engine.addEntityListener(Family.all(BodyComponent.class).get(), physicsSystem);
        this.engine.addSystem(new MovementSystem());
        this.batch = new SpriteBatch();
        this.batchText = new SpriteBatch();
        this.camera = new OrthographicCamera(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        this.camera.update();

        this.engine.addSystem(new RenderingSystem(this.batch, this.camera));
        this.engine.addSystem(new DirectionSystem());
        this.engine.addSystem(new PlayerAnimationSystem());
        this.engine.addSystem(new ShootingSystem(this));
        this.engine.addSystem(new CleanupSystem(this));
        this.engine.addSystem(new LifetimeSystem());
        this.engine.addSystem(new HostilitySystem(this));
        this.engine.addSystem(new HealthSystem());
        this.engine.addSystem(new TextRenderingSystem(this.batchText, this.camera));
        this.world.setContactListener(new CollisionSystem(this));
    }

    public void create() {
        int[][] mapBlueprint = gen.generateMap();

        /* Tiles textures */
        TextureRegion[] groundTiles = new TextureRegion[TEXTURE_VARIATIONS];
        TextureRegion[] wallTiles = new TextureRegion[TEXTURE_VARIATIONS];

        /* Texture generation */
        for (int i = 0; i < TEXTURE_VARIATIONS; i++) {
            wallTiles[i] = new TextureRegion(texGen.generateTurbulenceTexture(Color.GRAY, Color.BLACK, 3));
            groundTiles[i] = new TextureRegion(texGen.generateTurbulenceTexture(Color.LIGHT_GRAY, Color.GRAY, 5));
            /* Magic method, do not remove */
            // groundTiles[i]   = new TextureRegion(texGen.generateMetalTexture(Color.LIGHT_GRAY,Color.WHITE , 12, 0.012f, 30));

        }

        /* Creating TiledMap and filling it with tiles corresponding to the blueprint */
        TiledMap tiledMap = new TiledMap();
        TiledMapTileLayer layer = new TiledMapTileLayer(WIDTH, HEIGHT, TILE_SIZE, TILE_SIZE);

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();

                switch (mapBlueprint[x][y]) {
                    //wall
                    case 0: {
                        StaticTiledMapTile tile = new StaticTiledMapTile(wallTiles[rand.nextInt(TEXTURE_VARIATIONS)]);
                        tile.setId(0);
                        cell.setTile(tile);
                        createWall(x, y);
                    }
                    break;

                    //ground
                    case 1: {
                        StaticTiledMapTile tile = new StaticTiledMapTile(groundTiles[rand.nextInt(TEXTURE_VARIATIONS)]);
                        tile.setId(1);
                        cell.setTile(tile);
                    }
                    break;
                }
                layer.setCell(x, y, cell);
            }
        }

        tiledMap.getLayers().add(layer);
        map = tiledMap;
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        createPlayer();
        MobPlacer.placeMobs(rand, this, gen.getBspTree(), 1);
        
        
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(.1f);
        rayHandler.useDiffuseLight(true);

        lightSystem = new LightSystem(rayHandler, 20, Color.WHITE, 10, 0, 0);
        lightSystem.setSoftnessLength(0f);
        lightSystem.attachToBody(bm.get(player).body);
    }

    private void createWall(int x, int y) {
        BodyDef bodyDef;
        PolygonShape rectShape;
        FixtureDef fixtureDef;

        //variables for reusing
        bodyDef = new BodyDef();
        rectShape = new PolygonShape();
        rectShape.setAsBox(TILE_SIZE / PIXELS_TO_METERS, TILE_SIZE / PIXELS_TO_METERS);
        fixtureDef = new FixtureDef();

        // wall body definition
        bodyDef.type = BodyDef.BodyType.StaticBody;

        //wall fixture definition
        fixtureDef.shape = rectShape;
        fixtureDef.friction = .2f;
        fixtureDef.restitution = 0;

        fixtureDef.filter.categoryBits = CATEGORY_WALL;

        bodyDef.position.set((x + 0.5f) * TILE_SIZE, (y + 0.5f) * TILE_SIZE);

        Entity wall = new Entity();
        wall.add(new BodyComponent(null, bodyDef, fixtureDef));

        this.engine.addEntity(wall);
    }

    private void createPlayer() {
        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();

        Vector2 playerPos = PlayerPlacer.placePlayer(this.rand, this.gen.getBspTree());
        BossPlacer.placeBoss(this, playerPos, gen.getBspTree());
        
        
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set((playerPos.x + 0.5f) * GameWorld.TILE_SIZE, (playerPos.y + 0.5f) * GameWorld.TILE_SIZE);
        bodyDef.fixedRotation = true;

        PolygonShape rectShape = new PolygonShape();
        rectShape.setAsBox(16 / PIXELS_TO_METERS, 16 / PIXELS_TO_METERS);

        fixtureDef.shape = rectShape;
        fixtureDef.density = 0.01f;
        fixtureDef.friction = 0.25f;
        fixtureDef.filter.categoryBits = CATEGORY_PLAYER;
        fixtureDef.filter.maskBits = MASK_PLAYER;

        this.player = new Entity();
        player.add(new PlayerComponent(this.playerClass));
        player.add(new BodyComponent(null, bodyDef, fixtureDef));

        player.add(new SpriteComponent(null, 0));
        player.add(new MovementComponent(new Vector2(0, 0)));
        player.add(new SpeedComponent(500f));
        player.add(new ShootingComponent(null));
        player.add(new DirectionComponent(Direction.DOWN));
        if (playerClass == 0) {
            player.add(new WeaponComponent(new EnergyStaff()));
           // player.add(new HealthComponent(getPlayerHpTotal()));
            player.add(new HealthComponent(10000));
        } else {
            player.add(new WeaponComponent(new StoneSword()));
            player.add(new HealthComponent(getPlayerHpTotal()));
        }

        engine.addEntity(player);
    }

    private ShapeRenderer sr = new ShapeRenderer();

    public void render(float delta) {
        Body b = bm.get(player).body;
        if (b != null) {
            Vector2 playerPos = b.getPosition();
            camera.position.set(playerPos.x, playerPos.y, 0);
        }

        camera.update();

        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        this.engine.update(delta);
        cleanupEntities();

        rayHandler.setCombinedMatrix(camera.combined.cpy().scl(32));
        rayHandler.updateAndRender();

        //debugRenderer.render(this.world, camera.combined);
    }

    public void addEntity(Entity e) {
        this.engine.addEntity(e);
    }

    public void removeEntity(Entity e) {
        this.removedEntities.add(e);
    }

    public void cleanupEntities() {
        for (Entity e : this.removedEntities) {
            this.engine.removeEntity(e);

            if (bm.has(e)) {
                Body body = bm.get(e).body;

                this.world.destroyBody(body);
            }
        }

        this.removedEntities.clear();
    }

    public void movePlayer(Vector2 movement) {
        if (movement.isZero(1f)) {
            mm.get(player).isMoving = false;
            return;
        }

        mm.get(player).isMoving = true;

        Vector2 m = mm.get(player).movement;
        m.x = movement.x;
        m.y = movement.y;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Vector2 getPlayerPos() {
        BodyComponent bc = bm.get(player);
        return bc.body.getPosition().cpy();
    }

    public void shoot(Vector3 target) {
        ShootingComponent s = sm.get(player);
        s.target = target;
    }

    public void stopShooting() {
        ShootingComponent s = sm.get(player);
        s.target = null;
    }

    public void spawnText(String text, Vector2 position, Color color) {
        Entity e = new Entity();

        e.add(new MovementComponent(new Vector2(0f, 1f), true, true));

        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();

        bodyDef.type = BodyDef.BodyType.KinematicBody;
        bodyDef.position.set(position.x * GameWorld.TILE_SIZE, position.y * GameWorld.TILE_SIZE);
        bodyDef.fixedRotation = true;

        //polygon
        PolygonShape rectShape = new PolygonShape();
        rectShape.setAsBox(0f, 0f);

        //fixture
        fixtureDef.shape = rectShape;
        fixtureDef.density = 0.01f;
        fixtureDef.friction = 0.25f;
        fixtureDef.restitution = 1.0f;
        
        fixtureDef.filter.categoryBits = GameWorld.CATEGORY_TEXT;
        fixtureDef.filter.maskBits = GameWorld.MASK_TEXT;

        fixtureDef.isSensor = true;

        e.add(new BodyComponent(null, bodyDef, fixtureDef));
        e.add(new SpeedComponent(25));
        e.add(new LifetimeComponent(35));

        e.add(new TextComponent(text, color));

        this.engine.addEntity(e);
    }
    
    public int getPlayerHp() {
        return hm.get(player).hp;
    }

    public int getPlayerHpTotal() {
        return (this.playerClass == 0) ? 100 : 300;
    }
    
    private Entity getBossEntity() {
        ImmutableArray<Entity> entities = this.engine.getEntitiesFor(Family.one(BossComponent.class).get());
        
        if (entities.size() != 0) {
            return entities.get(0);
        }
        
        return null;
    }
    
    public boolean isBossNearby() {
        Vector2 playerPos = this.getPlayerPos();
        Entity boss = getBossEntity();
        if (boss == null)
            return false;
        
        Body b = bm.get(boss).body;
        if (b == null)
            return false;
        
        Vector2 bossPos = b.getPosition();
        
        float dist = playerPos.dst(bossPos);
        return dist <= BOSS_RADIUS * GameWorld.TILE_SIZE;
    }
    
    public int getBossHp() {
        Entity bossEntity = getBossEntity();
        if (bossEntity == null)
            return -1;
        
        return hm.get(bossEntity).hp;
    }
    
    public int getBossHpTotal() {
        return 900;
    }
}
