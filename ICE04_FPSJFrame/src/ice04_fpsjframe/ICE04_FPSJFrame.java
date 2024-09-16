
package ice04_fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import javax.sound.sampled.*;
import java.io.File;

public class ICE04_FPSJFrame extends JPanel implements KeyListener, Runnable{

    private final int nScreenWidth = 800;
    private final int nScreenHeight = 600;
    private final int nMapWidth = 16;
    private final int nMapHeight = 16;
    private final float fFOV = (float) (Math.PI / 4.0);
    private final float fDepth = 16.0f;
    private final float fSpeed = 5.0f;

    private float fPlayerX = 1.5f; // Starting position
    private float fPlayerY = 1.5f;
    private float fPlayerA = 0.0f;

    private boolean[] keys = new boolean[4]; // W, A, S, D

    private enum GameState { STARTUP, IN_GAME, CONGRATS }
    private GameState gameState = GameState.STARTUP;

    // Levels and current level
    private String[] levels;
    private int currentLevel = 0;

    private final float fEndX = 14.5f; // Ending position
    private final float fEndY = 14.5f;

    private int playerHealth = 100;
    private long startTime;

    public ICE04_FPSJFrame() {
        
        setPreferredSize(new Dimension(nScreenWidth, nScreenHeight));
        setFocusable(true);
        addKeyListener(this);

        // Define multiple levels
        levels = new String[]{
            // Level 1
            "S.......#......."+
            "#..............."+
            "#.......########"+
            "#..............#"+
            "#......##......#"+
            "#......##......#"+
            "#..............#"+
            "###............#"+
            "##.............#"+
            "#......####..###"+
            "#......#.......#"+
            "#......#.......#"+
            "#..............#"+
            "#......#########"+
            "#..............E"+
            "################",

            // Level 2
            "S#######...#...."+
            "#..............."+
            "#...#######....."+
            "#...#....#......"+
            "#...#.###.#....."+
            "#..............."+
            "#....###........"+
            "#...###..E..####"+
            "################"
        };
    }

    @Override
    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);

        if (gameState == GameState.STARTUP) {
            drawStartupScreen(g);
        } else if (gameState == GameState.IN_GAME) {
            drawGame(g);
        } else if (gameState == GameState.CONGRATS) {
            drawCongratsScreen(g);
        }
    }

    private void drawStartupScreen(Graphics g) {
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, nScreenWidth, nScreenHeight);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("FPS Game", nScreenWidth / 2 - 100, nScreenHeight / 2 - 50);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press ENTER to Start", nScreenWidth / 2 - 130, nScreenHeight / 2);
    }

    private void drawCongratsScreen(Graphics g) {
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, nScreenWidth, nScreenHeight);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("Congratulations!", nScreenWidth / 2 - 150, nScreenHeight / 2 - 50);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("You reached the end!", nScreenWidth / 2 - 120, nScreenHeight / 2);
        g.drawString("Press ENTER to continue", nScreenWidth / 2 - 150, nScreenHeight / 2 + 50);
    }

    private void drawGame(Graphics g) {
        
        // Clear screen
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nScreenWidth, nScreenHeight);

        // Draw the map and player arrow
        drawMap(g);
        drawPlayerArrow(g, fPlayerX, fPlayerY, fPlayerA);

        // Perform raycasting for 3D effect
        for (int x = 0; x < nScreenWidth; x++) {
            
            float fRayAngle = (fPlayerA - fFOV / 2.0f) + ((float) x / nScreenWidth) * fFOV;
            float fStepSize = 0.1f;
            float fDistanceToWall = 0.0f;
            boolean bHitWall = false;
            float fEyeX = (float) Math.sin(fRayAngle);
            float fEyeY = (float) Math.cos(fRayAngle);

            while (!bHitWall && fDistanceToWall < fDepth) {
                
                fDistanceToWall += fStepSize;
                int nTestX = (int) (fPlayerX + fEyeX * fDistanceToWall);
                int nTestY = (int) (fPlayerY + fEyeY * fDistanceToWall);

                if (nTestX < 0 || nTestX >= nMapWidth || nTestY < 0 || nTestY >= nMapHeight) {
                    bHitWall = true;
                    fDistanceToWall = fDepth;
                } else {
                    if (levels[currentLevel].charAt(nTestY * nMapWidth + nTestX) == '#') {
                        bHitWall = true;
                    }
                }
            }

            // Calculate column height and draw
            int nCeiling = (int) ((nScreenHeight / 2.0) - nScreenHeight / ((float) fDistanceToWall));
            int nFloor = nScreenHeight - nCeiling;

            for (int y = 0; y < nScreenHeight; y++) {
                if (y < nCeiling) {
                    g.setColor(Color.BLACK);
                } else if (y > nCeiling && y <= nFloor) {
                    g.setColor(Color.GRAY);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.drawLine(x, y, x, y);
            }
        }

        // Draw HUD (Health and Time)
        g.setColor(Color.RED);
        g.drawString("Health: " + playerHealth, 10, 20);
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        g.setColor(Color.BLUE);
        g.drawString("Time: " + elapsedTime + "s", 10, 40);

        // Check if player reached the endpoint
        if (Math.abs(fPlayerX - fEndX) < 0.5 && Math.abs(fPlayerY - fEndY) < 0.5) {
            
            currentLevel++;
            if (currentLevel >= levels.length) {
                gameState = GameState.CONGRATS;
                // Integrate Chatbot TTS narration
                TextToSpeech.speak("Congratulations! You have completed the game.");
            } else {
                resetPlayerPosition();
                startTime = System.currentTimeMillis(); // Reset timer for next level
                TextToSpeech.speak("Level complete. Prepare for the next challenge.");
            }
        }
    }

    private void drawMap(Graphics g) {
        
        int tileSize = 12;
        int mapOffsetX = 10;
        int mapOffsetY = 10;

        for (int x = 0; x < nMapWidth; x++) {
            for (int y = 0; y < nMapHeight; y++) {
                char tile = levels[currentLevel].charAt(y * nMapWidth + x);
                Color color;
                switch (tile) {
                    case '#': color = Color.BLACK; break;
                    case 'S': color = Color.GREEN; break;
                    case 'E': color = Color.RED; break;
                    case 'T': color = Color.ORANGE; break; // Trap
                    case 'B': color = Color.YELLOW; break; // Movable Box
                    default: color = Color.WHITE; break;
                }
                g.setColor(color);
                g.fillRect(mapOffsetX + x * tileSize, mapOffsetY + y * tileSize, tileSize, tileSize);
            }
        }

        // Draw player on the mini-map
        int playerTileSize = 6;
        int playerXMap = (int) (fPlayerX);
        int playerYMap = (int) (fPlayerY);
        g.setColor(Color.BLUE);
        g.fillRect(mapOffsetX + playerXMap * tileSize + (tileSize - playerTileSize) / 2,
                   mapOffsetY + playerYMap * tileSize + (tileSize - playerTileSize) / 2,
                   playerTileSize, playerTileSize);
    }

    private void drawPlayerArrow(Graphics g, float x, float y, float angle) {
        
        Graphics2D g2d = (Graphics2D) g;
        int arrowLength = 10;
        int arrowWidth = 5;
        AffineTransform transform = new AffineTransform();
        transform.translate(x * 50, y * 50);
        transform.rotate(angle);

        Polygon arrow = new Polygon();
        arrow.addPoint(0, -arrowLength);
        arrow.addPoint(-arrowWidth, arrowLength);
        arrow.addPoint(arrowWidth, arrowLength);
        g2d.setTransform(transform);
        g2d.setColor(Color.BLUE);
        g2d.fillPolygon(arrow);
    }

    private void resetPlayerPosition() {
        
        fPlayerX = 1.5f;
        fPlayerY = 1.5f;
        fPlayerA = 0.0f;
        playerHealth = 100;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        
        if (e.getKeyCode() == KeyEvent.VK_ENTER && gameState == GameState.STARTUP) {
            gameState = GameState.IN_GAME;
            startTime = System.currentTimeMillis(); // Start timer
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER && gameState == GameState.CONGRATS) {
            gameState = GameState.STARTUP;
        }
        if (e.getKeyCode() == KeyEvent.VK_W) keys[0] = true;
        if (e.getKeyCode() == KeyEvent.VK_S) keys[1] = true;
        if (e.getKeyCode() == KeyEvent.VK_A) keys[2] = true;
        if (e.getKeyCode() == KeyEvent.VK_D) keys[3] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
       
        if (e.getKeyCode() == KeyEvent.VK_W) keys[0] = false;
        if (e.getKeyCode() == KeyEvent.VK_S) keys[1] = false;
        if (e.getKeyCode() == KeyEvent.VK_A) keys[2] = false;
        if (e.getKeyCode() == KeyEvent.VK_D) keys[3] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void run() {
        
        while (true) {
            // Handle player movement
            float fSpeed = 0.05f;
            if (keys[0]) { // W
                fPlayerX += Math.sin(fPlayerA) * fSpeed;
                fPlayerY += Math.cos(fPlayerA) * fSpeed;
            }
            if (keys[1]) { // S
                fPlayerX -= Math.sin(fPlayerA) * fSpeed;
                fPlayerY -= Math.cos(fPlayerA) * fSpeed;
            }
            if (keys[2]) { // A
                fPlayerA -= fSpeed;
            }
            if (keys[3]) { // D
                fPlayerA += fSpeed;
            }
            repaint();
            try {
                Thread.sleep(16); // Approx. 60 fps
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        
        JFrame frame = new JFrame("FPS Game");
        
        ICE04_FPSJFrame fpsGame = new ICE04_FPSJFrame(); // Use the correct class name
        frame.add(fpsGame);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        Thread gameThread = new Thread(fpsGame);
        gameThread.start();
    }
}

// TextToSpeech.java
// Simulate FreeTTS Integration
class TextToSpeech {
    
    public static void speak(String message) {
        
        System.out.println("TTS: " + message); // Replace this with actual FreeTTS logic
    }
}
