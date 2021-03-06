package com.pcg.roguelike.world.generator;

/**
 *
 * @author Senya
 */
import java.util.Random;

public class BSPGenerator {

    public int[][] generateMap() {
        bspTree = new BSPTree(0, 0, map.length, map[0].length);
        generateMap(bspTree);
        drawCorridorsFromTop(bspTree);
        createBorders();
        return map;
    }

    private enum Orientation {
        Horizontal, Vertical
    };

    private int height, width, minimalRoomSize, differenceDivider, minSpace = 1;

    public BSPTree getBspTree() {
        return bspTree;
    }

    private BSPTree bspTree;
    private Random rnd;
    private int[][] map;

    public BSPGenerator(int width, int height, int minimalRoomSize, int differenceDivider, Random rnd) {
        this.width = width;
        this.height = height;
        this.minimalRoomSize = minimalRoomSize;
        this.differenceDivider = differenceDivider;
        this.rnd = rnd;
        map = new int[width][height];
    }

    private void createBorders() {
        for (int i = 0; i < map.length; i++) {
            map[i][0] = 0;
        }

        for (int i = 0; i < map.length; i++) {
            map[i][map[0].length - 1] = 0;
        }

        for (int i = 0; i < map[0].length; i++) {
            map[0][i] = 0;
        }

        for (int i = 0; i < map[0].length; i++) {
            map[map.length - 1][i] = 0;
        }

    }

    private void generateRoom(BSPTree node) {
        int x = node.getStartX() + minSpace + rnd.nextInt(node.getWidth() / minimalRoomSize - minSpace + 1);
        int y = node.getStartY() + minSpace + rnd.nextInt(node.getHeight() / minimalRoomSize - minSpace + 1);

        int locwidth = node.getWidth() - (x - node.getStartX());
        int locheight = node.getHeight() - (y - node.getStartY());

        locwidth -= rnd.nextInt(locwidth / differenceDivider);
        locheight -= rnd.nextInt(locheight / differenceDivider);
        roomToArray(x, y, locwidth, locheight);
    }

    private void generateMap(BSPTree node) {
        if (node == null) {
            return;
        }

        if (node.getLeftChild() == null && node.getRightChild() == null) {
            generateRoom(node);
        } else {
            generateMap(node.getLeftChild());
            generateMap(node.getRightChild());
        }
    }

    private void roomToArray(int x, int y, int w, int h) {
        for (int i = x; i < w + x; i++) {
            for (int j = y; j < h + y; j++) {
                map[i][j] = 1;
            }
        }
    }

    private void drawCorridorsFromTop(BSPTree node) {
        if (node.getRightChild() == null || node.getLeftChild() == null) {
            return;
        }

        int startX = (node.getLeftChild().getEndX() + node.getLeftChild().getStartX()) / 2;

        int startY = (node.getLeftChild().getEndY() + node.getLeftChild().getStartY()) / 2;

        int endX = (node.getRightChild().getEndX() + node.getRightChild().getStartX()) / 2;

        int endY = (node.getRightChild().getEndY() + node.getRightChild().getStartY()) / 2;

        int temp;

        if (startX == endX) {
            if (startY > endY) {
                temp = endY;
                endY = startY;
                startY = temp;
            }

            for (int i = startY; i < endY; i++) {
                map[startX][i] = 1;
            }
        } else if (startY == endY) {
            if (startX > endX) {
                temp = endX;
                endX = startX;
                startX = temp;
            }

            for (int i = startX; i < endX; i++) {
                map[i][startY] = 1;
            }
        }

        drawCorridorsFromTop(node.getLeftChild());
        drawCorridorsFromTop(node.getRightChild());
    }

    public class BSPTree {

        private int startX;
        private int startY;
        private int endX;
        private int endY;
        private int width;

        private boolean spawnRoom;
        private boolean bossRoom;
        
        public int getStartX() {
            return startX;
        }

        public int getStartY() {
            return startY;
        }

        public int getEndX() {
            return endX;
        }

        public int getEndY() {
            return endY;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public BSPTree getLeftChild() {
            return leftChild;
        }

        public BSPTree getRightChild() {
            return rightChild;
        }

        public boolean isSpawnRoom() {
            return spawnRoom;
        }

        public void setSpawnRoom(boolean spawnRoom) {
            this.spawnRoom = spawnRoom;
        }

        public boolean isBossRoom() {
            return bossRoom;
        }

        public void setBossRoom(boolean bossRoom) {
            this.bossRoom = bossRoom;
        }

        
        
        private int height;
        private BSPTree leftChild, rightChild;
        private float maxPartitionSizeRatio = 1f;

        public BSPTree(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            width = Math.abs(endX - startX);
            height = Math.abs(endY - startY);

            if (shouldSplit(this)) {
                partition(this);
            }
        }

        private boolean shouldSplit(BSPTree node) {
            if (node.getWidth() >= minimalRoomSize * 2 && node.getHeight() >= minimalRoomSize * 2) {
                return true;
            }
            return false;
        }

        private void partition(BSPTree node) {
            Orientation splitOrient;
            int splitLocation;

            //Debug.Log("Start X: " + node.StartX + ", Start Y: " + node.StartY + ", Width: " + node.Width + ", Height: " + node.Height + ".");
            if (node.getWidth() / node.getHeight() > maxPartitionSizeRatio) {
                splitOrient = Orientation.Vertical;
            } else if (node.getHeight() / node.getWidth() > maxPartitionSizeRatio) {
                splitOrient = Orientation.Horizontal;
            } else {
                splitOrient = (rnd.nextInt(2) == 1) ? Orientation.Horizontal : Orientation.Vertical;
            }

            if (splitOrient == Orientation.Horizontal) {

                splitLocation = node.getStartY() + minimalRoomSize + rnd.nextInt(node.getEndY() - 2 * minimalRoomSize - node.getStartY() + 1);

                node.leftChild = new BSPTree(node.getStartX(), node.getStartY(), node.getEndX(), splitLocation);
                node.rightChild = new BSPTree(node.getStartX(), splitLocation, node.getEndX(), node.getEndY());

            } else {
                splitLocation = node.getStartX() + minimalRoomSize + rnd.nextInt(node.getEndX() - 2 * minimalRoomSize - node.getStartX() + 1);

                node.leftChild = new BSPTree(node.getStartX(), node.getStartY(), splitLocation, node.getEndY());
                node.rightChild = new BSPTree(splitLocation, node.getStartY(), node.getEndX(), node.getEndY());
            }

        }

        @Override
        public String toString() {
            return "BSPTree{" + "sX=" + startX + ", sY=" + startY + ", w=" + width + ", h=" + height + '}';
        }
        
        
    }
}
